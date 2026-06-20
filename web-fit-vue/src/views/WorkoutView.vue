<script setup lang="ts">
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Search, Refresh, Check, Timer, Trophy, TrendCharts, Warning } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'

// API imports
import {
  type TrainingPlan,
  type TrainingPlanDetail,
  queryTrainingPlan,
  listAllTrainingPlan,
  getTrainingPlanDetails,
  createTrainingPlan,
  updateTrainingPlan,
  deleteTrainingPlan,
  addTrainingPlanDetail,
  updateTrainingPlanDetail,
  deleteTrainingPlanDetail
} from '@/api/trainingPlan'
import {
  type TrainingSession,
  type TrainingSessionDetail,
  queryTrainingSession,
  getTrainingSessionDetails,
  createTrainingSession,
  updateTrainingSession,
  deleteTrainingSession,
  addTrainingSessionDetail,
  updateTrainingSessionDetail,
  deleteTrainingSessionDetail
} from '@/api/trainingSession'
import {
  type BodyMetric,
  queryBodyMetric,
  listBodyMetric,
  createBodyMetric,
  updateBodyMetric,
  deleteBodyMetric
} from '@/api/bodyMetric'
import { type GymAction, listAllGymAction } from '@/api/gymAction'
import type { PageData } from '@/api/gymEquipment'

// 统计相关 API
import {
  calculate1RM,
  getBest1RM,
  getLastSession,
  getMuscleFatigue,
  getVolumeTrend,
  getContributionWall,
  getConsistencyRanking,
  getProgressRanking,
  detectPlateau,
  checkPr,
  compareStrength
} from '@/api/trainingStats'

// 新组件导入
import FatigueHeatmap from '@/components/workout/FatigueHeatmap.vue'
import BodyMetricTrend from '@/components/workout/BodyMetricTrend.vue'
import ContributionWall from '@/components/workout/ContributionWall.vue'
import VolumeTrendChart from '@/components/workout/VolumeTrendChart.vue'
import RankingBoard from '@/components/workout/RankingBoard.vue'
import RestTimer from '@/components/workout/RestTimer.vue'
import PrCelebration from '@/components/workout/PrCelebration.vue'
import PlateauWarning from '@/components/workout/PlateauWarning.vue'

// 肌群映射
const MUSCLE_GROUP_MAP: Record<string, string> = {
  CHEST: '胸部', BACK: '背部', SHOULDER: '肩部', ARM: '手臂',
  LEG: '腿部', GLUTE: '臀部', CORE: '核心', FULL_BODY: '全身'
}

// 训练感受映射
const FEELING_MAP: Record<number, string> = {
  1: '很差', 2: '较差', 3: '一般', 4: '较好', 5: '很好'
}

// 当前激活的标签页
const activeTab = ref('plan')

// 动作列表（用于下拉选择）
const actionList = ref<GymAction[]>([])
const actionMap = computed(() => {
  const map: Record<string, GymAction> = {}
  actionList.value.forEach(a => { map[a.id] = a })
  return map
})

// 计划映射（用于训练记录显示计划名称）
const planMap = computed(() => {
  const map: Record<string, TrainingPlan> = {}
  planList.value.forEach(p => { map[p.id] = p })
  return map
})

// ═══════════════════════════════════════════════════════════════
// TAB 1 — 训练计划 (Training Plan)
// ═══════════════════════════════════════════════════════════════
const planLoading = ref(false)
const planList = ref<TrainingPlan[]>([])
const planPage = reactive({ current: 1, size: 20, total: 0 })
const planFilters = reactive({ planName: '', muscleGroup: '' })

// 计划对话框
const planDialogVisible = ref(false)
const planDialogMode = ref<'add' | 'edit'>('add')
const planFormRef = ref<FormInstance>()
const planForm = reactive<Partial<TrainingPlan>>({
  planName: '',
  description: '',
  muscleGroup: '',
  sortNo: 0,
  status: 1
})
const planFormRules: FormRules = {
  planName: [{ required: true, message: '请输入计划名称', trigger: 'blur' }],
  muscleGroup: [{ required: true, message: '请选择目标肌群', trigger: 'change' }]
}
const planSaving = ref(false)

// 计划详情对话框
const planDetailDialogVisible = ref(false)
const currentPlan = ref<TrainingPlan | null>(null)
const planDetailList = ref<TrainingPlanDetail[]>([])
const planDetailLoading = ref(false)

// 计划详情编辑对话框
const planDetailEditDialogVisible = ref(false)
const planDetailEditFormRef = ref<FormInstance>()
const planDetailEditForm = reactive<Partial<TrainingPlanDetail>>({
  actionId: '',
  targetSets: 3,
  targetReps: 10,
  targetWeight: 0,
  restSeconds: 60,
  notes: ''
})
const planDetailEditId = ref<string | null>(null)
const planDetailEditMode = ref<'add' | 'edit'>('add')
const planDetailSaving = ref(false)

// 加载训练计划列表
async function loadPlans() {
  planLoading.value = true
  try {
    const res = await queryTrainingPlan({
      page: planPage.current,
      size: planPage.size,
      planName: planFilters.planName || undefined,
      muscleGroup: planFilters.muscleGroup || undefined
    })
    const pd: PageData<TrainingPlan> = res.data
    planList.value = pd.records
    planPage.total = pd.total
  } catch (e: any) {
    ElMessage.error(e?.message || '查询失败')
  } finally {
    planLoading.value = false
  }
}

// 打开新增计划对话框
function openAddPlan() {
  planDialogMode.value = 'add'
  Object.assign(planForm, {
    planName: '',
    description: '',
    muscleGroup: '',
    sortNo: 0,
    status: 1
  })
  planDialogVisible.value = true
  setTimeout(() => planFormRef.value?.resetFields(), 0)
}

// 打开编辑计划对话框
function openEditPlan(plan: TrainingPlan) {
  planDialogMode.value = 'edit'
  Object.assign(planForm, {
    planName: plan.planName,
    description: plan.description || '',
    muscleGroup: plan.muscleGroup || '',
    sortNo: plan.sortNo,
    status: plan.status
  })
  currentPlan.value = plan
  planDialogVisible.value = true
  setTimeout(() => planFormRef.value?.clearValidate(), 0)
}

// 保存计划
async function savePlan() {
  const ok = await planFormRef.value?.validate().catch(() => false)
  if (!ok) return
  planSaving.value = true
  try {
    if (planDialogMode.value === 'edit' && currentPlan.value) {
      await updateTrainingPlan(currentPlan.value.id, planForm)
      ElMessage.success('更新成功')
    } else {
      await createTrainingPlan(planForm)
      ElMessage.success('创建成功')
    }
    planDialogVisible.value = false
    loadPlans()
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  } finally {
    planSaving.value = false
  }
}

// 删除计划
async function deletePlan(plan: TrainingPlan) {
  try {
    await ElMessageBox.confirm(`确定删除训练计划「${plan.planName}」？`, '删除确认', { type: 'warning' })
    await deleteTrainingPlan(plan.id)
    ElMessage.success('删除成功')
    loadPlans()
  } catch { /* cancelled */ }
}

// 打开计划详情对话框
async function openPlanDetails(plan: TrainingPlan) {
  currentPlan.value = plan
  planDetailDialogVisible.value = true
  await loadPlanDetails(plan.id)
}

// 加载计划详情列表
async function loadPlanDetails(planId: string) {
  planDetailLoading.value = true
  try {
    const res = await getTrainingPlanDetails(planId)
    planDetailList.value = res.data || []
  } catch (e: any) {
    ElMessage.error(e?.message || '查询失败')
  } finally {
    planDetailLoading.value = false
  }
}

// 打开新增计划详情对话框
function openAddPlanDetail() {
  planDetailEditMode.value = 'add'
  planDetailEditId.value = null
  Object.assign(planDetailEditForm, {
    actionId: '',
    targetSets: 3,
    targetReps: 10,
    targetWeight: 0,
    restSeconds: 60,
    notes: ''
  })
  planDetailEditDialogVisible.value = true
  setTimeout(() => planDetailEditFormRef.value?.resetFields(), 0)
}

// 打开编辑计划详情对话框
function openEditPlanDetail(detail: TrainingPlanDetail) {
  planDetailEditMode.value = 'edit'
  planDetailEditId.value = detail.id
  Object.assign(planDetailEditForm, {
    actionId: detail.actionId,
    targetSets: detail.targetSets,
    targetReps: detail.targetReps,
    targetWeight: detail.targetWeight,
    restSeconds: detail.restSeconds,
    notes: detail.notes || ''
  })
  planDetailEditDialogVisible.value = true
  setTimeout(() => planDetailEditFormRef.value?.clearValidate(), 0)
}

// 保存计划详情
async function savePlanDetail() {
  const ok = await planDetailEditFormRef.value?.validate().catch(() => false)
  if (!ok) return
  planDetailSaving.value = true
  try {
    if (planDetailEditMode.value === 'edit' && planDetailEditId.value) {
      await updateTrainingPlanDetail(planDetailEditId.value, planDetailEditForm)
      ElMessage.success('更新成功')
    } else if (currentPlan.value) {
      await addTrainingPlanDetail(currentPlan.value.id, planDetailEditForm)
      ElMessage.success('添加成功')
    }
    planDetailEditDialogVisible.value = false
    if (currentPlan.value) {
      await loadPlanDetails(currentPlan.value.id)
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  } finally {
    planDetailSaving.value = false
  }
}

// 删除计划详情
async function deletePlanDetail(detail: TrainingPlanDetail) {
  try {
    await ElMessageBox.confirm('确定删除此动作？', '删除确认', { type: 'warning' })
    await deleteTrainingPlanDetail(detail.id)
    ElMessage.success('删除成功')
    if (currentPlan.value) {
      await loadPlanDetails(currentPlan.value.id)
    }
  } catch { /* cancelled */ }
}

// ═══════════════════════════════════════════════════════════════
// TAB 2 — 训练记录 (Training Session)
// ═══════════════════════════════════════════════════════════════
const sessionLoading = ref(false)
const sessionList = ref<TrainingSession[]>([])
const sessionPage = reactive({ current: 1, size: 20, total: 0 })
const sessionFilters = reactive({ startDate: '', endDate: '' })

// 训练记录对话框
const sessionDialogVisible = ref(false)
const sessionDialogMode = ref<'add' | 'edit'>('add')
const sessionFormRef = ref<FormInstance>()
const sessionForm = reactive<Partial<TrainingSession>>({
  planId: '',
  sessionDate: new Date().toISOString().split('T')[0],
  startTime: '',
  endTime: '',
  totalVolume: 0,
  totalDuration: 0,
  feeling: 3,
  notes: ''
})
const sessionFormRules: FormRules = {
  sessionDate: [{ required: true, message: '请选择训练日期', trigger: 'change' }]
}
const sessionSaving = ref(false)

// 训练记录详情对话框
const sessionDetailDialogVisible = ref(false)
const currentSession = ref<TrainingSession | null>(null)
const sessionDetailList = ref<TrainingSessionDetail[]>([])
const sessionDetailLoading = ref(false)

// 训练记录详情编辑对话框
const sessionDetailEditDialogVisible = ref(false)
const sessionDetailEditFormRef = ref<FormInstance>()
const sessionDetailEditForm = reactive<Partial<TrainingSessionDetail>>({
  actionId: '',
  setNo: 1,
  targetWeight: 0,
  targetReps: 10,
  actualWeight: 0,
  actualReps: 0,
  isCompleted: 0,
  notes: ''
})
const sessionDetailEditId = ref<string | null>(null)
const sessionDetailEditMode = ref<'add' | 'edit'>('add')
const sessionDetailSaving = ref(false)

// 加载训练记录列表
async function loadSessions() {
  sessionLoading.value = true
  try {
    const res = await queryTrainingSession({
      page: sessionPage.current,
      size: sessionPage.size,
      startDate: sessionFilters.startDate || undefined,
      endDate: sessionFilters.endDate || undefined
    })
    const pd: PageData<TrainingSession> = res.data
    sessionList.value = pd.records
    sessionPage.total = pd.total
  } catch (e: any) {
    ElMessage.error(e?.message || '查询失败')
  } finally {
    sessionLoading.value = false
  }
}

// 打开新增训练记录对话框
function openAddSession() {
  sessionDialogMode.value = 'add'
  Object.assign(sessionForm, {
    planId: '',
    sessionDate: new Date().toISOString().split('T')[0],
    startTime: '',
    endTime: '',
    totalVolume: 0,
    totalDuration: 0,
    feeling: 3,
    notes: ''
  })
  sessionDialogVisible.value = true
  setTimeout(() => sessionFormRef.value?.resetFields(), 0)
}

// 打开编辑训练记录对话框
function openEditSession(session: TrainingSession) {
  sessionDialogMode.value = 'edit'
  Object.assign(sessionForm, {
    planId: session.planId || '',
    sessionDate: session.sessionDate,
    startTime: session.startTime || '',
    endTime: session.endTime || '',
    totalVolume: session.totalVolume,
    totalDuration: session.totalDuration,
    feeling: session.feeling,
    notes: session.notes || ''
  })
  currentSession.value = session
  sessionDialogVisible.value = true
  setTimeout(() => sessionFormRef.value?.clearValidate(), 0)
}

// 保存训练记录
async function saveSession() {
  const ok = await sessionFormRef.value?.validate().catch(() => false)
  if (!ok) return
  sessionSaving.value = true
  try {
    if (sessionDialogMode.value === 'edit' && currentSession.value) {
      await updateTrainingSession(currentSession.value.id, sessionForm)
      ElMessage.success('更新成功')
    } else {
      await createTrainingSession(sessionForm)
      ElMessage.success('创建成功')
    }
    sessionDialogVisible.value = false
    loadSessions()
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  } finally {
    sessionSaving.value = false
  }
}

// 删除训练记录
async function deleteSession(session: TrainingSession) {
  try {
    await ElMessageBox.confirm(`确定删除此训练记录？`, '删除确认', { type: 'warning' })
    await deleteTrainingSession(session.id)
    ElMessage.success('删除成功')
    loadSessions()
  } catch { /* cancelled */ }
}

// 历史数据对照
const sessionDetailHistory = ref<Record<string, { weight: number; reps: number }>>({})

// 打开训练记录详情对话框
async function openSessionDetails(session: TrainingSession) {
  currentSession.value = session
  sessionDetailDialogVisible.value = true
  await loadSessionDetails(session.id)
}

// 加载训练记录详情列表
async function loadSessionDetails(sessionId: string) {
  sessionDetailLoading.value = true
  try {
    const res = await getTrainingSessionDetails(sessionId)
    sessionDetailList.value = res.data || []
    // 加载每个动作的历史数据
    await loadSessionDetailHistory()
  } catch (e: any) {
    ElMessage.error(e?.message || '查询失败')
  } finally {
    sessionDetailLoading.value = false
  }
}

// 加载历史数据对照
async function loadSessionDetailHistory() {
  sessionDetailHistory.value = {}
  for (const detail of sessionDetailList.value) {
    if (detail.actionId && !sessionDetailHistory.value[detail.actionId]) {
      try {
        const res = await getLastSession(detail.actionId)
        if (res.data) {
          sessionDetailHistory.value[detail.actionId] = {
            weight: res.data.actualWeight || res.data.targetWeight || 0,
            reps: res.data.actualReps || res.data.targetReps || 0
          }
        }
      } catch {
        // 忽略错误，可能是首次训练该动作
      }
    }
  }
}

// 计算历史差异百分比
function getHistoryDiff(current: TrainingSessionDetail, history: { weight: number; reps: number }): number {
  if (!history) return 0
  const currentVolume = (current.actualWeight || current.targetWeight || 0) * (current.actualReps || current.targetReps || 1)
  const historyVolume = history.weight * history.reps
  if (historyVolume === 0) return 0
  return Math.round(((currentVolume - historyVolume) / historyVolume) * 100)
}

// 打开新增训练记录详情对话框
function openAddSessionDetail() {
  sessionDetailEditMode.value = 'add'
  sessionDetailEditId.value = null
  Object.assign(sessionDetailEditForm, {
    actionId: '',
    setNo: 1,
    targetWeight: 0,
    targetReps: 10,
    actualWeight: 0,
    actualReps: 0,
    isCompleted: 0,
    notes: ''
  })
  sessionDetailEditDialogVisible.value = true
  setTimeout(() => sessionDetailEditFormRef.value?.resetFields(), 0)
}

// 打开编辑训练记录详情对话框
function openEditSessionDetail(detail: TrainingSessionDetail) {
  sessionDetailEditMode.value = 'edit'
  sessionDetailEditId.value = detail.id
  Object.assign(sessionDetailEditForm, {
    actionId: detail.actionId,
    setNo: detail.setNo,
    targetWeight: detail.targetWeight,
    targetReps: detail.targetReps,
    actualWeight: detail.actualWeight,
    actualReps: detail.actualReps,
    isCompleted: detail.isCompleted,
    notes: detail.notes || ''
  })
  sessionDetailEditDialogVisible.value = true
  setTimeout(() => sessionDetailEditFormRef.value?.clearValidate(), 0)
}

// 保存训练记录详情
async function saveSessionDetail() {
  const ok = await sessionDetailEditFormRef.value?.validate().catch(() => false)
  if (!ok) return
  sessionDetailSaving.value = true
  try {
    if (sessionDetailEditMode.value === 'edit' && sessionDetailEditId.value) {
      await updateTrainingSessionDetail(sessionDetailEditId.value, sessionDetailEditForm)
      ElMessage.success('更新成功')
    } else if (currentSession.value) {
      await addTrainingSessionDetail(currentSession.value.id, sessionDetailEditForm)
      ElMessage.success('添加成功')
    }
    sessionDetailEditDialogVisible.value = false
    if (currentSession.value) {
      await loadSessionDetails(currentSession.value.id)
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  } finally {
    sessionDetailSaving.value = false
  }
}

// 删除训练记录详情
async function deleteSessionDetail(detail: TrainingSessionDetail) {
  try {
    await ElMessageBox.confirm('确定删除此组数据？', '删除确认', { type: 'warning' })
    await deleteTrainingSessionDetail(detail.id)
    ElMessage.success('删除成功')
    if (currentSession.value) {
      await loadSessionDetails(currentSession.value.id)
    }
  } catch { /* cancelled */ }
}

// 切换完成状态
async function toggleCompleted(detail: TrainingSessionDetail) {
  const newStatus = detail.isCompleted === 1 ? 0 : 1
  try {
    await updateTrainingSessionDetail(detail.id, { isCompleted: newStatus })
    detail.isCompleted = newStatus
    ElMessage.success(newStatus === 1 ? '已标记完成' : '已取消完成')
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  }
}

// ═══════════════════════════════════════════════════════════════
// TAB 3 — 身体指标 (Body Metric)
// ═══════════════════════════════════════════════════════════════
const metricLoading = ref(false)
const metricList = ref<BodyMetric[]>([])
const metricPage = reactive({ current: 1, size: 20, total: 0 })
const metricFilters = reactive({ startDate: '', endDate: '' })

// 身体指标对话框
const metricDialogVisible = ref(false)
const metricDialogMode = ref<'add' | 'edit'>('add')
const metricFormRef = ref<FormInstance>()
const metricForm = reactive<Partial<BodyMetric>>({
  metricDate: new Date().toISOString().split('T')[0],
  weight: undefined,
  bodyFat: undefined,
  bmi: undefined,
  muscleMass: undefined,
  notes: ''
})
const metricFormRules: FormRules = {
  metricDate: [{ required: true, message: '请选择记录日期', trigger: 'change' }]
}
const metricSaving = ref(false)

// 加载身体指标列表
async function loadMetrics() {
  metricLoading.value = true
  try {
    const res = await queryBodyMetric({
      page: metricPage.current,
      size: metricPage.size,
      startDate: metricFilters.startDate || undefined,
      endDate: metricFilters.endDate || undefined
    })
    const pd: PageData<BodyMetric> = res.data
    metricList.value = pd.records
    metricPage.total = pd.total
  } catch (e: any) {
    ElMessage.error(e?.message || '查询失败')
  } finally {
    metricLoading.value = false
  }
}

// 打开新增身体指标对话框
function openAddMetric() {
  metricDialogMode.value = 'add'
  Object.assign(metricForm, {
    metricDate: new Date().toISOString().split('T')[0],
    weight: undefined,
    bodyFat: undefined,
    bmi: undefined,
    muscleMass: undefined,
    notes: ''
  })
  metricDialogVisible.value = true
  setTimeout(() => metricFormRef.value?.resetFields(), 0)
}

// 打开编辑身体指标对话框
function openEditMetric(metric: BodyMetric) {
  metricDialogMode.value = 'edit'
  Object.assign(metricForm, {
    metricDate: metric.metricDate,
    weight: metric.weight,
    bodyFat: metric.bodyFat,
    bmi: metric.bmi,
    muscleMass: metric.muscleMass,
    notes: metric.notes || ''
  })
  metricDialogVisible.value = true
  setTimeout(() => metricFormRef.value?.clearValidate(), 0)
}

// 保存身体指标
async function saveMetric() {
  const ok = await metricFormRef.value?.validate().catch(() => false)
  if (!ok) return
  metricSaving.value = true
  try {
    if (metricDialogMode.value === 'edit') {
      await updateBodyMetric(metricForm.id!, metricForm)
      ElMessage.success('更新成功')
    } else {
      await createBodyMetric(metricForm)
      ElMessage.success('创建成功')
    }
    metricDialogVisible.value = false
    loadMetrics()
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  } finally {
    metricSaving.value = false
  }
}

// 删除身体指标
async function deleteMetric(metric: BodyMetric) {
  try {
    await ElMessageBox.confirm(`确定删除此记录？`, '删除确认', { type: 'warning' })
    await deleteBodyMetric(metric.id)
    ElMessage.success('删除成功')
    loadMetrics()
  } catch { /* cancelled */ }
}

// 计算BMI
function calculateBmi() {
  if (metricForm.weight && metricForm.bmi) {
    // BMI = 体重(kg) / 身高(m)²
    // 这里假设用户输入的是体重，需要身高才能计算BMI
    // 暂时不实现，因为数据库没有身高字段
  }
}

// ═══════════════════════════════════════════════════════════════
// PR 庆祝弹窗
// ═══════════════════════════════════════════════════════════════
const prCelebrationVisible = ref(false)
const prCelebrationData = ref<any>(null)

// 处理肌群点击（用于智能筛选训练计划）
const handleMuscleClick = (muscleGroup: string) => {
  planFilters.muscleGroup = muscleGroup
  activeTab.value = 'plan'
  loadPlans()
}

// 处理 PR 同步到计划
const handlePrSyncPlan = () => {
  ElMessage.success('已同步到训练计划')
  prCelebrationVisible.value = false
}

// 处理 PR 仅记录
const handlePrJustRecord = () => {
  ElMessage.success('已记录个人最佳')
  prCelebrationVisible.value = false
}

// ═══════════════════════════════════════════════════════════════
// 初始化
// ═══════════════════════════════════════════════════════════════
onMounted(async () => {
  // 加载动作列表
  try {
    const res = await listAllGymAction()
    actionList.value = res.data || []
  } catch (e: any) {
    console.error('加载动作列表失败', e)
  }
  // 加载各标签页数据
  loadPlans()
  loadSessions()
  loadMetrics()
})
</script>

<template>
  <div class="workout-container">
    <el-tabs v-model="activeTab" type="border-card" class="workout-tabs">
      <!-- ═══ TAB 1: 训练计划 ═══ -->
      <el-tab-pane name="plan">
        <template #label>
          <span>📋 训练计划</span>
        </template>
        <!-- 筛选工具栏 -->
        <div class="tab-toolbar">
          <el-input v-model="planFilters.planName" placeholder="搜索计划名称..." clearable style="width:200px" @keyup.enter="loadPlans" />
          <el-select v-model="planFilters.muscleGroup" placeholder="目标肌群" clearable style="width:140px" @change="loadPlans">
            <el-option v-for="(label, value) in MUSCLE_GROUP_MAP" :key="value" :label="label" :value="value" />
          </el-select>
          <el-button type="primary" :icon="Search" @click="loadPlans">查询</el-button>
          <el-button :icon="Refresh" @click="planFilters.planName='';planFilters.muscleGroup='';loadPlans()">重置</el-button>
          <div style="flex:1" />
          <el-button type="primary" :icon="Plus" @click="openAddPlan">新增计划</el-button>
        </div>
        <!-- 计划列表 -->
        <el-table :data="planList" v-loading="planLoading" stripe border size="small" max-height="calc(100vh - 380px)">
          <el-table-column prop="planName" label="计划名称" width="180" show-overflow-tooltip />
          <el-table-column prop="muscleGroup" label="目标肌群" width="100" align="center">
            <template #default="{row}">
              <el-tag size="small" type="success">{{ MUSCLE_GROUP_MAP[row.muscleGroup] || row.muscleGroup }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="描述" width="200" show-overflow-tooltip />
          <el-table-column prop="sortNo" label="排序" width="70" align="center" />
          <el-table-column prop="status" label="状态" width="70" align="center">
            <template #default="{row}">{{ row.status === 1 ? '启用' : '禁用' }}</template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="160" />
          <el-table-column label="操作" width="200" align="center" fixed="right">
            <template #default="{row}">
              <el-button type="primary" link size="small" @click="openPlanDetails(row)">详情</el-button>
              <el-button type="primary" link :icon="Edit" size="small" @click="openEditPlan(row)">编辑</el-button>
              <el-button type="danger" link :icon="Delete" size="small" @click="deletePlan(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="tab-pagination" v-if="planPage.total > 0">
          <el-pagination v-model:current-page="planPage.current" v-model:page-size="planPage.size" :total="planPage.total" :page-sizes="[10,20,50]" layout="total,sizes,prev,pager,next" background size="small" @current-change="loadPlans" @size-change="loadPlans" />
        </div>
      </el-tab-pane>

      <!-- ═══ TAB 2: 训练记录 ═══ -->
      <el-tab-pane name="session">
        <template #label>
          <span>🏋️ 训练记录</span>
        </template>
        <!-- 筛选工具栏 -->
        <div class="tab-toolbar">
          <el-date-picker v-model="sessionFilters.startDate" type="date" placeholder="开始日期" value-format="YYYY-MM-DD" style="width:140px" @change="loadSessions" />
          <el-date-picker v-model="sessionFilters.endDate" type="date" placeholder="结束日期" value-format="YYYY-MM-DD" style="width:140px" @change="loadSessions" />
          <el-button type="primary" :icon="Search" @click="loadSessions">查询</el-button>
          <el-button :icon="Refresh" @click="sessionFilters.startDate='';sessionFilters.endDate='';loadSessions()">重置</el-button>
          <div style="flex:1" />
          <el-button type="primary" :icon="Plus" @click="openAddSession">新增记录</el-button>
        </div>
        <!-- 记录列表 -->
        <el-table :data="sessionList" v-loading="sessionLoading" stripe border size="small" max-height="calc(100vh - 380px)">
          <el-table-column prop="sessionDate" label="训练日期" width="120" />
          <el-table-column prop="planId" label="训练计划" width="150" show-overflow-tooltip>
            <template #default="{row}">
              <span v-if="row.planId">{{ planMap[row.planId]?.planName || '已删除计划' }}</span>
              <span v-else style="color:#999">自由训练</span>
            </template>
          </el-table-column>
          <el-table-column prop="totalVolume" label="总容量(kg)" width="110" align="center" />
          <el-table-column prop="totalDuration" label="时长(分钟)" width="110" align="center">
            <template #default="{row}">{{ Math.round((row.totalDuration || 0) / 60) }}</template>
          </el-table-column>
          <el-table-column prop="feeling" label="感受" width="80" align="center">
            <template #default="{row}">
              <el-tag :type="row.feeling >= 4 ? 'success' : row.feeling === 3 ? 'warning' : 'danger'" size="small">
                {{ FEELING_MAP[row.feeling] || '-' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="notes" label="备注" width="150" show-overflow-tooltip />
          <el-table-column label="操作" width="180" align="center" fixed="right">
            <template #default="{row}">
              <el-button type="primary" link size="small" @click="openSessionDetails(row)">详情</el-button>
              <el-button type="primary" link :icon="Edit" size="small" @click="openEditSession(row)">编辑</el-button>
              <el-button type="danger" link :icon="Delete" size="small" @click="deleteSession(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="tab-pagination" v-if="sessionPage.total > 0">
          <el-pagination v-model:current-page="sessionPage.current" v-model:page-size="sessionPage.size" :total="sessionPage.total" :page-sizes="[10,20,50]" layout="total,sizes,prev,pager,next" background size="small" @current-change="loadSessions" @size-change="loadSessions" />
        </div>
      </el-tab-pane>

      <!-- ═══ TAB 3: 身体指标 ═══ -->
      <el-tab-pane name="metric">
        <template #label>
          <span>📊 身体指标</span>
        </template>
        <!-- 筛选工具栏 -->
        <div class="tab-toolbar">
          <el-date-picker v-model="metricFilters.startDate" type="date" placeholder="开始日期" value-format="YYYY-MM-DD" style="width:140px" @change="loadMetrics" />
          <el-date-picker v-model="metricFilters.endDate" type="date" placeholder="结束日期" value-format="YYYY-MM-DD" style="width:140px" @change="loadMetrics" />
          <el-button type="primary" :icon="Search" @click="loadMetrics">查询</el-button>
          <el-button :icon="Refresh" @click="metricFilters.startDate='';metricFilters.endDate='';loadMetrics()">重置</el-button>
          <div style="flex:1" />
          <el-button type="primary" :icon="Plus" @click="openAddMetric">新增记录</el-button>
        </div>
        <!-- 指标列表 -->
        <el-table :data="metricList" v-loading="metricLoading" stripe border size="small" max-height="calc(100vh - 380px)">
          <el-table-column prop="metricDate" label="记录日期" width="120" />
          <el-table-column prop="weight" label="体重(kg)" width="100" align="center" />
          <el-table-column prop="bodyFat" label="体脂率(%)" width="100" align="center" />
          <el-table-column prop="bmi" label="BMI" width="80" align="center" />
          <el-table-column prop="muscleMass" label="肌肉量(kg)" width="110" align="center" />
          <el-table-column prop="notes" label="备注" width="200" show-overflow-tooltip />
          <el-table-column prop="createTime" label="创建时间" width="160" />
          <el-table-column label="操作" width="140" align="center" fixed="right">
            <template #default="{row}">
              <el-button type="primary" link :icon="Edit" size="small" @click="openEditMetric(row)">编辑</el-button>
              <el-button type="danger" link :icon="Delete" size="small" @click="deleteMetric(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="tab-pagination" v-if="metricPage.total > 0">
          <el-pagination v-model:current-page="metricPage.current" v-model:page-size="metricPage.size" :total="metricPage.total" :page-sizes="[10,20,50]" layout="total,sizes,prev,pager,next" background size="small" @current-change="loadMetrics" @size-change="loadMetrics" />
        </div>
      </el-tab-pane>

      <!-- ═══ TAB 4: 数据看板 ═══ -->
      <el-tab-pane name="dashboard">
        <template #label>
          <span>📈 数据看板</span>
        </template>
        <div class="dashboard-grid">
          <!-- 疲劳热力图 -->
          <div class="dashboard-card">
            <h3 class="card-title">肌群疲劳监控</h3>
            <FatigueHeatmap @muscle-click="handleMuscleClick" />
          </div>
          <!-- 身体指标趋势 -->
          <div class="dashboard-card">
            <h3 class="card-title">身体指标趋势</h3>
            <BodyMetricTrend />
          </div>
        </div>
      </el-tab-pane>

      <!-- ═══ TAB 5: 训练分析 ═══ -->
      <el-tab-pane name="analytics">
        <template #label>
          <span>📊 训练分析</span>
        </template>
        <div class="analytics-grid">
          <!-- 容量趋势图 -->
          <div class="analytics-card full-width">
            <h3 class="card-title">训练容量趋势</h3>
            <VolumeTrendChart />
          </div>
          <!-- 贡献墙 -->
          <div class="analytics-card full-width">
            <h3 class="card-title">训练贡献墙</h3>
            <ContributionWall />
          </div>
          <!-- 平台期预警 -->
          <div class="analytics-card full-width">
            <PlateauWarning />
          </div>
        </div>
      </el-tab-pane>

      <!-- ═══ TAB 6: 排行榜 ═══ -->
      <el-tab-pane name="ranking">
        <template #label>
          <span>🏆 排行榜</span>
        </template>
        <RankingBoard />
      </el-tab-pane>

      <!-- ═══ TAB 7: 计时器 ═══ -->
      <el-tab-pane name="timer">
        <template #label>
          <span>⏱️ 计时器</span>
        </template>
        <div class="timer-container">
          <RestTimer />
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- PR 庆祝弹窗 -->
    <PrCelebration
      v-model:visible="prCelebrationVisible"
      :pr-data="prCelebrationData"
      @sync-plan="handlePrSyncPlan"
      @just-record="handlePrJustRecord"
    />

    <!-- ═══════════════════════════════════════════════════════════ -->
    <!-- DIALOGS -->
    <!-- ═══════════════════════════════════════════════════════════ -->

    <!-- 训练计划对话框 -->
    <el-dialog v-model="planDialogVisible" :title="planDialogMode === 'add' ? '新增训练计划' : '编辑训练计划'" width="500px" destroy-on-close>
      <el-form ref="planFormRef" :model="planForm" :rules="planFormRules" label-width="90px">
        <el-form-item label="计划名称" prop="planName">
          <el-input v-model="planForm.planName" placeholder="如：推日计划、腿日计划" />
        </el-form-item>
        <el-form-item label="目标肌群" prop="muscleGroup">
          <el-select v-model="planForm.muscleGroup" style="width:100%">
            <el-option v-for="(label, value) in MUSCLE_GROUP_MAP" :key="value" :label="label" :value="value" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="planForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="排序号">
          <el-input-number v-model="planForm.sortNo" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="planForm.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="planDialogVisible=false">取消</el-button>
        <el-button type="primary" :loading="planSaving" @click="savePlan">{{ planDialogMode === 'add' ? '创建' : '更新' }}</el-button>
      </template>
    </el-dialog>

    <!-- 训练计划详情对话框 -->
    <el-dialog v-model="planDetailDialogVisible" :title="`计划详情 - ${currentPlan?.planName || ''}`" width="800px" destroy-on-close>
      <div class="plan-detail-toolbar">
        <el-button type="primary" :icon="Plus" size="small" @click="openAddPlanDetail">添加动作</el-button>
      </div>
      <el-table :data="planDetailList" v-loading="planDetailLoading" stripe border size="small" max-height="400px">
        <el-table-column prop="sortNo" label="序号" width="60" align="center" />
        <el-table-column label="动作名称" width="150">
          <template #default="{row}">{{ actionMap[row.actionId]?.name || '未知动作' }}</template>
        </el-table-column>
        <el-table-column prop="targetSets" label="目标组数" width="90" align="center" />
        <el-table-column prop="targetReps" label="目标次数" width="90" align="center" />
        <el-table-column prop="targetWeight" label="目标重量(kg)" width="110" align="center" />
        <el-table-column prop="restSeconds" label="休息(秒)" width="90" align="center" />
        <el-table-column prop="notes" label="备注" width="150" show-overflow-tooltip />
        <el-table-column label="操作" width="120" align="center">
          <template #default="{row}">
            <el-button type="primary" link :icon="Edit" size="small" @click="openEditPlanDetail(row)">编辑</el-button>
            <el-button type="danger" link :icon="Delete" size="small" @click="deletePlanDetail(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 训练计划详情编辑对话框 -->
    <el-dialog v-model="planDetailEditDialogVisible" :title="planDetailEditMode === 'add' ? '添加动作' : '编辑动作'" width="500px" destroy-on-close>
      <el-form ref="planDetailEditFormRef" :model="planDetailEditForm" label-width="90px">
        <el-form-item label="动作" prop="actionId">
          <el-select v-model="planDetailEditForm.actionId" style="width:100%" filterable placeholder="请选择动作">
            <el-option v-for="action in actionList" :key="action.id" :label="action.name" :value="action.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标组数">
          <el-input-number v-model="planDetailEditForm.targetSets" :min="1" :max="20" />
        </el-form-item>
        <el-form-item label="目标次数">
          <el-input-number v-model="planDetailEditForm.targetReps" :min="1" :max="100" />
        </el-form-item>
        <el-form-item label="目标重量(kg)">
          <el-input-number v-model="planDetailEditForm.targetWeight" :min="0" :step="2.5" :precision="2" />
        </el-form-item>
        <el-form-item label="休息(秒)">
          <el-input-number v-model="planDetailEditForm.restSeconds" :min="0" :max="600" :step="10" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="planDetailEditForm.notes" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="planDetailEditDialogVisible=false">取消</el-button>
        <el-button type="primary" :loading="planDetailSaving" @click="savePlanDetail">{{ planDetailEditMode === 'add' ? '添加' : '更新' }}</el-button>
      </template>
    </el-dialog>

    <!-- 训练记录对话框 -->
    <el-dialog v-model="sessionDialogVisible" :title="sessionDialogMode === 'add' ? '新增训练记录' : '编辑训练记录'" width="550px" destroy-on-close>
      <el-form ref="sessionFormRef" :model="sessionForm" :rules="sessionFormRules" label-width="100px">
        <el-form-item label="训练日期" prop="sessionDate">
          <el-date-picker v-model="sessionForm.sessionDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="训练计划">
          <el-select v-model="sessionForm.planId" style="width:100%" clearable placeholder="可选择训练计划或自由训练">
            <el-option v-for="plan in planList" :key="plan.id" :label="plan.planName" :value="plan.id" />
          </el-select>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="开始时间">
              <el-time-picker v-model="sessionForm.startTime" value-format="HH:mm:ss" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束时间">
              <el-time-picker v-model="sessionForm.endTime" value-format="HH:mm:ss" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="总容量(kg)">
              <el-input-number v-model="sessionForm.totalVolume" :min="0" :step="10" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="训练感受">
              <el-select v-model="sessionForm.feeling" style="width:100%">
                <el-option v-for="(label, value) in FEELING_MAP" :key="value" :label="label" :value="Number(value)" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="sessionForm.notes" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="sessionDialogVisible=false">取消</el-button>
        <el-button type="primary" :loading="sessionSaving" @click="saveSession">{{ sessionDialogMode === 'add' ? '创建' : '更新' }}</el-button>
      </template>
    </el-dialog>

    <!-- 训练记录详情对话框 -->
    <el-dialog v-model="sessionDetailDialogVisible" :title="`训练详情 - ${currentSession?.sessionDate || ''}`" width="1000px" destroy-on-close>
      <div class="session-detail-toolbar">
        <el-button type="primary" :icon="Plus" size="small" @click="openAddSessionDetail">添加动作</el-button>
      </div>
      <el-table :data="sessionDetailList" v-loading="sessionDetailLoading" stripe border size="small" max-height="400px">
        <el-table-column prop="sortNo" label="序号" width="60" align="center" />
        <el-table-column label="动作名称" width="120">
          <template #default="{row}">{{ actionMap[row.actionId]?.name || '未知动作' }}</template>
        </el-table-column>
        <el-table-column prop="setNo" label="组号" width="60" align="center" />
        <el-table-column prop="targetWeight" label="目标(kg)" width="90" align="center" />
        <el-table-column prop="targetReps" label="目标次数" width="90" align="center" />
        <el-table-column prop="actualWeight" label="实际(kg)" width="90" align="center" />
        <el-table-column prop="actualReps" label="实际次数" width="90" align="center" />
        <!-- 历史数据对照列 -->
        <el-table-column label="历史对照" width="140" align="center">
          <template #default="{row}">
            <div v-if="sessionDetailHistory[row.actionId]" class="history-compare">
              <span class="history-data">
                上次: {{ sessionDetailHistory[row.actionId].weight }}kg × {{ sessionDetailHistory[row.actionId].reps }}
              </span>
              <span v-if="getHistoryDiff(row, sessionDetailHistory[row.actionId]) > 0" class="diff-up">
                ↑{{ getHistoryDiff(row, sessionDetailHistory[row.actionId]) }}%
              </span>
              <span v-else-if="getHistoryDiff(row, sessionDetailHistory[row.actionId]) < 0" class="diff-down">
                ↓{{ Math.abs(getHistoryDiff(row, sessionDetailHistory[row.actionId])) }}%
              </span>
              <span v-else class="diff-same">→</span>
            </div>
            <span v-else style="color:#999">首次</span>
          </template>
        </el-table-column>
        <el-table-column prop="isCompleted" label="完成" width="70" align="center">
          <template #default="{row}">
            <el-button :type="row.isCompleted === 1 ? 'success' : 'info'" size="small" :icon="Check" circle @click="toggleCompleted(row)" />
          </template>
        </el-table-column>
        <el-table-column prop="isPr" label="PR" width="60" align="center">
          <template #default="{row}">
            <el-tag v-if="row.isPr === 1" type="danger" size="small">PR</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
          <template #default="{row}">
            <el-button type="primary" link :icon="Edit" size="small" @click="openEditSessionDetail(row)">编辑</el-button>
            <el-button type="danger" link :icon="Delete" size="small" @click="deleteSessionDetail(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 训练记录详情编辑对话框 -->
    <el-dialog v-model="sessionDetailEditDialogVisible" :title="sessionDetailEditMode === 'add' ? '添加动作' : '编辑动作'" width="500px" destroy-on-close>
      <el-form ref="sessionDetailEditFormRef" :model="sessionDetailEditForm" label-width="90px">
        <el-form-item label="动作" prop="actionId">
          <el-select v-model="sessionDetailEditForm.actionId" style="width:100%" filterable placeholder="请选择动作">
            <el-option v-for="action in actionList" :key="action.id" :label="action.name" :value="action.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="组号">
          <el-input-number v-model="sessionDetailEditForm.setNo" :min="1" :max="20" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="目标重量">
              <el-input-number v-model="sessionDetailEditForm.targetWeight" :min="0" :step="2.5" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="目标次数">
              <el-input-number v-model="sessionDetailEditForm.targetReps" :min="1" :max="100" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="实际重量">
              <el-input-number v-model="sessionDetailEditForm.actualWeight" :min="0" :step="2.5" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="实际次数">
              <el-input-number v-model="sessionDetailEditForm.actualReps" :min="0" :max="100" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="是否完成">
          <el-switch v-model="sessionDetailEditForm.isCompleted" :active-value="1" :inactive-value="0" active-text="是" inactive-text="否" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="sessionDetailEditForm.notes" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="sessionDetailEditDialogVisible=false">取消</el-button>
        <el-button type="primary" :loading="sessionDetailSaving" @click="saveSessionDetail">{{ sessionDetailEditMode === 'add' ? '添加' : '更新' }}</el-button>
      </template>
    </el-dialog>

    <!-- 身体指标对话框 -->
    <el-dialog v-model="metricDialogVisible" :title="metricDialogMode === 'add' ? '新增身体指标' : '编辑身体指标'" width="500px" destroy-on-close>
      <el-form ref="metricFormRef" :model="metricForm" :rules="metricFormRules" label-width="100px">
        <el-form-item label="记录日期" prop="metricDate">
          <el-date-picker v-model="metricForm.metricDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="体重(kg)">
              <el-input-number v-model="metricForm.weight" :min="30" :max="200" :step="0.1" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="体脂率(%)">
              <el-input-number v-model="metricForm.bodyFat" :min="5" :max="50" :step="0.1" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="BMI">
              <el-input-number v-model="metricForm.bmi" :min="10" :max="50" :step="0.1" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="肌肉量(kg)">
              <el-input-number v-model="metricForm.muscleMass" :min="10" :max="100" :step="0.1" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="metricForm.notes" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="metricDialogVisible=false">取消</el-button>
        <el-button type="primary" :loading="metricSaving" @click="saveMetric">{{ metricDialogMode === 'add' ? '创建' : '更新' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.workout-container { padding: 20px; }
.workout-tabs { box-shadow: none; }
.workout-tabs :deep(.el-tabs__header) { margin-bottom: 0; background: #fff; }
.workout-tabs :deep(.el-tabs__content) { padding: 16px 20px; }
.tab-toolbar { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; margin-bottom: 14px; }
.tab-pagination { display: flex; justify-content: flex-end; margin-top: 12px; }
.plan-detail-toolbar, .session-detail-toolbar { margin-bottom: 12px; }

/* Dashboard & Analytics Layout */
.dashboard-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}
.dashboard-card, .analytics-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 16px;
}
.card-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 16px;
  color: #303133;
}
.analytics-grid {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.analytics-card.full-width {
  width: 100%;
}
.timer-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
}

/* History Comparison Styles */
.history-compare {
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: 12px;
}
.history-data {
  color: #606266;
}
.diff-up {
  color: #67c23a;
  font-weight: bold;
}
.diff-down {
  color: #f56c6c;
  font-weight: bold;
}
.diff-same {
  color: #909399;
}
</style>
