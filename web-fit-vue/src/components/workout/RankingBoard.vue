<script setup lang="ts">
import { ref, onMounted } from 'vue'
import {
  getConsistencyRankingV2,
  getVolumeRanking,
  getPeak1RMRanking,
  getProgressRankingV2,
  type RankingItemVO
} from '@/api/trainingStats'

const loading = ref(false)
const activeTab = ref('consistency')
const days = ref(30)

/** 坚持榜子模式：cumulative 累计打卡 / streak 连续打卡 */
const consistencyMode = ref<string>('cumulative')

const consistencyData = ref<RankingItemVO[]>([])
const volumeData = ref<RankingItemVO[]>([])
const peak1rmData = ref<RankingItemVO[]>([])
const progressData = ref<RankingItemVO[]>([])

async function loadData() {
  loading.value = true
  try {
    const [cRes, vRes, pRes, pgRes] = await Promise.all([
      getConsistencyRankingV2(days.value, consistencyMode.value),
      getVolumeRanking(days.value),
      getPeak1RMRanking(days.value),
      getProgressRankingV2(days.value)
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

function switchConsistencyMode(mode: string) {
  if (consistencyMode.value === mode) return
  consistencyMode.value = mode
  loadConsistency()
}

async function loadConsistency() {
  loading.value = true
  try {
    const cRes = await getConsistencyRankingV2(days.value, consistencyMode.value)
    consistencyData.value = cRes.data || []
  } catch {
    consistencyData.value = []
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
  return ''
}

function getUserLabel(row: RankingItemVO): string {
  return row.empName || row.empNo || row.userId
}

onMounted(loadData)
</script>

<template>
  <div v-loading="loading" class="ranking-board">
    <div class="ranking-header">
      <span class="ranking-title">🏆 健身榜单</span>
      <el-radio-group v-model="days" size="small" @change="loadData">
        <el-radio-button :value="7">近7天</el-radio-button>
        <el-radio-button :value="30">近30天</el-radio-button>
        <el-radio-button :value="90">近90天</el-radio-button>
      </el-radio-group>
    </div>

    <el-tabs v-model="activeTab" type="border-card">
      <!-- 坚持榜 -->
      <el-tab-pane label="🔥 坚持榜" name="consistency">
        <div class="consistency-mode-bar">
          <el-radio-group v-model="consistencyMode" size="small" @change="loadConsistency">
            <el-radio-button value="cumulative">累计打卡</el-radio-button>
            <el-radio-button value="streak">连续打卡</el-radio-button>
          </el-radio-group>
        </div>
        <el-table :data="consistencyData" stripe size="small" max-height="360">
          <el-table-column label="排名" width="70" align="center">
            <template #default="{ $index }">
              <span :class="getRankClass($index)">{{ getMedal($index) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="用户" min-width="120">
            <template #default="{ row }">{{ getUserLabel(row) }}</template>
          </el-table-column>
          <el-table-column :label="consistencyMode === 'streak' ? '连续天数' : '累计天数'" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="consistencyMode === 'streak' ? 'warning' : 'success'">{{ row.value }} 天</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="趋势" width="70" align="center" prop="trend" />
        </el-table>
      </el-tab-pane>

      <!-- 容量榜 -->
      <el-tab-pane label="💪 容量榜" name="volume">
        <el-table :data="volumeData" stripe size="small" max-height="360">
          <el-table-column label="排名" width="70" align="center">
            <template #default="{ $index }">
              <span :class="getRankClass($index)">{{ getMedal($index) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="用户" min-width="120">
            <template #default="{ row }">{{ getUserLabel(row) }}</template>
          </el-table-column>
          <el-table-column label="总容量" width="110" align="center">
            <template #default="{ row }">
              <el-tag>{{ row.value }} kg</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="趋势" width="70" align="center" prop="trend" />
        </el-table>
      </el-tab-pane>

      <!-- 1RM 巅峰榜 -->
      <el-tab-pane label="🏋️ 1RM巅峰榜" name="peak-1rm">
        <el-table :data="peak1rmData" stripe size="small" max-height="360">
          <el-table-column label="排名" width="70" align="center">
            <template #default="{ $index }">
              <span :class="getRankClass($index)">{{ getMedal($index) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="用户" min-width="120">
            <template #default="{ row }">{{ getUserLabel(row) }}</template>
          </el-table-column>
          <el-table-column label="三大项1RM" width="120" align="center">
            <template #default="{ row }">
              <el-tag type="warning">{{ row.value }} kg</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="趋势" width="70" align="center" prop="trend" />
        </el-table>
      </el-tab-pane>

      <!-- 进步榜 -->
      <el-tab-pane label="📈 进步榜" name="progress">
        <el-table :data="progressData" stripe size="small" max-height="360">
          <el-table-column label="排名" width="70" align="center">
            <template #default="{ $index }">
              <span :class="getRankClass($index)">{{ getMedal($index) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="用户" min-width="120">
            <template #default="{ row }">{{ getUserLabel(row) }}</template>
          </el-table-column>
          <el-table-column label="增长率" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.value > 0 ? 'success' : 'danger'">
                {{ row.value > 0 ? '+' : '' }}{{ row.value }}%
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="趋势" width="70" align="center" prop="trend" />
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.ranking-board { padding: 16px; background: var(--el-bg-color-page); border-radius: 8px; }
.ranking-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.ranking-title { font-size: 18px; font-weight: 700; }

.consistency-mode-bar { margin-bottom: 12px; }

.rank-gold   { font-weight: 700; color: #f0ad4e; }
.rank-silver { font-weight: 700; color: #999; }
.rank-bronze { font-weight: 700; color: #d9534f; }
</style>
