<template>
  <div class="wx-login-page">
    <div class="wx-login-box">
      <!-- 加载中 -->
      <div v-if="status === 'loading'" class="state-box">
        <van-loading size="36px" color="var(--brand-deep)" />
        <div class="state-text">正在登录…</div>
        <div class="state-sub">请稍候片刻</div>
      </div>
      <!-- 失败 -->
      <div v-else class="state-box">
        <van-icon name="warning-o" size="36px" color="var(--danger)" />
        <div class="state-text">授权失败</div>
        <div class="state-sub">{{ errorMsg }}</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { useMemberStore } from '@/stores/member'
import { h5Api } from '@/api/h5'

const route = useRoute()
const router = useRouter()
const memberStore = useMemberStore()

const status = ref<'loading' | 'error'>('loading')
const errorMsg = ref('')

onMounted(async () => {
  const token = route.query.token as string | undefined
  // state 作为透传参数，用于登录后跳转目标
  const state = (route.query.state as string) || ''

  // 没有 token，直接提示失败
  if (!token) {
    status.value = 'error'
    errorMsg.value = '未获取到授权令牌'
    setTimeout(() => router.replace('/login'), 1200)
    return
  }

  // 存储 token
  memberStore.setToken(token)

  // 拉取会员信息，失败不阻塞跳转
  try {
    const profile = await h5Api.profile()
    memberStore.setMember(profile)
  } catch {
    // token 已存入，后续可重试拉取
  }

  showToast({ type: 'success', message: '登录成功' })

  // 1 秒后跳转，state 是路径则跳该路径，否则默认 /home
  setTimeout(() => {
    const target = state.startsWith('/') ? state : '/home'
    router.replace(target)
  }, 1000)
})
</script>

<style scoped>
.wx-login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--page-bg);
  padding: 24px;
}
.wx-login-box {
  text-align: center;
  animation: x-fade-in var(--dur-slow) var(--ease-out) both;
}
.state-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
}
.state-text {
  font-family: var(--font-serif);
  font-size: 15px;
  color: var(--ink-2);
  letter-spacing: 0.16em;
}
.state-sub {
  font-size: 12px;
  color: var(--muted);
  letter-spacing: 0.04em;
}
</style>
