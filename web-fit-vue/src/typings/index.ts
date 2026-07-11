export interface LoginRecord {
  id: string
  userId: string
  empNo: string
  loginType: string
  createTime: string
  updateTime: string
}

export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export interface WebStatsDTO {
  myWebCount: number
  totalWebCount: number
  totalMiniProgramCount: number
  totalAllCount: number
}

export interface UserWithStatsDTO {
  id: string
  username: string
  wxOpenid: string
  empNo: string
  empName: string
  nickname: string
  avatarUrl: string
  status: number
  loginCount: number
  lastLoginTime: string | null
  createTime: string
  updateTime: string
}
