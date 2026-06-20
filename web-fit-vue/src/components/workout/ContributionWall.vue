<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { getContributionWall } from '@/api/trainingStats'

const loading = ref(false)
const year = ref(new Date().getFullYear())
const contributionData = ref<Array<{ date: string; count: number; dayOfWeek: number }>>([])

// 生成日历网格数据
const calendarWeeks = computed(() => {
  if (contributionData.value.length === 0) return []

  const dataMap = new Map<string, number>()
  contributionData.value.forEach(d => dataMap.set(d.date, d.count))

  const start = new Date(year.value, 0, 1)
  const end = new Date(year.value, 11, 31)
  const weeks: Array<Array<{ date: string; count: number; dayOfWeek: number; inYear: boolean }>> = []

  // 找到第一个周一
  const firstDay = start.getDay() === 0 ? 6 : start.getDay() - 1
  const calStart = new Date(start)
  calStart.setDate(calStart.getDate() - firstDay)

  let current = new Date(calStart)
  while (current <= end || current.getDay() !== 1) {
    const week: Array<{ date: string; count: number; dayOfWeek: number; inYear: boolean }> = []
    for (let i = 0; i < 7; i++) {
      const dateStr = current.toISOString().split('T')[0]
      const inYear = current.getFullYear() === year.value
      week.push({
        date: dateStr,
        count: dataMap.get(dateStr) || 0,
        dayOfWeek: i,
        inYear
      })
      current.setDate(current.getDate() + 1)
    }
    weeks.push(week)
    if (current > end && current.getDay() === 1) break
  }
  return weeks
})

function getColor(count: number): string {
  if (count === 0) return '#ebedf0'
  if (count === 1) return '#9be9a8'
  if (count === 2) return '#40c463'
  if (count >= 3) return '#30a14e'
  return '#216e39'
}

async function loadData() {
  loading.value = true
  try {
    const res = await getContributionWall(year.value)
    contributionData.value = (res.data || []) as Array<{ date: string; count: number; dayOfWeek: number }>
  } catch {
    contributionData.value = []
  } finally {
    loading.value = false
  }
}

const totalDays = computed(() => contributionData.value.filter(d => d.count > 0).length)
const totalSessions = computed(() => contributionData.value.reduce((sum, d) => sum + d.count, 0))

onMounted(loadData)
</script>

<template>
  <div v-loading="loading" class="contribution-wall">
    <div class="wall-header">
      <span class="wall-title">📅 训练贡献墙 ({{ year }})</span>
      <div class="wall-controls">
        <el-button-group>
          <el-button size="small" :disabled="year <= 2020" @click="year--; loadData()">‹</el-button>
          <el-button size="small" disabled>{{ year }}</el-button>
          <el-button size="small" @click="year++; loadData()">›</el-button>
        </el-button-group>
      </div>
    </div>
    <div class="wall-stats">
      <el-statistic title="训练天数" :value="totalDays" suffix="天" />
      <el-statistic title="训练次数" :value="totalSessions" suffix="次" />
    </div>
    <div class="wall-calendar">
      <div class="day-labels">
        <span>一</span><span>三</span><span>五</span>
      </div>
      <div class="weeks-grid">
        <div v-for="(week, wi) in calendarWeeks" :key="wi" class="week-col">
          <div
            v-for="(day, di) in week"
            :key="di"
            class="day-cell"
            :class="{ 'out-year': !day.inYear }"
            :style="{ backgroundColor: getColor(day.count) }"
            :title="`${day.date}: ${day.count}次训练`"
          />
        </div>
      </div>
    </div>
    <div class="wall-legend">
      <span>少</span>
      <span class="legend-cell" style="background:#ebedf0" />
      <span class="legend-cell" style="background:#9be9a8" />
      <span class="legend-cell" style="background:#40c463" />
      <span class="legend-cell" style="background:#30a14e" />
      <span class="legend-cell" style="background:#216e39" />
      <span>多</span>
    </div>
  </div>
</template>

<style scoped>
.contribution-wall { padding: 16px; background: #fafafa; border-radius: 8px; }
.wall-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.wall-title { font-size: 16px; font-weight: 600; }
.wall-stats { display: flex; gap: 32px; margin-bottom: 16px; }
.wall-calendar { display: flex; gap: 8px; overflow-x: auto; padding-bottom: 8px; }
.day-labels { display: flex; flex-direction: column; gap: 3px; padding-top: 2px; font-size: 10px; color: #999; width: 16px; }
.day-labels span { height: 13px; line-height: 13px; text-align: center; }
.weeks-grid { display: flex; gap: 3px; }
.week-col { display: flex; flex-direction: column; gap: 3px; }
.day-cell {
  width: 13px; height: 13px; border-radius: 2px; cursor: pointer;
  transition: transform 0.1s;
}
.day-cell:hover { transform: scale(1.3); }
.day-cell.out-year { opacity: 0.3; }
.wall-legend { display: flex; align-items: center; gap: 4px; margin-top: 12px; justify-content: flex-end; font-size: 11px; color: #666; }
.legend-cell { width: 13px; height: 13px; border-radius: 2px; }
</style>
