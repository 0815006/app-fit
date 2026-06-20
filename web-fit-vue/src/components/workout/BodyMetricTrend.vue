<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, DataZoomComponent } from 'echarts/components'
import { queryBodyMetric, type BodyMetric } from '@/api/bodyMetric'

use([CanvasRenderer, LineChart, GridComponent, TooltipComponent, LegendComponent, DataZoomComponent])

const loading = ref(false)
const metrics = ref<BodyMetric[]>([])
const dateRange = ref<[string, string] | null>(null)

const chartOption = computed(() => {
  const dates = metrics.value.map(m => m.metricDate)
  const weights = metrics.value.map(m => m.weight)
  const bodyFats = metrics.value.map(m => m.bodyFat)
  const bmis = metrics.value.map(m => m.bmi)
  const muscleMasses = metrics.value.map(m => m.muscleMass)

  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['体重(kg)', '体脂率(%)', 'BMI', '肌肉量(kg)'], bottom: 0 },
    grid: { left: 60, right: 60, top: 30, bottom: 60 },
    xAxis: { type: 'category', data: dates, axisLabel: { rotate: 30 } },
    yAxis: [
      { type: 'value', name: '体重/肌肉量(kg)', position: 'left' },
      { type: 'value', name: '体脂率(%) / BMI', position: 'right' }
    ],
    dataZoom: [{ type: 'inside' }, { type: 'slider' }],
    series: [
      { name: '体重(kg)', type: 'line', data: weights, smooth: true, itemStyle: { color: '#409eff' } },
      { name: '体脂率(%)', type: 'line', yAxisIndex: 1, data: bodyFats, smooth: true, itemStyle: { color: '#f56c6c' } },
      { name: 'BMI', type: 'line', yAxisIndex: 1, data: bmis, smooth: true, itemStyle: { color: '#e6a23c' } },
      { name: '肌肉量(kg)', type: 'line', data: muscleMasses, smooth: true, itemStyle: { color: '#67c23a' } }
    ]
  }
})

async function loadData() {
  loading.value = true
  try {
    const params: Record<string, string> = {}
    if (dateRange.value) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    const res = await queryBodyMetric({ ...params, page: 1, size: 1000 })
    metrics.value = (res.data?.records || []).reverse()
  } catch {
    metrics.value = []
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div v-loading="loading" class="body-metric-trend">
    <div class="trend-header">
      <span class="trend-title">📈 身体指标趋势</span>
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
        style="width: 260px"
        @change="loadData"
      />
    </div>
    <v-chart v-if="metrics.length > 0" :option="chartOption" style="height: 350px" autoresize />
    <el-empty v-else description="暂无数据" />
  </div>
</template>

<style scoped>
.body-metric-trend { padding: 16px; background: #fafafa; border-radius: 8px; }
.trend-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.trend-title { font-size: 16px; font-weight: 600; }
</style>
