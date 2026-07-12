<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import {
  getConsistencyRankingV2,
  getMaxSingleVolumeRanking,
  getPeak1RMRanking,
  getProgressRankingV2,
  type RankingItemVO
} from '@/api/trainingStats'

const loading = ref(false)
const days = ref(30)

type LiftType = 'bench' | 'squat' | 'deadlift' | 'all'
const volumeLift = ref<LiftType>('bench')
const peakLift = ref<LiftType>('bench')
const progressLift = ref<LiftType>('bench')

const liftOptions: { label: string; value: LiftType }[] = [
  { label: '卧推', value: 'bench' },
  { label: '深蹲', value: 'squat' },
  { label: '硬拉', value: 'deadlift' },
  { label: '三大项之和', value: 'all' }
]

const consistencyData = ref<RankingItemVO[]>([])
const volumeData = ref<RankingItemVO[]>([])
const peak1rmData = ref<RankingItemVO[]>([])
const progressData = ref<RankingItemVO[]>([])

async function loadVolume() {
  try {
    const res = await getMaxSingleVolumeRanking(days.value, volumeLift.value)
    volumeData.value = res.data || []
  } catch {
    volumeData.value = []
  }
}

async function loadPeak() {
  try {
    const res = await getPeak1RMRanking(days.value, peakLift.value)
    peak1rmData.value = res.data || []
  } catch {
    peak1rmData.value = []
  }
}

async function loadProgress() {
  try {
    const res = await getProgressRankingV2(days.value, progressLift.value)
    progressData.value = res.data || []
  } catch {
    progressData.value = []
  }
}

async function loadAll() {
  loading.value = true
  try {
    const [cRes, vRes, pRes, pgRes] = await Promise.all([
      getConsistencyRankingV2(days.value),
      getMaxSingleVolumeRanking(days.value, volumeLift.value),
      getPeak1RMRanking(days.value, peakLift.value),
      getProgressRankingV2(days.value, progressLift.value)
    ])
    consistencyData.value = cRes.data || []
    volumeData.value = vRes.data || []
    peak1rmData.value = pRes.data || []
    progressData.value = pgRes.data || []
  } catch {
    consistencyData.value = []
    volumeData.value = []
    peak1rmData.value = []
    progressData.value = []
  } finally {
    loading.value = false
  }
}

function getMedal(index: number): string {
  if (index === 0) return '🥇'
  if (index === 1) return '🥈'
  if (index === 2) return '🥉'
  return `${index + 1}`
}

function getRankClass(index: number): string {
  if (index === 0) return 'rank-gold'
  if (index === 1) return 'rank-silver'
  if (index === 2) return 'rank-bronze'
  return 'rank-normal'
}

function getUserLabel(row: RankingItemVO): string {
  return row.nickname || row.empName || row.empNo || row.userId
}

onMounted(loadAll)
</script>

<template>
  <div class="ranking-page">
    <!-- 页面标题栏 -->
    <div class="page-header">
      <h2 class="page-title">🏆 健身榜单</h2>
      <div class="page-header-right">
        <el-radio-group v-model="days" size="small" @change="loadAll">
          <el-radio-button :value="7">近7天</el-radio-button>
          <el-radio-button :value="30">近30天</el-radio-button>
          <el-radio-button :value="90">近90天</el-radio-button>
        </el-radio-group>
        <el-button type="primary" size="small" :icon="Refresh" :loading="loading" @click="loadAll">
          刷新榜单
        </el-button>
      </div>
    </div>

    <!-- 四榜网格 -->
    <div v-loading="loading" class="ranking-grid">
      <!-- 坚持榜 -->
      <el-card shadow="hover" class="rank-card">
        <template #header>
          <div class="card-header">
            <span class="card-icon">🔥</span>
            <span>坚持榜</span>
            <el-tag size="small" type="info">打卡天数</el-tag>
          </div>
        </template>
        <div class="rank-list">
          <div
            v-for="(item, idx) in consistencyData.slice(0, 10)"
            :key="item.userId"
            class="rank-item"
          >
            <span :class="['rank-badge', getRankClass(idx)]">{{ getMedal(idx) }}</span>
            <span class="rank-user">{{ getUserLabel(item) }}</span>
            <span class="rank-value">{{ item.value }} 天</span>
          </div>
          <el-empty v-if="consistencyData.length === 0" description="暂无数据" :image-size="60" />
        </div>
      </el-card>

      <!-- 容量榜 -->
      <el-card shadow="hover" class="rank-card">
        <template #header>
          <div class="card-header">
            <span class="card-icon">👑</span>
            <span>容量榜</span>
            <el-radio-group
              v-model="volumeLift"
              size="small"
              @change="loadVolume"
            >
              <el-radio-button
                v-for="opt in liftOptions"
                :key="opt.value"
                :value="opt.value"
              >{{ opt.label }}</el-radio-button>
            </el-radio-group>
          </div>
        </template>
        <div class="rank-list">
          <div
            v-for="(item, idx) in volumeData.slice(0, 10)"
            :key="item.userId"
            class="rank-item"
          >
            <span :class="['rank-badge', getRankClass(idx)]">{{ getMedal(idx) }}</span>
            <span class="rank-user">{{ getUserLabel(item) }}</span>
            <span class="rank-value">{{ item.value }} kg</span>
          </div>
          <el-empty v-if="volumeData.length === 0" description="暂无数据" :image-size="60" />
        </div>
      </el-card>

      <!-- 1RM巅峰榜 -->
      <el-card shadow="hover" class="rank-card">
        <template #header>
          <div class="card-header">
            <span class="card-icon">🏋️</span>
            <span>1RM巅峰榜</span>
            <el-radio-group
              v-model="peakLift"
              size="small"
              @change="loadPeak"
            >
              <el-radio-button
                v-for="opt in liftOptions"
                :key="opt.value"
                :value="opt.value"
              >{{ opt.label }}</el-radio-button>
            </el-radio-group>
          </div>
        </template>
        <div class="rank-list">
          <div
            v-for="(item, idx) in peak1rmData.slice(0, 10)"
            :key="item.userId"
            class="rank-item"
          >
            <span :class="['rank-badge', getRankClass(idx)]">{{ getMedal(idx) }}</span>
            <span class="rank-user">{{ getUserLabel(item) }}</span>
            <span class="rank-value">{{ item.value }} kg</span>
          </div>
          <el-empty v-if="peak1rmData.length === 0" description="暂无数据" :image-size="60" />
        </div>
      </el-card>

      <!-- 进步榜 -->
      <el-card shadow="hover" class="rank-card">
        <template #header>
          <div class="card-header">
            <span class="card-icon">📈</span>
            <span>进步榜</span>
            <el-radio-group
              v-model="progressLift"
              size="small"
              @change="loadProgress"
            >
              <el-radio-button
                v-for="opt in liftOptions"
                :key="opt.value"
                :value="opt.value"
              >{{ opt.label }}</el-radio-button>
            </el-radio-group>
          </div>
        </template>
        <div class="rank-list">
          <div
            v-for="(item, idx) in progressData.slice(0, 10)"
            :key="item.userId"
            class="rank-item"
          >
            <span :class="['rank-badge', getRankClass(idx)]">{{ getMedal(idx) }}</span>
            <span class="rank-user">{{ getUserLabel(item) }}</span>
            <span :class="['rank-value', item.value > 0 ? 'up' : 'down']">
              {{ item.value > 0 ? '+' : '' }}{{ item.value }}%
            </span>
          </div>
          <el-empty v-if="progressData.length === 0" description="暂无数据" :image-size="60" />
        </div>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.ranking-page {
  padding: 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.page-header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.page-title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}

.ranking-grid {
  flex: 1;
  display: grid;
  grid-template-columns: 1fr 1fr 1fr 1fr;
  grid-template-rows: 1fr;
  gap: 16px;
  max-height: 1400px;
  min-height: 0;
  max-width: 1680px;
  margin: 0 auto;
  width: 100%;
}

.rank-card {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.rank-card :deep(.el-card__body) {
  flex: 1;
  overflow-y: auto;
  padding: 0;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.card-icon {
  font-size: 18px;
}

.rank-list {
  padding: 8px 0;
}

.rank-item {
  display: flex;
  align-items: center;
  padding: 8px 16px;
  gap: 12px;
  transition: background 0.15s;
}

.rank-item:hover {
  background: var(--el-fill-color-light);
}

.rank-badge {
  width: 32px;
  text-align: center;
  font-weight: 700;
  font-size: 15px;
  flex-shrink: 0;
}

.rank-gold   { color: #f0ad4e; }
.rank-silver { color: #999; }
.rank-bronze { color: #d9534f; }
.rank-normal { color: var(--el-text-color-secondary); }

.rank-user {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
}

.rank-value {
  font-weight: 600;
  font-size: 14px;
  color: var(--el-text-color-primary);
  flex-shrink: 0;
}

.rank-value.up   { color: #67c23a; }
.rank-value.down { color: #f56c6c; }
</style>
