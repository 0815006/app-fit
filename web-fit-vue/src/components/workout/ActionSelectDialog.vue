<script setup lang="ts">
import { ref, watch } from 'vue'
import { listByMuscleGroup, type GymAction } from '@/api/gymAction'
import { listRecByActionId, type GymActionRecommendation } from '@/api/gymActionRecommendation'
import { ElMessage } from 'element-plus'

const props = defineProps<{
  visible: boolean
  muscleGroup: string
  /** 具体肌肉名称，用于标题上下文展示 */
  muscleName?: string
}>()

const emit = defineEmits<{
  (e: 'update:visible', val: boolean): void
  (e: 'start', actionId: string, actionName: string): void
}>()

const loading = ref(false)

interface ActionWithRec extends GymAction {
  recs: GymActionRecommendation[]
}

const actions = ref<ActionWithRec[]>([])

const dialogTitle = ref('选择训练动作')

watch(() => props.visible, async (val) => {
  if (!val || !props.muscleGroup) return
  loading.value = true
  // 根据是否有具体肌肉名称调整标题
  dialogTitle.value = props.muscleName
    ? `选择训练动作 — ${props.muscleName}`
    : `选择训练动作`
  try {
    const resActions = await listByMuscleGroup(props.muscleGroup)
    const actionList = resActions.data || []

    const results: ActionWithRec[] = await Promise.all(
      actionList.map(async (action): Promise<ActionWithRec> => {
        try {
          const recRes = await listRecByActionId(action.id)
          return { ...action, recs: recRes.data || [] }
        } catch {
          return { ...action, recs: [] }
        }
      })
    )
    actions.value = results
  } catch {
    ElMessage.error('加载动作列表失败')
    actions.value = []
  } finally {
    loading.value = false
  }
})

function handleClose(): void {
  emit('update:visible', false)
}

function handleStart(actionId: string, actionName: string): void {
  emit('update:visible', false)
  emit('start', actionId, actionName)
}

function formatRec(recs: GymActionRecommendation[]): string {
  if (!recs || recs.length === 0) return '暂无推荐'
  const r = recs[0]
  const parts: string[] = []
  if (r.minSets && r.maxSets) parts.push(`${r.minSets}~${r.maxSets} 组`)
  if (r.minReps && r.maxReps) parts.push(`${r.minReps}~${r.maxReps} 次`)
  if (r.recommendRestTime) parts.push(`组间休息 ${r.recommendRestTime}s`)
  return parts.join('，') || '暂无推荐'
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    :title="dialogTitle"
    width="680px"
    :close-on-click-modal="false"
    @update:model-value="emit('update:visible', $event)"
  >
    <div v-loading="loading" class="action-list">
      <el-card
        v-for="action in actions"
        :key="action.id"
        class="action-item"
        shadow="hover"
      >
        <div class="action-header">
          <div class="action-info">
            <span class="action-name">{{ action.name }}</span>
            <el-tag v-if="action.actionType" size="small" type="info" effect="plain">
              {{ action.actionType }}
            </el-tag>
          </div>
          <el-button type="primary" size="small" @click="handleStart(action.id, action.name)">
            开始训练
          </el-button>
        </div>
        <div class="action-guide">
          <span class="guide-label">📋 推荐：</span>
          {{ formatRec(action.recs) }}
        </div>
        <div v-if="action.actionGuide" class="action-guide">
          <span class="guide-label">📖 动作要领：</span>
          {{ action.actionGuide }}
        </div>
      </el-card>

      <el-empty v-if="!loading && actions.length === 0" description="该肌群下暂无动作" />
    </div>
  </el-dialog>
</template>

<style scoped>
.action-list {
  max-height: 480px;
  overflow-y: auto;
}

.action-item {
  margin-bottom: 10px;
}

.action-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.action-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.action-name {
  font-size: 15px;
  font-weight: 600;
}

.action-guide {
  margin-top: 8px;
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
}

.guide-label {
  font-weight: 600;
  color: #303133;
}
</style>
