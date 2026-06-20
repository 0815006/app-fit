<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, DataZoomComponent } from 'echarts/components'
import { getVolumeTrend } from '@/api/trainingStats'

use([CanvasRenderer, BarChart, LineChart, GridComponent, TooltipComponent, LegendComponent, DataZoomComponent])

const loading = ref(false)
const groupBy = ref('week')
const dateRange = ref<[string, string] | null>(null)
const trendData = ref<Array<{ period: string; volume: number }>>([])

const chartOption = computed(() => {
  const periods = trendData.value.map(d => d.period)
  const volumes = trendData.value.map(d => d.volume)

  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 60, right: 20, top: 30, bottom: 60 },
    xAxis: { type: 'category', data: periods, axisLabel: { rotate: 30 } },
    yAxis: { type: 'value', name: '总容量(kg)' },
    dataZoom: [{ type: 'inside' }, { type: 'slider' }],
    series: [
      {
        name: '训练容量',
        type: 'bar',
        data: volumes,
        itemStyle: {
          color: '#409eff',
          borderRadius: [4, 4, 0, 0]
        }
      }
    ]
  }
})

async function loadData() {
  loading.value = true
  try {
    const params: Record<string, any> = { groupBy: groupBy.value }
    if (dateRange.value) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    const res = await getVolumeTrend(params)
    trendData.value = (res.data || []) as Array<{ period: string; volume: number }>
  } catch {
    trendData.value = []
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div v-loading="loading" class="volume-trend">
    <div class="trend-header">
      <span class="trend-title">📊 训练容量趋势</span>
      <div class="trend-controls">
        <el-radio-group v-model="groupBy" size="small" @change="loadData">
          <el-radio-button value="week">按周</el-radio-button>
          <el-radio-button value="month">按月</el-radio-button>
        </el-radio-group>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始"
          end-placeholder="结束"
          value-format="YYYY-MM-DD"
          style="width: 240px; margin-left: 12px"
          @change="loadData"
        />
      </div>
    </div>
    <v-chart v-if="trendData.length > 0" :option="chartOption" style="height: 300px" autoresize />
    <el-empty v-else description="暂无数据" />
  </div>
</template>

<style scoped>
.volume-trend { padding: 16px; background: #fafafa; border-radius: 8px; }
.trend-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; flex-wrap: wrap; gap: 12px; }
.trend-title { font-size: 16px; font-weight: 600; }
.trend-controls { display: flex; align-items: center; }
</style>
