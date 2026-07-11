import request from '@/utils/request'
import type { ApiResult } from './loginRecord'
import type { PageData } from './gymEquipment'

export interface GymAction {
  id: string
  name: string
  alias: string
  pinyinBref: string
  actionType: string
  movementPattern: string
  difficultyLevel: number
  imageUrls: string
  videoUrl: string
  actionGuide: string
  safetyTips: string
  searchKeywords: string
  isCommon: number
  status: number
  createTime: string
  updateTime: string
}

export function queryGymAction(params: {
  page?: number; size?: number; name?: string
  actionType?: string; movementPattern?: string; difficultyLevel?: number
}): Promise<ApiResult<PageData<GymAction>>> {
  return request.get('/gym-action', { params })
}

export function listAllGymAction(): Promise<ApiResult<GymAction[]>> {
  return request.get('/gym-action/all')
}

/** GET /api/gym-action/by-muscle-group/{muscleGroup} */
export function listByMuscleGroup(muscleGroup: string): Promise<ApiResult<GymAction[]>> {
  return request.get(`/gym-action/by-muscle-group/${encodeURIComponent(muscleGroup)}`)
}

export function getGymAction(id: string): Promise<ApiResult<GymAction>> {
  return request.get(`/gym-action/${id}`)
}

export function createGymAction(data: Partial<GymAction>): Promise<ApiResult<GymAction>> {
  return request.post('/gym-action', data)
}

export function updateGymAction(id: string, data: Partial<GymAction>): Promise<ApiResult<GymAction>> {
  return request.put(`/gym-action/${id}`, data)
}

export function deleteGymAction(id: string): Promise<ApiResult<null>> {
  return request.delete(`/gym-action/${id}`)
}
