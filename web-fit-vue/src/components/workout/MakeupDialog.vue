<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { listAllGymAction, type GymAction } from '@/api/gymAction'
import { makeupWorkout } from '@/api/gymWorkout'

const props = defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  (e: 'update:visible', val: boolean): void
  (e: 'completed'): void
}>()

const loadingActions = ref(false)
const actions = ref<GymAction[]>([])
const selectedActionId = ref('')
const selectedDateTime = ref('')
const weight = ref<number | undefined>(undefined)
const reps = ref<number | undefined>(undefined)
const setCount = ref<number | undefined>(undefined)
const exhaustionPercent = ref(100)
const submitting = ref(false)

watch(() => props.visible, async (val) => {
  if (val) {
    selectedActionId.value = ''
    weight.value = undefined
    reps.value = undefined
    setCount.value = undefined
    exhaustionPercent.value = 100
    // 默认当前时间
    const now = new Date()
    selectedDateTime.value = now.toISOString().slice(0, 16)
    await loadActions()
  }
})

async function loadActions(): Promise<void> {
  loadingActions.value = true
  try {
    const res = await listAllGymAction()
    actions.value = res.data || []
  } catch {
    ElMessage.error('加载动作列表失败')
  } finally {
    loadingActions.value = false
  }
}

async function handleSubmit(): Promise<void> {
  if (!selectedActionId.value) {
    ElMessage.warning('请选择动作')
    return
  }
  if (!selectedDateTime.value) {
    ElMessage.warning('请选择日期时间')
    return
  }
  if (exhaustionPercent.value < 50 || exhaustionPercent.value > 120) {
    ElMessage.warning('力竭度需在 50% ~ 120% 之间')
    return
  }
  submitting.value = true
  try {
    // 转换为 ISO 格式传给后端
    const startTime = new Date(selectedDateTime.value).toISOString()
    await makeupWorkout(
      selectedActionId.value,
      startTime,
      exhaustionPercent.value / 100,
      weight.value ?? undefined,
      reps.value ?? undefined,
      setCount.value ?? undefined
    )
    ElMessage.success('补打卡成功')
    emit('update:visible', false)
    emit('completed')
  } catch {
    ElMessage.error('补打卡失败')
  } finally {
    submitting.value = false
  }
}

function formatTooltip(val: number): string {
  return val + '%'
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    title="自由补打卡"
    width="480px"
    :close-on-click-modal="false"
    @update:model-value="emit('update:visible', $event)"
  >
    <div class="makeup-content">
      <div class="form-item">
        <label class="form-label">选择日期时间</label>
        <el-date-picker
          v-model="selectedDateTime"
          type="datetime"
          placeholder="选择日期时间"
          format="YYYY-MM-DD HH:mm"
          value-format="YYYY-MM-DDTHH:mm"
          class="form-picker"
        />
      </div>

      <div class="form-item">
        <label class="form-label">选择动作</label>
        <el-select
          v-model="selectedActionId"
          placeholder="请选择训练动作"
          filterable
          class="form-select"
          :loading="loadingActions"
        >
          <el-option
            v-for="action in actions"
            :key="action.id"
            :label="action.name"
            :value="action.id"
          />
        </el-select>
      </div>

      <div class="form-item">
        <label class="form-label">训练数据（可选）</label>
        <div class="data-input-row">
          <div class="data-input-item">
            <span class="data-input-sublabel">重量(kg)</span>
            <el-input-number
              v-model="weight"
              :min="0"
              :precision="1"
              :step="2.5"
              placeholder="重量"
              controls-position="right"
              class="data-number-input"
            />
          </div>
          <div class="data-input-item">
            <span class="data-input-sublabel">次数</span>
            <el-input-number
              v-model="reps"
              :min="0"
              :step="1"
              placeholder="次数"
              controls-position="right"
              class="data-number-input"
            />
          </div>
          <div class="data-input-item">
            <span class="data-input-sublabel">组数</span>
            <el-input-number
              v-model="setCount"
              :min="0"
              :step="1"
              placeholder="组数"
              controls-position="right"
              class="data-number-input"
            />
          </div>
        </div>
      </div>

      <div class="form-item">
        <label class="form-label">力竭程度</label>
        <div class="slider-wrap">
          <el-slider
            v-model="exhaustionPercent"
            :min="50"
            :max="120"
            :step="1"
            :format-tooltip="formatTooltip"
            show-input
          />
        </div>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="emit('update:visible', false)">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          提交
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.makeup-content {
  padding: 8px 0;
}

.form-item {
  margin-bottom: 18px;
}

.form-label {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.form-picker,
.form-select {
  width: 100%;
}

.data-input-row {
  display: flex;
  gap: 12px;
}

.data-input-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.data-input-sublabel {
  font-size: 12px;
  color: #909399;
}

.data-number-input {
  width: 100%;
}

.slider-wrap {
  padding: 0 4px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
