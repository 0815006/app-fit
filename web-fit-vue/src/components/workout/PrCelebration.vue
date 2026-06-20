<script setup lang="ts">
import { ref, computed, watch } from 'vue'

const props = defineProps<{
  visible: boolean
  prData: {
    actionName: string
    previousBest: number
    newBest: number
    weight: number
    reps: number
  } | null
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'sync-plan'): void
  (e: 'just-record'): void
}>()

const dialogVisible = ref(false)

watch(() => props.visible, (val) => {
  dialogVisible.value = val
})

watch(dialogVisible, (val) => {
  emit('update:visible', val)
})

function handleSync() {
  emit('sync-plan')
  dialogVisible.value = false
}

function handleJustRecord() {
  emit('just-record')
  dialogVisible.value = false
}

function handleClose() {
  dialogVisible.value = false
}

const improvement = computed(() => {
  if (!props.prData || props.prData.previousBest <= 0) return 0
  return ((props.prData.newBest - props.prData.previousBest) / props.prData.previousBest * 100).toFixed(1)
})
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    title="🎉 破纪录啦！"
    width="400px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div v-if="prData" class="pr-celebration">
      <div class="pr-icon">🏆</div>
      <div class="pr-title">{{ prData.actionName }}</div>
      <div class="pr-stats">
        <div class="pr-stat-item">
          <div class="pr-stat-label">重量</div>
          <div class="pr-stat-value">{{ prData.weight }} kg</div>
        </div>
        <div class="pr-stat-item">
          <div class="pr-stat-label">次数</div>
          <div class="pr-stat-value">{{ prData.reps }}</div>
        </div>
        <div class="pr-stat-item">
          <div class="pr-stat-label">1RM</div>
          <div class="pr-stat-value highlight">{{ prData.newBest.toFixed(1) }} kg</div>
        </div>
      </div>
      <div class="pr-improvement">
        比上次提升 <span class="improvement-value">+{{ improvement }}%</span>
      </div>
      <div class="pr-actions">
        <el-button type="primary" @click="handleSync">同步到训练计划</el-button>
        <el-button @click="handleJustRecord">仅记录</el-button>
      </div>
    </div>
  </el-dialog>
</template>

<style scoped>
.pr-celebration {
  text-align: center;
  padding: 20px;
}

.pr-icon {
  font-size: 64px;
  margin-bottom: 16px;
  animation: bounce 0.6s ease-in-out;
}

@keyframes bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-20px); }
}

.pr-title {
  font-size: 20px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 20px;
}

.pr-stats {
  display: flex;
  justify-content: space-around;
  margin-bottom: 20px;
}

.pr-stat-item {
  text-align: center;
}

.pr-stat-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.pr-stat-value {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
}

.pr-stat-value.highlight {
  color: #f56c6c;
  font-size: 22px;
}

.pr-improvement {
  font-size: 14px;
  color: #67c23a;
  margin-bottom: 24px;
}

.improvement-value {
  font-weight: bold;
  font-size: 16px;
}

.pr-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}
</style>
