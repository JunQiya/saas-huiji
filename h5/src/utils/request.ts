import axios, { type AxiosInstance, type InternalAxiosRequestConfig } from 'axios'

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
  // 走相对路径, 由 vite 代理转发到后端 8081, 避免直连导致的 CORS/连接问题
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

// 将后端/网络错误统一为中文文案, 供页面 catch 展示(避免出现英文提示与重复 toast)
function toChineseError(error: any): Error {
  const status = error?.response?.status
  const body = error?.response?.data as ApiResult | undefined
  if (body?.message) return new Error(body.message)
  if (status === 401) return new Error('登录已过期')
  if (status != null) {
    const map: Record<number, string> = {
      400: '请求参数有误', 403: '无权限执行该操作', 404: '请求的资源不存在',
      405: '请求方式不支持', 408: '请求超时，请重试', 409: '数据冲突，请刷新后重试',
      422: '参数校验失败', 429: '请求过于频繁，请稍后再试', 500: '服务异常，请稍后重试',
      502: '网关异常，请稍后重试', 503: '服务暂不可用，请稍后重试', 504: '网关超时，请稍后重试'
    }
    return new Error(map[status] || '网络异常，请稍后重试')
  }
  if (error?.code === 'ECONNABORTED') return new Error('请求超时，请稍后再试')
  if (error?.code === 'ERR_NETWORK') return new Error('网络连接失败，请检查网络')
  const raw = String(error?.message || '')
  if (/network\s*error/i.test(raw)) return new Error('网络连接失败，请检查网络')
  if (/timeout/i.test(raw)) return new Error('请求超时，请稍后再试')
  if (error?.response) return new Error('服务异常，请稍后再试')
  // 兜底一律返回中文, 避免把浏览器英文错误文案暴露给用户
  return new Error('网络异常，请稍后再试')
}

// 响应拦截：拆 ok/false，401 清 token 跳登录；错误文案统一中文、由页面 catch 提示
instance.interceptors.response.use(
  (response) => {
    const result = response.data as ApiResult
    if (result && typeof result.ok === 'boolean') {
      if (result.ok) {
        return result.data as never
      }
      // 业务失败: 交给页面 catch 统一提示, 避免拦截器重复 toast
      return Promise.reject(new Error(result.message || '请求失败'))
    }
    return response.data as never
  },
  (error) => {
    const status = error?.response?.status
    if (status === 401) {
      localStorage.removeItem('memberToken')
      localStorage.removeItem('memberInfo')
      setTimeout(() => {
        if (location.pathname !== '/login') {
          // 保留原始目标, 登录后跳回
          const redirect = encodeURIComponent(location.pathname + location.search)
          location.href = `/login?redirect=${redirect}`
        }
      }, 300)
    }
    return Promise.reject(toChineseError(error))
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
