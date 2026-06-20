<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { detectPlateau } from '@/api/trainingStats'

const loading = ref(false)
const plateauData = ref<Array<{ actionId: string; actionName: string; weeks: number; latestVolume: number }>>([])

async function loadData() {
  loading.value = true
  try {
    const res = await detectPlateau(6)
    plateauData.value = (res.data || []) as Array<{ actionId: string; actionName: string; weeks: number; latestVolume: number }>
  } catch {
    plateauData.value = []
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div v-loading="loading" class="plateau-warning">
    <div class="warning-header">
      <span class="warning-title">⚠️ 平台期预警</span>
      <el-button text size="small" @click="loadData">
        <el-icon><Refresh /></el-icon> 刷新
      </el-button>
    </div>
    <el-alert
      v-if="plateauData.length > 0"
      type="warning"
      :closable="false"
      show-icon
    >
      <template #title>
        检测到 {{ plateauData.length }} 个动作连续6周无增长，建议更换动作模式
      </template>
    </el-alert>
    <el-table v-if="plateauData.length > 0" :data="plateauData" stripe size="small" style="margin-top: 12px">
      <el-table-column prop="actionName" label="动作名称" />
      <el-table-column prop="weeks" label="停滞周数" width="100" align="center">
        <template #default="{ row }">
          <el-tag type="warning">{{ row.weeks }} 周</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="latestVolume" label="最近周容量(kg)" width="140" align="center" />
    </el-table>
    <el-empty v-else description="暂无平台期预警，继续保持！" :image-size="60" />
  </div>
</template>

<style scoped>
.plateau-warning { padding: 16px; background: #fafafa; border-radius: 8px; }
.warning-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.warning-title { font-size: 16px; font-weight: 600; }
</style>
