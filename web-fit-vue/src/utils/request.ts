import axios from 'axios'
import { getEmpNo } from './currentUser'
import type { ApiResult } from '@/typings'

const TOKEN_KEY = 'satoken'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

// 请求拦截器：注入 satoken 和 X-Emp-No
request.interceptors.request.use((config) => {
  // Sa-Token 认证
  const satoken = localStorage.getItem(TOKEN_KEY)
  if (satoken) {
    config.headers['satoken'] = satoken
  }
  // 兼容原有 EmpContext（后端会优先使用 Sa-Token 推导）
  config.headers['X-Emp-No'] = getEmpNo()
  return config
})

// 响应拦截器：处理 401 和统一 Result
request.interceptors.response.use(
  (response) => {
    const result = response.data as ApiResult<unknown>
    if (result.code === 401) {
      localStorage.removeItem(TOKEN_KEY)
      window.location.href = '/login'
      return Promise.reject(new Error(result.message || '登录已过期'))
    }
    if (result.code !== 200) {
      console.error(`API Error: ${result.message}`)
      return Promise.reject(new Error(result.message || '请求失败'))
    }
    return result
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem(TOKEN_KEY)
      window.location.href = '/login'
    }
    console.error('Network error:', error)
    return Promise.reject(error)
  }
)

export default request
