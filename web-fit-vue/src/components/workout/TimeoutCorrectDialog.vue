<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { TimeoutRecordVO } from '@/api/gymWorkout'
import { correctTimeout } from '@/api/gymWorkout'

const props = defineProps<{
  visible: boolean
  record: TimeoutRecordVO | null
}>()

const emit = defineEmits<{
  (e: 'update:visible', val: boolean): void
  (e: 'corrected'): void
}>()

const actualMinutes = ref(15)
const exhaustionPercent = ref(100)
const submitting = ref(false)

watch(() => props.visible, (val) => {
  if (val) {
    actualMinutes.value = 15
    exhaustionPercent.value = 100
  }
})

async function handleCorrect(): Promise<void> {
  if (!props.record) return
  if (actualMinutes.value < 1) {
    ElMessage.warning('实际训练分钟数至少为 1')
    return
  }
  if (exhaustionPercent.value < 50 || exhaustionPercent.value > 120) {
    ElMessage.warning('力竭度需在 50% ~ 120% 之间')
    return
  }
  submitting.value = true
  try {
    await correctTimeout(
      props.record.recordId,
      actualMinutes.value,
      exhaustionPercent.value / 100
    )
    ElMessage.success('修正完成')
    emit('update:visible', false)
    emit('corrected')
  } catch {
    ElMessage.error('修正失败')
  } finally {
    submitting.value = false
  }
}

function formatTooltip(val: number): string {
  return val + '%'
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    title="超时训练修正"
    width="480px"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :show-close="false"
    @update:model-value="emit('update:visible', $event)"
  >
    <div class="timeout-content">
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        class="timeout-alert"
      >
        <template #title>
          检测到你于 <strong>{{ record?.startTimeLabel || '' }}</strong>
          开始了 <strong>{{ record?.actionName || '' }}</strong>，可能忘记点结束了。
        </template>
      </el-alert>

      <div class="form-item">
        <label class="form-label">实际训练了多少分钟？</label>
        <el-input-number
          v-model="actualMinutes"
          :min="1"
          :max="180"
          :step="1"
          class="form-input"
        />
      </div>

      <div class="form-item">
        <label class="form-label">当时的力竭程度</label>
        <div class="slider-wrap">
          <el-slider
            v-model="exhaustionPercent"
            :min="50"
            :max="120"
            :step="1"
            :format-tooltip="formatTooltip"
            show-input
          />
        </div>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button
          type="primary"
          size="large"
          :loading="submitting"
          @click="handleCorrect"
        >
          修正并结束
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.timeout-content {
  padding: 8px 0;
}

.timeout-alert {
  margin-bottom: 20px;
}

.form-item {
  margin-bottom: 16px;
}

.form-label {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.form-input {
  width: 180px;
}

.slider-wrap {
  padding: 0 4px;
}

.dialog-footer {
  display: flex;
  justify-content: center;
}
</style>
