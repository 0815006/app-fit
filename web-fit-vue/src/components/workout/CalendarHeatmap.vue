<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { getCheckinDates } from '@/api/gymWorkout'

const scrollRef = ref<HTMLElement | null>(null)

const loading = ref(false)
const loadingMore = ref(false)
const checkedDateSet = ref<Set<string>>(new Set())

/** 当前展示的最早月份游标 */
const earliestYear = ref(0)
const earliestMonth = ref(0)

interface MonthBlock {
  year: number
  month: number
  label: string
  cells: { day: number; date: string; checked: boolean; empty: boolean }[]
}

const months = ref<MonthBlock[]>([])

const totalDays = computed(() => checkedDateSet.value.size)

/** 日历起始：2026年1月 */
const START_YEAR = 2026
const START_MONTH = 1

/** 每批加载月数 */
const BATCH_SIZE = 12

async function load(): Promise<void> {
  loading.value = true
  try {
    const res = await getCheckinDates()
    const dates = res.data || []
    checkedDateSet.value = new Set(dates)
    months.value = []
    loadInitialMonths()
  } catch {
    checkedDateSet.value = new Set()
    months.value = []
    loadInitialMonths()
  } finally {
    loading.value = false
    await nextTick()
    if (scrollRef.value) {
      scrollRef.value.scrollTop = scrollRef.value.scrollHeight
    }
  }
}

/** 初次加载：从 2026年1月 到当前月 */
function loadInitialMonths(): void {
  const now = new Date()
  const currentYear = now.getFullYear()
  const currentMonth = now.getMonth() + 1 // 1-based

  // 月数 = 从2026-01到当前月的跨度 + 1
  const totalMonths = (currentYear - START_YEAR) * 12 + (currentMonth - START_MONTH) + 1

  const result: MonthBlock[] = []
  for (let i = totalMonths - 1; i >= 0; i--) {
    const d = new Date(currentYear, currentMonth - 1 - i, 1)
    result.push(buildMonthBlock(d.getFullYear(), d.getMonth() + 1))
  }

  const oldest = result[0]
  earliestYear.value = oldest.year
  earliestMonth.value = oldest.month
  months.value = result
}

/** 滚动加载更早的月份 */
async function loadMoreMonths(): Promise<void> {
  if (earliestYear.value <= START_YEAR && earliestMonth.value <= START_MONTH) return

  loadingMore.value = true
  await nextTick()

  const newBlocks: MonthBlock[] = []
  let y = earliestYear.value
  let m = earliestMonth.value

  for (let i = 0; i < BATCH_SIZE; i++) {
    m--
    if (m <= 0) {
      m = 12
      y--
    }
    if (y < START_YEAR || (y === START_YEAR && m < START_MONTH)) break
    newBlocks.push(buildMonthBlock(y, m))
  }

  if (newBlocks.length > 0) {
    newBlocks.reverse()
    months.value = [...newBlocks, ...months.value]
    const oldest = newBlocks[0]
    earliestYear.value = oldest.year
    earliestMonth.value = oldest.month
  }

  loadingMore.value = false
}

function buildMonthBlock(year: number, month: number): MonthBlock {
  const label = `${year}年${month}月`
  const daysInMonth = new Date(year, month, 0).getDate()

  const firstDayRaw = new Date(year, month - 1, 1).getDay()
  const firstDayOfWeek = firstDayRaw === 0 ? 7 : firstDayRaw

  const cells: MonthBlock['cells'] = []

  for (let w = 1; w < firstDayOfWeek; w++) {
    cells.push({ day: 0, date: '', checked: false, empty: true })
  }

  for (let day = 1; day <= daysInMonth; day++) {
    const dateStr = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
    cells.push({ day, date: dateStr, checked: checkedDateSet.value.has(dateStr), empty: false })
  }

  return { year, month, label, cells }
}

/** 滚动事件：触顶加载更多 */
function handleScroll(e: Event): void {
  const el = e.target as HTMLElement
  if (!el || loadingMore.value) return
  if (el.scrollTop <= 10) {
    const h = el.scrollHeight
    loadMoreMonths().then(() => {
      nextTick(() => {
        el.scrollTop = el.scrollHeight - h
      })
    })
  }
}

const WEEKDAY_LABELS = ['一', '二', '三', '四', '五', '六', '日']

onMounted(() => {
  load()
})
</script>

<template>
  <div class="calendar-heatmap" v-loading="loading">
    <div class="heatmap-header">
      <h4 class="heatmap-title">📅 打卡日历</h4>
      <span class="heatmap-count">{{ totalDays }} 天</span>
    </div>

    <div ref="scrollRef" class="heatmap-scroll" @scroll="handleScroll">
      <div v-if="loadingMore" class="load-more-hint">加载更多...</div>
      <div v-for="m in months" :key="`${m.year}-${m.month}`" class="month-block">
        <div class="month-label">{{ m.label }}</div>
        <div class="month-grid">
          <span
            v-for="wd in WEEKDAY_LABELS"
            :key="wd"
            class="weekday-header"
          >{{ wd }}</span>
          <span
            v-for="(cell, ci) in m.cells"
            :key="ci"
            class="day-dot"
            :class="{
              'dot-checked': cell.checked,
              'dot-empty': cell.empty,
            }"
            :title="cell.date"
          ></span>
        </div>
      </div>
    </div>

    <div class="heatmap-legend">
      <span class="legend-dot dot-checked"></span>
      <span>已打卡</span>
      <span class="legend-dot" style="margin-left: 12px;"></span>
      <span>未打卡</span>
    </div>
  </div>
</template>

<style scoped>
.calendar-heatmap {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}

.heatmap-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  flex-shrink: 0;
}

.heatmap-title {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: #303133;
}

.heatmap-count {
  font-size: 13px;
  color: #67c23a;
  font-weight: 600;
}

.heatmap-scroll {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
  padding-right: 4px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px 10px;
  align-content: start;
}

.load-more-hint {
  grid-column: 1 / -1;
  text-align: center;
  font-size: 11px;
  color: #c0c4cc;
  padding: 4px 0;
}

.month-block {
  margin-bottom: 2px;
}

.month-label {
  font-size: 10px;
  color: #909399;
  margin-bottom: 2px;
  padding-left: 1px;
}

.month-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 2px;
}

.weekday-header {
  font-size: 8px;
  color: #c0c4cc;
  text-align: center;
  line-height: 1;
  padding-bottom: 1px;
}

.day-dot {
  aspect-ratio: 1;
  border-radius: 2px;
  display: block;
  min-width: 8px;
  background: #ebeef5;
  transition: background 0.15s;
}

.day-dot.dot-checked {
  background: #67c23a;
  border-radius: 2px;
}

.day-dot.dot-empty {
  background: transparent;
}

.heatmap-legend {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: #909399;
  margin-top: 8px;
  flex-shrink: 0;
  padding-top: 8px;
  border-top: 1px solid #ebeef5;
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 2px;
  display: inline-block;
  background: #ebeef5;
}

.legend-dot.dot-checked {
  background: #67c23a;
}
</style>
