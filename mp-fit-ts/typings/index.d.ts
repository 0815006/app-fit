/** 登录记录 */
interface LoginRecord {
  id: string
  empNo: string
  loginType: string
  createTime: string
  updateTime: string
}

/** 技术选型项 */
interface TechItem {
  category: string
  name: string
  version: string
}

interface IAppOption {
  globalData: Record<string, unknown>
}
