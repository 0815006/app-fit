<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createBooking, updateBookingInfo, cancelBooking, cancelGroupFuture } from '@/api/meetingBooking'
import type { SlotInfo } from '@/api/meetingBooking'

const props = defineProps<{
  visible: boolean
  mode: 'create' | 'edit-mine' | 'view-others' | 'view-lock'
  slotData: SlotInfo | null
  roomId: string
  bookingDate: string
}>()

const emit = defineEmits<{
  (e: 'update:visible', val: boolean): void
  (e: 'saved'): void
}>()

// ── Readonly flags ──
const isReadonly = computed(() => props.mode === 'view-others' || props.mode === 'view-lock')
const isMine = computed(() => props.mode === 'edit-mine')

// ── Form ──
const meetingTitle = ref('')
const attendees = ref('')
const weeklyWeeks = ref<number | undefined>(undefined)
const weeklyEnabled = ref(false)
const saving = ref(false)

function resetForm(): void {
  meetingTitle.value = ''
  attendees.value = ''
  weeklyWeeks.value = undefined
  weeklyEnabled.value = false
  if (props.slotData?.booking) {
    meetingTitle.value = props.slotData.booking.meetingTitle || ''
    attendees.value = props.slotData.booking.attendees || ''
  }
}

watch(() => props.visible, (val) => {
  if (val) resetForm()
})

const dialogTitle = computed(() => {
  switch (props.mode) {
    case 'create': return '预定会议时段'
    case 'edit-mine': return '我的会议详情'
    case 'view-others': return '他人预约详情'
    case 'view-lock': return '行政征用详情'
    default: return ''
  }
})

const startTimeLabel = computed(() => props.slotData?.timeLabel || '')
const endTimeLabel = computed(() => {
  if (!props.slotData) return ''
  const s = props.slotData.slot + 1
  const hour = 8 + Math.floor(s / 2)
  const minute = (s % 2) * 30
  return `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`
})

async function handleCreate(): Promise<void> {
  saving.value = true
  try {
    await createBooking({
      roomId: props.roomId,
      bookingDate: props.bookingDate,
      startSlot: props.slotData!.slot,
      endSlot: props.slotData!.slot + 1,
      meetingTitle: meetingTitle.value,
      attendees: attendees.value,
      weeklyWeeks: weeklyEnabled.value ? (weeklyWeeks.value || 1) : undefined,
    })
    ElMessage.success('预定成功')
    emit('saved')
    emit('update:visible', false)
  } catch (e: any) {
    ElMessage.error(e?.message || '预定失败')
  } finally {
    saving.value = false
  }
}

async function handleUpdate(): Promise<void> {
  saving.value = true
  try {
    await updateBookingInfo(props.slotData!.booking!.bookingId, {
      meetingTitle: meetingTitle.value,
      attendees: attendees.value,
    })
    ElMessage.success('已保存')
    emit('saved')
    emit('update:visible', false)
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleCancel(): Promise<void> {
  try {
    await ElMessageBox.confirm('确定要取消此预定吗？', '取消确认', { type: 'warning' })
  } catch {
    return // 用户点了取消，不报错
  }
  try {
    await cancelBooking(props.slotData!.booking!.bookingId)
    ElMessage.success('已取消')
    emit('saved')
    emit('update:visible', false)
  } catch (e: any) {
    ElMessage.error(e?.message || '取消失败')
  }
}

async function handleCancelGroup(): Promise<void> {
  try {
    await ElMessageBox.confirm('将取消后续所有周期约，已过去的保留不动。确定吗？', '一键取消后续', { type: 'warning' })
  } catch {
    return
  }
  try {
    const count = await cancelGroupFuture(props.slotData!.booking!.groupId, props.bookingDate)
    ElMessage.success(`已取消 ${(count as any).data} 场后续预定`)
    emit('saved')
    emit('update:visible', false)
  } catch (e: any) {
    ElMessage.error(e?.message || '取消失败')
  }
}

function handleClose(): void {
  emit('update:visible', false)
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    :title="dialogTitle"
    width="460px"
    :close-on-click-modal="!isReadonly"
    @update:model-value="handleClose"
  >
    <!-- Readonly: Lock Info -->
    <template v-if="mode === 'view-lock'">
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="时段">
          {{ startTimeLabel }} - {{ endTimeLabel }}
        </el-descriptions-item>
        <el-descriptions-item label="征用原因">
          {{ slotData?.lock?.reason || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="发起部门">
          {{ slotData?.lock?.deptName || '-' }}
        </el-descriptions-item>
      </el-descriptions>
    </template>

    <!-- Readonly: Other's Booking -->
    <template v-else-if="mode === 'view-others'">
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="时段">
          {{ startTimeLabel }} - {{ endTimeLabel }}
        </el-descriptions-item>
        <el-descriptions-item label="预定人">
          {{ slotData?.booking?.empName || slotData?.booking?.empNo }}
        </el-descriptions-item>
        <el-descriptions-item label="会议名称">
          {{ slotData?.booking?.meetingTitle || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="参会人">
          {{ slotData?.booking?.attendees || '-' }}
        </el-descriptions-item>
      </el-descriptions>
    </template>

    <!-- Edit / Create -->
    <template v-else>
      <el-descriptions :column="1" border size="small" style="margin-bottom: 16px">
        <el-descriptions-item label="时段">
          {{ startTimeLabel }} - {{ endTimeLabel }}
        </el-descriptions-item>
        <el-descriptions-item v-if="isMine && slotData?.booking" label="预定人">
          {{ slotData.booking.empName || slotData.booking.empNo }}
        </el-descriptions-item>
      </el-descriptions>

      <el-form label-width="80px">
        <el-form-item label="会议名称">
          <el-input v-model="meetingTitle" placeholder="选填" :disabled="saving" />
        </el-form-item>
        <el-form-item label="参会人">
          <el-input v-model="attendees" placeholder="选填" :disabled="saving" />
        </el-form-item>

        <!-- Weekly fix (only for create mode) -->
        <template v-if="mode === 'create'">
          <el-form-item label="周期约">
            <el-switch v-model="weeklyEnabled" :disabled="saving" />
          </el-form-item>
          <el-form-item v-if="weeklyEnabled" label="固定周数">
            <el-input-number v-model="weeklyWeeks" :min="1" :max="8" :disabled="saving" />
            <span style="margin-left: 8px; font-size: 12px; color: #909399">最大 8 周</span>
          </el-form-item>
        </template>
      </el-form>
    </template>

    <template #footer>
      <div :style="{ display: 'flex', justifyContent: isReadonly ? 'center' : 'space-between', alignItems: 'center' }">
        <!-- Left: cancel buttons -->
        <div v-if="!isReadonly && mode === 'edit-mine' && slotData?.booking" style="display: flex; gap: 8px;">
          <el-button type="danger" plain size="small" @click="handleCancel">
            取消预定
          </el-button>
          <el-button
            v-if="slotData.booking.isWeeklyFix"
            type="danger"
            size="small"
            @click="handleCancelGroup"
          >
            一键取消后续所有
          </el-button>
        </div>
        <div v-else-if="mode === 'create'" style="display: flex; gap: 8px;">
          <el-button type="danger" plain size="small" @click="handleClose">
            取消预定
          </el-button>
        </div>

        <!-- Right: close / save -->
        <div v-if="isReadonly" style="display: flex; gap: 8px;">
          <el-button type="primary" @click="handleClose">我知道了</el-button>
        </div>
        <div v-else style="display: flex; gap: 8px;">
          <el-button @click="handleClose" :disabled="saving">关闭</el-button>
          <el-button
            v-if="mode === 'create'"
            type="primary"
            :loading="saving"
            @click="handleCreate"
          >
            保存
          </el-button>
          <el-button
            v-if="mode === 'edit-mine'"
            type="primary"
            :loading="saving"
            @click="handleUpdate"
          >
            保存修改
          </el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>
