import request from '@/utils/request'
import type { ApiResult } from './loginRecord'
import type { PageData } from './gymEquipment'

export interface GymActionMuscleRel {
  id: string
  actionId: string
  muscleId: string
  isPrimary: number
  actionName?: string
  muscleName?: string
}

export function queryGymActionMuscleRel(params: {
  page?: number; size?: number; actionId?: string; muscleId?: string
}): Promise<ApiResult<PageData<GymActionMuscleRel>>> {
  return request.get('/gym-action-muscle-rel', { params })
}

export function listByActionId(actionId: string): Promise<ApiResult<GymActionMuscleRel[]>> {
  return request.get(`/gym-action-muscle-rel/by-action/${actionId}`)
}

export function listRelByMuscleGroup(muscleGroup: string): Promise<ApiResult<GymActionMuscleRel[]>> {
  return request.get(`/gym-action-muscle-rel/by-muscle-group/${encodeURIComponent(muscleGroup)}`)
}

export function createGymActionMuscleRel(data: Partial<GymActionMuscleRel>): Promise<ApiResult<GymActionMuscleRel>> {
  return request.post('/gym-action-muscle-rel', data)
}

export function deleteGymActionMuscleRel(id: string): Promise<ApiResult<null>> {
  return request.delete(`/gym-action-muscle-rel/${id}`)
}
