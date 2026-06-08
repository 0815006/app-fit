import request from '@/utils/request'
import type { ApiResult } from './loginRecord'
import type { PageData } from './gymEquipment'

export interface GymActionRecommendation {
  id: string
  actionId: string
  trainingGoal: string
  minSets: number
  maxSets: number
  minReps: number
  maxReps: number
  recommendRestTime: number
  intensityTips: string
  actionName?: string
  createTime: string
  updateTime: string
}

export function queryGymActionRecommendation(params: {
  page?: number; size?: number; actionId?: string; trainingGoal?: string
}): Promise<ApiResult<PageData<GymActionRecommendation>>> {
  return request.get('/gym-action-recommendation', { params })
}

export function listRecByActionId(actionId: string): Promise<ApiResult<GymActionRecommendation[]>> {
  return request.get(`/gym-action-recommendation/by-action/${actionId}`)
}

export function getGymActionRecommendation(id: string): Promise<ApiResult<GymActionRecommendation>> {
  return request.get(`/gym-action-recommendation/${id}`)
}

export function createGymActionRecommendation(data: Partial<GymActionRecommendation>): Promise<ApiResult<GymActionRecommendation>> {
  return request.post('/gym-action-recommendation', data)
}

export function updateGymActionRecommendation(id: string, data: Partial<GymActionRecommendation>): Promise<ApiResult<GymActionRecommendation>> {
  return request.put(`/gym-action-recommendation/${id}`, data)
}

export function deleteGymActionRecommendation(id: string): Promise<ApiResult<null>> {
  return request.delete(`/gym-action-recommendation/${id}`)
}
