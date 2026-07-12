<script setup lang="ts">
import { ref, watch, onBeforeUnmount } from 'vue'
import { ElMessageBox } from 'element-plus'
import { listByActionId } from '@/api/gymActionMuscleRel'
import { listByActionIdEquipment } from '@/api/gymActionEquipmentRel'
import { listRecByActionId } from '@/api/gymActionRecommendation'
import type { GymActionMuscleRel } from '@/api/gymActionMuscleRel'
import type { GymActionEquipmentRel } from '@/api/gymActionEquipmentRel'
import type { GymActionRecommendation } from '@/api/gymActionRecommendation'

const props = defineProps<{
  visible: boolean
  actionId: string
  actionName: string
}>()

const emit = defineEmits<{
  (e: 'update:visible', val: boolean): void
  (e: 'end', elapsedSeconds: number): void
  (e: 'cancel'): void
}>()

// ---- 计时 ----
const elapsed = ref(0)
let timer: ReturnType<typeof setInterval> | null = null

// ---- 关联数据 ----
const muscles = ref<GymActionMuscleRel[]>([])
const equipments = ref<GymActionEquipmentRel[]>([])
const recommendations = ref<GymActionRecommendation[]>([])

const infoLoading = ref(false)

watch(() => props.visible, (val) => {
  if (val) {
    elapsed.value = 0
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

/** 训练目标中文映射 */
function goalLabel(goal: string): string {
  const map: Record<string, string> = {
    HYPERTROPHY: '增肌',
    STRENGTH: '力量',
    FAT_LOSS: '减脂',
    ENDURANCE: '耐力',
  }
  return map[goal] || goal
}

/** 推荐配置展示文本 */
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

    <!-- ====== 关联信息展示区（纯展示） ====== -->
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
  padding: 20px 0 16px;
}

.timer-display {
  font-size: 72px;
  font-weight: 700;
  font-family: 'Courier New', monospace;
  color: #303133;
  letter-spacing: 4px;
}

.timer-hint {
  margin-top: 12px;
  font-size: 14px;
  color: #909399;
}

/* ========== 关联信息区 ========== */
.action-info-section {
  margin-top: 8px;
  padding: 16px 8px 4px;
  border-top: 1px solid #ebeef5;
  min-height: 40px;
}

.info-block {
  margin-bottom: 14px;
}

.info-label {
  font-size: 13px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 8px;
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
  margin-bottom: 8px;
  padding: 6px 8px;
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
  padding: 12px 0;
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
