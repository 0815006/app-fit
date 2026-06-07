import axios from 'axios'
import { getEmpNo } from './currentUser'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

request.interceptors.request.use((config) => {
  config.headers['X-Emp-No'] = getEmpNo()
  return config
})

request.interceptors.response.use(
  (response) => {
    const result = response.data
    if (result.code !== 200) {
      console.error(`API Error: ${result.message}`)
      return Promise.reject(new Error(result.message || '请求失败'))
    }
    return result
  },
  (error) => {
    console.error('Network error:', error)
    return Promise.reject(error)
  }
)

export default request
