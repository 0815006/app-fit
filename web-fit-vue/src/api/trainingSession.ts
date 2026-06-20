import request from '@/utils/request'
import type { ApiResult } from './loginRecord'
import type { PageData } from './gymEquipment'

// 训练记录
export interface TrainingSession {
  id: string
  empNo: string
  planId: string
  sessionDate: string
  startTime: string
  endTime: string
  totalVolume: number
  totalDuration: number
  feeling: number
  notes: string
  createTime: string
  updateTime: string
  // 关联计划信息（前端展示用）
  planName?: string
}

// 训练记录详情
export interface TrainingSessionDetail {
  id: string
  sessionId: string
  planDetailId: string
  actionId: string
  sortNo: number
  setNo: number
  targetWeight: number
  targetReps: number
  actualWeight: number
  actualReps: number
  isCompleted: number
  isPr: number
  notes: string
  createTime: string
  updateTime: string
  // 关联动作信息（前端展示用）
  actionName?: string
}

// 查询训练记录分页
export function queryTrainingSession(params: {
  page?: number
  size?: number
  startDate?: string
  endDate?: string
}): Promise<ApiResult<PageData<TrainingSession>>> {
  return request.get('/training-session', { params })
}

// 获取训练记录详情
export function getTrainingSession(id: string): Promise<ApiResult<TrainingSession>> {
  return request.get(`/training-session/${id}`)
}

// 获取训练记录详情列表
export function getTrainingSessionDetails(sessionId: string): Promise<ApiResult<TrainingSessionDetail[]>> {
  return request.get(`/training-session/${sessionId}/details`)
}

// 创建训练记录
export function createTrainingSession(data: Partial<TrainingSession>): Promise<ApiResult<TrainingSession>> {
  return request.post('/training-session', data)
}

// 更新训练记录
export function updateTrainingSession(id: string, data: Partial<TrainingSession>): Promise<ApiResult<TrainingSession>> {
  return request.put(`/training-session/${id}`, data)
}

// 删除训练记录
export function deleteTrainingSession(id: string): Promise<ApiResult<null>> {
  return request.delete(`/training-session/${id}`)
}

// 添加训练记录详情
export function addTrainingSessionDetail(sessionId: string, data: Partial<TrainingSessionDetail>): Promise<ApiResult<TrainingSessionDetail>> {
  return request.post(`/training-session/${sessionId}/details`, data)
}

// 更新训练记录详情
export function updateTrainingSessionDetail(detailId: string, data: Partial<TrainingSessionDetail>): Promise<ApiResult<TrainingSessionDetail>> {
  return request.put(`/training-session/details/${detailId}`, data)
}

// 删除训练记录详情
export function deleteTrainingSessionDetail(detailId: string): Promise<ApiResult<null>> {
  return request.delete(`/training-session/details/${detailId}`)
}
