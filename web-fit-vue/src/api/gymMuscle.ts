import request from '@/utils/request'
import type { ApiResult } from './loginRecord'
import type { PageData } from './gymEquipment'

export interface GymMuscle {
  id: string
  muscleCode: string
  muscleName: string
  muscleGroup: string
  sortNo: number
}

export function queryGymMuscle(params: {
  page?: number; size?: number; muscleName?: string; muscleGroup?: string
}): Promise<ApiResult<PageData<GymMuscle>>> {
  return request.get('/gym-muscle', { params })
}

export function listAllGymMuscle(): Promise<ApiResult<GymMuscle[]>> {
  return request.get('/gym-muscle/all')
}

export function getGymMuscle(id: string): Promise<ApiResult<GymMuscle>> {
  return request.get(`/gym-muscle/${id}`)
}

export function createGymMuscle(data: Partial<GymMuscle>): Promise<ApiResult<GymMuscle>> {
  return request.post('/gym-muscle', data)
}

export function updateGymMuscle(id: string, data: Partial<GymMuscle>): Promise<ApiResult<GymMuscle>> {
  return request.put(`/gym-muscle/${id}`, data)
}

export function deleteGymMuscle(id: string): Promise<ApiResult<null>> {
  return request.delete(`/gym-muscle/${id}`)
}
