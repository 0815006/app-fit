import request from '@/utils/request'
import type { ApiResult } from './loginRecord'

export interface TripTag {
  id: string
  name: string
  type: string
  isPreset: number
  createTime: string
  updateTime: string
}

/** 获取所有标签 */
export function listAllTripTags(): Promise<ApiResult<TripTag[]>> {
  return request.get('/trip-tag/all')
}

/** 根据类型获取标签 */
export function listTripTagsByType(type: string): Promise<ApiResult<TripTag[]>> {
  return request.get(`/trip-tag/type/${type}`)
}

/** 根据ID获取标签 */
export function getTripTag(id: string): Promise<ApiResult<TripTag>> {
  return request.get(`/trip-tag/${id}`)
}

/** 创建标签 */
export function createTripTag(data: Partial<TripTag>): Promise<ApiResult<TripTag>> {
  return request.post('/trip-tag', data)
}

/** 更新标签 */
export function updateTripTag(id: string, data: Partial<TripTag>): Promise<ApiResult<TripTag>> {
  return request.put(`/trip-tag/${id}`, data)
}

/** 删除标签 */
export function deleteTripTag(id: string): Promise<ApiResult<null>> {
  return request.delete(`/trip-tag/${id}`)
}
