<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Edit, Delete } from '@element-plus/icons-vue'

// ── API imports ──
import { type GymAction, queryGymAction, listAllGymAction, createGymAction, updateGymAction, deleteGymAction } from '@/api/gymAction'
import { type GymMuscle, queryGymMuscle, listAllGymMuscle, createGymMuscle, updateGymMuscle, deleteGymMuscle } from '@/api/gymMuscle'
import { type GymEquipment, queryGymEquipment, listAllGymEquipment, createGymEquipment, updateGymEquipment, deleteGymEquipment } from '@/api/gymEquipment'
import { type GymActionMuscleRel, queryGymActionMuscleRel, createGymActionMuscleRel, deleteGymActionMuscleRel } from '@/api/gymActionMuscleRel'
import { type GymActionEquipmentRel, queryGymActionEquipmentRel, createGymActionEquipmentRel, deleteGymActionEquipmentRel } from '@/api/gymActionEquipmentRel'
import { type GymActionRecommendation, queryGymActionRecommendation, createGymActionRecommendation, updateGymActionRecommendation, deleteGymActionRecommendation } from '@/api/gymActionRecommendation'
import type { PageData } from '@/api/gymEquipment'

// ── Enum Maps ──
const ACTION_TYPE_MAP: Record<string, string> = {
  COMPOUND: '复合动作', ISOLATION: '孤立动作', CARDIO: '有氧训练',
  STRETCH: '拉伸动作', MOBILITY: '灵活性训练', PLYOMETRIC: '爆发力训练',
}
const MOVEMENT_PATTERN_MAP: Record<string, string> = {
  PUSH: '推', PULL: '拉', SQUAT: '深蹲', HINGE: '髋铰链', LUNGE: '弓步',
  CARRY: '搬运', ROTATION: '旋转', CORE: '核心稳定', CARDIO: '有氧',
}
const DIFFICULTY_MAP: Record<number, string> = { 1: '初学者', 2: '中级', 3: '进阶高级' }
const EQUIPMENT_TYPE_MAP: Record<string, string> = {
  FREE_WEIGHT: '自由力量', MACHINE: '固定器械', CABLE: '绳索器械',
  BODY_WEIGHT: '自重训练', CARDIO_EQUIPMENT: '有氧器械', FUNCTIONAL: '功能训练器械',
}
const MUSCLE_GROUP_MAP: Record<string, string> = {
  CHEST: '胸部', BACK: '背部', SHOULDER: '肩部', ARM: '手臂',
  LEG: '腿部', GLUTE: '臀部', CORE: '核心', FULL_BODY: '全身',
}
const TRAINING_GOAL_MAP: Record<string, string> = {
  HYPERTROPHY: '增肌', FAT_LOSS: '减脂', STRENGTH: '力量', ENDURANCE: '耐力',
}

function enumOptions(map: Record<string, string>) {
  return Object.entries(map).map(([value, label]) => ({ value, label }))
}

// ── Active Tab ──
const activeTab = ref('action')

// ═══════════════════════════════════════════════════════════════
// TAB 1 — 动作库 (GymAction)
// ═══════════════════════════════════════════════════════════════
const aLoading = ref(false)
const aData = ref<GymAction[]>([])
const aPage = reactive({ current: 1, size: 20, total: 0 })
const aFilters = reactive({ name: '', actionType: '', movementPattern: '', difficultyLevel: undefined as number | undefined })
const aDialog = ref(false)
const aEditing = ref(false)
const aForm = reactive<Partial<GymAction>>({ name: '', alias: '', pinyinBref: '', actionType: '', movementPattern: '', difficultyLevel: 1, imageUrls: '', videoUrl: '', actionGuide: '', safetyTips: '', searchKeywords: '', isCommon: 1, status: 1 })
const aEditId = ref<string | null>(null)
const aFormRef = ref()
const aSaving = ref(false)

async function loadActions() {
  aLoading.value = true
  try {
    const params: any = { page: aPage.current, size: aPage.size }
    if (aFilters.name) params.name = aFilters.name
    if (aFilters.actionType) params.actionType = aFilters.actionType
    if (aFilters.movementPattern) params.movementPattern = aFilters.movementPattern
    if (aFilters.difficultyLevel) params.difficultyLevel = aFilters.difficultyLevel
    const res = await queryGymAction(params)
    const pd: PageData<GymAction> = res.data
    aData.value = pd.records; aPage.total = pd.total
  } catch (e: any) { ElMessage.error(e?.message || '查询失败') }
  finally { aLoading.value = false }
}
function openAddAction() {
  aEditing.value = false; aEditId.value = null
  Object.assign(aForm, { name: '', alias: '', pinyinBref: '', actionType: '', movementPattern: '', difficultyLevel: 1, imageUrls: '', videoUrl: '', actionGuide: '', safetyTips: '', searchKeywords: '', isCommon: 1, status: 1 })
  aDialog.value = true; setTimeout(() => aFormRef.value?.resetFields(), 0)
}
function openEditAction(row: GymAction) {
  aEditing.value = true; aEditId.value = row.id
  Object.assign(aForm, { name: row.name, alias: row.alias || '', pinyinBref: row.pinyinBref || '', actionType: row.actionType, movementPattern: row.movementPattern, difficultyLevel: row.difficultyLevel, imageUrls: row.imageUrls || '', videoUrl: row.videoUrl || '', actionGuide: row.actionGuide || '', safetyTips: row.safetyTips || '', searchKeywords: row.searchKeywords || '', isCommon: row.isCommon, status: row.status })
  aDialog.value = true; setTimeout(() => aFormRef.value?.clearValidate(), 0)
}
async function saveAction() {
  const ok = await aFormRef.value?.validate().catch(() => false); if (!ok) return
  aSaving.value = true
  try {
    if (aEditing.value && aEditId.value) { await updateGymAction(aEditId.value, aForm); ElMessage.success('更新成功') }
    else { await createGymAction(aForm); ElMessage.success('创建成功') }
    aDialog.value = false; loadActions()
  } catch (e: any) { ElMessage.error(e?.message || '操作失败') }
  finally { aSaving.value = false }
}
async function deleteAction(row: GymAction) {
  try {
    await ElMessageBox.confirm(`确定删除动作「${row.name}」？`, '删除确认', { type: 'warning' })
    await deleteGymAction(row.id); ElMessage.success('删除成功'); loadActions()
  } catch { /* cancelled */ }
}

// ═══════════════════════════════════════════════════════════════
// TAB 2 — 肌群字典 (GymMuscle)
// ═══════════════════════════════════════════════════════════════
const mLoading = ref(false)
const mData = ref<GymMuscle[]>([])
const mPage = reactive({ current: 1, size: 50, total: 0 })
const mFilters = reactive({ muscleName: '', muscleGroup: '' })
const mDialog = ref(false)
const mEditing = ref(false)
const mEditId = ref<string | null>(null)
const mForm = reactive<Partial<GymMuscle>>({ muscleCode: '', muscleName: '', muscleGroup: '', sortNo: 0 })
const mFormRef = ref()
const mSaving = ref(false)

async function loadMuscles() {
  mLoading.value = true
  try {
    const params: any = { page: mPage.current, size: mPage.size }
    if (mFilters.muscleName) params.muscleName = mFilters.muscleName
    if (mFilters.muscleGroup) params.muscleGroup = mFilters.muscleGroup
    const res = await queryGymMuscle(params)
    const pd: PageData<GymMuscle> = res.data
    mData.value = pd.records; mPage.total = pd.total
  } catch (e: any) { ElMessage.error(e?.message || '查询失败') }
  finally { mLoading.value = false }
}
function openAddMuscle() {
  mEditing.value = false; mEditId.value = null
  Object.assign(mForm, { muscleCode: '', muscleName: '', muscleGroup: '', sortNo: 0 })
  mDialog.value = true; setTimeout(() => mFormRef.value?.resetFields(), 0)
}
function openEditMuscle(row: GymMuscle) {
  mEditing.value = true; mEditId.value = row.id
  Object.assign(mForm, { muscleCode: row.muscleCode, muscleName: row.muscleName, muscleGroup: row.muscleGroup, sortNo: row.sortNo })
  mDialog.value = true; setTimeout(() => mFormRef.value?.clearValidate(), 0)
}
async function saveMuscle() {
  const ok = await mFormRef.value?.validate().catch(() => false); if (!ok) return
  mSaving.value = true
  try {
    if (mEditing.value && mEditId.value) { await updateGymMuscle(mEditId.value, mForm); ElMessage.success('更新成功') }
    else { await createGymMuscle(mForm); ElMessage.success('创建成功') }
    mDialog.value = false; loadMuscles()
  } catch (e: any) { ElMessage.error(e?.message || '操作失败') }
  finally { mSaving.value = false }
}
async function deleteMuscle(row: GymMuscle) {
  try {
    await ElMessageBox.confirm(`确定删除肌群「${row.muscleName}」？`, '删除确认', { type: 'warning' })
    await deleteGymMuscle(row.id); ElMessage.success('删除成功'); loadMuscles()
  } catch { /* cancelled */ }
}

// ═══════════════════════════════════════════════════════════════
// TAB 3 — 器械字典 (GymEquipment)
// ═══════════════════════════════════════════════════════════════
const eLoading = ref(false)
const eData = ref<GymEquipment[]>([])
const ePage = reactive({ current: 1, size: 50, total: 0 })
const eFilters = reactive({ equipmentName: '', equipmentType: '' })
const eDialog = ref(false)
const eEditing = ref(false)
const eEditId = ref<string | null>(null)
const eForm = reactive<Partial<GymEquipment>>({ equipmentCode: '', equipmentName: '', equipmentType: '' })
const eFormRef = ref()
const eSaving = ref(false)

async function loadEquipments() {
  eLoading.value = true
  try {
    const params: any = { page: ePage.current, size: ePage.size }
    if (eFilters.equipmentName) params.equipmentName = eFilters.equipmentName
    if (eFilters.equipmentType) params.equipmentType = eFilters.equipmentType
    const res = await queryGymEquipment(params)
    const pd: PageData<GymEquipment> = res.data
    eData.value = pd.records; ePage.total = pd.total
  } catch (e: any) { ElMessage.error(e?.message || '查询失败') }
  finally { eLoading.value = false }
}
function openAddEquipment() {
  eEditing.value = false; eEditId.value = null
  Object.assign(eForm, { equipmentCode: '', equipmentName: '', equipmentType: '' })
  eDialog.value = true; setTimeout(() => eFormRef.value?.resetFields(), 0)
}
function openEditEquipment(row: GymEquipment) {
  eEditing.value = true; eEditId.value = row.id
  Object.assign(eForm, { equipmentCode: row.equipmentCode, equipmentName: row.equipmentName, equipmentType: row.equipmentType })
  eDialog.value = true; setTimeout(() => eFormRef.value?.clearValidate(), 0)
}
async function saveEquipment() {
  const ok = await eFormRef.value?.validate().catch(() => false); if (!ok) return
  eSaving.value = true
  try {
    if (eEditing.value && eEditId.value) { await updateGymEquipment(eEditId.value, eForm); ElMessage.success('更新成功') }
    else { await createGymEquipment(eForm); ElMessage.success('创建成功') }
    eDialog.value = false; loadEquipments()
  } catch (e: any) { ElMessage.error(e?.message || '操作失败') }
  finally { eSaving.value = false }
}
async function deleteEquipment(row: GymEquipment) {
  try {
    await ElMessageBox.confirm(`确定删除器械「${row.equipmentName}」？`, '删除确认', { type: 'warning' })
    await deleteGymEquipment(row.id); ElMessage.success('删除成功'); loadEquipments()
  } catch { /* cancelled */ }
}

// ═══════════════════════════════════════════════════════════════
// TAB 4 — 动作-肌群关联 (GymActionMuscleRel)
// ═══════════════════════════════════════════════════════════════
const amLoading = ref(false)
const amData = ref<GymActionMuscleRel[]>([])
const amPage = reactive({ current: 1, size: 50, total: 0 })
const amFilters = reactive({ actionId: '', muscleId: '' })
const amDialog = ref(false)
const amForm = reactive({ actionId: '', muscleId: '', isPrimary: 0 })
const amFormRef = ref()
const amSaving = ref(false)
// dropdown options
const actionOptions = ref<{ value: string; label: string }[]>([])
const muscleOptions = ref<{ value: string; label: string }[]>([])

async function loadActionOptions() {
  const res = await listAllGymAction()
  actionOptions.value = (res.data || []).map(a => ({ value: a.id, label: a.name }))
}
async function loadMuscleOptions() {
  const res = await listAllGymMuscle()
  muscleOptions.value = (res.data || []).map(m => ({ value: m.id, label: m.muscleName }))
}
async function loadActionMuscleRels() {
  amLoading.value = true
  try {
    const params: any = { page: amPage.current, size: amPage.size }
    if (amFilters.actionId) params.actionId = amFilters.actionId
    if (amFilters.muscleId) params.muscleId = amFilters.muscleId
    const res = await queryGymActionMuscleRel(params)
    const pd: PageData<GymActionMuscleRel> = res.data
    amData.value = pd.records; amPage.total = pd.total
  } catch (e: any) { ElMessage.error(e?.message || '查询失败') }
  finally { amLoading.value = false }
}
function openAddActionMuscle() {
  amForm.actionId = ''; amForm.muscleId = ''; amForm.isPrimary = 0
  amDialog.value = true; setTimeout(() => amFormRef.value?.resetFields(), 0)
}
async function saveActionMuscle() {
  const ok = await amFormRef.value?.validate().catch(() => false); if (!ok) return
  amSaving.value = true
  try {
    await createGymActionMuscleRel(amForm); ElMessage.success('添加关联成功')
    amDialog.value = false; loadActionMuscleRels()
  } catch (e: any) { ElMessage.error(e?.message || '操作失败') }
  finally { amSaving.value = false }
}
async function deleteActionMuscleRel(row: GymActionMuscleRel) {
  try {
    await ElMessageBox.confirm(`确定删除此关联？`, '删除确认', { type: 'warning' })
    await deleteGymActionMuscleRel(row.id); ElMessage.success('删除成功'); loadActionMuscleRels()
  } catch { /* cancelled */ }
}

// ═══════════════════════════════════════════════════════════════
// TAB 5 — 动作-器械关联 (GymActionEquipmentRel)
// ═══════════════════════════════════════════════════════════════
const aeLoading = ref(false)
const aeData = ref<GymActionEquipmentRel[]>([])
const aePage = reactive({ current: 1, size: 50, total: 0 })
const aeFilters = reactive({ actionId: '', equipmentId: '' })
const aeDialog = ref(false)
const aeForm = reactive({ actionId: '', equipmentId: '' })
const aeFormRef = ref()
const aeSaving = ref(false)
const equipOptions = ref<{ value: string; label: string }[]>([])

async function loadEquipOptions() {
  const res = await listAllGymEquipment()
  equipOptions.value = (res.data || []).map(e => ({ value: e.id, label: e.equipmentName }))
}
async function loadActionEquipRels() {
  aeLoading.value = true
  try {
    const params: any = { page: aePage.current, size: aePage.size }
    if (aeFilters.actionId) params.actionId = aeFilters.actionId
    if (aeFilters.equipmentId) params.equipmentId = aeFilters.equipmentId
    const res = await queryGymActionEquipmentRel(params)
    const pd: PageData<GymActionEquipmentRel> = res.data
    aeData.value = pd.records; aePage.total = pd.total
  } catch (e: any) { ElMessage.error(e?.message || '查询失败') }
  finally { aeLoading.value = false }
}
function openAddActionEquip() {
  aeForm.actionId = ''; aeForm.equipmentId = ''
  aeDialog.value = true; setTimeout(() => aeFormRef.value?.resetFields(), 0)
}
async function saveActionEquip() {
  const ok = await aeFormRef.value?.validate().catch(() => false); if (!ok) return
  aeSaving.value = true
  try {
    await createGymActionEquipmentRel(aeForm); ElMessage.success('添加关联成功')
    aeDialog.value = false; loadActionEquipRels()
  } catch (e: any) { ElMessage.error(e?.message || '操作失败') }
  finally { aeSaving.value = false }
}
async function deleteActionEquipRel(row: GymActionEquipmentRel) {
  try {
    await ElMessageBox.confirm(`确定删除此关联？`, '删除确认', { type: 'warning' })
    await deleteGymActionEquipmentRel(row.id); ElMessage.success('删除成功'); loadActionEquipRels()
  } catch { /* cancelled */ }
}

// ═══════════════════════════════════════════════════════════════
// TAB 6 — 训练建议 (GymActionRecommendation)
// ═══════════════════════════════════════════════════════════════
const rLoading = ref(false)
const rData = ref<GymActionRecommendation[]>([])
const rPage = reactive({ current: 1, size: 50, total: 0 })
const rFilters = reactive({ actionId: '', trainingGoal: '' })
const rDialog = ref(false)
const rEditing = ref(false)
const rEditId = ref<string | null>(null)
const rForm = reactive<Partial<GymActionRecommendation>>({ actionId: '', trainingGoal: '', minSets: 3, maxSets: 5, minReps: 8, maxReps: 12, recommendRestTime: 60, intensityTips: '' })
const rFormRef = ref()
const rSaving = ref(false)

async function loadRecommendations() {
  rLoading.value = true
  try {
    const params: any = { page: rPage.current, size: rPage.size }
    if (rFilters.actionId) params.actionId = rFilters.actionId
    if (rFilters.trainingGoal) params.trainingGoal = rFilters.trainingGoal
    const res = await queryGymActionRecommendation(params)
    const pd: PageData<GymActionRecommendation> = res.data
    rData.value = pd.records; rPage.total = pd.total
  } catch (e: any) { ElMessage.error(e?.message || '查询失败') }
  finally { rLoading.value = false }
}
function openAddRec() {
  rEditing.value = false; rEditId.value = null
  Object.assign(rForm, { actionId: '', trainingGoal: '', minSets: 3, maxSets: 5, minReps: 8, maxReps: 12, recommendRestTime: 60, intensityTips: '' })
  rDialog.value = true; setTimeout(() => rFormRef.value?.resetFields(), 0)
}
function openEditRec(row: GymActionRecommendation) {
  rEditing.value = true; rEditId.value = row.id
  Object.assign(rForm, { actionId: row.actionId, trainingGoal: row.trainingGoal, minSets: row.minSets, maxSets: row.maxSets, minReps: row.minReps, maxReps: row.maxReps, recommendRestTime: row.recommendRestTime, intensityTips: row.intensityTips || '' })
  rDialog.value = true; setTimeout(() => rFormRef.value?.clearValidate(), 0)
}
async function saveRec() {
  const ok = await rFormRef.value?.validate().catch(() => false); if (!ok) return
  rSaving.value = true
  try {
    if (rEditing.value && rEditId.value) { await updateGymActionRecommendation(rEditId.value, rForm); ElMessage.success('更新成功') }
    else { await createGymActionRecommendation(rForm); ElMessage.success('创建成功') }
    rDialog.value = false; loadRecommendations()
  } catch (e: any) { ElMessage.error(e?.message || '操作失败') }
  finally { rSaving.value = false }
}
async function deleteRec(row: GymActionRecommendation) {
  try {
    await ElMessageBox.confirm(`确定删除此训练建议？`, '删除确认', { type: 'warning' })
    await deleteGymActionRecommendation(row.id); ElMessage.success('删除成功'); loadRecommendations()
  } catch { /* cancelled */ }
}

// ── Tab change: load data for the active tab ──
watch(activeTab, (tab) => {
  if (tab === 'action') loadActions()
  else if (tab === 'muscle') loadMuscles()
  else if (tab === 'equipment') loadEquipments()
  else if (tab === 'actionMuscle') loadActionMuscleRels()
  else if (tab === 'actionEquip') loadActionEquipRels()
  else if (tab === 'recommendation') loadRecommendations()
})

onMounted(async () => {
  loadActions()
  // preload dropdown options
  loadActionOptions(); loadMuscleOptions(); loadEquipOptions()
})
</script>

<template>
  <div class="gym-library-container">
    <el-tabs v-model="activeTab" type="border-card" class="library-tabs">
      <!-- ═══ TAB 1: 动作库 ═══ -->
      <el-tab-pane name="action">
        <template #label>
          <span>🏋️ 动作库</span>
        </template>
        <!-- filters -->
        <div class="tab-toolbar">
          <el-input v-model="aFilters.name" placeholder="搜索动作名称..." clearable style="width:200px" @keyup.enter="loadActions" />
          <el-select v-model="aFilters.actionType" placeholder="动作类型" clearable style="width:140px" @change="loadActions">
            <el-option v-for="o in enumOptions(ACTION_TYPE_MAP)" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
          <el-select v-model="aFilters.movementPattern" placeholder="动作模式" clearable style="width:130px" @change="loadActions">
            <el-option v-for="o in enumOptions(MOVEMENT_PATTERN_MAP)" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
          <el-select v-model="aFilters.difficultyLevel" placeholder="难度" clearable style="width:110px" @change="loadActions">
            <el-option v-for="(label, val) in DIFFICULTY_MAP" :key="val" :label="label" :value="Number(val)" />
          </el-select>
          <el-button type="primary" :icon="Search" @click="loadActions">查询</el-button>
          <el-button :icon="Refresh" @click="aFilters.name='';aFilters.actionType='';aFilters.movementPattern='';aFilters.difficultyLevel=undefined;loadActions()">重置</el-button>
          <div style="flex:1" />
          <el-button type="primary" :icon="Plus" @click="openAddAction">新增动作</el-button>
        </div>
        <el-table :data="aData" v-loading="aLoading" stripe border size="small" max-height="calc(100vh - 380px)">
          <el-table-column prop="name" label="动作名称" width="160" show-overflow-tooltip />
          <el-table-column prop="alias" label="别名/英文" width="140" show-overflow-tooltip />
          <el-table-column prop="actionType" label="动作类型" width="100" align="center">
            <template #default="{row}"><el-tag size="small">{{ ACTION_TYPE_MAP[row.actionType] || row.actionType }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="movementPattern" label="动作模式" width="90" align="center">
            <template #default="{row}"><el-tag size="small" type="success">{{ MOVEMENT_PATTERN_MAP[row.movementPattern] || row.movementPattern }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="difficultyLevel" label="难度" width="80" align="center">
            <template #default="{row}">{{ DIFFICULTY_MAP[row.difficultyLevel] || row.difficultyLevel }}</template>
          </el-table-column>
          <el-table-column prop="pinyinBref" label="拼音简拼" width="90" />
          <el-table-column prop="isCommon" label="常用" width="60" align="center">
            <template #default="{row}">{{ row.isCommon === 1 ? '✅' : '—' }}</template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="60" align="center">
            <template #default="{row}">{{ row.status === 1 ? '启用' : '禁用' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="140" align="center" fixed="right">
            <template #default="{row}">
              <el-button type="primary" link :icon="Edit" size="small" @click="openEditAction(row)">编辑</el-button>
              <el-button type="danger" link :icon="Delete" size="small" @click="deleteAction(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="tab-pagination" v-if="aPage.total > 0">
          <el-pagination v-model:current-page="aPage.current" v-model:page-size="aPage.size" :total="aPage.total" :page-sizes="[10,20,50]" layout="total,sizes,prev,pager,next" background size="small" @current-change="loadActions" @size-change="loadActions" />
        </div>
      </el-tab-pane>

      <!-- ═══ TAB 2: 肌群字典 ═══ -->
      <el-tab-pane name="muscle">
        <template #label><span>💪 肌群字典</span></template>
        <div class="tab-toolbar">
          <el-input v-model="mFilters.muscleName" placeholder="搜索肌群名称..." clearable style="width:200px" @keyup.enter="loadMuscles" />
          <el-select v-model="mFilters.muscleGroup" placeholder="肌群大类" clearable style="width:140px" @change="loadMuscles">
            <el-option v-for="o in enumOptions(MUSCLE_GROUP_MAP)" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
          <el-button type="primary" :icon="Search" @click="loadMuscles">查询</el-button>
          <el-button :icon="Refresh" @click="mFilters.muscleName='';mFilters.muscleGroup='';loadMuscles()">重置</el-button>
          <div style="flex:1" />
          <el-button type="primary" :icon="Plus" @click="openAddMuscle">新增肌群</el-button>
        </div>
        <el-table :data="mData" v-loading="mLoading" stripe border size="small" max-height="calc(100vh - 380px)">
          <el-table-column prop="muscleCode" label="肌群编码" width="170" show-overflow-tooltip />
          <el-table-column prop="muscleName" label="肌群名称" width="140" />
          <el-table-column prop="muscleGroup" label="肌群大类" width="110" align="center">
            <template #default="{row}"><el-tag size="small" type="warning">{{ MUSCLE_GROUP_MAP[row.muscleGroup] || row.muscleGroup }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="sortNo" label="排序号" width="80" align="center" />
          <el-table-column label="操作" width="140" align="center" fixed="right">
            <template #default="{row}">
              <el-button type="primary" link :icon="Edit" size="small" @click="openEditMuscle(row)">编辑</el-button>
              <el-button type="danger" link :icon="Delete" size="small" @click="deleteMuscle(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="tab-pagination" v-if="mPage.total > 0">
          <el-pagination v-model:current-page="mPage.current" v-model:page-size="mPage.size" :total="mPage.total" :page-sizes="[20,50]" layout="total,sizes,prev,pager,next" background size="small" @current-change="loadMuscles" @size-change="loadMuscles" />
        </div>
      </el-tab-pane>

      <!-- ═══ TAB 3: 器械字典 ═══ -->
      <el-tab-pane name="equipment">
        <template #label><span>🔧 器械字典</span></template>
        <div class="tab-toolbar">
          <el-input v-model="eFilters.equipmentName" placeholder="搜索器械名称..." clearable style="width:200px" @keyup.enter="loadEquipments" />
          <el-select v-model="eFilters.equipmentType" placeholder="器械类型" clearable style="width:140px" @change="loadEquipments">
            <el-option v-for="o in enumOptions(EQUIPMENT_TYPE_MAP)" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
          <el-button type="primary" :icon="Search" @click="loadEquipments">查询</el-button>
          <el-button :icon="Refresh" @click="eFilters.equipmentName='';eFilters.equipmentType='';loadEquipments()">重置</el-button>
          <div style="flex:1" />
          <el-button type="primary" :icon="Plus" @click="openAddEquipment">新增器械</el-button>
        </div>
        <el-table :data="eData" v-loading="eLoading" stripe border size="small" max-height="calc(100vh - 380px)">
          <el-table-column prop="equipmentCode" label="器械编码" width="170" show-overflow-tooltip />
          <el-table-column prop="equipmentName" label="器械名称" width="130" />
          <el-table-column prop="equipmentType" label="器械类型" width="120" align="center">
            <template #default="{row}"><el-tag size="small" type="danger">{{ EQUIPMENT_TYPE_MAP[row.equipmentType] || row.equipmentType }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="160" />
          <el-table-column prop="updateTime" label="更新时间" width="160" />
          <el-table-column label="操作" width="140" align="center" fixed="right">
            <template #default="{row}">
              <el-button type="primary" link :icon="Edit" size="small" @click="openEditEquipment(row)">编辑</el-button>
              <el-button type="danger" link :icon="Delete" size="small" @click="deleteEquipment(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="tab-pagination" v-if="ePage.total > 0">
          <el-pagination v-model:current-page="ePage.current" v-model:page-size="ePage.size" :total="ePage.total" :page-sizes="[20,50]" layout="total,sizes,prev,pager,next" background size="small" @current-change="loadEquipments" @size-change="loadEquipments" />
        </div>
      </el-tab-pane>

      <!-- ═══ TAB 4: 动作-肌群关联 ═══ -->
      <el-tab-pane name="actionMuscle">
        <template #label><span>🔄 动作-肌群</span></template>
        <div class="tab-toolbar">
          <el-select v-model="amFilters.actionId" placeholder="筛选动作..." clearable filterable style="width:200px" @change="loadActionMuscleRels">
            <el-option v-for="o in actionOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
          <el-button type="primary" :icon="Search" @click="loadActionMuscleRels">查询</el-button>
          <el-button :icon="Refresh" @click="amFilters.actionId='';amFilters.muscleId='';loadActionMuscleRels()">重置</el-button>
          <div style="flex:1" />
          <el-button type="primary" :icon="Plus" @click="openAddActionMuscle">添加关联</el-button>
        </div>
        <el-table :data="amData" v-loading="amLoading" stripe border size="small" max-height="calc(100vh - 380px)">
          <el-table-column prop="actionName" label="动作名称" width="180" show-overflow-tooltip />
          <el-table-column prop="muscleName" label="肌群名称" width="150" show-overflow-tooltip />
          <el-table-column prop="isPrimary" label="主肌群" width="80" align="center">
            <template #default="{row}">{{ row.isPrimary === 1 ? '⭐ 是' : '—' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="80" align="center">
            <template #default="{row}">
              <el-button type="danger" link :icon="Delete" size="small" @click="deleteActionMuscleRel(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="tab-pagination" v-if="amPage.total > 0">
          <el-pagination v-model:current-page="amPage.current" v-model:page-size="amPage.size" :total="amPage.total" :page-sizes="[20,50]" layout="total,sizes,prev,pager,next" background size="small" @current-change="loadActionMuscleRels" @size-change="loadActionMuscleRels" />
        </div>
      </el-tab-pane>

      <!-- ═══ TAB 5: 动作-器械关联 ═══ -->
      <el-tab-pane name="actionEquip">
        <template #label><span>🔗 动作-器械</span></template>
        <div class="tab-toolbar">
          <el-select v-model="aeFilters.actionId" placeholder="筛选动作..." clearable filterable style="width:200px" @change="loadActionEquipRels">
            <el-option v-for="o in actionOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
          <el-button type="primary" :icon="Search" @click="loadActionEquipRels">查询</el-button>
          <el-button :icon="Refresh" @click="aeFilters.actionId='';aeFilters.equipmentId='';loadActionEquipRels()">重置</el-button>
          <div style="flex:1" />
          <el-button type="primary" :icon="Plus" @click="openAddActionEquip">添加关联</el-button>
        </div>
        <el-table :data="aeData" v-loading="aeLoading" stripe border size="small" max-height="calc(100vh - 380px)">
          <el-table-column prop="actionName" label="动作名称" width="200" show-overflow-tooltip />
          <el-table-column prop="equipmentName" label="器械名称" width="170" show-overflow-tooltip />
          <el-table-column label="操作" width="80" align="center">
            <template #default="{row}">
              <el-button type="danger" link :icon="Delete" size="small" @click="deleteActionEquipRel(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="tab-pagination" v-if="aePage.total > 0">
          <el-pagination v-model:current-page="aePage.current" v-model:page-size="aePage.size" :total="aePage.total" :page-sizes="[20,50]" layout="total,sizes,prev,pager,next" background size="small" @current-change="loadActionEquipRels" @size-change="loadActionEquipRels" />
        </div>
      </el-tab-pane>

      <!-- ═══ TAB 6: 训练建议 ═══ -->
      <el-tab-pane name="recommendation">
        <template #label><span>🤖 训练建议</span></template>
        <div class="tab-toolbar">
          <el-select v-model="rFilters.actionId" placeholder="筛选动作..." clearable filterable style="width:200px" @change="loadRecommendations">
            <el-option v-for="o in actionOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
          <el-select v-model="rFilters.trainingGoal" placeholder="训练目标" clearable style="width:130px" @change="loadRecommendations">
            <el-option v-for="o in enumOptions(TRAINING_GOAL_MAP)" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
          <el-button type="primary" :icon="Search" @click="loadRecommendations">查询</el-button>
          <el-button :icon="Refresh" @click="rFilters.actionId='';rFilters.trainingGoal='';loadRecommendations()">重置</el-button>
          <div style="flex:1" />
          <el-button type="primary" :icon="Plus" @click="openAddRec">新增建议</el-button>
        </div>
        <el-table :data="rData" v-loading="rLoading" stripe border size="small" max-height="calc(100vh - 380px)">
          <el-table-column prop="actionName" label="动作名称" width="160" show-overflow-tooltip />
          <el-table-column prop="trainingGoal" label="训练目标" width="90" align="center">
            <template #default="{row}"><el-tag size="small" type="success">{{ TRAINING_GOAL_MAP[row.trainingGoal] || row.trainingGoal }}</el-tag></template>
          </el-table-column>
          <el-table-column label="推荐组数" width="90" align="center">
            <template #default="{row}">{{ row.minSets }}-{{ row.maxSets }} 组</template>
          </el-table-column>
          <el-table-column label="推荐次数" width="100" align="center">
            <template #default="{row}">{{ row.minReps }}-{{ row.maxReps }} 次</template>
          </el-table-column>
          <el-table-column prop="recommendRestTime" label="组间休息(秒)" width="110" align="center" />
          <el-table-column prop="intensityTips" label="强度建议" width="220" show-overflow-tooltip />
          <el-table-column label="操作" width="140" align="center" fixed="right">
            <template #default="{row}">
              <el-button type="primary" link :icon="Edit" size="small" @click="openEditRec(row)">编辑</el-button>
              <el-button type="danger" link :icon="Delete" size="small" @click="deleteRec(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="tab-pagination" v-if="rPage.total > 0">
          <el-pagination v-model:current-page="rPage.current" v-model:page-size="rPage.size" :total="rPage.total" :page-sizes="[20,50]" layout="total,sizes,prev,pager,next" background size="small" @current-change="loadRecommendations" @size-change="loadRecommendations" />
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- ═══════════════════════════════════════════════════════════ -->
    <!-- DIALOGS -->
    <!-- ═══════════════════════════════════════════════════════════ -->

    <!-- Action Dialog -->
    <el-dialog v-model="aDialog" :title="aEditing ? '编辑动作' : '新增动作'" width="650px" destroy-on-close>
      <el-form ref="aFormRef" :model="aForm" :rules="{ name: [{required:true,message:'请输入动作名称'}], actionType: [{required:true,message:'请选择动作类型'}], movementPattern: [{required:true,message:'请选择动作模式'}] }" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="动作名称" prop="name"><el-input v-model="aForm.name" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="别名/英文" prop="alias"><el-input v-model="aForm.alias" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="拼音简拼"><el-input v-model="aForm.pinyinBref" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="动作类型" prop="actionType"><el-select v-model="aForm.actionType" style="width:100%"><el-option v-for="o in enumOptions(ACTION_TYPE_MAP)" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="动作模式" prop="movementPattern"><el-select v-model="aForm.movementPattern" style="width:100%"><el-option v-for="o in enumOptions(MOVEMENT_PATTERN_MAP)" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="难度等级"><el-select v-model="aForm.difficultyLevel" style="width:100%"><el-option :value="1" label="初学者" /><el-option :value="2" label="中级" /><el-option :value="3" label="进阶高级" /></el-select></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="是否常用"><el-switch v-model="aForm.isCommon" :active-value="1" :inactive-value="0" active-text="是" inactive-text="否" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="状态"><el-switch v-model="aForm.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="图片URLs"><el-input v-model="aForm.imageUrls" placeholder='JSON数组，如：["url1","url2"]' /></el-form-item>
        <el-form-item label="视频URL"><el-input v-model="aForm.videoUrl" placeholder="视频/GIF地址" /></el-form-item>
        <el-form-item label="动作要领"><el-input v-model="aForm.actionGuide" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="安全提示"><el-input v-model="aForm.safetyTips" /></el-form-item>
        <el-form-item label="搜索关键字"><el-input v-model="aForm.searchKeywords" placeholder='JSON数组' /></el-form-item>
      </el-form>
      <template #footer><el-button @click="aDialog=false">取消</el-button><el-button type="primary" :loading="aSaving" @click="saveAction">{{ aEditing ? '更新' : '创建' }}</el-button></template>
    </el-dialog>

    <!-- Muscle Dialog -->
    <el-dialog v-model="mDialog" :title="mEditing ? '编辑肌群' : '新增肌群'" width="500px" destroy-on-close>
      <el-form ref="mFormRef" :model="mForm" :rules="{ muscleCode: [{required:true}], muscleName: [{required:true}], muscleGroup: [{required:true}] }" label-width="90px">
        <el-form-item label="肌群编码" prop="muscleCode"><el-input v-model="mForm.muscleCode" :disabled="mEditing" /></el-form-item>
        <el-form-item label="肌群名称" prop="muscleName"><el-input v-model="mForm.muscleName" /></el-form-item>
        <el-form-item label="肌群大类" prop="muscleGroup"><el-select v-model="mForm.muscleGroup" style="width:100%"><el-option v-for="o in enumOptions(MUSCLE_GROUP_MAP)" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item>
        <el-form-item label="排序号"><el-input-number v-model="mForm.sortNo" :min="0" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="mDialog=false">取消</el-button><el-button type="primary" :loading="mSaving" @click="saveMuscle">{{ mEditing ? '更新' : '创建' }}</el-button></template>
    </el-dialog>

    <!-- Equipment Dialog -->
    <el-dialog v-model="eDialog" :title="eEditing ? '编辑器械' : '新增器械'" width="500px" destroy-on-close>
      <el-form ref="eFormRef" :model="eForm" :rules="{ equipmentCode: [{required:true}], equipmentName: [{required:true}], equipmentType: [{required:true}] }" label-width="90px">
        <el-form-item label="器械编码" prop="equipmentCode"><el-input v-model="eForm.equipmentCode" :disabled="eEditing" /></el-form-item>
        <el-form-item label="器械名称" prop="equipmentName"><el-input v-model="eForm.equipmentName" /></el-form-item>
        <el-form-item label="器械类型" prop="equipmentType"><el-select v-model="eForm.equipmentType" style="width:100%"><el-option v-for="o in enumOptions(EQUIPMENT_TYPE_MAP)" :key="o.value" :label="`${o.label} (${o.value})`" :value="o.value" /></el-select></el-form-item>
      </el-form>
      <template #footer><el-button @click="eDialog=false">取消</el-button><el-button type="primary" :loading="eSaving" @click="saveEquipment">{{ eEditing ? '更新' : '创建' }}</el-button></template>
    </el-dialog>

    <!-- Action-Muscle Rel Dialog -->
    <el-dialog v-model="amDialog" title="添加动作-肌群关联" width="500px" destroy-on-close>
      <el-form ref="amFormRef" :model="amForm" :rules="{ actionId: [{required:true}], muscleId: [{required:true}] }" label-width="90px">
        <el-form-item label="动作" prop="actionId"><el-select v-model="amForm.actionId" style="width:100%" filterable><el-option v-for="o in actionOptions" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item>
        <el-form-item label="肌群" prop="muscleId"><el-select v-model="amForm.muscleId" style="width:100%" filterable><el-option v-for="o in muscleOptions" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item>
        <el-form-item label="主肌群"><el-switch v-model="amForm.isPrimary" :active-value="1" :inactive-value="0" active-text="是(主目标肌群)" inactive-text="否(协同肌群)" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="amDialog=false">取消</el-button><el-button type="primary" :loading="amSaving" @click="saveActionMuscle">添加</el-button></template>
    </el-dialog>

    <!-- Action-Equipment Rel Dialog -->
    <el-dialog v-model="aeDialog" title="添加动作-器械关联" width="500px" destroy-on-close>
      <el-form ref="aeFormRef" :model="aeForm" :rules="{ actionId: [{required:true}], equipmentId: [{required:true}] }" label-width="90px">
        <el-form-item label="动作" prop="actionId"><el-select v-model="aeForm.actionId" style="width:100%" filterable><el-option v-for="o in actionOptions" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item>
        <el-form-item label="器械" prop="equipmentId"><el-select v-model="aeForm.equipmentId" style="width:100%" filterable><el-option v-for="o in equipOptions" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item>
      </el-form>
      <template #footer><el-button @click="aeDialog=false">取消</el-button><el-button type="primary" :loading="aeSaving" @click="saveActionEquip">添加</el-button></template>
    </el-dialog>

    <!-- Recommendation Dialog -->
    <el-dialog v-model="rDialog" :title="rEditing ? '编辑训练建议' : '新增训练建议'" width="550px" destroy-on-close>
      <el-form ref="rFormRef" :model="rForm" :rules="{ actionId: [{required:true}], trainingGoal: [{required:true}] }" label-width="120px">
        <el-form-item label="动作" prop="actionId"><el-select v-model="rForm.actionId" style="width:100%" filterable><el-option v-for="o in actionOptions" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item>
        <el-form-item label="训练目标" prop="trainingGoal"><el-select v-model="rForm.trainingGoal" style="width:100%"><el-option v-for="o in enumOptions(TRAINING_GOAL_MAP)" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="最小组数"><el-input-number v-model="rForm.minSets" :min="1" :max="20" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="最大组数"><el-input-number v-model="rForm.maxSets" :min="1" :max="20" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="最少次数"><el-input-number v-model="rForm.minReps" :min="0" :max="100" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="最大次数"><el-input-number v-model="rForm.maxReps" :min="1" :max="100" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="组间休息(秒)"><el-input-number v-model="rForm.recommendRestTime" :min="0" :max="600" :step="10" /></el-form-item>
        <el-form-item label="强度建议"><el-input v-model="rForm.intensityTips" placeholder="如：建议使用 75%-85% 1RM 重量" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="rDialog=false">取消</el-button><el-button type="primary" :loading="rSaving" @click="saveRec">{{ rEditing ? '更新' : '创建' }}</el-button></template>
    </el-dialog>
  </div>
</template>

<style scoped>
.gym-library-container { padding: 20px; }
.library-tabs { box-shadow: none; }
.library-tabs :deep(.el-tabs__header) { margin-bottom: 0; background: #fff; }
.library-tabs :deep(.el-tabs__content) { padding: 16px 20px; }
.tab-toolbar { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; margin-bottom: 14px; }
.tab-pagination { display: flex; justify-content: flex-end; margin-top: 12px; }
</style>
