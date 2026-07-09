import request from '@/utils/request'
import type { ApiResult } from './loginRecord'

export interface MeetingAdminLock {
  id: string
  roomId: string
  lockDate: string
  startSlot: number
  endSlot: number
  reason: string
  deptName: string
  operatorEmpNo: string
  createTime: string
}

/** 创建强制征用 */
export function createAdminLock(params: {
  roomId: string
  lockDate: string
  startSlot: number
  endSlot: number
  reason?: string
  deptName?: string
}): Promise<ApiResult<MeetingAdminLock>> {
  return request.post('/meeting-admin-lock', params)
}

/** 释放征用 */
export function releaseAdminLock(id: string): Promise<ApiResult<null>> {
  return request.delete(`/meeting-admin-lock/${id}`)
}

/** 查询指定会议室某日征用记录 */
export function getRoomLocks(roomId: string, date: string): Promise<ApiResult<MeetingAdminLock[]>> {
  return request.get(`/meeting-admin-lock/room/${roomId}`, { params: { date } })
}
