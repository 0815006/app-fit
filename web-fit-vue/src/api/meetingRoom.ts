import request from '@/utils/request'
import type { ApiResult } from './loginRecord'

export interface MeetingRoom {
  id: string
  name: string
  location: string
  capacity: number
  facilities: string
  photoUrl: string
  isActive: number
  sortOrder: number
  createTime: string
  updateTime: string
}

/** 查询所有会议室（含下架） */
export function listAllMeetingRooms(): Promise<ApiResult<MeetingRoom[]>> {
  return request.get('/meeting-room')
}

/** 查询上架会议室 */
export function listActiveMeetingRooms(): Promise<ApiResult<MeetingRoom[]>> {
  return request.get('/meeting-room/active')
}

/** 新增会议室 */
export function createMeetingRoom(room: Partial<MeetingRoom>): Promise<ApiResult<MeetingRoom>> {
  return request.post('/meeting-room', room)
}

/** 编辑会议室 */
export function updateMeetingRoom(id: string, room: Partial<MeetingRoom>): Promise<ApiResult<MeetingRoom>> {
  return request.put(`/meeting-room/${id}`, room)
}

/** 删除会议室 */
export function deleteMeetingRoom(id: string): Promise<ApiResult<null>> {
  return request.delete(`/meeting-room/${id}`)
}

/** 上架/下架切换 */
export function toggleMeetingRoom(id: string): Promise<ApiResult<number>> {
  return request.put(`/meeting-room/${id}/toggle`)
}
