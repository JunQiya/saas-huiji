import axios, { type AxiosInstance, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResult } from '@/types'

// axios 封装：统一 baseURL、注入 Authorization、拦截响应 ok/false、401 清 token 跳登录
const service: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// 请求拦截：注入 token
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('token')
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 是否正在跳登录，避免 401 重复提示
let isRedirecting = false

// 响应拦截：统一处理 ok/false
service.interceptors.response.use(
  (response) => {
    const res = response.data as ApiResult
    // 二进制/非对象直接返回
    if (typeof res !== 'object' || res === null) {
      return response
    }
    if (res.ok === true) {
      return res.data
    }
    if (res.ok === false) {
      // 业务失败
      if (res.code === 'SESSION_EXPIRED') {
        handleSessionExpired(res.message || '登录已过期')
      } else {
        ElMessage.error(res.message || '请求失败')
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    // 非标准结构，原样返回
    return res
  },
  async (error) => {
    // blob 请求的错误响应是 Blob，需转为 JSON 才能解析错误信息
    if (error.response?.config?.responseType === 'blob' && error.response.data instanceof Blob) {
      try {
        const text = await error.response.data.text()
        error.response.data = JSON.parse(text)
      } catch {/* 非 JSON 则保留原 Blob */}
    }
    const status = error.response?.status
    const body = error.response?.data
    const code = body?.code
    // 将 axios 的英文错误文案统一转成中文, 避免暴露给用户
    const msg = body?.message || zhHttpError(error)
    error.message = msg
    if (status === 401 || code === 'SESSION_EXPIRED') {
      handleSessionExpired(msg)
    } else if (status === 403 || code === 'FORBIDDEN') {
      ElMessage.error('无权限执行该操作')
    } else {
      ElMessage.error(msg)
    }
    return Promise.reject(error)
  }
)

// 把 axios/浏览器产生的英文错误转成中文文案
function zhHttpError(error: any): string {
  const status = error?.response?.status
  if (status != null) {
    const map: Record<number, string> = {
      400: '请求参数有误',
      401: '登录已过期，请重新登录',
      403: '无权限执行该操作',
      404: '请求的资源不存在',
      405: '请求方式不支持',
      408: '请求超时，请重试',
      409: '数据冲突，请刷新后重试',
      422: '参数校验失败',
      429: '请求过于频繁，请稍后再试',
      500: '服务异常，请稍后重试',
      502: '网关异常，请稍后重试',
      503: '服务暂不可用，请稍后重试',
      504: '网关超时，请稍后重试'
    }
    return map[status] || '网络异常，请稍后重试'
  }
  const code = (error?.code || '') as string
  if (code === 'ECONNABORTED' || /timeout/i.test(error?.message || '')) {
    return '请求超时，请重试'
  }
  if (code === 'ERR_NETWORK' || /network error/i.test(error?.message || '')) {
    return '网络异常，请检查网络连接'
  }
  if (code === 'ERR_CANCELED') {
    return '请求已取消'
  }
  return '网络异常，请稍后重试'
}

function handleSessionExpired(message: string) {
  if (isRedirecting) return
  isRedirecting = true
  ElMessage.error(message)
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  // 跳登录（避免循环依赖，直接用 location）
  const redirect = encodeURIComponent(window.location.pathname + window.location.search)
  window.location.href = `/login?redirect=${redirect}`
  setTimeout(() => {
    isRedirecting = false
  }, 1500)
}

export default service
