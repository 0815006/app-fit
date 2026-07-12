<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getDashboard,
  startWorkout,
  endWorkout,
  getWeeklySummary,
  type DashboardVO,
  type TimeoutRecordVO,
  type WeeklyWorkoutVO,
} from '@/api/gymWorkout'
import MuscleDashboard from '@/components/workout/MuscleDashboard.vue'
import ActionSelectDialog from '@/components/workout/ActionSelectDialog.vue'
import TrainingTimerDialog from '@/components/workout/TrainingTimerDialog.vue'
import ExhaustionDialog from '@/components/workout/ExhaustionDialog.vue'
import type { EndWorkoutParams } from '@/api/gymWorkout'
import TimeoutCorrectDialog from '@/components/workout/TimeoutCorrectDialog.vue'
import MakeupDialog from '@/components/workout/MakeupDialog.vue'
import WeeklySummary from '@/components/workout/WeeklySummary.vue'
import CalendarHeatmap from '@/components/workout/CalendarHeatmap.vue'

// ---- 状态 ----
const loading = ref(false)
const dashboard = ref<DashboardVO | null>(null)

/** 本周训练摘要 */
const weeklyRecords = ref<WeeklyWorkoutVO[]>([])
const weeklyLoading = ref(false)

// 弹窗控制
const actionSelectVisible = ref(false)
const selectedMuscleGroup = ref('')
/** 高亮的二级肌肉编码（如 CHEST_MAJOR），用于 ActionSelectDialog 置顶 */
const highlightMuscleCode = ref('')
/** 高亮的二级肌肉中文名（如 胸大肌），用于 ActionSelectDialog 标题 */
const highlightMuscleName = ref('')

const timerVisible = ref(false)
const currentActionId = ref('')
const currentActionName = ref('')

const exhaustionVisible = ref(false)
const currentRecordId = ref('')

const timeoutVisible = ref(false)
const timeoutRecord = ref<TimeoutRecordVO | null>(null)

const makeupVisible = ref(false)

// ---- 顶部提示 ----
const topTip = ref('')

function buildTopTip(readyNames: string[]): string {
  if (readyNames.length === 0) return '所有肌群均在恢复中，休息一下...'
  return '今天适合训练的部位：' + readyNames.join('、')
}

// ---- 数据加载 ----
async function loadDashboard(): Promise<void> {
  loading.value = true
  try {
    const res = await getDashboard()
    dashboard.value = res.data
    topTip.value = buildTopTip(dashboard.value?.readyMuscleNames || [])

    // 检查超时记录
    if (dashboard.value?.timeoutRecord) {
      timeoutRecord.value = dashboard.value.timeoutRecord
      timeoutVisible.value = true
    }
  } catch {
    ElMessage.error('加载看板失败')
  } finally {
    loading.value = false
  }
}

/** 加载本周训练摘要 */
async function loadWeeklySummary(): Promise<void> {
  weeklyLoading.value = true
  try {
    const res = await getWeeklySummary()
    weeklyRecords.value = res.data || []
  } catch {
    // 静默失败
  } finally {
    weeklyLoading.value = false
  }
}

// ---- 二级肌肉入口（点击卡片内二级肌肉行）----
function handleMuscleSelectDetail(payload: { muscleGroup: string; muscleCode: string; muscleName: string }): void {
  selectedMuscleGroup.value = payload.muscleGroup
  highlightMuscleCode.value = payload.muscleCode
  highlightMuscleName.value = payload.muscleName
  actionSelectVisible.value = true
}

// ---- 暂存的训练数据（计时结束后先存这里，力竭度确认后一起提交）----
const pendingWorkoutData = ref<{ weight?: number; reps?: number; setCount?: number }>({})

// ---- 动作选择 -> 开始训练 ----
async function handleActionStart(actionId: string, actionName: string, actionType?: string): Promise<void> {
  try {
    const res = await startWorkout(actionId)
    currentRecordId.value = res.data
    currentActionId.value = actionId
    currentActionName.value = actionName
    currentActionType.value = actionType || ''
    timerVisible.value = true
  } catch {
    ElMessage.error('开始训练失败')
  }
}

// ---- 暂存当前动作类型，传给 TrainingTimerDialog ----
const currentActionType = ref('')

// ---- 计时结束 -> 暂存训练数据 -> 弹出力竭度 ----
function handleTimerEnd(payload: { elapsedSeconds: number; weight?: number; reps?: number; setCount?: number }): void {
  pendingWorkoutData.value = {
    weight: payload.weight ?? undefined,
    reps: payload.reps ?? undefined,
    setCount: payload.setCount ?? undefined,
  }
  exhaustionVisible.value = true
}

// ---- 力竭度提交 ----
async function handleExhaustionSubmit(score: number): Promise<void> {
  try {
    await endWorkout(currentRecordId.value, {
      weight: pendingWorkoutData.value.weight ?? null,
      reps: pendingWorkoutData.value.reps ?? null,
      setCount: pendingWorkoutData.value.setCount ?? null,
      exhaustionScore: score,
    })
    ElMessage.success('训练记录已保存')
    pendingWorkoutData.value = {}
    await loadDashboard()
    await loadWeeklySummary()
  } catch {
    ElMessage.error('保存失败')
  }
}

// ---- 取消训练 ----
function handleTimerCancel(): void {
  ElMessage.info('已取消本次训练')
}

// ---- 超时修正 ----
async function handleTimeoutCorrected(): Promise<void> {
  await loadDashboard()
  await loadWeeklySummary()
}

// ---- 补打卡 ----
async function handleMakeupCompleted(): Promise<void> {
  await loadDashboard()
  await loadWeeklySummary()
}

onMounted(() => {
  loadDashboard()
  loadWeeklySummary()
})
</script>

<template>
  <div class="gym-workout-view">
    <!-- 顶部提示条 -->
    <div class="top-bar">
      <div class="top-bar-inner">
        <el-alert
          :title="topTip"
          type="success"
          :closable="false"
          show-icon
          class="top-tip"
        />
        <el-button type="warning" size="small" @click="makeupVisible = true">
          自由补打卡
        </el-button>
      </div>
    </div>

    <div class="gym-content-row">
      <!-- 左侧：肌群看板 + 本周训练概览 -->
      <div class="gym-content-left">
        <MuscleDashboard
          :muscle-groups="dashboard?.muscleGroups || []"
          :loading="loading"
          @select-muscle="handleMuscleSelectDetail"
        />

        <WeeklySummary
          :records="weeklyRecords"
          :loading="weeklyLoading"
        />
      </div>

      <!-- 右侧：半年打卡日历 -->
      <div class="gym-content-right">
        <CalendarHeatmap />
      </div>
    </div>

    <!-- 动作选择弹窗（方案B：整群平铺 + 二级置顶高亮） -->
    <ActionSelectDialog
      v-model:visible="actionSelectVisible"
      :muscle-group="selectedMuscleGroup"
      :highlight-muscle-code="highlightMuscleCode"
      :highlight-muscle-name="highlightMuscleName"
      @start="handleActionStart"
    />

    <!-- 训练计时弹窗 -->
    <TrainingTimerDialog
      v-model:visible="timerVisible"
      :action-id="currentActionId"
      :action-name="currentActionName"
      :action-type="currentActionType"
      @end="handleTimerEnd"
      @cancel="handleTimerCancel"
    />

    <!-- 力竭度评价弹窗 -->
    <ExhaustionDialog
      v-model:visible="exhaustionVisible"
      @submit="handleExhaustionSubmit"
    />

    <!-- 超时修正弹窗 -->
    <TimeoutCorrectDialog
      v-model:visible="timeoutVisible"
      :record="timeoutRecord"
      @corrected="handleTimeoutCorrected"
    />

    <!-- 补打卡弹窗 -->
    <MakeupDialog
      v-model:visible="makeupVisible"
      @completed="handleMakeupCompleted"
    />
  </div>
</template>

<style scoped>
.gym-workout-view {
  padding: 16px 20px;
  height: 100%;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.top-bar {
  margin-bottom: 12px;
  flex-shrink: 0;
}

.top-bar-inner {
  display: flex;
  align-items: center;
  gap: 12px;
}

.top-tip {
  flex: 1;
}

.gym-content-row {
  flex: 1;
  display: flex;
  gap: 16px;
  min-height: 0;
  overflow: hidden;
}

.gym-content-left {
  flex: 2;
  overflow-y: auto;
  min-height: 0;
  padding-right: 4px;
}

.gym-content-right {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  background: #fafafa;
  border-radius: 10px;
  border: 1px solid #ebeef5;
  padding: 12px 14px;
}
</style>
