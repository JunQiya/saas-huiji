import { defineStore } from 'pinia'
import type { LoginUser, Role } from '@/types'
import { authApi } from '@/api'

// 安全解析 localStorage 中的用户信息, 避免损坏数据导致应用启动崩溃
function safeParseUser(raw: string | null): LoginUser | null {
  if (!raw) return null
  try {
    const u = JSON.parse(raw) as LoginUser
    return u && typeof u === 'object' ? u : null
  } catch {
    // 数据损坏: 清理并视为未登录, 避免白屏
    localStorage.removeItem('user')
    localStorage.removeItem('token')
    return null
  }
}

// 解析 JWT 的过期时间(秒), 用于主动检测登录过期
export function parseTokenExp(token: string): number {
  try {
    const parts = token.split('.')
    if (parts.length !== 3) return 0
    const payload = JSON.parse(atob(parts[1].replace(/-/g, '+').replace(/_/g, '/')))
    return Number(payload.exp || 0) * 1000
  } catch {
    return 0
  }
}

const TOKEN_EXP_KEY = 'token_exp'

// 用户状态：token / userInfo / permissions，持久化到 localStorage
export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: safeParseUser(localStorage.getItem('user'))
  }),
  getters: {
    isLogin: (state) => !!state.token,
    role: (state): Role | undefined => state.userInfo?.role,
    isTenantAdmin: (state) => state.userInfo?.role === 'TENANT_ADMIN',
    /** token 是否已过期(前端预判) */
    tokenExpired: (state) => {
      if (!state.token) return false
      const exp = Number(localStorage.getItem(TOKEN_EXP_KEY)) || 0
      return exp > 0 && Date.now() >= exp
    }
  },
  actions: {
    async login(username: string, password: string) {
      const res = await authApi.login({ username, password })
      this.token = res.token
      this.userInfo = res.user
      localStorage.setItem('token', res.token)
      localStorage.setItem('user', JSON.stringify(res.user))
      // 记录 token 过期时间(JWT exp)
      const exp = parseTokenExp(res.token)
      if (exp > 0) localStorage.setItem(TOKEN_EXP_KEY, String(exp))
      else localStorage.removeItem(TOKEN_EXP_KEY)
      return res
    },
    async fetchProfile() {
      const user = await authApi.profile()
      this.userInfo = user
      localStorage.setItem('user', JSON.stringify(user))
      return user
    },
    setToken(token: string) {
      this.token = token
      localStorage.setItem('token', token)
      const exp = parseTokenExp(token)
      if (exp > 0) localStorage.setItem(TOKEN_EXP_KEY, String(exp))
      else localStorage.removeItem(TOKEN_EXP_KEY)
    },
    logout() {
      // 尽量通知后端，失败不阻塞
      authApi.logout().catch(() => {})
      this.token = ''
      this.userInfo = null
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      localStorage.removeItem(TOKEN_EXP_KEY)
    },
    /** 清理会话并跳登录(登录过期自动退出) */
    forceLogout(message = '登录已过期，请重新登录') {
      this.token = ''
      this.userInfo = null
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      localStorage.removeItem(TOKEN_EXP_KEY)
      // 避免循环依赖, 用 location 直接跳转
      if (!location.pathname.startsWith('/login')) {
        location.href = '/login?reason=expired'
      }
    }
  }
})
