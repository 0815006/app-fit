<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getConsistencyRanking, getProgressRanking } from '@/api/trainingStats'

const loading = ref(false)
const activeTab = ref('consistency')
const days = ref(30)

const consistencyData = ref<Array<{ userId: string; days: number }>>([])
const progressData = ref<Array<{ userId: string; growthRate: number; beforeTotal: number; afterTotal: number }>>([])

async function loadData() {
  loading.value = true
  try {
    const [cRes, pRes] = await Promise.all([
      getConsistencyRanking(days.value),
      getProgressRanking(days.value)
    ])
    consistencyData.value = (cRes.data || []) as Array<{ userId: string; days: number }>
    progressData.value = (pRes.data || []) as Array<{ userId: string; growthRate: number; beforeTotal: number; afterTotal: number }>
  } catch {
    consistencyData.value = []
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

onMounted(loadData)
</script>

<template>
  <div v-loading="loading" class="ranking-board">
    <div class="ranking-header">
      <span class="ranking-title">🏆 在线榜单</span>
      <el-radio-group v-model="days" size="small" @change="loadData">
        <el-radio-button :value="7">近7天</el-radio-button>
        <el-radio-button :value="30">近30天</el-radio-button>
        <el-radio-button :value="90">近90天</el-radio-button>
      </el-radio-group>
    </div>
    <el-tabs v-model="activeTab" type="border-card">
      <el-tab-pane label="坚持榜" name="consistency">
        <el-table :data="consistencyData" stripe size="small" max-height="300">
          <el-table-column label="排名" width="60" align="center">
            <template #default="{ $index }">{{ getMedal($index) }}</template>
          </el-table-column>
          <el-table-column prop="userId" label="用户ID" />
          <el-table-column prop="days" label="训练天数" align="center">
            <template #default="{ row }">
              <el-tag type="success">{{ row.days }} 天</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="进步榜" name="progress">
        <el-table :data="progressData" stripe size="small" max-height="300">
          <el-table-column label="排名" width="60" align="center">
            <template #default="{ $index }">{{ getMedal($index) }}</template>
          </el-table-column>
          <el-table-column prop="userId" label="用户ID" />
          <el-table-column prop="growthRate" label="增长率" align="center">
            <template #default="{ row }">
              <el-tag :type="row.growthRate > 0 ? 'success' : 'danger'">
                {{ row.growthRate > 0 ? '+' : '' }}{{ row.growthRate }}%
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="beforeTotal" label="前期1RM" align="center" />
          <el-table-column prop="afterTotal" label="后期1RM" align="center" />
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.ranking-board { padding: 16px; background: #fafafa; border-radius: 8px; }
.ranking-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.ranking-title { font-size: 16px; font-weight: 600; }
</style>
