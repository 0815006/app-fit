<script setup lang="ts">
import { ref, computed, onUnmounted } from 'vue'

const props = withDefaults(defineProps<{
  defaultSeconds?: number
}>(), {
  defaultSeconds: 60
})

const emit = defineEmits<{
  (e: 'complete'): void
}>()

const totalSeconds = ref(props.defaultSeconds)
const remainingSeconds = ref(props.defaultSeconds)
const isRunning = ref(false)
const isPaused = ref(false)
let timer: ReturnType<typeof setInterval> | null = null

const displayTime = computed(() => {
  const mins = Math.floor(remainingSeconds.value / 60)
  const secs = remainingSeconds.value % 60
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
})

const progress = computed(() => {
  return ((totalSeconds.value - remainingSeconds.value) / totalSeconds.value) * 100
})

const timerColor = computed(() => {
  if (remainingSeconds.value <= 10) return '#f56c6c'
  if (remainingSeconds.value <= 30) return '#e6a23c'
  return '#67c23a'
})

function start(seconds?: number) {
  if (seconds !== undefined) {
    totalSeconds.value = seconds
    remainingSeconds.value = seconds
  }
  isRunning.value = true
  isPaused.value = false
  timer = setInterval(() => {
    if (remainingSeconds.value > 0) {
      remainingSeconds.value--
    } else {
      stop()
      emit('complete')
      playBeep()
    }
  }, 1000)
}

function pause() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
  isPaused.value = true
  isRunning.value = false
}

function resume() {
  isRunning.value = true
  isPaused.value = false
  timer = setInterval(() => {
    if (remainingSeconds.value > 0) {
      remainingSeconds.value--
    } else {
      stop()
      emit('complete')
      playBeep()
    }
  }, 1000)
}

function stop() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
  isRunning.value = false
  isPaused.value = false
  remainingSeconds.value = totalSeconds.value
}

function reset(seconds?: number) {
  stop()
  if (seconds !== undefined) {
    totalSeconds.value = seconds
  }
  remainingSeconds.value = totalSeconds.value
}

function playBeep() {
  try {
    const ctx = new (window.AudioContext || (window as any).webkitAudioContext)()
    const osc = ctx.createOscillator()
    const gain = ctx.createGain()
    osc.connect(gain)
    gain.connect(ctx.destination)
    osc.frequency.value = 800
    gain.gain.value = 0.3
    osc.start()
    setTimeout(() => { osc.stop(); ctx.close() }, 300)
  } catch { /* ignore */ }
}

onUnmounted(() => {
  if (timer) clearInterval(timer)
})

defineExpose({ start, pause, resume, stop, reset, isRunning, isPaused, remainingSeconds })
</script>

<template>
  <div class="rest-timer">
    <div class="timer-display" :style="{ color: timerColor }">
      <div class="timer-time">{{ displayTime }}</div>
      <el-progress
        type="circle"
        :percentage="progress"
        :width="120"
        :stroke-width="8"
        :color="timerColor"
        class="timer-progress"
      >
        <template #default>
          <span class="timer-time-inner">{{ displayTime }}</span>
        </template>
      </el-progress>
    </div>
    <div class="timer-controls">
      <el-button v-if="!isRunning && !isPaused" type="primary" circle @click="start()">
        <el-icon><VideoPlay /></el-icon>
      </el-button>
      <el-button v-if="isRunning" type="warning" circle @click="pause">
        <el-icon><VideoPause /></el-icon>
      </el-button>
      <el-button v-if="isPaused" type="success" circle @click="resume">
        <el-icon><VideoPlay /></el-icon>
      </el-button>
      <el-button type="info" circle @click="reset()">
        <el-icon><RefreshRight /></el-icon>
      </el-button>
    </div>
    <div class="timer-presets">
      <el-button size="small" @click="reset(30)">30s</el-button>
      <el-button size="small" @click="reset(60)">60s</el-button>
      <el-button size="small" @click="reset(90)">90s</el-button>
      <el-button size="small" @click="reset(120)">120s</el-button>
    </div>
  </div>
</template>

<style scoped>
.rest-timer { text-align: center; padding: 16px; }
.timer-display { position: relative; display: inline-block; margin-bottom: 12px; }
.timer-time { font-size: 32px; font-weight: bold; font-family: monospace; }
.timer-progress { margin: 0 auto; }
.timer-time-inner { font-size: 24px; font-weight: bold; font-family: monospace; }
.timer-controls { display: flex; gap: 8px; justify-content: center; margin-bottom: 12px; }
.timer-presets { display: flex; gap: 4px; justify-content: center; }
</style>
