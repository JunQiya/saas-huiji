import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { MemberProfile } from '@/api/h5'

// 会员 store：memberToken / memberInfo，持久化到 localStorage
export const useMemberStore = defineStore('member', () => {
  const memberToken = ref<string>(localStorage.getItem('memberToken') || '')
  const memberInfo = ref<MemberProfile | null>(
    (() => {
      const raw = localStorage.getItem('memberInfo')
      try {
        return raw ? (JSON.parse(raw) as MemberProfile) : null
      } catch {
        return null
      }
    })()
  )

  const isLogin = computed(() => !!memberToken.value)

  function setToken(token: string) {
    memberToken.value = token
    if (token) {
      localStorage.setItem('memberToken', token)
    } else {
      localStorage.removeItem('memberToken')
    }
  }

  function setMember(info: MemberProfile | null) {
    memberInfo.value = info
    if (info) {
      localStorage.setItem('memberInfo', JSON.stringify(info))
    } else {
      localStorage.removeItem('memberInfo')
    }
  }

  function logout() {
    setToken('')
    setMember(null)
  }

  return {
    memberToken,
    memberInfo,
    isLogin,
    setToken,
    setMember,
    logout
  }
})
