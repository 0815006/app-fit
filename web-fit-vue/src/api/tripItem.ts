import request from '@/utils/request'
import type { ApiResult } from './loginRecord'

export interface TripItem {
  id: string
  userId: string | null
  name: string
  category: string
  defaultContainer: string
  importanceLevel: string
  createTime: string
  updateTime: string
}

export interface PageData<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

/** 分页查询物品 */
export function queryTripItems(params: {
  page?: number
  size?: number
  name?: string
  category?: string
}): Promise<ApiResult<PageData<TripItem>>> {
  return request.get('/trip-item', { params })
}

/** 获取所有物品(用于下拉选择) */
export function listAllTripItems(): Promise<ApiResult<TripItem[]>> {
  return request.get('/trip-item/all')
}

/** 根据ID获取物品 */
export function getTripItem(id: string): Promise<ApiResult<TripItem>> {
  return request.get(`/trip-item/${id}`)
}

/** 创建物品 */
export function createTripItem(data: Partial<TripItem>): Promise<ApiResult<TripItem>> {
  return request.post('/trip-item', data)
}

/** 更新物品 */
export function updateTripItem(id: string, data: Partial<TripItem>): Promise<ApiResult<TripItem>> {
  return request.put(`/trip-item/${id}`, data)
}

/** 删除物品 */
export function deleteTripItem(id: string): Promise<ApiResult<null>> {
  return request.delete(`/trip-item/${id}`)
}
