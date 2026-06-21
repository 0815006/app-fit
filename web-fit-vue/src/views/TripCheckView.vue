<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus,
  Refresh,
  Delete,
  Edit,
  Lock,
  Search,
  Close,
  Check,
  Warning
} from '@element-plus/icons-vue'
import {
  listAllTripTags,
  type TripTag
} from '@/api/tripTag'
import {
  createTripPlan,
  getTripPlanDetails,
  updateTripPlanStatus,
  updateTripPlanDetailChecked,
  updateTripPlanDetailExcludeFlag,
  updateTripPlanDetailQuantity,
  addTripPlanDetail,
  generateTripPlanDetails,
  listMyTripPlans,
  deleteTripPlan,
  type TripPlan,
  type TripPlanDetail
} from '@/api/tripPlan'

// 页面状态枚举
type PageStatus = 'HOME' | 'TAILOR' | 'CHECKING'

// 当前页面状态
const pageStatus = ref<PageStatus>('HOME')

// 所有标签
const allTags = ref<TripTag[]>([])
const tagsByType = computed(() => {
  const result: Record<string, TripTag[]> = {}
  for (const tag of allTags.value) {
    if (!result[tag.type]) {
      result[tag.type] = []
    }
    result[tag.type].push(tag)
  }
  return result
})

// 日常场景快捷入口
const routineScenarios = [
  { icon: '🏢', name: '日常通勤', tag: '日常通勤' },
  { icon: '🏋️', name: '去健身房', tag: '去健身房' },
  { icon: '🏊', name: '去游泳', tag: '游泳' },
  { icon: '🏥', name: '去医院', tag: '去医院' }
]

// 专项计划表单
const tripForm = ref({
  title: '',
  destination: '',
  tripDays: 1,
  departureTime: '',
  returnTime: ''
})

// 选中的标签
const selectedTags = ref<string[]>([])

// 当前计划
const currentPlan = ref<TripPlan | null>(null)

// 计划明细
const planDetails = ref<TripPlanDetail[]>([])

// 搜索关键字
const searchKeyword = ref('')

// 筛选的标签
const filterTag = ref<string>('all')

// 手动添加物品
const newItemName = ref('')

// 临行30秒弹窗
const showPocketMode = ref(false)

// 加载状态
const loading = ref(false)

// 我的计划列表
const myPlans = ref<TripPlan[]>([])
const plansLoading = ref(false)

// 创建计划弹窗
const showCreateDialog = ref(false)

// 编辑计划弹窗
const showEditDialog = ref(false)

// 是否为编辑模式
const isEditMode = ref(false)

// 编辑中的计划ID
const editingPlanId = ref<string | null>(null)

// 按容器分组的明细
const detailsByContainer = computed(() => {
  const filtered = planDetails.value.filter(d => {
    // 排除已标记排除的
    if (d.excludeFlag === 1) return false
    // 搜索过滤
    if (searchKeyword.value && !d.itemName.includes(searchKeyword.value)) return false
    // 标签过滤 - 通过血缘快照中的tagId匹配
    if (filterTag.value !== 'all') {
      try {
        const sources = JSON.parse(d.sourceContextsJson || '[]') as Array<{ tagId: string; tagName: string }>
        const filterTagObj = allTags.value.find(t => t.name === filterTag.value)
        if (filterTagObj && !sources.some(s => s.tagId === filterTagObj.id)) {
          return false
        }
      } catch {
        // ignore
      }
    }
    return true
  })

  const suitcase = filtered.filter(d => d.container === 'SUITCASE')
  const backpack = filtered.filter(d => d.container === 'BACKPACK')
  const pocket = filtered.filter(d => d.container === 'POCKET')

  return { suitcase, backpack, pocket }
})

// 装箱进度
const checkProgress = computed(() => {
  const activeDetails = planDetails.value.filter(d => d.excludeFlag !== 1)
  const checked = activeDetails.filter(d => d.isChecked === 1).length
  const total = activeDetails.length
  return { checked, total, percent: total > 0 ? Math.round((checked / total) * 100) : 0 }
})

// 是否全部装箱完成
const allChecked = computed(() => {
  const activeDetails = planDetails.value.filter(d => d.excludeFlag !== 1)
  return activeDetails.length > 0 && activeDetails.every(d => d.isChecked === 1)
})

// 口袋模式核心物品
const pocketCriticalItems = computed(() => {
  return planDetails.value.filter(d =>
    d.container === 'POCKET' &&
    d.importanceLevel === 'CRITICAL' &&
    d.excludeFlag !== 1
  )
})

// 获取所有标签
async function loadTags() {
  try {
    const res = await listAllTripTags()
    if (res.code === 200) {
      allTags.value = res.data
    }
  } catch (e) {
    console.error('加载标签失败', e)
  }
}

// 获取我的计划列表
async function loadMyPlans() {
  plansLoading.value = true
  try {
    const res = await listMyTripPlans()
    if (res.code === 200) {
      // 按 id 去重，防止后端返回重复数据
      const seen = new Set<string>()
      myPlans.value = res.data.filter(plan => {
        if (seen.has(plan.id)) return false
        seen.add(plan.id)
        return true
      })
    }
  } catch (e) {
    console.error('加载计划列表失败', e)
  } finally {
    plansLoading.value = false
  }
}

// 加载已有计划
async function loadPlan(plan: TripPlan) {
  loading.value = true
  try {
    currentPlan.value = plan
    // 加载计划明细
    const detailRes = await getTripPlanDetails(plan.id)
    if (detailRes.code === 200) {
      planDetails.value = detailRes.data
      // 根据状态进入不同页面
      if (plan.status === 'DRAFT') {
        pageStatus.value = 'TAILOR'
      } else {
        pageStatus.value = 'CHECKING'
      }
    }
  } catch (e) {
    console.error('加载计划失败', e)
    ElMessage.error('加载计划失败')
  } finally {
    loading.value = false
  }
}

// 删除计划
async function handleDeletePlan(plan: TripPlan) {
  try {
    await ElMessageBox.confirm(
      `确定要删除计划「${plan.title}」吗？`,
      '删除确认',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    const res = await deleteTripPlan(plan.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      await loadMyPlans()
    }
  } catch (e) {
    if (e !== 'cancel') {
      console.error('删除计划失败', e)
      ElMessage.error('删除失败')
    }
  }
}

// 获取状态标签类型
function getStatusTagType(status: string): string {
  switch (status) {
    case 'DRAFT': return 'info'
    case 'PACKING': return 'warning'
    case 'FINISHED': return 'success'
    case 'ARCHIVED': return 'info'
    default: return 'info'
  }
}

// 获取状态显示文本
function getStatusText(status: string): string {
  switch (status) {
    case 'DRAFT': return '草稿'
    case 'PACKING': return '装箱中'
    case 'FINISHED': return '已完成'
    case 'ARCHIVED': return '已归档'
    default: return status
  }
}

// 日常场景快捷开始
async function startRoutine(tagName: string) {
  // 防止重复点击
  if (loading.value) return
  loading.value = true
  try {
    // 找到对应标签
    const tag = allTags.value.find(t => t.name === tagName)
    if (!tag) {
      ElMessage.warning(`未找到标签: ${tagName}`)
      return
    }

    // 检查是否已存在同名的活跃计划（PACKING 状态），如果有则直接加载
    const existingPlan = myPlans.value.find(
      p => p.title === tagName && (p.status === 'PACKING' || p.status === 'DRAFT')
    )
    if (existingPlan) {
      await loadPlan(existingPlan)
      return
    }

    // 创建计划
    const planRes = await createTripPlan({
      title: tagName,
      tripDays: 1,
      status: 'PACKING'
    })

    if (planRes.code === 200) {
      currentPlan.value = planRes.data

      // 生成明细
      const detailRes = await generateTripPlanDetails(planRes.data.id, [tag.id])
      if (detailRes.code === 200) {
        planDetails.value = detailRes.data
        pageStatus.value = 'CHECKING'
      }
      // 刷新左侧计划列表
      await loadMyPlans()
    }
  } catch (e) {
    console.error('创建日常计划失败', e)
    ElMessage.error('创建计划失败')
  } finally {
    loading.value = false
  }
}

// 打开创建计划弹窗
function openCreateDialog() {
  isEditMode.value = false
  editingPlanId.value = null
  tripForm.value = {
    title: '',
    destination: '',
    tripDays: 1,
    departureTime: '',
    returnTime: ''
  }
  selectedTags.value = []
  showCreateDialog.value = true
}

// 打开编辑计划弹窗
function openEditDialog() {
  if (!currentPlan.value) return
  isEditMode.value = true
  editingPlanId.value = currentPlan.value.id
  tripForm.value = {
    title: currentPlan.value.title,
    destination: currentPlan.value.destination || '',
    tripDays: currentPlan.value.tripDays,
    departureTime: currentPlan.value.departureTime || '',
    returnTime: currentPlan.value.returnTime || ''
  }
  // 从计划明细中提取标签
  const tagIds = new Set<string>()
  for (const detail of planDetails.value) {
    try {
      const sources = JSON.parse(detail.sourceContextsJson || '[]') as Array<{ tagId: string }>
      for (const source of sources) {
        tagIds.add(source.tagId)
      }
    } catch {
      // ignore
    }
  }
  selectedTags.value = Array.from(tagIds)
  showEditDialog.value = true
}

// 提交创建/编辑计划
async function submitPlanForm() {
  if (!tripForm.value.title) {
    ElMessage.warning('请输入计划标题')
    return
  }
  if (selectedTags.value.length === 0) {
    ElMessage.warning('请至少选择一个场景标签')
    return
  }

  loading.value = true
  try {
    if (isEditMode.value && editingPlanId.value) {
      // 编辑模式：重新生成明细
      const detailRes = await generateTripPlanDetails(editingPlanId.value, selectedTags.value)
      if (detailRes.code === 200) {
        planDetails.value = detailRes.data
        ElMessage.success('计划已更新')
        await loadMyPlans()
      }
      showEditDialog.value = false
    } else {
      // 创建模式
      const planRes = await createTripPlan({
        title: tripForm.value.title,
        destination: tripForm.value.destination || null,
        tripDays: tripForm.value.tripDays,
        departureTime: tripForm.value.departureTime || null,
        returnTime: tripForm.value.returnTime || null,
        status: 'DRAFT'
      })

      if (planRes.code === 200) {
        currentPlan.value = planRes.data

        // 生成明细
        const detailRes = await generateTripPlanDetails(planRes.data.id, selectedTags.value)
        if (detailRes.code === 200) {
          planDetails.value = detailRes.data
        }
        showCreateDialog.value = false
        await loadMyPlans()
        ElMessage.success('计划已创建')
      }
    }
  } catch (e) {
    console.error('提交计划失败', e)
    ElMessage.error('操作失败')
  } finally {
    loading.value = false
  }
}

// 锁定清单，开始核对
async function lockAndStartChecking() {
  if (!currentPlan.value) return

  loading.value = true
  try {
    await updateTripPlanStatus(currentPlan.value.id, 'PACKING')
    currentPlan.value.status = 'PACKING'
    pageStatus.value = 'CHECKING'
    ElMessage.success('清单已锁定，开始核对')
  } catch (e) {
    console.error('锁定清单失败', e)
    ElMessage.error('锁定清单失败')
  } finally {
    loading.value = false
  }
}

// 切换装箱状态
async function toggleChecked(detail: TripPlanDetail) {
  const newStatus = detail.isChecked === 1 ? 0 : 1
  try {
    await updateTripPlanDetailChecked(detail.id, newStatus)
    detail.isChecked = newStatus
  } catch (e) {
    console.error('更新装箱状态失败', e)
    ElMessage.error('更新失败')
  }
}

// 临时排除物品
async function toggleExclude(detail: TripPlanDetail) {
  const newFlag = detail.excludeFlag === 1 ? 0 : 1
  try {
    await updateTripPlanDetailExcludeFlag(detail.id, newFlag)
    detail.excludeFlag = newFlag
  } catch (e) {
    console.error('更新排除状态失败', e)
    ElMessage.error('更新失败')
  }
}

// 更新物品数量
async function updateQuantity(detail: TripPlanDetail, newQuantity: number) {
  try {
    await updateTripPlanDetailQuantity(detail.id, newQuantity)
    detail.targetQuantity = newQuantity
  } catch (e) {
    console.error('更新数量失败', e)
    ElMessage.error('更新数量失败')
  }
}

// 手动添加物品
async function addManualItem() {
  if (!newItemName.value.trim() || !currentPlan.value) {
    return
  }

  try {
    const res = await addTripPlanDetail(currentPlan.value.id, {
      itemId: 'manual_' + Date.now(),
      itemName: newItemName.value.trim(),
      container: 'BACKPACK',
      importanceLevel: 'IMPORTANT',
      targetQuantity: 1,
      isChecked: 0,
      excludeFlag: 0,
      sourceContextsJson: '[]',
      versionNo: 1
    })

    if (res.code === 200) {
      planDetails.value.push(res.data)
      newItemName.value = ''
      ElMessage.success('物品已添加')
    }
  } catch (e) {
    console.error('添加物品失败', e)
    ElMessage.error('添加失败')
  }
}

// 开启临行30秒
function startPocketMode() {
  showPocketMode.value = true
}

// 确认出发
function confirmDeparture() {
  showPocketMode.value = false
  resetToHome()
  ElMessage.success('妥当，出发！祝旅途愉快！')
}

// 重置到首页
function resetToHome() {
  pageStatus.value = 'HOME'
  currentPlan.value = null
  planDetails.value = []
  selectedTags.value = []
  tripForm.value = {
    title: '',
    destination: '',
    tripDays: 1,
    departureTime: '',
    returnTime: ''
  }
  searchKeyword.value = ''
  filterTag.value = 'all'
  // 刷新计划列表
  loadMyPlans()
}

// 获取重要级别标签类型
function getImportanceTagType(level: string): string {
  switch (level) {
    case 'CRITICAL': return 'danger'
    case 'IMPORTANT': return 'warning'
    case 'OPTIONAL': return 'info'
    default: return 'info'
  }
}

// 获取容器图标
function getContainerIcon(container: string): string {
  switch (container) {
    case 'SUITCASE': return '📦'
    case 'BACKPACK': return '🎒'
    case 'POCKET': return '🧥'
    default: return '📦'
  }
}

// 获取容器名称
function getContainerName(container: string): string {
  switch (container) {
    case 'SUITCASE': return '行李箱'
    case 'BACKPACK': return '双肩包'
    case 'POCKET': return '随身口袋'
    default: return '未知'
  }
}

onMounted(() => {
  loadTags()
  loadMyPlans()
})
</script>

<template>
  <div class="trip-check-view">
    <div class="main-layout">
      <!-- 左侧：我的计划列表 -->
      <aside class="plans-sidebar">
        <div class="sidebar-header">
          <span class="sidebar-title">计划</span>
          <div class="sidebar-actions">
            <el-button :icon="Refresh" circle size="small" @click="loadMyPlans" :loading="plansLoading" />
            <el-button type="primary" :icon="Plus" circle size="small" @click="openCreateDialog" />
          </div>
        </div>
        <div v-loading="plansLoading" class="plans-list">
          <div v-if="myPlans.length === 0" class="empty-plans">暂无计划</div>
          <div
            v-for="plan in myPlans"
            :key="plan.id"
            class="plan-item"
            :class="{ active: currentPlan?.id === plan.id }"
            @click="loadPlan(plan)"
          >
            <div class="plan-item-main">
              <div class="plan-item-title">{{ plan.title }}</div>
              <div class="plan-item-sub">
                <el-tag :type="getStatusTagType(plan.status)" size="small" effect="plain">
                  {{ getStatusText(plan.status) }}
                </el-tag>
                <span v-if="plan.destination" class="meta-text">{{ plan.destination }}</span>
                <span class="meta-text">{{ plan.tripDays }}天</span>
              </div>
            </div>
            <el-button
              class="plan-delete-btn"
              :icon="Delete"
              type="danger"
              text
              size="small"
              @click.stop="handleDeletePlan(plan)"
            />
          </div>
        </div>
      </aside>

      <!-- 右侧：内容展示区 -->
      <main class="content-area">
        <!-- 未选择计划时的默认页面 -->
        <div v-if="!currentPlan" class="default-content">
          <div class="home-header">
            <h1>出行清单</h1>
            <p class="subtitle">智能全场景备忘系统</p>
          </div>

          <!-- 日常秒开区域 -->
          <section class="routine-section">
            <h2>日常秒开</h2>
            <div class="routine-cards">
              <div
                v-for="scenario in routineScenarios"
                :key="scenario.tag"
                class="routine-card"
                @click="startRoutine(scenario.tag)"
              >
                <span class="card-icon">{{ scenario.icon }}</span>
                <span class="card-name">{{ scenario.name }}</span>
              </div>
            </div>
          </section>

          <div class="quick-create">
            <el-button type="primary" @click="openCreateDialog">
              <el-icon><Plus /></el-icon>
              <span>创建专项计划</span>
            </el-button>
          </div>
        </div>

        <!-- 已选择计划时的内容展示 -->
        <div v-else class="plan-content">
          <!-- 计划头部 -->
          <div class="plan-header">
            <div class="plan-info">
              <h2>{{ currentPlan.title }}</h2>
              <div class="plan-meta">
                <el-tag :type="getStatusTagType(currentPlan.status)" effect="plain">
                  {{ getStatusText(currentPlan.status) }}
                </el-tag>
                <span v-if="currentPlan.destination" class="meta-item">{{ currentPlan.destination }}</span>
                <span class="meta-item">{{ currentPlan.tripDays }}天</span>
              </div>
            </div>
            <div class="plan-actions">
              <el-button :icon="Edit" circle @click="openEditDialog" title="编辑" />
              <el-button
                v-if="currentPlan.status === 'DRAFT'"
                type="primary"
                :icon="Lock"
                @click="lockAndStartChecking"
                :loading="loading"
                title="锁定清单"
              />
            </div>
          </div>

          <!-- 清单内容 -->
          <div class="checklist-content">
            <!-- 顶部总览与筛选栏 -->
            <div class="checking-header">
              <div class="progress-section">
                <el-progress
                  :percentage="checkProgress.percent"
                  :status="checkProgress.percent === 100 ? 'success' : ''"
                />
                <span class="progress-text">
                  {{ checkProgress.checked }}/{{ checkProgress.total }}
                </span>
              </div>

              <div class="filter-section">
                <el-input
                  v-model="searchKeyword"
                  placeholder="搜索..."
                  :prefix-icon="Search"
                  clearable
                  size="small"
                  style="width: 160px"
                />
                <div class="filter-tags">
                  <el-check-tag
                    :checked="filterTag === 'all'"
                    @change="filterTag = 'all'"
                    size="small"
                  >
                    全部
                  </el-check-tag>
                  <el-check-tag
                    v-for="tag in allTags"
                    :key="tag.id"
                    :checked="filterTag === tag.name"
                    @change="filterTag = tag.name"
                    size="small"
                  >
                    {{ tag.name }}
                  </el-check-tag>
                </div>
              </div>
            </div>

            <!-- 中部容器面板 -->
            <div class="container-panels">
              <!-- 行李箱 -->
              <div class="container-panel">
                <h3>📦 行李箱</h3>
                <div class="item-list">
                  <div
                    v-for="detail in detailsByContainer.suitcase"
                    :key="detail.id"
                    class="item-row"
                    :class="{ checked: detail.isChecked === 1 }"
                    @click="toggleChecked(detail)"
                  >
                    <el-checkbox :model-value="detail.isChecked === 1" @click.stop />
                    <span class="item-name">{{ detail.itemName }}</span>
                    <span class="item-qty">x{{ detail.targetQuantity }}</span>
                    <el-button
                      :icon="Close"
                      type="danger"
                      text
                      size="small"
                      @click.stop="toggleExclude(detail)"
                    />
                  </div>
                  <div v-if="detailsByContainer.suitcase.length === 0" class="empty-hint">暂无物品</div>
                </div>
              </div>

              <!-- 双肩包 -->
              <div class="container-panel">
                <h3>🎒 双肩包</h3>
                <div class="item-list">
                  <div
                    v-for="detail in detailsByContainer.backpack"
                    :key="detail.id"
                    class="item-row"
                    :class="{ checked: detail.isChecked === 1 }"
                    @click="toggleChecked(detail)"
                  >
                    <el-checkbox :model-value="detail.isChecked === 1" @click.stop />
                    <span class="item-name">{{ detail.itemName }}</span>
                    <span class="item-qty">x{{ detail.targetQuantity }}</span>
                    <el-button
                      :icon="Close"
                      type="danger"
                      text
                      size="small"
                      @click.stop="toggleExclude(detail)"
                    />
                  </div>
                  <div v-if="detailsByContainer.backpack.length === 0" class="empty-hint">暂无物品</div>
                </div>
              </div>

              <!-- 随身口袋 -->
              <div class="container-panel">
                <h3>🧥 随身口袋</h3>
                <div class="item-list">
                  <div
                    v-for="detail in detailsByContainer.pocket"
                    :key="detail.id"
                    class="item-row"
                    :class="{ checked: detail.isChecked === 1 }"
                    @click="toggleChecked(detail)"
                  >
                    <el-checkbox :model-value="detail.isChecked === 1" @click.stop />
                    <span class="item-name">{{ detail.itemName }}</span>
                    <span class="item-qty">x{{ detail.targetQuantity }}</span>
                    <el-button
                      :icon="Close"
                      type="danger"
                      text
                      size="small"
                      @click.stop="toggleExclude(detail)"
                    />
                  </div>
                  <div v-if="detailsByContainer.pocket.length === 0" class="empty-hint">暂无物品</div>
                </div>
              </div>
            </div>

            <!-- 手动补漏 -->
            <div class="manual-add-bottom">
              <el-input
                v-model="newItemName"
                placeholder="手动添加物品..."
                @keyup.enter="addManualItem"
                size="small"
              >
                <template #append>
                  <el-button :icon="Plus" @click="addManualItem" />
                </template>
              </el-input>
            </div>

            <!-- 底部操作栏 -->
            <div class="bottom-bar">
              <el-button v-if="allChecked" type="warning" :icon="Warning" @click="startPocketMode">
                临行30秒
              </el-button>
            </div>
          </div>
        </div>
      </main>
    </div>

    <!-- 创建/编辑计划弹窗 -->
    <el-dialog
      v-model="showCreateDialog"
      :title="isEditMode ? '编辑计划' : '创建专项计划'"
      width="520px"
      :close-on-click-modal="false"
    >
      <el-form :model="tripForm" label-width="80px" size="default">
        <el-form-item label="标题">
          <el-input v-model="tripForm.title" placeholder="如: 8月三亚度假" />
        </el-form-item>
        <el-form-item label="目的地">
          <el-input v-model="tripForm.destination" placeholder="可选" />
        </el-form-item>
        <el-form-item label="天数">
          <el-input-number v-model="tripForm.tripDays" :min="1" :max="365" />
        </el-form-item>
        <el-form-item label="出发">
          <el-date-picker
            v-model="tripForm.departureTime"
            type="datetime"
            placeholder="选择出发时间"
          />
        </el-form-item>
        <el-form-item label="返回">
          <el-date-picker
            v-model="tripForm.returnTime"
            type="datetime"
            placeholder="选择返回时间"
          />
        </el-form-item>
        <el-form-item label="标签">
          <div class="tag-selector">
            <div v-for="(tags, type) in tagsByType" :key="type" class="tag-group">
              <span class="tag-type-label">{{ type }}</span>
              <el-check-tag
                v-for="tag in tags"
                :key="tag.id"
                :checked="selectedTags.includes(tag.id)"
                @change="() => {
                  if (selectedTags.includes(tag.id)) {
                    selectedTags = selectedTags.filter(t => t !== tag.id)
                  } else {
                    selectedTags.push(tag.id)
                  }
                }"
                size="small"
              >
                {{ tag.name }}
              </el-check-tag>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="submitPlanForm" :loading="loading">
          {{ isEditMode ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 临行30秒弹窗 -->
    <el-dialog
      v-model="showPocketMode"
      title="临行30秒 - 口袋模式"
      width="420px"
      :close-on-click-modal="false"
    >
      <div class="pocket-content">
        <p class="pocket-hint">请确认以下核心物品已随身携带：</p>
        <div class="pocket-items">
          <div
            v-for="detail in pocketCriticalItems"
            :key="detail.id"
            class="pocket-item"
            :class="{ checked: detail.isChecked === 1 }"
            @click="toggleChecked(detail)"
          >
            <el-checkbox :model-value="detail.isChecked === 1" @click.stop />
            <span class="item-name">{{ detail.itemName }}</span>
          </div>
          <div v-if="pocketCriticalItems.length === 0" class="empty-hint">无核心物品</div>
        </div>
      </div>
      <template #footer>
        <el-button @click="showPocketMode = false">取消</el-button>
        <el-button type="primary" :icon="Check" @click="confirmDeparture">妥当，出发</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.trip-check-view {
  height: 100dvh;
  padding: 16px;
  overflow: hidden;
  background: #f5f7fa;
}

/* 主布局 */
.main-layout {
  display: grid;
  grid-template-columns: 240px 1fr;
  gap: 16px;
  height: 100%;
}

/* 左侧计划列表 */
.plans-sidebar {
  background: #fff;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.sidebar-title {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.sidebar-actions {
  display: flex;
  gap: 4px;
  align-items: center;
}

.plans-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.empty-plans {
  text-align: center;
  color: #999;
  padding: 32px 0;
  font-size: 13px;
}

.plan-item {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s;
  margin-bottom: 4px;
}

.plan-item:hover {
  background: #f5f7fa;
}

.plan-item.active {
  background: #e6f7ff;
}

.plan-item-main {
  flex: 1;
  min-width: 0;
}

.plan-item-title {
  font-size: 13px;
  font-weight: 500;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 4px;
}

.plan-item-sub {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #999;
}

.meta-text {
  font-size: 11px;
}

.plan-delete-btn {
  opacity: 0;
  transition: opacity 0.15s;
}

.plan-item:hover .plan-delete-btn {
  opacity: 1;
}

/* 右侧内容区 */
.content-area {
  background: #fff;
  border-radius: 8px;
  overflow-y: auto;
  padding: 20px;
}

/* 默认内容 */
.default-content {
  max-width: 800px;
  margin: 0 auto;
  padding: 40px 0;
}

.home-header {
  text-align: center;
  margin-bottom: 32px;
}

.home-header h1 {
  font-size: 24px;
  margin: 0 0 8px 0;
  color: #333;
}

.home-header .subtitle {
  color: #999;
  font-size: 13px;
  margin: 0;
}

.routine-section {
  margin-bottom: 32px;
}

.routine-section h2 {
  font-size: 15px;
  margin: 0 0 12px 0;
  color: #666;
}

.routine-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.routine-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 16px 12px;
  background: #f5f7fa;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.routine-card:hover {
  background: #e6f7ff;
  transform: translateY(-2px);
}

.routine-card .card-icon {
  font-size: 28px;
  margin-bottom: 6px;
}

.routine-card .card-name {
  font-size: 13px;
  font-weight: 500;
  color: #333;
}

.quick-create {
  text-align: center;
}

/* 计划内容 */
.plan-content {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.plan-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.plan-info h2 {
  margin: 0 0 6px 0;
  font-size: 18px;
  color: #333;
}

.plan-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #999;
  font-size: 13px;
}

.meta-item {
  color: #666;
}

.plan-actions {
  display: flex;
  gap: 8px;
}

.checklist-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.checking-header {
  background: #fafafa;
  padding: 12px 16px;
  border-radius: 8px;
  margin-bottom: 12px;
}

.progress-section {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.progress-section .el-progress {
  flex: 1;
}

.progress-text {
  font-size: 12px;
  color: #666;
  white-space: nowrap;
}

.filter-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.filter-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.tag-selector {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.tag-group {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.tag-type-label {
  font-size: 12px;
  color: #666;
  min-width: 50px;
}

.container-panels {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  flex: 1;
  min-height: 0;
}

.container-panel {
  background: #fafafa;
  border-radius: 8px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.container-panel h3 {
  font-size: 14px;
  margin: 0 0 10px 0;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
  color: #333;
}

.item-list {
  flex: 1;
  overflow-y: auto;
}

.item-row {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 8px;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.15s;
}

.item-row:hover {
  background: #f0f0f0;
}

.item-row.checked {
  opacity: 0.5;
}

.item-row.checked .item-name {
  text-decoration: line-through;
}

.item-name {
  flex: 1;
  font-size: 13px;
  color: #333;
}

.item-qty {
  color: #999;
  font-size: 11px;
}

.empty-hint {
  text-align: center;
  color: #ccc;
  padding: 20px;
  font-size: 12px;
}

.manual-add-bottom {
  margin-top: 12px;
  max-width: 320px;
}

.bottom-bar {
  position: sticky;
  bottom: 0;
  background: #fff;
  padding: 12px 16px;
  border-radius: 8px;
  margin-top: 12px;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.05);
  display: flex;
  justify-content: center;
}

/* 口袋模式弹窗 */
.pocket-content {
  padding: 8px 0;
}

.pocket-hint {
  font-size: 14px;
  margin: 0 0 12px 0;
  text-align: center;
  color: #666;
}

.pocket-items {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.pocket-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s;
}

.pocket-item:hover {
  background: #e6f7ff;
}

.pocket-item.checked {
  background: #f6ffed;
}

.pocket-item .item-name {
  font-size: 14px;
  font-weight: 500;
}

/* 响应式 */
@media (max-width: 1200px) {
  .container-panels {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .routine-cards {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .main-layout {
    grid-template-columns: 1fr;
  }
  
  .plans-sidebar {
    max-height: 200px;
  }
}
</style>
