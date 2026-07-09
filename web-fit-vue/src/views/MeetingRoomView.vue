<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { currentEmpNo } from '@/utils/currentUser'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, RefreshRight, Search } from '@element-plus/icons-vue'
import {
  listAllMeetingRooms,
  deleteMeetingRoom,
  toggleMeetingRoom,
  type MeetingRoom,
} from '@/api/meetingRoom'
import { getBoard, type RoomBoard, type SlotInfo } from '@/api/meetingBooking'
import { releaseAdminLock } from '@/api/meetingAdminLock'
import MeetingRoomDialog from '@/components/meeting/MeetingRoomDialog.vue'
import BookingDialog from '@/components/meeting/BookingDialog.vue'
import AdminLockDialog from '@/components/meeting/AdminLockDialog.vue'

// ── Admin check ──
const isAdmin = computed(() => currentEmpNo.value === '2036377')

// ── Time helpers ──
const CHINESE_WEEKDAYS = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']

function getTodayStr(): string {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function getDayOfWeekStr(dateStr: string): string {
  if (!dateStr) return ''
  const d = new Date(dateStr.replace(/-/g, '/'))
  return CHINESE_WEEKDAYS[d.getDay()]
}

function dateAddDays(dateStr: string, days: number): string {
  const d = new Date(dateStr.replace(/-/g, '/'))
  d.setDate(d.getDate() + days)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function slotToTimeLabel(slot: number): string {
  const hour = 8 + Math.floor(slot / 2)
  const minute = (slot % 2) * 30
  return `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`
}

// 20 half-hour slots: 08:00 - 17:30
const SLOT_COUNT = 20
const timeHeaders = computed(() => {
  const arr: string[] = []
  for (let i = 0; i < SLOT_COUNT; i++) {
    arr.push(slotToTimeLabel(i))
  }
  return arr
})

// ── Meeting Room Management State ──
const rooms = ref<MeetingRoom[]>([])
const roomsLoading = ref(false)

// Room dialog
const roomDialogVisible = ref(false)
const editRoomData = ref<MeetingRoom | null>(null)

async function loadRooms(): Promise<void> {
  roomsLoading.value = true
  try {
    const res = await listAllMeetingRooms()
    rooms.value = res.data || []
  } catch (e: any) {
    ElMessage.error(e?.message || '加载会议室失败')
  } finally {
    roomsLoading.value = false
  }
}

function handleAddRoom(): void {
  editRoomData.value = null
  roomDialogVisible.value = true
}

function handleEditRoom(room: MeetingRoom): void {
  editRoomData.value = room
  roomDialogVisible.value = true
}

async function handleDeleteRoom(room: MeetingRoom): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定要删除会议室 "${room.name}" 吗？`, '删除确认', { type: 'warning' })
    await deleteMeetingRoom(room.id)
    ElMessage.success('已删除')
    await loadRooms()
    await loadBoard()
  } catch { /* cancelled */ }
}

async function handleToggleRoom(room: MeetingRoom): Promise<void> {
  try {
    await toggleMeetingRoom(room.id)
    ElMessage.success(room.isActive === 1 ? '已下架' : '已上架')
    await loadRooms()
    await loadBoard()
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  }
}

// ── Board State ──
const boardDate = ref(getTodayStr())
const boardLoading = ref(false)
const boardData = ref<RoomBoard[]>([])

const isTodaySelected = computed(() => boardDate.value === getTodayStr())

const dateDisplay = computed(() => {
  if (!boardDate.value) return ''
  const wd = getDayOfWeekStr(boardDate.value)
  const label = isTodaySelected.value ? ' (今天)' : ''
  return `${boardDate.value} ${wd}${label}`
})

async function loadBoard(): Promise<void> {
  boardLoading.value = true
  try {
    const res = await getBoard(boardDate.value)
    boardData.value = res.data?.rooms || []
  } catch (e: any) {
    ElMessage.error(e?.message || '加载看板失败')
  } finally {
    boardLoading.value = false
  }
}

function goToPrevDay(): void {
  boardDate.value = dateAddDays(boardDate.value, -1)
  loadBoard()
}

function goToNextDay(): void {
  boardDate.value = dateAddDays(boardDate.value, 1)
  loadBoard()
}

function goToToday(): void {
  if (isTodaySelected.value) return
  boardDate.value = getTodayStr()
  loadBoard()
}

function handleBoardDateChange(val: string | null): void {
  if (val) loadBoard()
}

// ── Click handling ──
const bookingDialogVisible = ref(false)
const bookingMode = ref<'create' | 'edit-mine' | 'view-others' | 'view-lock'>('create')
const selectedSlot = ref<SlotInfo | null>(null)
const selectedRoomId = ref('')

// Admin lock dialog
const lockDialogVisible = ref(false)
const lockStartSlot = ref(0)
const lockEndSlot = ref(0)
const lockRoomId = ref('')

function getCellClass(slot: SlotInfo): string {
  switch (slot.type) {
    case 'FREE': return 'cell-free'
    case 'MY_BOOKING': return 'cell-my'
    case 'BOOKED': return 'cell-others'
    case 'ADMIN_LOCK': return 'cell-lock'
    default: return 'cell-free'
  }
}

function getCellText(slot: SlotInfo): string {
  switch (slot.type) {
    case 'FREE': return ''
    case 'MY_BOOKING': return slot.booking?.meetingTitle || slot.booking?.empName || '我'
    case 'BOOKED': return slot.booking?.empName || '已预定'
    case 'ADMIN_LOCK': return slot.lock?.reason || '行政征用'
    default: return ''
  }
}

function getCellTooltip(slot: SlotInfo): string {
  switch (slot.type) {
    case 'FREE': return `${slot.timeLabel} - 空闲`
    case 'MY_BOOKING': {
      const b = slot.booking
      return `${slot.timeLabel} - ${b?.meetingTitle || '我的预定'}\n${b?.empName || b?.empNo}\n${b?.attendees || ''}`
    }
    case 'BOOKED': {
      const b = slot.booking
      return `${slot.timeLabel} - ${b?.empName || b?.empNo}\n${b?.meetingTitle || ''}`
    }
    case 'ADMIN_LOCK': {
      const l = slot.lock
      return `${slot.timeLabel} - 行政征用\n${l?.reason || ''}\n${l?.deptName || ''}`
    }
    default: return ''
  }
}

function isPastSlot(slotIndex: number): boolean {
  // A slot is past if its end time is before current time AND we're viewing today
  if (!isTodaySelected.value) return false
  const now = new Date()
  const slotEndHour = 8 + Math.floor((slotIndex + 1) / 2)
  const slotEndMinute = ((slotIndex + 1) % 2) * 30
  const slotEndTime = new Date(now.getFullYear(), now.getMonth(), now.getDate(), slotEndHour, slotEndMinute)
  return now > slotEndTime
}

function handleSlotClick(slot: SlotInfo, roomId: string): void {
  selectedRoomId.value = roomId
  selectedSlot.value = slot

  switch (slot.type) {
    case 'FREE':
      if (isPastSlot(slot.slot)) {
        ElMessage.warning('无法预约过去的时间')
        return
      }
      bookingMode.value = 'create'
      bookingDialogVisible.value = true
      break
    case 'MY_BOOKING':
      bookingMode.value = 'edit-mine'
      bookingDialogVisible.value = true
      break
    case 'BOOKED':
      bookingMode.value = 'view-others'
      bookingDialogVisible.value = true
      break
    case 'ADMIN_LOCK':
      bookingMode.value = 'view-lock'
      bookingDialogVisible.value = true
      break
  }
}

function handleSlotRightClick(slot: SlotInfo, roomId: string): void {
  if (!isAdmin.value) return
  if (slot.type === 'ADMIN_LOCK') {
    // Right-click on admin lock: offer to release
    ElMessageBox.confirm('确定要释放此征用时段吗？释放后原预定将恢复。', '释放征用', {
      type: 'warning',
      confirmButtonText: '释放',
      cancelButtonText: '取消',
    }).then(async () => {
      if (slot.lock?.lockId) {
        await releaseAdminLock(slot.lock.lockId)
        ElMessage.success('征用已释放')
        await loadBoard()
      }
    }).catch(() => {})
    return
  }
  if (isPastSlot(slot.slot)) {
    ElMessage.warning('无法征用过去的时间')
    return
  }
  lockRoomId.value = roomId
  lockStartSlot.value = slot.slot
  lockEndSlot.value = slot.slot + 1
  lockDialogVisible.value = true
}

function handleBoardRefresh(): void {
  loadBoard()
}

// ── Board cell drag-select (simple single click for now, extends to range) ──
const datePickerRef = ref<any>(null)
function openDatePicker(): void {
  const el = datePickerRef.value?.$el
  if (el) {
    const input = el.querySelector('input')
    if (input) input.click()
  }
}

// ── Init ──
onMounted(() => {
  loadRooms()
  loadBoard()
})
</script>

<template>
  <div class="meeting-container">
    <!-- ═══════════════════════════════════════════════════════ -->
    <!-- Admin: Meeting Room Management -->
    <!-- ═══════════════════════════════════════════════════════ -->
    <el-card v-if="isAdmin" shadow="hover" class="manage-card">
      <template #header>
        <div class="card-header-row">
          <span class="card-title">🏢 会议室管理</span>
          <el-button type="primary" size="small" :icon="Plus" @click="handleAddRoom">
            新增会议室
          </el-button>
        </div>
      </template>

      <div v-loading="roomsLoading" class="room-grid">
        <div v-if="!roomsLoading && rooms.length === 0" class="empty-hint">
          暂无会议室，请点击"新增会议室"创建
        </div>
        <div
          v-for="room in rooms"
          :key="room.id"
          :class="['room-card', { 'room-inactive': room.isActive === 0 }]"
        >
          <div class="room-card-body">
            <div class="room-name">{{ room.name }}</div>
            <div class="room-meta">
              <span>📍 {{ room.location }}</span>
              <span>👥 {{ room.capacity }}座</span>
            </div>
            <div class="room-tags" v-if="room.facilities && room.facilities !== '[]'">
              <el-tag
                v-for="(f, idx) in JSON.parse(room.facilities)"
                :key="idx"
                size="small"
                type="info"
              >
                {{ f }}
              </el-tag>
            </div>
          </div>
          <div class="room-card-actions">
            <el-button size="small" @click="handleEditRoom(room)">编辑</el-button>
            <el-button size="small" :type="room.isActive === 1 ? 'warning' : 'success'" @click="handleToggleRoom(room)">
              {{ room.isActive === 1 ? '下架' : '上架' }}
            </el-button>
            <el-button size="small" type="danger" @click="handleDeleteRoom(room)">删除</el-button>
          </div>
        </div>
      </div>
    </el-card>

    <!-- ═══════════════════════════════════════════════════════ -->
    <!-- Board: Date Navigator -->
    <!-- ═══════════════════════════════════════════════════════ -->
    <el-card shadow="hover" class="board-card" :body-style="{ padding: '0' }">
      <!-- Date Nav -->
      <div class="board-date-nav">
        <button class="date-arrow-btn" @click="goToPrevDay" :disabled="boardLoading">◀ 前一天</button>

        <div class="date-picker-wrapper" @click="openDatePicker">
          <span class="date-display">{{ dateDisplay }}</span>
          <el-date-picker
            ref="datePickerRef"
            v-model="boardDate"
            type="date"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            class="date-hidden-input"
            @change="handleBoardDateChange"
          />
        </div>

        <button class="date-arrow-btn" @click="goToNextDay" :disabled="boardLoading">后一天 ▶</button>

        <button
          v-if="!isTodaySelected"
          class="back-today-btn breathing"
          @click="goToToday"
        >
          ⭕ 回到今天
        </button>

        <el-button :icon="RefreshRight" circle size="small" @click="handleBoardRefresh" :loading="boardLoading" />
      </div>

      <div class="board-divider" />

      <!-- Board Grid -->
      <div class="board-table-wrap" v-loading="boardLoading">
        <div v-if="boardData.length === 0 && !boardLoading" class="board-empty">
          <p>暂无上架的会议室</p>
        </div>

        <div v-else class="board-scroll">
          <table class="board-table">
            <!-- Header row: room info + time slot headers -->
            <thead>
              <tr>
                <th class="col-room-header">会议室</th>
                <th
                  v-for="(label, i) in timeHeaders"
                  :key="i"
                  :class="['col-slot-header', { 'col-slot-past': isPastSlot(i) }]"
                >
                  {{ label }}
                </th>
              </tr>
            </thead>

            <tbody>
              <tr v-for="roomBoard in boardData" :key="roomBoard.roomId">
                <!-- Room info cell -->
                <td class="col-room-info">
                  <div class="room-info-name">{{ roomBoard.roomName }}</div>
                  <div class="room-info-meta">
                    <span>👥{{ roomBoard.capacity }}</span>
                    <span v-if="roomBoard.facilities.length > 0" class="room-info-tags">
                      <el-tag
                        v-for="(f, idx) in roomBoard.facilities.slice(0, 2)"
                        :key="idx"
                        size="small"
                        type="info"
                      >{{ f }}</el-tag>
                    </span>
                  </div>
                </td>

                <!-- Slot cells -->
                <td
                  v-for="slot in roomBoard.slots"
                  :key="slot.slot"
                  :class="[
                    'col-slot-cell',
                    getCellClass(slot),
                    { 'cell-past': isPastSlot(slot.slot) },
                  ]"
                  :title="getCellTooltip(slot)"
                  @click="handleSlotClick(slot, roomBoard.roomId)"
                  @contextmenu.prevent="handleSlotRightClick(slot, roomBoard.roomId)"
                >
                  <span class="cell-text">{{ getCellText(slot) }}</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </el-card>

    <!-- ═══════════════════════════════════════════════════════ -->
    <!-- Dialogs -->
    <!-- ═══════════════════════════════════════════════════════ -->
    <MeetingRoomDialog
      v-model:visible="roomDialogVisible"
      :edit-data="editRoomData"
      @saved="loadRooms(); loadBoard()"
    />

    <BookingDialog
      v-model:visible="bookingDialogVisible"
      :mode="bookingMode"
      :slot-data="selectedSlot"
      :room-id="selectedRoomId"
      :booking-date="boardDate"
      @saved="loadBoard()"
    />

    <AdminLockDialog
      v-model:visible="lockDialogVisible"
      :room-id="lockRoomId"
      :lock-date="boardDate"
      :start-slot="lockStartSlot"
      :end-slot="lockEndSlot"
      @saved="loadBoard()"
    />
  </div>
</template>

<style scoped>
.meeting-container {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.card-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-title {
  font-weight: 600;
  font-size: 15px;
}

/* ── Room Cards Grid ── */
.room-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px;
}

.room-card {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 14px 16px;
  border: 1px solid #e4e7ed;
  border-radius: 10px;
  background: #fff;
  transition: box-shadow 0.2s, border-color 0.2s;
}

.room-card:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  border-color: #409eff;
}

.room-inactive {
  opacity: 0.55;
  border-color: #f0f0f0;
  background: #fafafa;
}

.room-card-body {
  margin-bottom: 10px;
}

.room-name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.room-meta {
  display: flex;
  gap: 12px;
  font-size: 13px;
  color: #909399;
  margin-bottom: 6px;
}

.room-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.room-card-actions {
  display: flex;
  gap: 6px;
}

.empty-hint {
  text-align: center;
  color: #c0c4cc;
  padding: 20px;
}

/* ── Date Navigator ── */
.board-date-nav {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 18px 24px;
  flex-wrap: wrap;
}

.date-arrow-btn {
  padding: 8px 16px;
  border: 1px solid #e4e7ed;
  background: #fafafa;
  border-radius: 8px;
  font-size: 13px;
  color: #606266;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.date-arrow-btn:hover:not(:disabled) {
  border-color: #409eff;
  color: #409eff;
  background: #ecf5ff;
}

.date-arrow-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.date-picker-wrapper {
  position: relative;
  cursor: pointer;
  user-select: none;
}

.date-display {
  display: inline-block;
  padding: 8px 20px;
  font-size: 17px;
  font-weight: 700;
  color: #303133;
  border-bottom: 2px dashed transparent;
  transition: all 0.2s;
}

.date-picker-wrapper:hover .date-display {
  color: #409eff;
  border-bottom-color: #409eff;
}

.date-hidden-input {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  opacity: 0;
  overflow: hidden;
}

.date-hidden-input :deep(.el-input__wrapper) {
  cursor: pointer;
}

.back-today-btn {
  padding: 6px 14px;
  border: 1.5px solid #409eff;
  background: #ecf5ff;
  border-radius: 20px;
  font-size: 13px;
  color: #409eff;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
  font-weight: 500;
}

.back-today-btn:hover {
  background: #409eff;
  color: #fff;
}

.breathing {
  animation: breathe 2s ease-in-out infinite;
}

@keyframes breathe {
  0%, 100% { box-shadow: 0 0 0 0 rgba(64, 158, 255, 0.4); }
  50% { box-shadow: 0 0 0 10px rgba(64, 158, 255, 0); }
}

.board-divider {
  height: 1px;
  background: linear-gradient(to right, #e4e7ed, #f0f0f0, #e4e7ed);
  margin: 0 24px;
}

/* ── Board Grid ── */
.board-table-wrap {
  padding: 16px 24px 24px;
  min-height: 120px;
}

.board-empty {
  text-align: center;
  color: #c0c4cc;
  padding: 40px;
  font-size: 14px;
}

.board-scroll {
  overflow-x: auto;
}

.board-table {
  border-collapse: collapse;
  width: auto;
  min-width: 100%;
  font-size: 12px;
}

.col-room-header {
  position: sticky;
  left: 0;
  background: #f5f7fa;
  padding: 10px 12px;
  font-weight: 600;
  color: #303133;
  min-width: 140px;
  white-space: nowrap;
  border: 1px solid #e4e7ed;
  z-index: 2;
}

.col-slot-header {
  padding: 10px 4px;
  font-weight: 600;
  color: #606266;
  min-width: 52px;
  text-align: center;
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
  font-size: 11px;
}

.col-slot-past {
  opacity: 0.5;
}

.col-room-info {
  position: sticky;
  left: 0;
  background: #fff;
  padding: 8px 12px;
  border: 1px solid #e4e7ed;
  z-index: 1;
  white-space: nowrap;
}

.room-info-name {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.room-info-meta {
  font-size: 11px;
  color: #909399;
  display: flex;
  gap: 6px;
  align-items: center;
  flex-wrap: wrap;
}

.room-info-tags {
  display: inline-flex;
  gap: 3px;
}

/* ── Slot Cell ── */
.col-slot-cell {
  min-width: 52px;
  height: 42px;
  text-align: center;
  vertical-align: middle;
  border: 1px solid #ebeef5;
  cursor: pointer;
  transition: all 0.15s;
  position: relative;
}

.col-slot-cell:hover {
  outline: 2px solid #409eff;
  outline-offset: -1px;
  z-index: 1;
}

.cell-text {
  display: block;
  font-size: 11px;
  font-weight: 500;
  line-height: 1.2;
  padding: 2px 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Free */
.cell-free {
  background: #ffffff;
}
.cell-free:hover {
  background: #f0f9eb;
}

/* My booking */
.cell-my {
  background: #409eff;
  color: #fff;
}
.cell-my .cell-text {
  color: #fff;
}

/* Others booking */
.cell-others {
  background: #c0c4cc;
  color: #303133;
}
.cell-others .cell-text {
  color: #303133;
}

/* Admin lock — diagonal stripe pattern */
.cell-lock {
  background: repeating-linear-gradient(
    45deg,
    #fef0f0,
    #fef0f0 4px,
    #fde2e2 4px,
    #fde2e2 8px
  );
  color: #f56c6c;
}
.cell-lock .cell-text {
  color: #c45656;
  font-weight: 700;
}

/* Past slot overlay */
.cell-past {
  filter: brightness(0.75);
  cursor: not-allowed;
}

.cell-past:hover {
  outline: none;
}

/* ── Responsive ── */
@media (max-width: 768px) {
  .meeting-container {
    padding: 12px;
    gap: 12px;
  }

  .board-date-nav {
    padding: 12px 14px;
    gap: 8px;
  }

  .board-table-wrap {
    padding: 10px 14px 16px;
  }

  .date-arrow-btn {
    padding: 6px 10px;
    font-size: 12px;
  }

  .date-display {
    font-size: 15px;
    padding: 6px 12px;
  }
}
</style>
