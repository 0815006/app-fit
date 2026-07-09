export interface LoginRecord {
  id: string
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
