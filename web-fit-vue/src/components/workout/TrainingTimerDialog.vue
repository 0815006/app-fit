<script setup lang="ts">
import { ref, watch, onBeforeUnmount } from 'vue'
import { ElMessageBox } from 'element-plus'

const props = defineProps<{
  visible: boolean
  actionName: string
}>()

const emit = defineEmits<{
  (e: 'update:visible', val: boolean): void
  (e: 'end', elapsedSeconds: number): void
  (e: 'cancel'): void
}>()

const elapsed = ref(0) // 秒
let timer: ReturnType<typeof setInterval> | null = null

watch(() => props.visible, (val) => {
  if (val) {
    elapsed.value = 0
    timer = setInterval(() => {
      elapsed.value++
    }, 1000)
  } else {
    stopTimer()
  }
})

onBeforeUnmount(() => {
  stopTimer()
})

function stopTimer(): void {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

function formatTime(seconds: number): string {
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  if (h > 0) {
    return `${pad(h)}:${pad(m)}:${pad(s)}`
  }
  return `${pad(m)}:${pad(s)}`
}

function pad(n: number): string {
  return n.toString().padStart(2, '0')
}

async function handleEnd(): Promise<void> {
  try {
    await ElMessageBox.confirm('确定要结束本次动作训练吗？', '确认结束', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    stopTimer()
    emit('update:visible', false)
    emit('end', elapsed.value)
  } catch {
    // 用户取消
  }
}

function handleCancel(): void {
  stopTimer()
  emit('update:visible', false)
  emit('cancel')
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    :title="`训练计时 — ${actionName}`"
    width="500px"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :show-close="false"
    @update:model-value="emit('update:visible', $event)"
  >
    <div class="timer-container">
      <div class="timer-display">{{ formatTime(elapsed) }}</div>
      <p class="timer-hint">专注训练，完成后点击结束</p>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleCancel">更换动作 / 取消</el-button>
        <el-button type="primary" @click="handleEnd">结束训练</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.timer-container {
  text-align: center;
  padding: 30px 0;
}

.timer-display {
  font-size: 72px;
  font-weight: 700;
  font-family: 'Courier New', monospace;
  color: #303133;
  letter-spacing: 4px;
}

.timer-hint {
  margin-top: 16px;
  font-size: 14px;
  color: #909399;
}

.dialog-footer {
  display: flex;
  justify-content: space-between;
}
</style>
