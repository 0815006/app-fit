import request from '@/utils/request'
import type { ApiResult } from './loginRecord'

export interface TripPlan {
  id: string
  userId: string
  title: string
  status: string
  tripDays: number
  destination: string | null
  departureTime: string | null
  returnTime: string | null
  isPublic: number
  parentTemplateId: string | null
  createTime: string
  updateTime: string
}

export interface TripPlanDetail {
  id: string
  planId: string
  itemId: string
  itemName: string
  container: string
  importanceLevel: string
  targetQuantity: number
  isChecked: number
  excludeFlag: number
  sourceContextsJson: string
  versionNo: number
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

/** 分页查询计划 */
export function queryTripPlans(params: {
  page?: number
  size?: number
  status?: string
}): Promise<ApiResult<PageData<TripPlan>>> {
  return request.get('/trip-plan', { params })
}

/** 获取用户所有计划 */
export function listMyTripPlans(): Promise<ApiResult<TripPlan[]>> {
  return request.get('/trip-plan/my')
}

/** 根据ID获取计划 */
export function getTripPlan(id: string): Promise<ApiResult<TripPlan>> {
  return request.get(`/trip-plan/${id}`)
}

/** 创建计划 */
export function createTripPlan(data: Partial<TripPlan>): Promise<ApiResult<TripPlan>> {
  return request.post('/trip-plan', data)
}

/** 更新计划 */
export function updateTripPlan(id: string, data: Partial<TripPlan>): Promise<ApiResult<TripPlan>> {
  return request.put(`/trip-plan/${id}`, data)
}

/** 删除计划 */
export function deleteTripPlan(id: string): Promise<ApiResult<null>> {
  return request.delete(`/trip-plan/${id}`)
}

/** 更新计划状态 */
export function updateTripPlanStatus(id: string, status: string): Promise<ApiResult<null>> {
  return request.put(`/trip-plan/${id}/status`, null, { params: { status } })
}

/** 根据标签生成计划明细 */
export function generateTripPlanDetails(id: string, tagIds: string[]): Promise<ApiResult<TripPlanDetail[]>> {
  return request.post(`/trip-plan/${id}/generate`, tagIds)
}

/** 获取计划明细列表 */
export function getTripPlanDetails(id: string): Promise<ApiResult<TripPlanDetail[]>> {
  return request.get(`/trip-plan/${id}/details`)
}

/** 更新明细装箱状态 */
export function updateTripPlanDetailChecked(id: string, isChecked: number): Promise<ApiResult<null>> {
  return request.put(`/trip-plan/detail/${id}/check`, null, { params: { isChecked } })
}

/** 更新明细排除标志 */
export function updateTripPlanDetailExcludeFlag(id: string, excludeFlag: number): Promise<ApiResult<null>> {
  return request.put(`/trip-plan/detail/${id}/exclude`, null, { params: { excludeFlag } })
}

/** 更新明细数量 */
export function updateTripPlanDetailQuantity(id: string, targetQuantity: number): Promise<ApiResult<null>> {
  return request.put(`/trip-plan/detail/${id}/quantity`, null, { params: { targetQuantity } })
}

/** 手动添加物品到计划 */
export function addTripPlanDetail(id: string, data: Partial<TripPlanDetail>): Promise<ApiResult<TripPlanDetail>> {
  return request.post(`/trip-plan/${id}/detail`, data)
}
