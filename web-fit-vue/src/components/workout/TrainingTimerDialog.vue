<script setup lang="ts">
import { ref, watch, onBeforeUnmount, computed } from 'vue'
import { ElMessageBox } from 'element-plus'
import { listByActionId } from '@/api/gymActionMuscleRel'
import { listByActionIdEquipment } from '@/api/gymActionEquipmentRel'
import { listRecByActionId } from '@/api/gymActionRecommendation'
import type { GymActionMuscleRel } from '@/api/gymActionMuscleRel'
import type { GymActionEquipmentRel } from '@/api/gymActionEquipmentRel'
import type { GymActionRecommendation } from '@/api/gymActionRecommendation'

// ---- 折叠面板 ----
const collapseActive = ref<string[]>([])

// ---- 输入模式 ----
type InputMode = 'weighted' | 'bodyweight' | 'cardio'

// ---- 自重动作关键词 ----
const BODYWEIGHT_KEYWORDS = [
  '引体向上', '双杠臂屈伸', '平板支撑', '悬垂举腿', '仰卧抬腿', '基础卷腹',
  '俯卧撑', '仰卧起坐', '平板', '臀桥',
]

const props = defineProps<{
  visible: boolean
  actionId: string
  actionName: string
  actionType?: string
}>()

const emit = defineEmits<{
  (e: 'update:visible', val: boolean): void
  (e: 'end', payload: { elapsedSeconds: number; weight?: number; reps?: number; setCount?: number }): void
  (e: 'cancel'): void
}>()

// ---- 输入模式判定 ----
const inputMode = computed<InputMode>(() => {
  if (props.actionType && ['CARDIO', 'STRETCH', 'MOBILITY'].includes(props.actionType)) return 'cardio'
  if (BODYWEIGHT_KEYWORDS.some(k => props.actionName.includes(k))) return 'bodyweight'
  return 'weighted'
})

// ---- 计时 ----
const elapsed = ref(0)
let timer: ReturnType<typeof setInterval> | null = null

// ---- 关联数据 ----
const muscles = ref<GymActionMuscleRel[]>([])
const equipments = ref<GymActionEquipmentRel[]>([])
const recommendations = ref<GymActionRecommendation[]>([])
const infoLoading = ref(false)

// ---- 训练数据输入 ----
const weight = ref<number | null>(null)
const reps = ref<number | null>(null)
const setCount = ref<number | null>(null)

// 实时计算 1RM 估值
const estimated1RM = computed(() => {
  if (weight.value == null || reps.value == null || weight.value <= 0 || reps.value <= 0) return null
  return (weight.value * (1 + reps.value / 30)).toFixed(1)
})

// 实时计算总容量
const totalVolume = computed(() => {
  if (weight.value == null || reps.value == null || setCount.value == null || weight.value <= 0 || reps.value <= 0 || setCount.value <= 0) return null
  return (weight.value * reps.value * setCount.value).toFixed(0)
})

watch(() => props.visible, (val) => {
  if (val) {
    elapsed.value = 0
    weight.value = null
    reps.value = null
    setCount.value = null
    timer = setInterval(() => {
      elapsed.value++
    }, 1000)
    loadActionInfo()
  } else {
    stopTimer()
  }
})

onBeforeUnmount(() => {
  stopTimer()
})

async function loadActionInfo(): Promise<void> {
  if (!props.actionId) return
  infoLoading.value = true
  try {
    const [muscleRes, equipRes, recRes] = await Promise.all([
      listByActionId(props.actionId),
      listByActionIdEquipment(props.actionId),
      listRecByActionId(props.actionId),
    ])
    muscles.value = muscleRes.data || []
    equipments.value = equipRes.data || []
    recommendations.value = recRes.data || []
  } catch {
    // 静默失败，不影响计时主功能
  } finally {
    infoLoading.value = false
  }
}

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

function goalLabel(goal: string): string {
  const map: Record<string, string> = {
    HYPERTROPHY: '增肌',
    STRENGTH: '力量',
    FAT_LOSS: '减脂',
    ENDURANCE: '耐力',
  }
  return map[goal] || goal
}

function recDisplay(rec: GymActionRecommendation): string {
  const parts: string[] = []
  if (rec.minSets != null && rec.maxSets != null) {
    if (rec.minSets === rec.maxSets) {
      parts.push(`${rec.maxSets} 组`)
    } else {
      parts.push(`${rec.minSets}~${rec.maxSets} 组`)
    }
  }
  if (rec.minReps != null && rec.maxReps != null && rec.minReps > 0) {
    if (rec.minReps === rec.maxReps) {
      parts.push(`× ${rec.maxReps} 次`)
    } else {
      parts.push(`× ${rec.minReps}~${rec.maxReps} 次`)
    }
  }
  if (rec.recommendRestTime != null && rec.recommendRestTime > 0) {
    parts.push(`组休 ${formatRest(rec.recommendRestTime)}`)
  }
  return parts.join('  ')
}

function formatRest(s: number): string {
  if (s >= 60) {
    const m = Math.floor(s / 60)
    return s % 60 === 0 ? `${m}m` : `${m}m${s % 60}s`
  }
  return `${s}s`
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
    emit('end', {
      elapsedSeconds: elapsed.value,
      weight: weight.value ?? undefined,
      reps: reps.value ?? undefined,
      setCount: setCount.value ?? undefined,
    })
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
    width="560px"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :show-close="false"
    @update:model-value="emit('update:visible', $event)"
  >
    <!-- ====== 计时器 ====== -->
    <div class="timer-container">
      <div class="timer-display">{{ formatTime(elapsed) }}</div>
      <p class="timer-hint">专注训练，完成后点击结束</p>
    </div>

    <!-- ====== 训练数据输入区 ====== -->
    <div class="data-input-section">
      <div class="data-input-title">📊 训练数据</div>

      <!-- 模式 A：负重动作 -->
      <template v-if="inputMode === 'weighted'">
        <div class="data-form">
          <div class="form-item">
            <label class="form-label">重量 (kg)</label>
            <el-input-number
              v-model="weight"
              :min="0"
              :step="2.5"
              :precision="1"
              placeholder="如 60"
              controls-position="right"
              class="form-input"
            />
          </div>
          <div class="form-item">
            <label class="form-label">次数</label>
            <el-input-number
              v-model="reps"
              :min="0"
              :step="1"
              placeholder="如 10"
              controls-position="right"
              class="form-input"
            />
          </div>
          <div class="form-item">
            <label class="form-label">组数</label>
            <el-input-number
              v-model="setCount"
              :min="0"
              :step="1"
              placeholder="如 4"
              controls-position="right"
              class="form-input"
            />
          </div>
        </div>
        <div v-if="estimated1RM != null || totalVolume != null" class="calc-result">
          <span v-if="estimated1RM != null" class="calc-item">
            估计 1RM：<strong>{{ estimated1RM }} kg</strong>
          </span>
          <span v-if="totalVolume != null" class="calc-item calc-separator">|</span>
          <span v-if="totalVolume != null" class="calc-item">
            总容量：<strong>{{ totalVolume }} kg</strong>
          </span>
        </div>
      </template>

      <!-- 模式 B：自重动作 -->
      <template v-else-if="inputMode === 'bodyweight'">
        <div class="data-form">
          <div class="form-item">
            <label class="form-label">次数</label>
            <el-input-number
              v-model="reps"
              :min="0"
              :step="1"
              placeholder="如 8"
              controls-position="right"
              class="form-input"
            />
          </div>
          <div class="form-item">
            <label class="form-label">组数</label>
            <el-input-number
              v-model="setCount"
              :min="0"
              :step="1"
              placeholder="如 4"
              controls-position="right"
              class="form-input"
            />
          </div>
        </div>
        <div class="data-hint">💡 自重动作，无需填写重量</div>
      </template>

      <!-- 模式 C：有氧/拉伸 -->
      <template v-else>
        <div class="data-hint">💡 有氧训练，仅记录训练时长和力竭度，无需额外填写重量/次数数据</div>
      </template>
    </div>

    <!-- ====== 关联信息展示区（可折叠） ====== -->
    <el-collapse v-model="collapseActive" class="info-collapse">
      <el-collapse-item title="📋 训练参考信息" name="info">
        <div class="action-info-section" v-loading="infoLoading">
          <!-- 涉及肌群 -->
          <div v-if="muscles.length > 0" class="info-block">
            <div class="info-label">🏋️ 涉及肌群</div>
            <div class="info-tags">
              <el-tag
                v-for="m in muscles"
                :key="m.id"
                :type="m.isPrimary === 1 ? '' : 'info'"
                size="small"
                effect="plain"
              >
                {{ m.muscleName }}
                <template v-if="m.isPrimary === 1">
                  <span class="primary-badge">主</span>
                </template>
              </el-tag>
            </div>
          </div>

          <!-- 涉及器械 -->
          <div v-if="equipments.length > 0" class="info-block">
            <div class="info-label">🔧 涉及器械</div>
            <div class="info-tags">
              <el-tag
                v-for="eq in equipments"
                :key="eq.id"
                size="small"
                effect="plain"
              >
                {{ eq.equipmentName }}
              </el-tag>
            </div>
          </div>

          <!-- 训练建议 -->
          <div v-if="recommendations.length > 0" class="info-block">
            <div class="info-label">📋 训练建议</div>
            <div
              v-for="rec in recommendations"
              :key="rec.id"
              class="rec-row"
            >
              <el-tag size="small" effect="dark" round style="margin-right: 8px;">
                {{ goalLabel(rec.trainingGoal) }}
              </el-tag>
              <span class="rec-detail">{{ recDisplay(rec) }}</span>
              <div v-if="rec.intensityTips" class="rec-tips">
                💡 {{ rec.intensityTips }}
              </div>
            </div>
          </div>

          <!-- 无数据占位 -->
          <div
            v-if="!infoLoading && muscles.length === 0 && equipments.length === 0 && recommendations.length === 0"
            class="info-empty"
          >
            <span class="empty-text">暂无关联数据</span>
          </div>
        </div>
      </el-collapse-item>
    </el-collapse>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleCancel">更换动作 / 取消</el-button>
        <el-button type="primary" @click="handleEnd">结束训练</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
/* ========== 计时器 ========== */
.timer-container {
  text-align: center;
  padding: 12px 0 8px;
}

.timer-display {
  font-size: 68px;
  font-weight: 700;
  font-family: 'Courier New', monospace;
  color: #303133;
  letter-spacing: 4px;
}

.timer-hint {
  margin-top: 8px;
  font-size: 14px;
  color: #909399;
}

/* ========== 训练数据输入区 ========== */
.data-input-section {
  margin-top: 12px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
}

.data-input-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}

.data-form {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.form-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.form-label {
  font-size: 13px;
  color: #606266;
  white-space: nowrap;
  min-width: 56px;
  text-align: right;
}

.form-input {
  width: 140px;
}

.calc-result {
  margin-top: 12px;
  padding: 8px 12px;
  background: #ecf5ff;
  border-radius: 6px;
  font-size: 13px;
  color: #409eff;
  display: flex;
  align-items: center;
  gap: 8px;
}

.calc-item strong {
  font-size: 15px;
}

.calc-separator {
  color: #c0c4cc;
}

.data-hint {
  font-size: 13px;
  color: #909399;
  padding: 8px 0 4px;
}

/* ========== 参考信息折叠区 ========== */
.info-collapse {
  margin-top: 8px;
}

.info-collapse :deep(.el-collapse-item__header) {
  font-size: 13px;
  color: #909399;
  border-bottom: none;
  padding: 0 4px;
}

:deep(.el-collapse-item__wrap) {
  border-bottom: none;
}

/* ========== 关联信息区 ========== */
.action-info-section {
  padding: 4px 8px;
  min-height: 20px;
}

.info-block {
  margin-bottom: 12px;
}

.info-label {
  font-size: 13px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 6px;
}

.info-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.primary-badge {
  display: inline-block;
  background: #e6a23c;
  color: #fff;
  font-size: 10px;
  border-radius: 2px;
  padding: 0 3px;
  margin-left: 4px;
  line-height: 16px;
  vertical-align: middle;
}

/* ========== 训练建议行 ========== */
.rec-row {
  display: flex;
  align-items: flex-start;
  flex-wrap: wrap;
  margin-bottom: 6px;
  padding: 4px 8px;
  background: #fafafa;
  border-radius: 6px;
}

.rec-detail {
  font-size: 13px;
  color: #303133;
  line-height: 24px;
}

.rec-tips {
  width: 100%;
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}

/* ========== 空数据 ========== */
.info-empty {
  text-align: center;
  padding: 8px 0;
}

.empty-text {
  font-size: 12px;
  color: #c0c4cc;
}

/* ========== 底部按钮 ========== */
.dialog-footer {
  display: flex;
  justify-content: space-between;
}
</style>
