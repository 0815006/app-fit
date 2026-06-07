import request from '@/utils/request'
import type { LoginRecord } from '@/typings'

export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export function createLoginRecord(loginType: string): Promise<ApiResult<LoginRecord>> {
  return request.post('/login-record', { loginType })
}

export function getLoginCount(empNo: string): Promise<ApiResult<number>> {
  return request.get(`/login-record/count/${empNo}`)
}
