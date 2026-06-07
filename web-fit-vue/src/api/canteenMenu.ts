import request from '@/utils/request'
import type { ApiResult } from './loginRecord'

export interface CanteenMenuRecord {
  id: string
  canteenZone: string
  menuDate: string
  weekDay: string
  mealType: string
  categoryName: string
  dishName: string
  unit: string
  price: number
  energyKcal: number
  isSpicy: number
  importBatchNo: string
  createTime: string
}

export interface PageData<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

/** Upload Excel file for canteen menu import */
export function uploadCanteenMenu(file: File): Promise<ApiResult<{ batchNo: string }>> {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/canteen-menu/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000, // 2 min for large Excel parsing
  })
}

/** Query canteen menu records */
export function queryCanteenMenu(params: {
  page?: number
  size?: number
  canteenZone?: string
  menuDate?: string
  mealType?: string
}): Promise<ApiResult<PageData<CanteenMenuRecord>>> {
  return request.get('/canteen-menu/records', { params })
}

/** Get all import batches */
export function getCanteenMenuBatches(): Promise<ApiResult<string[]>> {
  return request.get('/canteen-menu/batches')
}

/** Delete a batch and its records */
export function deleteCanteenMenuBatch(batchNo: string): Promise<ApiResult<null>> {
  return request.delete(`/canteen-menu/batches/${batchNo}`)
}
