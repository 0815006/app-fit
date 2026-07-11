<script setup lang="ts">
import { computed } from 'vue'
import type { WeeklyWorkoutVO } from '@/api/gymWorkout'

const props = defineProps<{
  records: WeeklyWorkoutVO[]
  loading: boolean
}>()

const WEEKDAY_NAMES: Record<number, string> = {
  1: '周一',
  2: '周二',
  3: '周三',
  4: '周四',
  5: '周五',
  6: '周六',
  7: '周日',
}

const GROUP_COLORS: Record<string, string> = {
  CHEST: '#f56c6c',
  BACK: '#e6a23c',
  SHOULDER: '#409eff',
  ARM: '#67c23a',
  LEG: '#b37feb',
  GLUTE: '#ff85c0',
  CORE: '#36cfc9',
  FULL_BODY: '#909399',
}

/** 获取今天的 dayOfWeek (1=周一) */
const todayDayOfWeek = computed(() => {
  const d = new Date().getDay()
  return d === 0 ? 7 : d
})

/** 按 dayOfWeek 分组 */
const groupedByDay = computed(() => {
  const map: Record<number, WeeklyWorkoutVO[]> = {}
  for (let i = 1; i <= 7; i++) {
    map[i] = []
  }
  for (const r of props.records) {
    if (map[r.dayOfWeek]) {
      map[r.dayOfWeek].push(r)
    }
  }
  return map
})

/** 总训练次数 */
const totalCount = computed(() => props.records.length)

/** 本周期数（当前是第几周） */
const currentWeekLabel = computed(() => {
  const now = new Date()
  const startOfYear = new Date(now.getFullYear(), 0, 1)
  const diff = now.getTime() - startOfYear.getTime()
  const weekNum = Math.ceil((diff / (1000 * 60 * 60 * 24) + startOfYear.getDay()) / 7)
  return `第 ${weekNum} 周`
})

/** 燃尽的肌群去重统计 */
const trainedGroups = computed(() => {
  const set = new Set<string>()
  for (const r of props.records) {
    set.add(r.muscleGroupName)
  }
  return [...set]
})

function formatTime(dateStr: string): string {
  if (!dateStr) return ''
  try {
    const d = new Date(dateStr)
    return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  } catch {
    return ''
  }
}

function formatScore(score: number): string {
  if (score == null) return '-'
  return (score * 100).toFixed(0) + '%'
}
</script>

<template>
  <div class="weekly-summary" v-loading="loading">
    <!-- 头部统计 -->
    <div class="summary-header">
      <h3 class="section-title">📊 {{ currentWeekLabel }} 训练概览</h3>
      <div class="summary-stats">
        <el-tag type="success" effect="dark" size="large">
          🏋️ 共 {{ totalCount }} 次训练
        </el-tag>
        <el-tag
          v-for="g in trainedGroups"
          :key="g"
          effect="plain"
          size="large"
          :color="GROUP_COLORS[Object.keys(GROUP_COLORS).find(k => props.records.find(r => r.muscleGroupName === g)?.muscleGroup === k) || '']"
          style="color: #fff; border: none;"
        >
          {{ g }}
        </el-tag>
      </div>
    </div>

    <!-- 一周七日网格 -->
    <div class="week-grid">
      <div
        v-for="day in 7"
        :key="day"
        class="day-column"
        :class="{ 'is-today': day === todayDayOfWeek, 'has-records': groupedByDay[day].length > 0 }"
      >
        <div class="day-header">
          <span class="day-name">{{ WEEKDAY_NAMES[day] }}</span>
          <el-badge
            v-if="groupedByDay[day].length > 0"
            :value="groupedByDay[day].length"
            type="primary"
          />
        </div>
        <div class="day-body">
          <template v-if="groupedByDay[day].length === 0">
            <div class="empty-day">—</div>
          </template>
          <div
            v-for="rec in groupedByDay[day]"
            :key="rec.startTime + rec.actionName"
            class="day-record"
            :style="{ borderLeftColor: GROUP_COLORS[rec.muscleGroup] || '#dcdfe6' }"
          >
            <span class="record-action">{{ rec.actionName }}</span>
            <span class="record-meta">
              <span
                class="record-group-tag"
                :style="{ background: GROUP_COLORS[rec.muscleGroup] || '#909399' }"
              >{{ rec.muscleGroupName }}</span>
              <span class="record-time">{{ formatTime(rec.startTime) }}</span>
            </span>
            <span class="record-exhaustion">力竭 {{ formatScore(rec.exhaustionScore) }}</span>
          </div>
        </div>
      </div>
    </div>

    <el-empty
      v-if="!loading && records.length === 0"
      description="本周暂无训练记录，快去打卡吧！"
      :image-size="80"
    />
  </div>
</template>

<style scoped>
.weekly-summary {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 2px solid #e4e7ed;
}

.summary-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 10px;
}

.section-title {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: #303133;
}

.summary-stats {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

/* 一周七日网格 */
.week-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 8px;
}

.day-column {
  background: #fafafa;
  border-radius: 10px;
  border: 1px solid #ebeef5;
  overflow: hidden;
  transition: all 0.2s;
  min-height: 120px;
}

.day-column.is-today {
  border-color: #409eff;
  background: #ecf5ff;
  box-shadow: 0 0 0 1px #409eff inset;
}

.day-column.has-records {
  background: #fff;
}

.day-header {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 4px 6px;
  background: rgba(0, 0, 0, 0.03);
  border-bottom: 1px solid #ebeef5;
}

.is-today .day-header {
  background: rgba(64, 158, 255, 0.1);
  border-bottom-color: #b3d8ff;
}

.day-name {
  font-size: 13px;
  font-weight: 600;
  color: #606266;
}

.is-today .day-name {
  color: #409eff;
}

.day-body {
  padding: 6px 4px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-height: 80px;
}

.empty-day {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 1;
  color: #c0c4cc;
  font-size: 18px;
  font-weight: 300;
}

.day-record {
  border-left: 3px solid #dcdfe6;
  padding: 4px 8px;
  background: #fff;
  border-radius: 4px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: 12px;
  transition: box-shadow 0.15s;
}

.day-record:hover {
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.08);
}

.record-action {
  font-weight: 600;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.record-meta {
  display: flex;
  align-items: center;
  gap: 4px;
}

.record-group-tag {
  font-size: 10px;
  color: #fff;
  padding: 1px 5px;
  border-radius: 3px;
  white-space: nowrap;
}

.record-time {
  color: #909399;
  font-size: 11px;
}

.record-exhaustion {
  color: #e6a23c;
  font-size: 11px;
  font-weight: 500;
}

@media (max-width: 900px) {
  .week-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}

@media (max-width: 580px) {
  .week-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
