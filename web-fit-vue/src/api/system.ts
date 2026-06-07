import request from '@/utils/request'

export interface SystemInfo {
  loginIp: string
}

export function getSystemInfo(): Promise<{ code: number; message: string; data: SystemInfo }> {
  return request.get('/system/info')
}
