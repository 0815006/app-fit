<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
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
  } catch {
    // cancelled
  }
}

// ── Helpers ──
function formatPrice(price: number): string {
  if (price === 0) return '免费'
  return `¥${price.toFixed(2)}`
}

onMounted(() => {
  loadBatches()
  loadData()
})
</script>

<template>
  <div class="canteen-menu-container">
    <!-- Upload Section -->
    <el-card shadow="hover" class="upload-card">
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

    <!-- Filter & Data Section -->
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
</style>
