import request from '@/utils/request'
import type { ApiResult } from './loginRecord'
import type { PageData } from './gymEquipment'

export interface GymActionEquipmentRel {
  id: string
  actionId: string
  equipmentId: string
  actionName?: string
  equipmentName?: string
}

export function queryGymActionEquipmentRel(params: {
  page?: number; size?: number; actionId?: string; equipmentId?: string
}): Promise<ApiResult<PageData<GymActionEquipmentRel>>> {
  return request.get('/gym-action-equipment-rel', { params })
}

export function listByActionIdEquipment(actionId: string): Promise<ApiResult<GymActionEquipmentRel[]>> {
  return request.get(`/gym-action-equipment-rel/by-action/${actionId}`)
}

export function createGymActionEquipmentRel(data: Partial<GymActionEquipmentRel>): Promise<ApiResult<GymActionEquipmentRel>> {
  return request.post('/gym-action-equipment-rel', data)
}

export function deleteGymActionEquipmentRel(id: string): Promise<ApiResult<null>> {
  return request.delete(`/gym-action-equipment-rel/${id}`)
}
