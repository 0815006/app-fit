import request from '@/utils/request'
import type { ApiResult, UserWithStatsDTO } from '@/typings'

// ========== 类型定义 ==========

export interface UserInfo {
  id: string
  username: string | null
  wxOpenid: string | null
  empNo: string
  empName: string | null
  nickname: string | null
  avatarUrl: string | null
  status: number
  createTime: string
  updateTime: string
}

export interface LoginResult {
  token: string
  isNewUser: boolean
  userInfo: UserInfo
}

// ========== API 函数 ==========

export function webLogin(username: string, password: string): Promise<ApiResult<LoginResult>> {
  return request.post('/auth/web-login', { username, password })
}

export function wxLogin(code: string): Promise<ApiResult<LoginResult>> {
  return request.post('/auth/wx-login', { code })
}

export function fetchCurrentUser(): Promise<ApiResult<UserInfo>> {
  return request.get('/user/current')
}

export function updateProfile(nickname: string, avatarUrl: string): Promise<ApiResult<string>> {
  return request.post('/user/update-profile', { nickname, avatarUrl })
}

export function changePassword(oldPassword: string, newPassword: string): Promise<ApiResult<string>> {
  return request.post('/user/change-password', { oldPassword, newPassword })
}

export function uploadAvatar(file: File): Promise<ApiResult<{ url: string }>> {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/upload/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function fetchUserListWithStats(): Promise<ApiResult<UserWithStatsDTO[]>> {
  return request.get('/user/list-with-stats')
}
