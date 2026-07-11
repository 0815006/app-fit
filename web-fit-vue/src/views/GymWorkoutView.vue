<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getDashboard,
  startWorkout,
  endWorkout,
  type DashboardVO,
  type TimeoutRecordVO,
} from '@/api/gymWorkout'
import { listAllGymMuscle, type GymMuscle } from '@/api/gymMuscle'
import MuscleDashboard from '@/components/workout/MuscleDashboard.vue'
import ActionSelectDialog from '@/components/workout/ActionSelectDialog.vue'
import TrainingTimerDialog from '@/components/workout/TrainingTimerDialog.vue'
import ExhaustionDialog from '@/components/workout/ExhaustionDialog.vue'
import TimeoutCorrectDialog from '@/components/workout/TimeoutCorrectDialog.vue'
import MakeupDialog from '@/components/workout/MakeupDialog.vue'

// ---- 状态 ----
const loading = ref(false)
const dashboard = ref<DashboardVO | null>(null)

/** 所有肌肉数据，用于双层展示的第二层 */
const allMuscles = ref<GymMuscle[]>([])

// 弹窗控制
const actionSelectVisible = ref(false)
const selectedMuscleGroup = ref('')
/** 当前选中的具体肌肉名称，用于弹窗标题上下文 */
const selectedMuscleName = ref('')

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

async function loadMuscles(): Promise<void> {
  try {
    const res = await listAllGymMuscle()
    allMuscles.value = res.data || []
  } catch {
    // 静默失败，肌肉列表为空时不影响第一层展示
  }
}

// ---- 大肌群入口（第一层快捷按钮）----
function handleMuscleSelect(group: string): void {
  selectedMuscleGroup.value = group
  selectedMuscleName.value = ''
  actionSelectVisible.value = true
}

// ---- 具体肌肉入口（第二层）----
function handleMuscleSelectDetail(muscle: GymMuscle): void {
  selectedMuscleGroup.value = muscle.muscleGroup
  selectedMuscleName.value = muscle.muscleName
  actionSelectVisible.value = true
}

// ---- 动作选择 -> 开始训练 ----
async function handleActionStart(actionId: string, actionName: string): Promise<void> {
  try {
    const res = await startWorkout(actionId)
    currentRecordId.value = res.data
    currentActionId.value = actionId
    currentActionName.value = actionName
    timerVisible.value = true
  } catch {
    ElMessage.error('开始训练失败')
  }
}

// ---- 计时结束 -> 弹出力竭度 ----
function handleTimerEnd(_elapsedSeconds: number): void {
  exhaustionVisible.value = true
}

// ---- 力竭度提交 ----
async function handleExhaustionSubmit(score: number): Promise<void> {
  try {
    await endWorkout(currentRecordId.value, score)
    ElMessage.success('训练记录已保存')
    await loadDashboard()
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
}

// ---- 补打卡 ----
async function handleMakeupCompleted(): Promise<void> {
  await loadDashboard()
}

onMounted(() => {
  loadDashboard()
  loadMuscles()
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

    <!-- 肌群看板（双层入口） -->
    <MuscleDashboard
      :muscle-groups="dashboard?.muscleGroups || []"
      :muscles="allMuscles"
      :loading="loading"
      @select="handleMuscleSelect"
      @select-muscle="handleMuscleSelectDetail"
    />

    <!-- 动作选择弹窗 -->
    <ActionSelectDialog
      v-model:visible="actionSelectVisible"
      :muscle-group="selectedMuscleGroup"
      :muscle-name="selectedMuscleName"
      @start="handleActionStart"
    />

    <!-- 训练计时弹窗 -->
    <TrainingTimerDialog
      v-model:visible="timerVisible"
      :action-name="currentActionName"
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
  overflow-y: auto;
}

.top-bar {
  margin-bottom: 16px;
}

.top-bar-inner {
  display: flex;
  align-items: center;
  gap: 12px;
}

.top-tip {
  flex: 1;
}
</style>
