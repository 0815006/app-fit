<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  (e: 'update:visible', val: boolean): void
  (e: 'submit', score: number): void
}>()

const score = ref(100)  // 默认 100%

watch(() => props.visible, (val) => {
  if (val) {
    score.value = 100
  }
})

function handleSubmit(): void {
  if (score.value < 50 || score.value > 120) {
    ElMessage.warning('力竭度需在 50% ~ 120% 之间')
    return
  }
  emit('update:visible', false)
  emit('submit', score.value / 100)
}

/** 格式化 slider 提示 */
function formatTooltip(val: number): string {
  return val + '%'
}

const SLIDER_MARKS: Record<number, string> = {
  50: '浅练',
  75: '适中',
  100: '力竭',
  120: '超负荷',
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    title="评价本次训练"
    width="480px"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :show-close="false"
    @update:model-value="emit('update:visible', $event)"
  >
    <div class="exhaustion-container">
      <p class="exhaustion-hint">滑动评价本次训练的力竭程度</p>

      <div class="slider-wrapper">
        <el-slider
          v-model="score"
          :min="50"
          :max="120"
          :step="1"
          :marks="SLIDER_MARKS"
          :format-tooltip="formatTooltip"
          show-input
        />
      </div>

      <div class="score-display">
        <span class="score-label">力竭度系数：</span>
        <el-tag type="warning" size="large">{{ (score / 100).toFixed(2) }}</el-tag>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button type="primary" size="large" @click="handleSubmit" class="submit-btn">
          确认提交
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.exhaustion-container {
  padding: 20px 0;
  text-align: center;
}

.exhaustion-hint {
  font-size: 15px;
  color: #303133;
  margin-bottom: 24px;
}

.slider-wrapper {
  padding: 0 10px;
}

.score-display {
  margin-top: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.score-label {
  font-size: 14px;
  color: #606266;
}

.dialog-footer {
  display: flex;
  justify-content: center;
}

.submit-btn {
  width: 200px;
}
</style>
