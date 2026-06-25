<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getMuscleFatigue } from '@/api/trainingStats'

const loading = ref(false)
const fatigueData = ref<Record<string, number>>({})

const MUSCLE_GROUPS = [
  { key: 'CHEST', label: '胸部', icon: '🫁' },
  { key: 'BACK', label: '背部', icon: '🔙' },
  { key: 'SHOULDER', label: '肩部', icon: '🦾' },
  { key: 'ARM', label: '手臂', icon: '💪' },
  { key: 'LEG', label: '腿部', icon: '🦵' },
  { key: 'GLUTE', label: '臀部', icon: '🍑' },
  { key: 'CORE', label: '核心', icon: '🎯' }
]

const emit = defineEmits<{
  (e: 'muscle-click', muscleGroup: string): void
}>()

// 疲劳状态
function getFatigueStatus(hours: number): { color: string; label: string; type: string } {
  if (hours === -1) return { color: '#67c23a', label: '恢复充分', type: 'success' }
  if (hours < 24) return { color: '#f56c6c', label: '极度疲劳', type: 'danger' }
  if (hours < 72) return { color: '#e6a23c', label: '恢复中', type: 'warning' }
  return { color: '#67c23a', label: '恢复充分', type: 'success' }
}

function getFatigueHours(key: string): string {
  const h = fatigueData.value[key]
  if (h === undefined || h === -1) return '从未训练'
  if (h < 24) return `${h}小时前`
  if (h < 48) return '昨天'
  return `${Math.floor(h / 24)}天前`
}

async function loadFatigue() {
  loading.value = true
  try {
    const res = await getMuscleFatigue()
    fatigueData.value = res.data || {}
  } catch {
    fatigueData.value = {}
  } finally {
    loading.value = false
  }
}

function handleClick(key: string) {
  emit('muscle-click', key)
}

onMounted(loadFatigue)

defineExpose({ refresh: loadFatigue })
</script>

<template>
  <div v-loading="loading" class="fatigue-heatmap">
    <div class="heatmap-title">
      <span>🔥 肌群疲劳热力图</span>
      <el-button text size="small" @click="loadFatigue">
        <el-icon><Refresh /></el-icon> 刷新
      </el-button>
    </div>
    <div class="heatmap-grid">
      <div
        v-for="mg in MUSCLE_GROUPS"
        :key="mg.key"
        class="muscle-card"
        :style="{ borderColor: getFatigueStatus(fatigueData[mg.key] ?? -1).color }"
        @click="handleClick(mg.key)"
      >
        <div class="muscle-icon">{{ mg.icon }}</div>
        <div class="muscle-label">{{ mg.label }}</div>
        <div class="muscle-status" :style="{ color: getFatigueStatus(fatigueData[mg.key] ?? -1).color }">
          {{ getFatigueStatus(fatigueData[mg.key] ?? -1).label }}
        </div>
        <div class="muscle-time">{{ getFatigueHours(mg.key) }}</div>
      </div>
    </div>
    <div class="heatmap-legend">
      <span class="legend-item"><span class="dot" style="background:#f56c6c" /> 极度疲劳 (<24h)</span>
      <span class="legend-item"><span class="dot" style="background:#e6a23c" /> 恢复中 (24-72h)</span>
      <span class="legend-item"><span class="dot" style="background:#67c23a" /> 恢复充分 (>72h)</span>
    </div>
  </div>
</template>

<style scoped>
.fatigue-heatmap { padding: 16px; background: #fafafa; border-radius: 8px; }
.heatmap-title { display: flex; justify-content: space-between; align-items: center; font-size: 16px; font-weight: 600; margin-bottom: 16px; }
.heatmap-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(120px, 1fr)); gap: 12px; }
.muscle-card {
  background: #fff; border-radius: 8px; padding: 16px 12px; text-align: center;
  border: 2px solid #ddd; cursor: pointer; transition: all 0.2s;
}
.muscle-card:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
.muscle-icon { font-size: 28px; margin-bottom: 6px; }
.muscle-label { font-size: 14px; font-weight: 500; margin-bottom: 4px; }
.muscle-status { font-size: 12px; font-weight: 600; }
.muscle-time { font-size: 11px; color: #999; margin-top: 2px; }
.heatmap-legend { display: flex; gap: 20px; margin-top: 16px; justify-content: center; }
.legend-item { display: flex; align-items: center; gap: 4px; font-size: 12px; color: #666; }
.dot { width: 10px; height: 10px; border-radius: 50%; display: inline-block; }
</style>
