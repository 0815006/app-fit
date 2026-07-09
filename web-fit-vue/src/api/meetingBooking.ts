import request from '@/utils/request'
import type { ApiResult } from './loginRecord'

export interface MeetingBooking {
  id: string
  roomId: string
  bookingDate: string
  startSlot: number
  endSlot: number
  empNo: string
  empName: string
  meetingTitle: string
  attendees: string
  isWeeklyFix: number
  groupId: string
  status: number
  createTime: string
  updateTime: string
}

/** 看板响应 */
export interface BoardResponse {
  rooms: RoomBoard[]
}

export interface RoomBoard {
  roomId: string
  roomName: string
  capacity: number
  facilities: string[]
  photoUrl: string
  slots: SlotInfo[]
}

export interface SlotInfo {
  slot: number
  timeLabel: string
  type: 'FREE' | 'MY_BOOKING' | 'BOOKED' | 'ADMIN_LOCK'
  booking: BookingInfo | null
  lock: LockInfo | null
}

export interface BookingInfo {
  bookingId: string
  empNo: string
  empName: string
  meetingTitle: string
  attendees: string
  isWeeklyFix: boolean
  groupId: string
}

export interface LockInfo {
  lockId: string
  reason: string
  deptName: string
}

/** 创建预定 */
export function createBooking(params: {
  roomId: string
  bookingDate: string
  startSlot: number
  endSlot: number
  empName?: string
  meetingTitle?: string
  attendees?: string
  weeklyWeeks?: number
}): Promise<ApiResult<MeetingBooking[]>> {
  return request.post('/meeting-booking', params)
}

/** 修改预定信息 */
export function updateBookingInfo(id: string, data: {
  meetingTitle?: string
  attendees?: string
}): Promise<ApiResult<MeetingBooking>> {
  return request.put(`/meeting-booking/${id}`, data)
}

/** 取消单次预定 */
export function cancelBooking(id: string): Promise<ApiResult<null>> {
  return request.delete(`/meeting-booking/${id}`)
}

/** 一键取消后续所有周期约 */
export function cancelGroupFuture(groupId: string, fromDate: string): Promise<ApiResult<number>> {
  return request.delete(`/meeting-booking/group/${groupId}`, { params: { fromDate } })
}

/** 查询指定会议室某日预定 */
export function getRoomBookings(roomId: string, date: string): Promise<ApiResult<MeetingBooking[]>> {
  return request.get(`/meeting-booking/room/${roomId}`, { params: { date } })
}

/** 全局看板 */
export function getBoard(date: string): Promise<ApiResult<BoardResponse>> {
  return request.get('/meeting-booking/board', { params: { date } })
}
