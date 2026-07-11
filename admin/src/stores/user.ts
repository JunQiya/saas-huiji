import { defineStore } from 'pinia'
import type { LoginUser, Role } from '@/types'
import { authApi } from '@/api'

// 用户状态：token / userInfo / permissions，持久化到 localStorage
export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: (() => {
      const raw = localStorage.getItem('user')
      return raw ? (JSON.parse(raw) as LoginUser) : null
    })() as LoginUser | null
  }),
  getters: {
    isLogin: (state) => !!state.token,
    role: (state): Role | undefined => state.userInfo?.role,
    isTenantAdmin: (state) => state.userInfo?.role === 'TENANT_ADMIN'
  },
  actions: {
    async login(username: string, password: string) {
      const res = await authApi.login({ username, password })
      this.token = res.token
      this.userInfo = res.user
      localStorage.setItem('token', res.token)
      localStorage.setItem('user', JSON.stringify(res.user))
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
    },
    logout() {
      // 尽量通知后端，失败不阻塞
      authApi.logout().catch(() => {})
      this.token = ''
      this.userInfo = null
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    }
  }
})
