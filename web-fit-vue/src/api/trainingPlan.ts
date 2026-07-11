import request from '@/utils/request'
import type { ApiResult } from './loginRecord'
import type { PageData } from './gymEquipment'

// 训练计划
export interface TrainingPlan {
  id: string
  userId: string
  planName: string
  description: string
  muscleGroup: string
  sortNo: number
  status: number
  createTime: string
  updateTime: string
}

// 训练计划详情
export interface TrainingPlanDetail {
  id: string
  planId: string
  actionId: string
  sortNo: number
  targetSets: number
  targetReps: number
  targetWeight: number
  restSeconds: number
  notes: string
  createTime: string
  updateTime: string
  // 关联动作信息（前端展示用）
  actionName?: string
}

// 查询训练计划分页
export function queryTrainingPlan(params: {
  page?: number
  size?: number
  planName?: string
  muscleGroup?: string
}): Promise<ApiResult<PageData<TrainingPlan>>> {
  return request.get('/training-plan', { params })
}

// 获取所有训练计划
export function listAllTrainingPlan(): Promise<ApiResult<TrainingPlan[]>> {
  return request.get('/training-plan/all')
}

// 获取训练计划详情
export function getTrainingPlan(id: string): Promise<ApiResult<TrainingPlan>> {
  return request.get(`/training-plan/${id}`)
}

// 获取训练计划详情列表
export function getTrainingPlanDetails(planId: string): Promise<ApiResult<TrainingPlanDetail[]>> {
  return request.get(`/training-plan/${planId}/details`)
}

// 创建训练计划
export function createTrainingPlan(data: Partial<TrainingPlan>): Promise<ApiResult<TrainingPlan>> {
  return request.post('/training-plan', data)
}

// 更新训练计划
export function updateTrainingPlan(id: string, data: Partial<TrainingPlan>): Promise<ApiResult<TrainingPlan>> {
  return request.put(`/training-plan/${id}`, data)
}

// 删除训练计划
export function deleteTrainingPlan(id: string): Promise<ApiResult<null>> {
  return request.delete(`/training-plan/${id}`)
}

// 添加训练计划详情
export function addTrainingPlanDetail(planId: string, data: Partial<TrainingPlanDetail>): Promise<ApiResult<TrainingPlanDetail>> {
  return request.post(`/training-plan/${planId}/details`, data)
}

// 更新训练计划详情
export function updateTrainingPlanDetail(detailId: string, data: Partial<TrainingPlanDetail>): Promise<ApiResult<TrainingPlanDetail>> {
  return request.put(`/training-plan/details/${detailId}`, data)
}

// 删除训练计划详情
export function deleteTrainingPlanDetail(detailId: string): Promise<ApiResult<null>> {
  return request.delete(`/training-plan/details/${detailId}`)
}
