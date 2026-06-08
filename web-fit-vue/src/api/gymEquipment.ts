import request from '@/utils/request'
import type { ApiResult } from './loginRecord'

export interface GymEquipment {
  id: string
  equipmentCode: string
  equipmentName: string
  equipmentType: string
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

/** Paginated query with optional filters */
export function queryGymEquipment(params: {
  page?: number
  size?: number
  equipmentName?: string
  equipmentType?: string
}): Promise<ApiResult<PageData<GymEquipment>>> {
  return request.get('/gym-equipment', { params })
}

/** List all (for dropdowns) */
export function listAllGymEquipment(): Promise<ApiResult<GymEquipment[]>> {
  return request.get('/gym-equipment/all')
}

/** Get by id */
export function getGymEquipment(id: string): Promise<ApiResult<GymEquipment>> {
  return request.get(`/gym-equipment/${id}`)
}

/** Create */
export function createGymEquipment(data: Partial<GymEquipment>): Promise<ApiResult<GymEquipment>> {
  return request.post('/gym-equipment', data)
}

/** Update */
export function updateGymEquipment(id: string, data: Partial<GymEquipment>): Promise<ApiResult<GymEquipment>> {
  return request.put(`/gym-equipment/${id}`, data)
}

/** Delete */
export function deleteGymEquipment(id: string): Promise<ApiResult<null>> {
  return request.delete(`/gym-equipment/${id}`)
}
