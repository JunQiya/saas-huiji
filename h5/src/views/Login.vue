<template>
  <div class="page login">
    <!-- 装饰星座背景 -->
    <svg class="bg-stars" viewBox="0 0 400 800" preserveAspectRatio="xMidYMid slice" aria-hidden="true">
      <g fill="#4a6583">
        <circle cx="40" cy="120" r="0.9" opacity="0.30" />
        <circle cx="120" cy="80" r="0.7" opacity="0.24" />
        <circle cx="240" cy="60" r="0.9" opacity="0.30" />
        <circle cx="320" cy="140" r="1" opacity="0.34" />
        <circle cx="370" cy="260" r="0.8" opacity="0.26" />
        <circle cx="60" cy="320" r="0.7" opacity="0.22" />
        <circle cx="380" cy="420" r="0.9" opacity="0.30" />
        <circle cx="20" cy="500" r="0.8" opacity="0.26" />
        <circle cx="320" cy="580" r="0.7" opacity="0.22" />
        <circle cx="80" cy="700" r="0.9" opacity="0.30" />
        <circle cx="260" cy="760" r="0.8" opacity="0.26" />
        <circle cx="180" cy="200" r="0.6" opacity="0.20" />
      </g>
      <g stroke="#4a6583" stroke-width="0.4" fill="none" opacity="0.30">
        <line x1="120" y1="80" x2="240" y2="60" />
        <line x1="240" y1="60" x2="320" y2="140" />
        <line x1="60" y1="320" x2="180" y2="200" />
        <line x1="20" y1="500" x2="80" y2="700" />
      </g>
    </svg>

    <div class="brand-zone">
      <div class="brand-mark">
        <svg viewBox="0 0 36 36" width="42" height="42">
          <circle cx="9" cy="9" r="0.8" fill="var(--brand-deep)" opacity="0.4" />
          <circle cx="27" cy="6" r="0.7" fill="var(--brand-deep)" opacity="0.32" />
          <circle cx="30" cy="22" r="0.8" fill="var(--brand-deep)" opacity="0.36" />
          <circle cx="7" cy="28" r="0.7" fill="var(--brand-deep)" opacity="0.28" />
          <g stroke="var(--brand-deep)" stroke-width="0.7" opacity="0.5" fill="none">
            <line x1="13" y1="14" x2="18" y2="20" />
            <line x1="18" y1="20" x2="23" y2="14" />
            <line x1="18" y1="20" x2="18" y2="26" />
          </g>
          <circle cx="13" cy="14" r="1.6" fill="var(--brand-deep)" />
          <circle cx="23" cy="14" r="1.6" fill="var(--brand-deep)" />
          <circle cx="18" cy="20" r="2.2" fill="var(--brand-deep)" />
          <circle cx="18" cy="26" r="1.2" fill="var(--brand-deep)" opacity="0.7" />
        </svg>
      </div>
      <div class="brand-name">星河·会记</div>
      <div class="brand-en">HUIJI · 夜读手记</div>
      <div class="brand-slogan">{{ slogan }}</div>
    </div>

    <div class="login-card x-fade">
      <div class="card-head">
        <div class="head-title">欢迎回来</div>
        <div class="head-sub">手机号验证码登录，5 分钟内有效</div>
      </div>

      <div class="form">
        <div class="form-item">
          <div class="form-label">手机号</div>
          <input
            v-model="phone"
            type="tel"
            maxlength="11"
            placeholder="请输入 11 位手机号"
            class="form-input"
            @keyup.enter="onLogin"
          />
        </div>
        <div class="form-item">
          <div class="form-label">验证码</div>
          <div class="form-row">
            <input
              v-model="code"
              type="text"
              maxlength="4"
              placeholder="4 位验证码"
              class="form-input"
              @keyup.enter="onLogin"
            />
            <button
              class="code-btn"
              :class="{ active: codeCountdown <= 0 && phoneValid }"
              :disabled="codeCountdown > 0 || !phoneValid"
              @click="onSendCode"
            >
              {{ codeCountdown > 0 ? `${codeCountdown}s` : '获取验证码' }}
            </button>
          </div>
        </div>

        <button class="submit-btn" :disabled="loading" @click="onLogin">
          {{ loading ? '登录中…' : '登 录' }}
        </button>

        <div class="divider-poem">
          <span class="dp-text">或</span>
        </div>

        <button class="wx-btn" @click="onWxLogin">
          <span class="wx-icon">微</span>
          <span>微信登录</span>
        </button>

        <div class="agreement">
          登录即代表同意
          <span class="agree-link" @click="showTerms">《服务协议》</span>
          <span class="agree-link" @click="showPrivacy">《隐私条款》</span>
        </div>
      </div>
    </div>

    <div class="footer-poem">记得星河 也记得你</div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast, showDialog } from 'vant'
import { h5Api } from '@/api/h5'
import { useMemberStore } from '@/stores/member'

const router = useRouter()
const route = useRoute()
const memberStore = useMemberStore()
const phone = ref('')
const code = ref('')
const loading = ref(false)
const codeCountdown = ref(0)
let timer: any

const phoneValid = computed(() => /^1\d{10}$/.test(phone.value))

const slogans = [
  '记得星河，也记得你',
  '把每一次到店，妥帖安放',
  '细水长流，是最好的生意',
  '今天的回访，明天的回头客',
  '所有温柔的事物，都在缓慢地生长'
]
const slogan = slogans[Math.floor(Math.random() * slogans.length)]

const sendingCode = ref(false)

async function onSendCode() {
  if (!phoneValid.value) {
    showToast('请输入正确的手机号')
    return
  }
  if (sendingCode.value || codeCountdown.value > 0) return
  sendingCode.value = true
  try {
    const res = await h5Api.sendSmsCode(phone.value)
    // dev 模式接口会回显验证码（生产对接短信网关后此字段为 null）
    if (res?.devCode) {
      code.value = res.devCode
      showToast({ type: 'success', message: `验证码已发送（演示：${res.devCode}）` })
    } else {
      showToast({ type: 'success', message: '验证码已发送' })
    }
    codeCountdown.value = 60
    if (timer) clearInterval(timer)
    timer = setInterval(() => {
      codeCountdown.value--
      if (codeCountdown.value <= 0) clearInterval(timer)
    }, 1000)
  } catch (e: any) {
    showToast(e?.message || '发送失败，请稍后再试')
  } finally {
    sendingCode.value = false
  }
}

async function onLogin() {
  if (!phoneValid.value) return showToast('请输入正确的手机号')
  if (code.value.length < 4) return showToast('请输入 4 位验证码')
  loading.value = true
  try {
    const res = await h5Api.login(phone.value, code.value)
    memberStore.setToken(res.memberToken)
    memberStore.setMember(res.member)
    // 登录成功后从 member token 中解析 tenantId，覆盖 URL 中可能被篡改的值
    try {
      const payload = JSON.parse(atob(res.memberToken.split('.')[1]))
      if (payload?.tenantId) {
        localStorage.setItem('tenantId', String(payload.tenantId))
      }
    } catch {/* token 解析失败时忽略，沿用 URL/localStorage 中的值 */}
    showToast({ type: 'success', message: '登录成功' })
    setTimeout(() => router.replace('/'), 400)
  } catch (e: any) {
    showToast(e?.message || '登录失败')
  } finally {
    loading.value = false
  }
}

// 获取租户 ID：优先 URL 参数，其次 localStorage，默认 1
// 注意：URL 参数存在被篡改风险，登录成功后会用 member token 中的 tenantId 覆盖
function getTenantId(): string {
  const fromUrl = new URLSearchParams(window.location.search).get('tenantId')
  if (fromUrl) {
    localStorage.setItem('tenantId', fromUrl)
    return fromUrl
  }
  return localStorage.getItem('tenantId') || '1'
}

// 微信授权登录：跳转到后端 OAuth 入口，由后端 302 到微信授权页
function onWxLogin() {
  const tenantId = getTenantId()
  const redirect = window.location.href
  // state 作为透传参数，登录成功后 WxLogin 页面用它决定跳转目标
  const state = (route.query.redirect as string) || '/home'
  const oauthUrl = `/api/wx/oauth/${tenantId}?redirect=${encodeURIComponent(redirect)}&state=${encodeURIComponent(state)}`
  window.location.href = oauthUrl
}

function showTerms() {
  showDialog({
    title: '服务协议',
    message: '欢迎使用星河·会记会员服务。使用本服务即代表您同意以下条款：\n\n1. 本平台提供的会员积分、储值、优惠券等服务仅限本人使用\n2. 储值余额不可提现，不可转让\n3. 积分有效期为 24 个月，过期自动清零\n4. 优惠券请在有效期内使用，过期作废\n5. 我们将保护您的个人隐私信息',
    confirmButtonText: '我知道了'
  })
}

function showPrivacy() {
  showDialog({
    title: '隐私条款',
    message: '我们重视您的隐私：\n\n1. 收集的信息仅用于会员服务（手机号、消费记录）\n2. 未经您同意，不会向第三方共享个人信息\n3. 您有权查看、更正、删除个人信息\n4. 联系方式：xinghe@mail.lxxno.cn',
    confirmButtonText: '我知道了'
  })
}

onMounted(() => {
  if (memberStore.memberToken) router.replace('/')
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.login {
  min-height: 100vh;
  padding: 0 0 40px;
  position: relative;
  overflow: hidden;
}
.bg-stars {
  position: absolute; inset: 0;
  width: 100%; height: 100%;
  pointer-events: none;
  z-index: 0;
}

/* 品牌区 */
.brand-zone {
  position: relative;
  text-align: center;
  padding: 72px 16px 32px;
  z-index: 1;
  animation: x-fade-in 0.5s var(--ease-out) both;
}
.brand-mark {
  width: 60px; height: 60px;
  margin: 0 auto 16px;
  display: flex; align-items: center; justify-content: center;
  background: var(--brand-softer);
  border-radius: 14px;
  border: 1px dashed var(--line-2);
}
.brand-name {
  font-family: var(--font-serif);
  font-size: 22px; font-weight: 500;
  color: var(--ink);
  letter-spacing: 0.08em;
  line-height: 1.2;
}
.brand-en {
  font-family: var(--font-num);
  font-size: 10px; color: var(--muted);
  letter-spacing: 0.32em;
  margin-top: 6px;
  font-weight: 400;
}
.brand-slogan {
  margin-top: 18px;
  font-family: var(--font-serif);
  font-size: 13px; color: var(--ink-2);
  letter-spacing: 0.16em;
  opacity: 0.88;
}

/* 登录卡片 */
.login-card {
  position: relative;
  z-index: 1;
  margin: 0 22px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-xl);
  padding: 24px 22px 20px;
  box-shadow: var(--shadow-md);
  animation: x-fade-in 0.6s var(--ease-out) 0.1s both;
}
.card-head { margin-bottom: 22px; }
.head-title {
  font-family: var(--font-serif);
  font-size: 19px; font-weight: 500; color: var(--ink);
  letter-spacing: 0.06em;
}
.head-sub {
  font-family: var(--font-serif);
  font-size: 12px; color: var(--muted);
  margin-top: 6px; letter-spacing: 0.06em;
}

.form { display: flex; flex-direction: column; gap: 16px; }
.form-item { display: flex; flex-direction: column; gap: 6px; }
.form-label {
  font-family: var(--font-serif);
  font-size: 11px; color: var(--muted);
  letter-spacing: 0.18em;
  font-weight: 500;
}
.form-input {
  width: 100%; height: 42px;
  padding: 0 14px;
  background: var(--surface-2);
  border: 1px solid var(--line);
  border-radius: var(--r);
  font-size: 14.5px; color: var(--ink);
  letter-spacing: 0.04em;
  outline: none;
  transition: border-color var(--dur) var(--ease-out), background-color var(--dur) var(--ease-out);
  font-family: inherit;
}
.form-input:focus {
  border-color: var(--brand-ink);
  background: var(--surface);
}
.form-input::placeholder { color: var(--muted-2); }
.form-row { display: flex; gap: 10px; }
.form-row .form-input { flex: 1; }
.code-btn {
  height: 42px;
  padding: 0 12px;
  background: var(--surface-2);
  border: 1px solid var(--line);
  border-radius: var(--r);
  color: var(--muted);
  font-size: 12.5px;
  font-family: var(--font-serif);
  letter-spacing: 0.04em;
  cursor: not-allowed;
  white-space: nowrap;
  transition: all var(--dur) var(--ease-out);
}
.code-btn.active {
  background: var(--brand-soft);
  border-color: transparent;
  color: var(--brand-ink);
  cursor: pointer;
}
.code-btn.active:active { transform: scale(0.98); }

.submit-btn {
  height: 44px;
  margin-top: 4px;
  background: var(--brand-deep);
  color: #fff;
  border: none;
  border-radius: var(--r);
  font-size: 14.5px; font-weight: 500;
  letter-spacing: 0.32em;
  font-family: var(--font-serif);
  cursor: pointer;
  transition: all var(--dur) var(--ease-out);
}
.submit-btn:hover:not(:disabled) { transform: scale(1.01); background: var(--brand-deep); }
.submit-btn:active:not(:disabled) { transform: scale(0.99); }
.submit-btn:disabled { background: var(--muted-2); cursor: not-allowed; }

/* 微信登录按钮 — 低饱和绿，不抢主按钮视觉 */
.wx-btn {
  height: 44px;
  background: rgba(7, 193, 96, 0.06);
  border: 1px solid rgba(7, 193, 96, 0.22);
  border-radius: var(--r);
  color: #2e7d4f;
  font-size: 14px;
  font-family: var(--font-serif);
  letter-spacing: 0.16em;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all var(--dur) var(--ease-out);
}
.wx-btn:active { transform: scale(0.99); background: rgba(7, 193, 96, 0.10); }
.wx-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px; height: 20px;
  background: #07c160;
  color: #fff;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
  font-family: var(--font-ui);
  letter-spacing: 0;
}

.agreement {
  font-size: 11px; color: var(--muted);
  text-align: center; margin-top: 12px;
  letter-spacing: 0.04em;
}
.agree-link { color: var(--brand-ink); margin: 0 2px; cursor: pointer; }

.footer-poem {
  text-align: center;
  margin-top: 28px;
  position: relative;
  z-index: 1;
  font-family: var(--font-serif);
  font-size: 11px; color: var(--muted-2);
  letter-spacing: 0.32em;
}
.footer-poem::before, .footer-poem::after {
  content: '·'; margin: 0 8px; color: var(--muted-2);
}
</style>
