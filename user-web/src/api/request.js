import axios from 'axios'
import { ElMessage } from 'element-plus'
import { clearSession, getToken } from '@/utils/session'
import { mockAdapter, USE_MOCK } from '@/mock'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000,
  ...(USE_MOCK ? { adapter: mockAdapter } : {}),
})

request.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 0) return body.data
      const error = new Error(body.message || `操作失败（错误码 ${body.code}）`)
      error.code = body.code
      ElMessage.error(error.message)
      return Promise.reject(error)
    }
    return body
  },
  (error) => {
    const status = error.response?.status
    const body = error.response?.data
    if (status === 401) {
      clearSession()
      ElMessage.error(body?.message || '登录已过期，请重新登录')
      if (!window.location.pathname.startsWith('/login')) {
        window.location.replace('/login')
      }
    } else if (status === 403) {
      ElMessage.error(body?.message || '当前角色无权访问该接口')
    } else {
      ElMessage.error(body?.message || '网络异常，请稍后重试')
    }
    return Promise.reject(error)
  },
)

export default request
