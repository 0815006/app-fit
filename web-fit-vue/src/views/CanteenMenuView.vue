<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { currentEmpNo } from '@/utils/currentUser'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled, Search, Refresh, Loading } from '@element-plus/icons-vue'
import {
  uploadCanteenMenu,
  queryCanteenMenu,
  getCanteenMenuBatches,
  deleteCanteenMenuBatch,
  type CanteenMenuRecord,
  type PageData,
} from '@/api/canteenMenu'

// ── Admin check ──
const isAdmin = computed(() => currentEmpNo.value === '2036377')

// ── Upload ──
const uploading = ref(false)
const uploadRef = ref()

function beforeUpload(file: File): boolean {
  const isExcel =
    file.type === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' ||
    file.type === 'application/vnd.ms-excel' ||
    file.name.endsWith('.xlsx') ||
    file.name.endsWith('.xls')
  if (!isExcel) {
    ElMessage.error('仅支持 .xlsx 或 .xls 格式的 Excel 文件')
    return false
  }
  return true
}

async function handleUpload(options: { file: File }): Promise<void> {
  uploading.value = true
  try {
    const res = await uploadCanteenMenu(options.file)
    ElMessage.success(`导入成功！批次号: ${res.data.batchNo}`)
    await loadBatches()
    await loadData()
    await loadTodayMenu()
  } catch (e: any) {
    ElMessage.error(e?.message || '导入失败')
  } finally {
    uploading.value = false
  }
}

// ── Filters ──
const filters = reactive({
  canteenZone: '',
  menuDate: '',
  mealType: '',
})

const zoneOptions = [
  { label: '全部区域', value: '' },
  { label: '一期', value: '一期' },
  { label: '二期', value: '二期' },
]

const mealTypeOptions = [
  { label: '全部餐次', value: '' },
  { label: '早餐', value: '早餐' },
  { label: '午餐', value: '午餐' },
  { label: '晚餐', value: '晚餐' },
  { label: '夜宵', value: '夜宵' },
]

// ── Table ──
const loading = ref(false)
const tableData = ref<CanteenMenuRecord[]>([])
const pagination = reactive({
  current: 1,
  size: 50,
  total: 0,
})

async function loadData(): Promise<void> {
  loading.value = true
  try {
    const params: any = {
      page: pagination.current,
      size: pagination.size,
    }
    if (filters.canteenZone) params.canteenZone = filters.canteenZone
    if (filters.menuDate) params.menuDate = filters.menuDate
    if (filters.mealType) params.mealType = filters.mealType

    const res = await queryCanteenMenu(params)
    const pageData: PageData<CanteenMenuRecord> = res.data
    tableData.value = pageData.records
    pagination.total = pageData.total
    pagination.current = pageData.current
    pagination.size = pageData.size
  } catch (e: any) {
    ElMessage.error(e?.message || '查询失败')
  } finally {
    loading.value = false
  }
}

function handleSearch(): void {
  pagination.current = 1
  loadData()
}

function handleReset(): void {
  filters.canteenZone = ''
  filters.menuDate = ''
  filters.mealType = ''
  pagination.current = 1
  loadData()
}

function handlePageChange(page: number): void {
  pagination.current = page
  loadData()
}

function handleSizeChange(size: number): void {
  pagination.size = size
  pagination.current = 1
  loadData()
}

// ── Batches ──
const batches = ref<string[]>([])

async function loadBatches(): Promise<void> {
  try {
    const res = await getCanteenMenuBatches()
    batches.value = res.data || []
  } catch {
    // ignore
  }
}

async function handleDeleteBatch(batchNo: string): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定要删除批次 ${batchNo} 的所有记录吗？`, '删除确认', {
      type: 'warning',
    })
    await deleteCanteenMenuBatch(batchNo)
    ElMessage.success('删除成功')
    await loadBatches()
    await loadData()
    await loadTodayMenu()
  } catch {
    // cancelled
  }
}

// ── Helpers ──
function formatPrice(price: number): string {
  if (price === 0) return '免费'
  return `¥${price.toFixed(2)}`
}

// ════════════════════════════════════════════════════════════════
// ── Today's Menu Display ──
// ════════════════════════════════════════════════════════════════

const CHINESE_WEEKDAYS = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']

function getTodayStr(): string {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function getDefaultMealType(): string {
  const hour = new Date().getHours()
  if (hour < 10) return '早餐'
  if (hour < 14) return '午餐'
  if (hour < 19) return '晚餐'
  return '夜宵'
}

function getDayOfWeekStr(dateStr: string): string {
  if (!dateStr) return ''
  const d = new Date(dateStr.replace(/-/g, '/'))
  return CHINESE_WEEKDAYS[d.getDay()]
}

function dateAddDays(dateStr: string, days: number): string {
  const d = new Date(dateStr.replace(/-/g, '/'))
  d.setDate(d.getDate() + days)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

// ── Today's Menu State ──
const todayZone = ref('一期')
const todayDate = ref(getTodayStr())
const todayMealType = ref(getDefaultMealType())
const todayLoading = ref(false)
const todayMenuRecords = ref<CanteenMenuRecord[]>([])
const todayShowSkeleton = ref(false)

const isTodaySelected = computed(() => todayDate.value === getTodayStr())

const todayDateDisplay = computed(() => {
  if (!todayDate.value) return ''
  const wd = getDayOfWeekStr(todayDate.value)
  const label = isTodaySelected.value ? ' (今天)' : ''
  return `${todayDate.value} ${wd}${label}`
})

// Group records by categoryName
const todayGrouped = computed(() => {
  const map: Record<string, CanteenMenuRecord[]> = {}
  for (const r of todayMenuRecords.value) {
    if (!map[r.categoryName]) map[r.categoryName] = []
    map[r.categoryName].push(r)
  }
  return Object.entries(map).map(([name, items]) => ({ name, items }))
})

const todayMealTypes = [
  { label: '早餐', icon: '🍳', value: '早餐', color: '#e6a23c' },
  { label: '午餐', icon: '🍛', value: '午餐', color: '#e67e22' },
  { label: '晚餐', icon: '🍲', value: '晚餐', color: '#8e44ad' },
  { label: '夜宵', icon: '🌙', value: '夜宵', color: '#5b6abf' },
]

async function loadTodayMenu(): Promise<void> {
  todayLoading.value = true
  todayShowSkeleton.value = true
  try {
    const res = await queryCanteenMenu({
      page: 1,
      size: 500,
      canteenZone: todayZone.value,
      menuDate: todayDate.value,
      mealType: todayMealType.value,
    })
    todayMenuRecords.value = (res.data as PageData<CanteenMenuRecord>).records
  } catch (e: any) {
    ElMessage.error(e?.message || '查询失败')
  } finally {
    todayLoading.value = false
    setTimeout(() => {
      todayShowSkeleton.value = false
    }, 150)
  }
}

function handleTodayZoneChange(zone: string): void {
  if (todayZone.value === zone) return
  todayZone.value = zone
  loadTodayMenu()
}

function handleTodayMealChange(type: string): void {
  if (todayMealType.value === type) return
  todayMealType.value = type
  loadTodayMenu()
}

function handleTodayDateChange(val: string | null): void {
  if (val) {
    loadTodayMenu()
  }
}

function goToPrevDay(): void {
  todayDate.value = dateAddDays(todayDate.value, -1)
  loadTodayMenu()
}

function goToNextDay(): void {
  todayDate.value = dateAddDays(todayDate.value, 1)
  loadTodayMenu()
}

function goToToday(): void {
  if (isTodaySelected.value) return
  todayDate.value = getTodayStr()
  loadTodayMenu()
}

// Date picker ref (programmatic trigger from display span)
const todayDatePickerRef = ref<any>(null)
function openDatePicker(): void {
  const el = todayDatePickerRef.value?.$el
  if (el) {
    const input = el.querySelector('input')
    if (input) input.click()
  }
}

// ── Dish helpers ──
function getCalorieClass(kcal: number): string {
  if (kcal <= 0) return ''
  if (kcal <= 100) return 'cal-low'
  if (kcal >= 500) return 'cal-high'
  return 'cal-mid'
}

function getMealColor(): string {
  const m = todayMealTypes.find(t => t.value === todayMealType.value)
  return m ? m.color : '#e67e22'
}

// ── Init ──
onMounted(() => {
  loadBatches()
  loadData()
  loadTodayMenu()
})
</script>

<template>
  <div class="canteen-menu-container">
    <!-- ═══════════════════════════════════════════════════════ -->
    <!-- Upload Section -->
    <!-- ═══════════════════════════════════════════════════════ -->
    <el-card v-if="isAdmin" shadow="hover" class="upload-card">
      <template #header>
        <span class="card-title">📋 食堂菜单导入</span>
      </template>
      <div class="upload-area">
        <el-upload
          ref="uploadRef"
          drag
          :show-file-list="false"
          :http-request="handleUpload"
          :before-upload="beforeUpload"
          accept=".xlsx,.xls"
        >
          <el-icon class="upload-icon"><UploadFilled /></el-icon>
          <div class="upload-text">
            <p>将 Excel 文件拖到此处，或 <em>点击上传</em></p>
            <p class="upload-hint">支持 .xlsx / .xls 格式，文件需包含"一期"和"二期"两个Sheet</p>
          </div>
        </el-upload>
        <div v-if="uploading" class="upload-progress">
          <el-icon class="is-loading" :size="20"><Loading /></el-icon>
          <span>正在解析上传中，请稍候...</span>
        </div>
      </div>

      <!-- Batch history -->
      <div v-if="batches.length > 0" class="batch-history">
        <el-divider />
        <p class="batch-title">历史导入批次（{{ batches.length }}）</p>
        <div class="batch-tags">
          <el-tag
            v-for="b in batches"
            :key="b"
            closable
            type="info"
            size="small"
            style="margin: 4px"
            @close="handleDeleteBatch(b)"
          >
            {{ b }}
          </el-tag>
        </div>
      </div>
    </el-card>

    <!-- ═══════════════════════════════════════════════════════ -->
    <!-- Today's Menu Display -->
    <!-- ═══════════════════════════════════════════════════════ -->
    <el-card shadow="hover" class="today-menu-card" :body-style="{ padding: '0' }">
      <!-- ── Three-Tier Control Area ── -->
      <div class="today-control-area">
        <!-- Tier 1: Canteen Zone -->
        <div class="control-tier tier-zone">
          <div class="segmented-control">
            <button
              :class="['seg-btn', { active: todayZone === '一期' }]"
              @click="handleTodayZoneChange('一期')"
            >
              🏢 一期食堂
            </button>
            <button
              :class="['seg-btn', { active: todayZone === '二期' }]"
              @click="handleTodayZoneChange('二期')"
            >
              🏢 二期食堂
            </button>
          </div>
        </div>

        <!-- Tier 2: Date Navigation -->
        <div class="control-tier tier-date">
          <div class="date-navigator">
            <button class="date-arrow-btn" @click="goToPrevDay" :disabled="todayLoading">
              ◀ 前一天
            </button>

            <div class="date-picker-wrapper" @click="openDatePicker">
              <span class="date-display">{{ todayDateDisplay }}</span>
              <el-date-picker
                ref="todayDatePickerRef"
                v-model="todayDate"
                type="date"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                popper-class="today-date-popper"
                class="today-date-hidden"
                @change="handleTodayDateChange"
              />
            </div>

            <button class="date-arrow-btn" @click="goToNextDay" :disabled="todayLoading">
              后一天 ▶
            </button>

            <button
              v-if="!isTodaySelected"
              class="back-today-btn breathing"
              @click="goToToday"
              title="回到今天"
            >
              ⭕ 回到今天
            </button>
          </div>
        </div>

        <!-- Tier 3: Meal Type Capsules -->
        <div class="control-tier tier-meal">
          <div class="capsule-row">
            <button
              v-for="mt in todayMealTypes"
              :key="mt.value"
              :class="['capsule-btn', `capsule-${mt.value}`, { active: todayMealType === mt.value }]"
              @click="handleTodayMealChange(mt.value)"
            >
              <span class="capsule-icon">{{ mt.icon }}</span>
              <span class="capsule-label">{{ mt.label }}</span>
            </button>
          </div>
        </div>
      </div>

      <!-- ── Divider ── -->
      <div class="today-divider" />

      <!-- ── Data Display Area ── -->
      <div class="today-display-area">
        <!-- Skeleton Loading -->
        <div v-if="todayShowSkeleton" class="today-skeleton-grid">
          <div v-for="i in 3" :key="'sk-' + i" class="skeleton-card">
            <div class="sk-header">
              <div class="sk-line sk-title"></div>
              <div class="sk-line sk-badge"></div>
            </div>
            <div class="sk-body">
              <div v-for="j in 3" :key="'skr-' + j" class="sk-row">
                <div class="sk-line sk-name"></div>
                <div class="sk-line sk-info"></div>
              </div>
            </div>
          </div>
        </div>

        <!-- Empty State -->
        <div v-else-if="!todayLoading && todayMenuRecords.length === 0" class="today-empty">
          <div class="empty-illustration">
            <span class="empty-icon">🍽️</span>
          </div>
          <p class="empty-title">今日该餐次暂无菜单排期</p>
          <p class="empty-sub">去其他时段看看吧~</p>
        </div>

        <!-- Card Grid -->
        <div v-else class="today-card-grid">
          <TransitionGroup name="card-fade">
            <div
              v-for="group in todayGrouped"
              :key="group.name"
              class="category-card"
              :style="{ '--accent-color': getMealColor() }"
            >
              <!-- Card Header -->
              <div class="category-header">
                <span class="category-name">{{ group.name }}</span>
                <span class="category-count">共 {{ group.items.length }} 款</span>
              </div>

              <!-- Card Body -->
              <div class="category-body">
                <div
                  v-for="(dish, idx) in group.items"
                  :key="dish.id || idx"
                  :class="['dish-item', { 'dish-spicy': dish.isSpicy === 1 }]"
                >
                  <!-- Row 1: Name + Spicy Tag -->
                  <div class="dish-primary">
                    <span class="dish-name">{{ dish.dishName }}</span>
                    <span v-if="dish.isSpicy === 1" class="spicy-badge">🌶️ 辣</span>
                  </div>

                  <!-- Row 2: Price + Calories -->
                  <div class="dish-secondary">
                    <span class="dish-price">{{ formatPrice(dish.price) }}<small v-if="dish.unit">/{{ dish.unit }}</small></span>
                    <span v-if="dish.energyKcal > 0" :class="['dish-calorie', getCalorieClass(dish.energyKcal)]">
                      🔥 {{ dish.energyKcal }} kcal
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </TransitionGroup>
        </div>
      </div>
    </el-card>

    <!-- ═══════════════════════════════════════════════════════ -->
    <!-- Filter & Data Table Section -->
    <!-- ═══════════════════════════════════════════════════════ -->
    <el-card shadow="hover" class="data-card">
      <template #header>
        <span class="card-title">🍽️ 菜单数据</span>
      </template>

      <!-- Filters -->
      <div class="filter-bar">
        <el-select
          v-model="filters.canteenZone"
          placeholder="食堂区域"
          clearable
          style="width: 140px"
        >
          <el-option v-for="o in zoneOptions" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>

        <el-date-picker
          v-model="filters.menuDate"
          type="date"
          placeholder="选择日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          clearable
          style="width: 160px"
        />

        <el-select
          v-model="filters.mealType"
          placeholder="餐次类型"
          clearable
          style="width: 140px"
        >
          <el-option v-for="o in mealTypeOptions" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>

        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </div>

      <!-- Table -->
      <el-table
        :data="tableData"
        v-loading="loading"
        stripe
        border
        style="width: 100%; margin-top: 16px"
        max-height="calc(100vh - 420px)"
        size="small"
      >
        <el-table-column prop="menuDate" label="日期" width="110" sortable />
        <el-table-column prop="weekDay" label="星期" width="60" />
        <el-table-column prop="canteenZone" label="区域" width="60">
          <template #default="{ row }">
            <el-tag :type="row.canteenZone === '一期' ? 'primary' : 'success'" size="small">
              {{ row.canteenZone }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="mealType" label="餐次" width="70" />
        <el-table-column prop="categoryName" label="类别" width="160" show-overflow-tooltip />
        <el-table-column prop="dishName" label="菜品名称" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span :class="{ 'spicy-dish': row.isSpicy === 1 }">{{ row.dishName }}</span>
            <el-tag v-if="row.isSpicy === 1" type="danger" size="small" style="margin-left: 4px">辣</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="unit" label="单位" width="60" />
        <el-table-column prop="price" label="价格" width="90" align="right">
          <template #default="{ row }">
            <span :style="{ color: row.price === 0 ? '#67c23a' : '#e6a23c', fontWeight: 600 }">
              {{ formatPrice(row.price) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="energyKcal" label="能量/kcal" width="100" align="right" sortable />
      </el-table>

      <!-- Pagination -->
      <div class="pagination-wrapper" v-if="pagination.total > 0">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[20, 50, 100, 200]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>

      <el-empty v-if="!loading && tableData.length === 0" description="暂无数据，请先上传菜单文件" />
    </el-card>
  </div>
</template>

<style scoped>
.canteen-menu-container {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.card-title {
  font-weight: 600;
  font-size: 15px;
}

/* ── Upload ── */
.upload-area {
  text-align: center;
}

.upload-icon {
  font-size: 48px;
  color: #409eff;
}

.upload-text p {
  margin: 8px 0;
  font-size: 14px;
  color: #606266;
}

.upload-text em {
  color: #409eff;
  font-style: normal;
  cursor: pointer;
}

.upload-hint {
  font-size: 12px !important;
  color: #c0c4cc !important;
}

.upload-progress {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 12px;
  color: #409eff;
  font-size: 14px;
}

/* ── Batch history ── */
.batch-history {
  margin-top: 4px;
}

.batch-title {
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
}

.batch-tags {
  display: flex;
  flex-wrap: wrap;
}

/* ── Filters ── */
.filter-bar {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

/* ── Table ── */
.spicy-dish {
  /* subtle styling for spicy dishes */
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

/* ═══════════════════════════════════════════════════════════════ */
/* ── Today's Menu Section ── */
/* ═══════════════════════════════════════════════════════════════ */

.today-menu-card {
  overflow: hidden;
  border-radius: 12px;
}

/* ── Three-Tier Control Area ── */
.today-control-area {
  background: #ffffff;
  padding: 24px 28px 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.control-tier {
  width: 100%;
}

/* ── Tier 1: Segmented Control (Canteen Zone) ── */
.segmented-control {
  display: inline-flex;
  background: #f2f3f5;
  border-radius: 10px;
  padding: 4px;
  gap: 4px;
}

.seg-btn {
  padding: 10px 28px;
  border: none;
  background: transparent;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 500;
  color: #606266;
  cursor: pointer;
  transition: all 0.25s ease;
  white-space: nowrap;
}

.seg-btn:hover {
  color: #303133;
  background: rgba(255, 255, 255, 0.6);
}

.seg-btn.active {
  background: #ffffff;
  color: #303133;
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08), 0 1px 2px rgba(0, 0, 0, 0.06);
}

/* ── Tier 2: Date Navigator ── */
.date-navigator {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
}

.date-arrow-btn {
  padding: 8px 16px;
  border: 1px solid #e4e7ed;
  background: #fafafa;
  border-radius: 8px;
  font-size: 13px;
  color: #606266;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.date-arrow-btn:hover:not(:disabled) {
  border-color: #409eff;
  color: #409eff;
  background: #ecf5ff;
}

.date-arrow-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* Date display (clickable calendar trigger) */
.date-picker-wrapper {
  position: relative;
  cursor: pointer;
  user-select: none;
}

.date-display {
  display: inline-block;
  padding: 8px 20px;
  font-size: 17px;
  font-weight: 700;
  color: #303133;
  border-bottom: 2px dashed transparent;
  transition: all 0.2s ease;
}

.date-picker-wrapper:hover .date-display {
  color: #409eff;
  border-bottom-color: #409eff;
}

/* Hidden date picker — invisible but clickable */
.today-date-hidden {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  opacity: 0;
  overflow: hidden;
}

.today-date-hidden :deep(.el-input__wrapper) {
  cursor: pointer;
}

.today-date-hidden :deep(.el-input__inner) {
  cursor: pointer;
}

/* Back to today button */
.back-today-btn {
  padding: 6px 14px;
  border: 1.5px solid #409eff;
  background: #ecf5ff;
  border-radius: 20px;
  font-size: 13px;
  color: #409eff;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
  font-weight: 500;
}

.back-today-btn:hover {
  background: #409eff;
  color: #fff;
}

/* Breathing light animation */
.breathing {
  animation: breathe 2s ease-in-out infinite;
}

@keyframes breathe {
  0%, 100% {
    box-shadow: 0 0 0 0 rgba(64, 158, 255, 0.4);
  }
  50% {
    box-shadow: 0 0 0 10px rgba(64, 158, 255, 0);
  }
}

/* ── Tier 3: Meal Type Capsules ── */
.capsule-row {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.capsule-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 22px;
  border: 2px solid #e4e7ed;
  background: #fafafa;
  border-radius: 24px;
  font-size: 14px;
  color: #909399;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  white-space: nowrap;
}

.capsule-btn:hover {
  border-color: #c0c4cc;
  color: #606266;
  background: #f5f5f5;
}

.capsule-btn.active {
  font-weight: 600;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

/* Meal-type-specific active colors */
.capsule-早餐.active {
  border-color: #e6a23c;
  background: #fdf6ec;
  color: #b88230;
}
.capsule-午餐.active {
  border-color: #e67e22;
  background: #fef0e6;
  color: #b8661a;
}
.capsule-晚餐.active {
  border-color: #8e44ad;
  background: #f5edf8;
  color: #713690;
}
.capsule-夜宵.active {
  border-color: #5b6abf;
  background: #eef0f9;
  color: #4958a3;
}

.capsule-icon {
  font-size: 16px;
}

.capsule-label {
  font-size: 14px;
}

/* ── Divider ── */
.today-divider {
  height: 1px;
  background: linear-gradient(to right, #e4e7ed, #f0f0f0, #e4e7ed);
  margin: 0 28px;
}

/* ── Display Area ── */
.today-display-area {
  padding: 24px 28px 28px;
  min-height: 200px;
}

/* ── Card Grid ── */
.today-card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 20px;
}

/* ── Category Card ── */
.category-card {
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid #ebeef5;
  overflow: hidden;
  transition: box-shadow 0.3s ease, transform 0.2s ease;
}

.category-card:hover {
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06), 0 1px 4px rgba(0, 0, 0, 0.04);
  transform: translateY(-1px);
}

/* Card fade-in transition */
.card-fade-enter-active {
  transition: all 0.4s ease;
}

.card-fade-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

/* ── Card Header ── */
.category-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  border-bottom: 2px solid var(--accent-color, #e67e22);
  background: #fafbfc;
}

.category-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  letter-spacing: 0.3px;
}

.category-count {
  font-size: 12px;
  color: #909399;
  background: #f0f2f5;
  border-radius: 12px;
  padding: 2px 10px;
  font-weight: 500;
}

/* ── Card Body: Dish Items ── */
.category-body {
  padding: 4px 0;
}

.dish-item {
  padding: 10px 18px;
  border-bottom: 1px solid #f5f5f5;
  transition: background 0.2s ease;
}

.dish-item:last-child {
  border-bottom: none;
}

.dish-item:hover {
  background: #fafbfc;
}

/* Spicy dish highlight */
.dish-spicy {
  background: #fffbfa;
  border-left: 3px solid #f56c6c;
  padding-left: 15px;
}

.dish-spicy:hover {
  background: #fff7f5;
}

/* ── Dish Primary Row ── */
.dish-primary {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.dish-name {
  font-size: 15px;
  font-weight: 500;
  color: #303133;
}

.spicy-badge {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  font-size: 11px;
  font-weight: 700;
  color: #f56c6c;
  background: #fef0f0;
  border: 1px solid #fbc4c4;
  border-radius: 4px;
  padding: 1px 6px;
  line-height: 1.5;
}

/* ── Dish Secondary Row ── */
.dish-secondary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
}

.dish-price {
  color: #e6a23c;
  font-weight: 600;
  font-size: 14px;
}

.dish-price small {
  font-weight: 400;
  color: #909399;
  font-size: 12px;
  margin-left: 1px;
}

.dish-calorie {
  font-size: 12px;
  font-weight: 500;
  padding: 1px 8px;
  border-radius: 10px;
  background: #f5f7fa;
  color: #909399;
}

.dish-calorie.cal-low {
  color: #67c23a;
  background: #f0f9eb;
}

.dish-calorie.cal-high {
  color: #e6a23c;
  background: #fef5e7;
  font-weight: 700;
}

.dish-calorie.cal-mid {
  color: #909399;
  background: #f5f7fa;
}

/* ── Skeleton Loading ── */
.today-skeleton-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 20px;
}

.skeleton-card {
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid #ebeef5;
  overflow: hidden;
}

.sk-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  border-bottom: 2px solid #ebeef5;
  background: #fafbfc;
}

.sk-body {
  padding: 12px 18px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.sk-row {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.sk-line {
  height: 14px;
  border-radius: 6px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e8e8e8 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
}

.sk-title {
  width: 50%;
  height: 18px;
}

.sk-badge {
  width: 60px;
  border-radius: 10px;
}

.sk-name {
  width: 70%;
}

.sk-info {
  width: 90%;
}

@keyframes shimmer {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

/* ── Empty State ── */
.today-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
}

.empty-illustration {
  margin-bottom: 16px;
}

.empty-icon {
  font-size: 64px;
  opacity: 0.6;
  display: block;
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-8px);
  }
}

.empty-title {
  font-size: 16px;
  font-weight: 500;
  color: #909399;
  margin: 0 0 8px;
}

.empty-sub {
  font-size: 14px;
  color: #c0c4cc;
  margin: 0;
}

/* ── Responsive ── */
@media (max-width: 768px) {
  .today-card-grid,
  .today-skeleton-grid {
    grid-template-columns: 1fr;
  }

  .today-control-area {
    padding: 16px 14px 12px;
    gap: 14px;
  }

  .today-display-area {
    padding: 16px 14px;
  }

  .segmented-control {
    width: 100%;
  }

  .seg-btn {
    flex: 1;
    text-align: center;
    padding: 10px 12px;
  }

  .date-navigator {
    gap: 8px;
  }

  .date-arrow-btn {
    padding: 6px 10px;
    font-size: 12px;
  }

  .date-display {
    font-size: 15px;
    padding: 6px 12px;
  }

  .capsule-row {
    gap: 8px;
  }

  .capsule-btn {
    padding: 6px 14px;
    font-size: 13px;
  }
}
</style>
