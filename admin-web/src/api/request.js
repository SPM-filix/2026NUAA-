import axios from 'axios'
import { ElMessage } from 'element-plus'
import { mockRequest } from '@/mock/server'
import { clearAuthStorage, getToken } from '@/utils/authStorage'

export const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 15000
})

// 401/403 由后端配合统一响应体返回（code 1002 / 1003）。
// axios 默认会把 4xx 当异常抛出，这里转成正常响应交给下面的统一处理。
client.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.data && typeof error.response.data.code === 'number') {
      return error.response
    }
    return Promise.reject(error)
  }
)

export class ApiError extends Error {
  constructor(code, message) {
    super(message)
    this.name = 'ApiError'
    this.code = code
  }
}

function toLogin() {
  const { pathname, search, hash } = window.location
  if (pathname === '/login') return
  const redirect = encodeURIComponent(`${pathname}${search}${hash}`)
  window.location.href = `/login?redirect=${redirect}`
}

function normalizeError(body, silent) {
  if (body.code === 1002) {
    clearAuthStorage()
    if (!silent) ElMessage.error(body.message || '登录已过期，请重新登录')
    toLogin()
    return
  }
  if (body.code === 1003) {
    ElMessage.error(body.message || '当前角色无权访问该接口')
    return
  }
  if (!silent) {
    ElMessage.error(body.message || `操作失败（错误码 ${body.code}）`)
  }
}

async function request({ method = 'GET', url, params, data, headers = {}, silent = false }) {
  const authHeaders = getToken() ? { Authorization: `Bearer ${getToken()}` } : {}
  let body

  try {
    if (USE_MOCK) {
      body = await mockRequest({
        method,
        url,
        params,
        data,
        headers: { ...authHeaders, ...headers }
      })
    } else {
      const response = await client.request({
        method,
        url,
        params,
        data,
        headers: { ...authHeaders, ...headers }
      })
      body = response.data
    }
  } catch (error) {
    if (error instanceof ApiError) throw error
    const message = error?.message || '网络异常，请稍后重试'
    if (!silent) ElMessage.error(message)
    throw new ApiError(5001, message)
  }

  if (!body || typeof body.code !== 'number') {
    const message = '响应格式不正确'
    if (!silent) ElMessage.error(message)
    throw new ApiError(5001, message)
  }

  if (body.code !== 0) {
    normalizeError(body, silent)
    throw new ApiError(body.code, body.message || `操作失败（错误码 ${body.code}）`)
  }

  return body.data
}

export const http = {
  get: (url, params, options) => request({ method: 'GET', url, params, ...options }),
  post: (url, data, options) => request({ method: 'POST', url, data, ...options }),
  put: (url, data, options) => request({ method: 'PUT', url, data, ...options }),
  patch: (url, data, options) => request({ method: 'PATCH', url, data, ...options }),
  delete: (url, options) => request({ method: 'DELETE', url, ...options })
}
