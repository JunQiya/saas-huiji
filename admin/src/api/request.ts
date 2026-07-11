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
    const msg = body?.message || error.message || '网络异常'
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
