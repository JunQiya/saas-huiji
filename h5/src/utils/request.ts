import axios, { type AxiosInstance, type InternalAxiosRequestConfig } from 'axios'
import { showToast } from 'vant'

// 后端统一响应结构
export interface ApiResult<T = unknown> {
  ok: boolean
  data?: T
  message?: string
  code?: string
}

// 分页响应数据
export interface PageData<T> {
  list: T[]
  total: number
  page: number
  size: number
}

const instance: AxiosInstance = axios.create({
  // 走相对路径, 由 vite 代理转发到后端 8080, 避免直连导致的 CORS/连接问题
  baseURL: '',
  timeout: 15000
})

// 请求拦截：注入 memberToken
instance.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = localStorage.getItem('memberToken')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截：拆 ok/false，401 清 token 跳登录
instance.interceptors.response.use(
  (response) => {
    const result = response.data as ApiResult
    if (result && typeof result.ok === 'boolean') {
      if (result.ok) {
        return result.data as never
      }
      showToast(result.message || '请求失败')
      return Promise.reject(new Error(result.message || '请求失败'))
    }
    return response.data as never
  },
  (error) => {
    const status = error?.response?.status
    const body = error?.response?.data as ApiResult | undefined
    if (status === 401) {
      localStorage.removeItem('memberToken')
      localStorage.removeItem('memberInfo')
      showToast(body?.message || '登录已过期')
      setTimeout(() => {
        if (location.pathname !== '/login') {
          location.href = '/login'
        }
      }, 300)
    } else {
      showToast(body?.message || error?.message || '网络异常')
    }
    return Promise.reject(error)
  }
)

// 封装请求方法，自动剥离外层 data
export const request = {
  get<T = unknown>(url: string, params?: Record<string, unknown>): Promise<T> {
    return instance.get<unknown, T>(url, { params })
  },
  post<T = unknown>(url: string, body?: unknown): Promise<T> {
    return instance.post<unknown, T>(url, body)
  },
  put<T = unknown>(url: string, body?: unknown): Promise<T> {
    return instance.put<unknown, T>(url, body)
  },
  delete<T = unknown>(url: string): Promise<T> {
    return instance.delete<unknown, T>(url)
  }
}

export default instance
