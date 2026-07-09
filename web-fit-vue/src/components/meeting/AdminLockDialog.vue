<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { createAdminLock } from '@/api/meetingAdminLock'

const props = defineProps<{
  visible: boolean
  roomId: string
  lockDate: string
  startSlot: number
  endSlot: number
}>()

const emit = defineEmits<{
  (e: 'update:visible', val: boolean): void
  (e: 'saved'): void
}>()

const reason = ref('')
const deptName = ref('')
const saving = ref(false)

const reasonOptions = [
  '集团高管战略会',
  '部门季度总结会',
  '客户来访接待',
  '外部培训',
  '董事会会议',
  '紧急项目攻坚',
  '其他',
]

const startLabel = computed(() => {
  const hour = 8 + Math.floor(props.startSlot / 2)
  const minute = (props.startSlot % 2) * 30
  return `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`
})

const endLabel = computed(() => {
  const hour = 8 + Math.floor(props.endSlot / 2)
  const minute = (props.endSlot % 2) * 30
  return `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`
})

function resetForm(): void {
  reason.value = ''
  deptName.value = ''
}

watch(() => props.visible, (val) => {
  if (val) resetForm()
})

async function handleSubmit(): Promise<void> {
  if (!reason.value) {
    ElMessage.warning('请填写或选择征用原因')
    return
  }
  saving.value = true
  try {
    await createAdminLock({
      roomId: props.roomId,
      lockDate: props.lockDate,
      startSlot: props.startSlot,
      endSlot: props.endSlot,
      reason: reason.value,
      deptName: deptName.value,
    })
    ElMessage.success('强制征用已生效')
    emit('saved')
    emit('update:visible', false)
  } catch (e: any) {
    ElMessage.error(e?.message || '征用失败')
  } finally {
    saving.value = false
  }
}

function handleClose(): void {
  emit('update:visible', false)
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    title="强制征用"
    width="480px"
    :close-on-click-modal="false"
    @update:model-value="handleClose"
  >
    <el-descriptions :column="1" border size="small" style="margin-bottom: 16px">
      <el-descriptions-item label="征用时段">
        {{ startLabel }} - {{ endLabel }}
      </el-descriptions-item>
    </el-descriptions>

    <el-form label-width="80px">
      <el-form-item label="征用原因" required>
        <el-select v-model="reason" placeholder="选择征用原因" clearable style="width: 100%">
          <el-option v-for="o in reasonOptions" :key="o" :label="o" :value="o" />
        </el-select>
        <el-input
          v-if="reason === '其他'"
          v-model="reason"
          placeholder="请输入具体原因"
          style="margin-top: 8px"
        />
      </el-form-item>
      <el-form-item label="发起部门">
        <el-input v-model="deptName" placeholder="如：行政部" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="danger" :loading="saving" @click="handleSubmit">
        确认征用
      </el-button>
    </template>
  </el-dialog>
</template>
