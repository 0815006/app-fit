/** 登录记录 */
interface LoginRecord {
  id: string
  empNo: string
  loginType: string
  createTime: string
  updateTime: string
}

interface IAppOption {
  globalData: Record<string, unknown>
}
