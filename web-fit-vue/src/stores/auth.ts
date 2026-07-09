import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { webLogin, fetchCurrentUser, type UserInfo } from '@/api/auth'
import { setEmpNo } from '@/utils/currentUser'

// ========== 常量 ==========
const TOKEN_KEY = 'satoken'

// ========== 全局单例状态 ==========
const token = ref<string>(localStorage.getItem(TOKEN_KEY) || '')
const currentUser = ref<UserInfo | null>(null)
const loading = ref(false)

export function useAuth() {
  const router = useRouter()

  const isLoggedIn = computed(() => !!token.value)

  /**
   * Web 端登录
   */
  async function login(username: string, password: string): Promise<void> {
    loading.value = true
    try {
      const res = await webLogin(username, password)
      const { token: t, userInfo } = res.data

      token.value = t
      currentUser.value = userInfo
      localStorage.setItem(TOKEN_KEY, t)

      // 同步 empNo 到 currentUser.ts（保持 UserSwitcher 兼容）
      if (userInfo.empNo) {
        setEmpNo(userInfo.empNo)
      }
    } finally {
      loading.value = false
    }
  }

  /**
   * 退出登录
   */
  function logout(): void {
    token.value = ''
    currentUser.value = null
    localStorage.removeItem(TOKEN_KEY)
    router.push('/login')
  }

  /**
   * 从 localStorage 恢复登录态
   */
  async function initFromStorage(): Promise<boolean> {
    const storedToken = localStorage.getItem(TOKEN_KEY)
    if (!storedToken) return false

    token.value = storedToken
    try {
      const res = await fetchCurrentUser()
      currentUser.value = res.data
      if (res.data.empNo) {
        setEmpNo(res.data.empNo)
      }
      return true
    } catch {
      // Token 失效
      token.value = ''
      localStorage.removeItem(TOKEN_KEY)
      return false
    }
  }

  /**
   * 获取当前 token（供 request.ts 拦截器使用）
   */
  function getToken(): string {
    return token.value
  }

  return {
    token,
    currentUser,
    loading,
    isLoggedIn,
    login,
    logout,
    initFromStorage,
    getToken,
  }
}
