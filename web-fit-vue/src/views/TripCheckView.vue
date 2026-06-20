<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
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

// 日常场景快捷开始
async function startRoutine(tagName: string) {
  loading.value = true
  try {
    // 找到对应标签
    const tag = allTags.value.find(t => t.name === tagName)
    if (!tag) {
      ElMessage.warning(`未找到标签: ${tagName}`)
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
    }
  } catch (e) {
    console.error('创建日常计划失败', e)
    ElMessage.error('创建计划失败')
  } finally {
    loading.value = false
  }
}

// 创建专项计划
async function createSpecialPlan() {
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
    // 创建计划
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
        pageStatus.value = 'TAILOR'
      }
    }
  } catch (e) {
    console.error('创建专项计划失败', e)
    ElMessage.error('创建计划失败')
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
})
</script>

<template>
  <div class="trip-check-view">
    <!-- 状态 A: 闲置/默认首页 -->
    <div v-if="pageStatus === 'HOME'" class="home-view">
      <div class="home-header">
        <h1>🧳 出行清单</h1>
        <p class="subtitle">智能全场景备忘系统</p>
      </div>

      <!-- 日常秒开区域 -->
      <section class="routine-section">
        <h2>⚡ 日常秒开</h2>
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

      <!-- 远途/专项计划台 -->
      <section class="special-section">
        <h2>✨ 专项计划</h2>
        <div class="special-form">
          <el-form :model="tripForm" label-width="100px">
            <el-form-item label="计划标题">
              <el-input v-model="tripForm.title" placeholder="如: 8月三亚度假" />
            </el-form-item>
            <el-form-item label="目的地">
              <el-input v-model="tripForm.destination" placeholder="可选" />
            </el-form-item>
            <el-form-item label="出行天数">
              <el-input-number v-model="tripForm.tripDays" :min="1" :max="365" />
            </el-form-item>
            <el-form-item label="出发时间">
              <el-date-picker
                v-model="tripForm.departureTime"
                type="datetime"
                placeholder="选择出发时间"
              />
            </el-form-item>
            <el-form-item label="返回时间">
              <el-date-picker
                v-model="tripForm.returnTime"
                type="datetime"
                placeholder="选择返回时间"
              />
            </el-form-item>
            <el-form-item label="场景标签">
              <div class="tag-selector">
                <div v-for="(tags, type) in tagsByType" :key="type" class="tag-group">
                  <span class="tag-type-label">{{ type }}:</span>
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
                  >
                    {{ tag.name }}
                  </el-check-tag>
                </div>
              </div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="createSpecialPlan" :loading="loading">
                ✨ 创建专项清单
              </el-button>
            </el-form-item>
          </el-form>
        </div>
      </section>
    </div>

    <!-- 状态 B: 清单裁剪与微调台 -->
    <div v-else-if="pageStatus === 'TAILOR'" class="tailor-view">
      <div class="tailor-header">
        <h2>📋 清单裁剪台</h2>
        <p v-if="currentPlan">{{ currentPlan.title }} - {{ currentPlan.tripDays }}天</p>
      </div>

      <div class="tailor-content">
        <el-table :data="planDetails.filter(d => d.excludeFlag !== 1)" stripe>
          <el-table-column prop="itemName" label="物品名称" />
          <el-table-column prop="container" label="载体">
            <template #default="{ row }">
              {{ getContainerIcon(row.container) }} {{ getContainerName(row.container) }}
            </template>
          </el-table-column>
          <el-table-column prop="importanceLevel" label="重要级别">
            <template #default="{ row }">
              <el-tag :type="getImportanceTagType(row.importanceLevel)">
                {{ row.importanceLevel }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="targetQuantity" label="数量" width="120">
            <template #default="{ row }">
              <el-input-number
                v-model="row.targetQuantity"
                :min="1"
                size="small"
                @change="updateQuantity(row, $event)"
              />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button type="danger" link @click="toggleExclude(row)">
                ❌ 排除
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 手动补漏 -->
        <div class="manual-add">
          <el-input
            v-model="newItemName"
            placeholder="输入物品名称，手动补漏"
            @keyup.enter="addManualItem"
          >
            <template #append>
              <el-button @click="addManualItem">＋ 添加</el-button>
            </template>
          </el-input>
        </div>

        <div class="tailor-actions">
          <el-button @click="resetToHome">返回</el-button>
          <el-button type="primary" @click="lockAndStartChecking" :loading="loading">
            🔒 锁定清单，开始核对
          </el-button>
        </div>
      </div>
    </div>

    <!-- 状态 C: 核心核对打包看板 -->
    <div v-else-if="pageStatus === 'CHECKING'" class="checking-view">
      <!-- 顶部总览与筛选栏 -->
      <div class="checking-header">
        <div class="progress-section">
          <el-progress
            :percentage="checkProgress.percent"
            :status="checkProgress.percent === 100 ? 'success' : ''"
          />
          <span class="progress-text">
            已装箱 {{ checkProgress.checked }} / {{ checkProgress.total }} 件
          </span>
        </div>

        <div class="filter-section">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索物品..."
            clearable
            style="width: 200px"
          />
          <div class="filter-tags">
            <el-check-tag
              :checked="filterTag === 'all'"
              @change="filterTag = 'all'"
            >
              全部
            </el-check-tag>
            <el-check-tag
              v-for="tag in allTags"
              :key="tag.id"
              :checked="filterTag === tag.name"
              @change="filterTag = tag.name"
            >
              {{ tag.name }}
            </el-check-tag>
          </div>
        </div>
      </div>

      <!-- 中部容器面板 -->
      <div class="container-panels">
        <!-- 行李箱 -->
        <div class="container-panel suitcase">
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
              <el-tag :type="getImportanceTagType(detail.importanceLevel)" size="small">
                {{ detail.importanceLevel }}
              </el-tag>
              <el-button
                type="danger"
                link
                size="small"
                @click.stop="toggleExclude(detail)"
              >
                ❌
              </el-button>
            </div>
            <div v-if="detailsByContainer.suitcase.length === 0" class="empty-hint">
              暂无物品
            </div>
          </div>
        </div>

        <!-- 双肩包 -->
        <div class="container-panel backpack">
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
              <el-tag :type="getImportanceTagType(detail.importanceLevel)" size="small">
                {{ detail.importanceLevel }}
              </el-tag>
              <el-button
                type="danger"
                link
                size="small"
                @click.stop="toggleExclude(detail)"
              >
                ❌
              </el-button>
            </div>
            <div v-if="detailsByContainer.backpack.length === 0" class="empty-hint">
              暂无物品
            </div>
          </div>
        </div>

        <!-- 随身口袋 -->
        <div class="container-panel pocket">
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
              <el-tag :type="getImportanceTagType(detail.importanceLevel)" size="small">
                {{ detail.importanceLevel }}
              </el-tag>
              <el-button
                type="danger"
                link
                size="small"
                @click.stop="toggleExclude(detail)"
              >
                ❌
              </el-button>
            </div>
            <div v-if="detailsByContainer.pocket.length === 0" class="empty-hint">
              暂无物品
            </div>
          </div>
        </div>
      </div>

      <!-- 手动补漏 -->
      <div class="manual-add-bottom">
        <el-input
          v-model="newItemName"
          placeholder="输入物品名称，手动补漏"
          @keyup.enter="addManualItem"
        >
          <template #append>
            <el-button @click="addManualItem">＋ 添加</el-button>
          </template>
        </el-input>
      </div>

      <!-- 底部固定悬浮条 -->
      <div class="bottom-bar">
        <el-button @click="resetToHome">重置</el-button>
        <el-button
          v-if="allChecked"
          type="danger"
          size="large"
          @click="startPocketMode"
        >
          🚨 开启临行30秒（口袋模式）
        </el-button>
        <el-button v-else type="primary" size="large" disabled>
          请完成所有物品核对
        </el-button>
      </div>
    </div>

    <!-- 临行30秒弹窗 -->
    <el-dialog
      v-model="showPocketMode"
      title="🚨 临行30秒 - 口袋模式"
      width="500px"
      :close-on-click-modal="false"
      class="pocket-dialog"
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
          <div v-if="pocketCriticalItems.length === 0" class="empty-hint">
            无核心物品
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="showPocketMode = false">取消</el-button>
        <el-button type="primary" @click="confirmDeparture">
          ✓ 妥当，出发！
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.trip-check-view {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 24px;
  overflow-y: auto;
}

/* 首页样式 */
.home-view {
  max-width: 1000px;
  margin: 0 auto;
  width: 100%;
}

.home-header {
  text-align: center;
  margin-bottom: 40px;
}

.home-header h1 {
  font-size: 32px;
  margin-bottom: 8px;
}

.home-header .subtitle {
  color: #666;
  font-size: 14px;
}

.routine-section,
.special-section {
  margin-bottom: 40px;
}

.routine-section h2,
.special-section h2 {
  font-size: 20px;
  margin-bottom: 16px;
}

.routine-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.routine-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: #f5f7fa;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s;
}

.routine-card:hover {
  background: #e6f7ff;
  transform: translateY(-4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.routine-card .card-icon {
  font-size: 40px;
  margin-bottom: 8px;
}

.routine-card .card-name {
  font-size: 16px;
  font-weight: 500;
}

.special-form {
  background: #f5f7fa;
  padding: 24px;
  border-radius: 12px;
}

.tag-selector {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.tag-group {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.tag-type-label {
  font-weight: 500;
  min-width: 80px;
}

/* 裁剪台样式 */
.tailor-view {
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;
}

.tailor-header {
  margin-bottom: 24px;
}

.tailor-header h2 {
  font-size: 24px;
  margin-bottom: 8px;
}

.tailor-content {
  background: #fff;
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.manual-add {
  margin-top: 16px;
  max-width: 400px;
}

.tailor-actions {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* 核对看板样式 */
.checking-view {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.checking-header {
  background: #fff;
  padding: 16px 24px;
  border-radius: 12px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.progress-section {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;
}

.progress-section .el-progress {
  flex: 1;
}

.progress-text {
  font-size: 14px;
  color: #666;
  white-space: nowrap;
}

.filter-section {
  display: flex;
  align-items: center;
  gap: 16px;
}

.filter-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.container-panels {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  flex: 1;
  min-height: 0;
}

.container-panel {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.container-panel h3 {
  font-size: 18px;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #eee;
}

.item-list {
  flex: 1;
  overflow-y: auto;
}

.item-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s;
}

.item-row:hover {
  background: #f5f7fa;
}

.item-row.checked {
  opacity: 0.5;
}

.item-row.checked .item-name {
  text-decoration: line-through;
}

.item-name {
  flex: 1;
  font-size: 14px;
}

.item-qty {
  color: #666;
  font-size: 12px;
}

.empty-hint {
  text-align: center;
  color: #999;
  padding: 24px;
}

.manual-add-bottom {
  margin-top: 16px;
  max-width: 400px;
}

.bottom-bar {
  position: sticky;
  bottom: 0;
  background: #fff;
  padding: 16px 24px;
  border-radius: 12px;
  margin-top: 16px;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.08);
  display: flex;
  justify-content: center;
  gap: 16px;
}

/* 口袋模式弹窗 */
.pocket-content {
  padding: 16px 0;
}

.pocket-hint {
  font-size: 16px;
  margin-bottom: 16px;
  text-align: center;
  color: #666;
}

.pocket-items {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.pocket-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.pocket-item:hover {
  background: #e6f7ff;
}

.pocket-item.checked {
  background: #f6ffed;
}

.pocket-item .item-name {
  font-size: 18px;
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
}
</style>
