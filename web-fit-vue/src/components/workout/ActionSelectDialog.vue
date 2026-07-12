<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { listByMuscleGroup, type GymAction } from '@/api/gymAction'
import { listRecByActionId, type GymActionRecommendation } from '@/api/gymActionRecommendation'
import { listRelByMuscleGroup, type GymActionMuscleRel } from '@/api/gymActionMuscleRel'
import { ElMessage } from 'element-plus'

const props = defineProps<{
  visible: boolean
  muscleGroup: string
  /** 用户点击的二级肌肉编码，用于置顶高亮（如 CHEST_MAJOR） */
  highlightMuscleCode?: string
  /** 用户点击的二级肌肉中文名，用于标题展示 */
  highlightMuscleName?: string
}>()

const emit = defineEmits<{
  (e: 'update:visible', val: boolean): void
  (e: 'start', actionId: string, actionName: string): void
}>()

const loading = ref(false)

interface ActionWithRec extends GymAction {
  recs: GymActionRecommendation[]
}

/** 所有动作（已按 highlightMuscleCode 排序） */
const actions = ref<ActionWithRec[]>([])

/** 被高亮的动作 ID 集合 */
const highlightedActionIds = ref<Set<string>>(new Set())

const dialogTitle = computed(() => {
  if (props.highlightMuscleName) {
    return `选择训练动作 — ${props.highlightMuscleName}`
  }
  return '选择训练动作'
})

/** 推荐区域的标题文本 */
const highlightLabel = computed(() => {
  if (props.highlightMuscleName) {
    return `🌟 推荐 — ${props.highlightMuscleName}`
  }
  return ''
})

/** 已被高亮的动作 */
const highlightedActions = computed(() =>
  actions.value.filter(a => highlightedActionIds.value.has(a.id))
)

/** 未被高亮的动作（常规区域） */
const normalActions = computed(() =>
  actions.value.filter(a => !highlightedActionIds.value.has(a.id))
)

watch(() => props.visible, async (val) => {
  if (!val || !props.muscleGroup) return
  loading.value = true
  highlightedActionIds.value = new Set()
  try {
    // 1. 加载该肌群所有动作
    const resActions = await listByMuscleGroup(props.muscleGroup)
    const actionList = resActions.data || []

    // 2. 加载该肌群所有动作-肌肉关联
    let rels: GymActionMuscleRel[] = []
    try {
      const resRels = await listRelByMuscleGroup(props.muscleGroup)
      rels = resRels.data || []
    } catch {
      // 静默，关联数据获取失败不影响主流程
    }

    // 3. 找出与 highlightMuscleCode 匹配的 actionId 集合
    if (props.highlightMuscleCode && rels.length > 0) {
      // 先从肌肉关联中找出对应 muscleCode 的 muscleId
      // rel 中只有 muscleId，没有 muscleCode，所以需要根据传入的 muscleCode 匹配
      // 实际上我们需要通过 gym_muscle 来做映射，但这里我们简化处理：
      // 利用 listRelByMuscleGroup 返回的数据中 muscleName 字段匹配
      // 或者直接用后端再传一个按 muscleCode 查行动作关联的接口
      //
      // 更简单的做法：前端用 muscleName 匹配（后端 enrichNames 已经填好了）
      const matchedRels = rels.filter(r => r.muscleName === props.highlightMuscleName)
      highlightedActionIds.value = new Set(matchedRels.map(r => r.actionId))
    }

    // 4. 加载每个动作的推荐配置
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

    // 5. 排序：高亮的排前面
    results.sort((a, b) => {
      const aHL = highlightedActionIds.value.has(a.id) ? 0 : 1
      const bHL = highlightedActionIds.value.has(b.id) ? 0 : 1
      if (aHL !== bHL) return aHL - bHL
      // 同区域按名称排序
      return a.name.localeCompare(b.name, 'zh')
    })

    actions.value = results
  } catch {
    ElMessage.error('加载动作列表失败')
    actions.value = []
  } finally {
    loading.value = false
  }
})

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
    width="720px"
    :close-on-click-modal="false"
    @update:model-value="emit('update:visible', $event)"
  >
    <div v-loading="loading" class="action-list">
      <!-- ============ 推荐区域：高亮动作 ============ -->
      <template v-if="highlightedActions.length > 0">
        <div class="highlight-section">
          <div class="highlight-header">{{ highlightLabel }}</div>
          <el-card
            v-for="action in highlightedActions"
            :key="action.id"
            class="action-item action-highlight"
            shadow="hover"
          >
            <div class="action-header">
              <div class="action-info">
                <span class="action-name">{{ action.name }}</span>
                <el-tag v-if="action.actionType" size="small" type="warning" effect="dark">
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
        </div>

        <!-- 分割区域 -->
        <div v-if="normalActions.length > 0" class="section-separator">
          —— 其他{{ highlightMuscleName ? '' : ' ' }}动作 ——
        </div>
      </template>

      <!-- ============ 常规区域：其他动作 ============ -->
      <el-card
        v-for="action in normalActions"
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
  max-height: 520px;
  overflow-y: auto;
}

/* ========= 高亮推荐区域 ========= */
.highlight-section {
  background: linear-gradient(to right, rgba(230, 162, 60, 0.06), transparent);
  border-left: 4px solid #e6a23c;
  border-radius: 8px;
  padding: 12px 14px 4px;
  margin-bottom: 16px;
}

.highlight-header {
  font-size: 15px;
  font-weight: 700;
  color: #e6a23c;
  margin-bottom: 10px;
  padding-left: 4px;
}

.action-highlight {
  border-color: #f0c78a;
}

.action-highlight :deep(.el-card__body) {
  background: rgba(230, 162, 60, 0.03);
}

/* ========= 分割线 ========= */
.section-separator {
  text-align: center;
  color: #c0c4cc;
  font-size: 13px;
  margin: 8px 0 14px;
  letter-spacing: 2px;
}

/* ========= 动作条目 ========= */
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
