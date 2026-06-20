import request from '@/utils/request'
import type { ApiResult } from './loginRecord'
import type { PageData } from './gymEquipment'

// 身体指标
export interface BodyMetric {
  id: string
  empNo: string
  metricDate: string
  weight: number
  bodyFat: number
  bmi: number
  muscleMass: number
  notes: string
  createTime: string
  updateTime: string
}

// 查询身体指标分页
export function queryBodyMetric(params: {
  page?: number
  size?: number
  startDate?: string
  endDate?: string
}): Promise<ApiResult<PageData<BodyMetric>>> {
  return request.get('/body-metric', { params })
}

// 获取身体指标列表（按日期范围）
export function listBodyMetric(params: {
  startDate?: string
  endDate?: string
}): Promise<ApiResult<BodyMetric[]>> {
  return request.get('/body-metric/list', { params })
}

// 获取身体指标详情
export function getBodyMetric(id: string): Promise<ApiResult<BodyMetric>> {
  return request.get(`/body-metric/${id}`)
}

// 创建身体指标
export function createBodyMetric(data: Partial<BodyMetric>): Promise<ApiResult<BodyMetric>> {
  return request.post('/body-metric', data)
}

// 更新身体指标
export function updateBodyMetric(id: string, data: Partial<BodyMetric>): Promise<ApiResult<BodyMetric>> {
  return request.put(`/body-metric/${id}`, data)
}

// 删除身体指标
export function deleteBodyMetric(id: string): Promise<ApiResult<null>> {
  return request.delete(`/body-metric/${id}`)
}
