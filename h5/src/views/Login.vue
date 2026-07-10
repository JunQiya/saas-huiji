<template>
  <div class="page login">
    <!-- 装饰背景 -->
    <div class="bg-decor decor-a"></div>
    <div class="bg-decor decor-b"></div>
    <div class="bg-decor decor-c"></div>

    <!-- 顶部品牌区 -->
    <div class="brand-zone">
      <div class="brand-mark">
        <span class="star"></span>
        <span class="orbit"></span>
      </div>
      <div class="brand-name">星河·会记</div>
      <div class="brand-en">XINGHE HUIJI</div>
      <div class="brand-slogan">{{ slogan }}</div>
    </div>

    <!-- 登录卡片 -->
    <div class="login-card">
      <div class="card-head">
        <div class="head-title">欢迎回来</div>
        <div class="head-sub">手机号验证码登录，验证码 8888</div>
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
              {{ codeCountdown > 0 ? `${codeCountdown}s 后重发` : '获取验证码' }}
            </button>
          </div>
        </div>

        <button class="submit-btn" :disabled="loading" @click="onLogin">
          {{ loading ? '登录中…' : '登 录' }}
        </button>

        <div class="agreement">
          登录即代表同意
          <span class="agree-link">《服务协议》</span>
          <span class="agree-link">《隐私条款》</span>
        </div>
      </div>
    </div>

    <!-- 底部小字 -->
    <div class="footer-poem">
      <div class="poem-line">— 记得星河，也记得你 —</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { h5Api } from '@/api/h5'
import { useMemberStore } from '@/stores/member'

const router = useRouter()
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

function onSendCode() {
  if (!phoneValid.value) {
    showToast('请输入正确的手机号')
    return
  }
  code.value = '8888'
  showToast({ type: 'success', message: '验证码已发送（演示用 8888）' })
  codeCountdown.value = 60
  timer = setInterval(() => {
    codeCountdown.value--
    if (codeCountdown.value <= 0) clearInterval(timer)
  }, 1000)
}

async function onLogin() {
  if (!phoneValid.value) return showToast('请输入正确的手机号')
  if (code.value.length < 4) return showToast('请输入 4 位验证码')
  loading.value = true
  try {
    const res = await h5Api.login(phone.value, code.value)
    memberStore.setToken(res.memberToken)
    memberStore.setMember(res.member)
    showToast({ type: 'success', message: '登录成功' })
    setTimeout(() => router.replace('/'), 400)
  } catch (e: any) {
    showToast(e?.message || '登录失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (memberStore.memberToken) router.replace('/')
})
</script>

<style scoped>
.login {
  min-height: 100vh;
  padding: 0 0 40px;
  position: relative;
  overflow: hidden;
  background:
    radial-gradient(800px 500px at 50% -100px, rgba(111, 148, 184, 0.10), transparent 60%),
    var(--page-bg);
}

/* 背景装饰 */
.bg-decor { position: absolute; border-radius: 50%; pointer-events: none; }
.decor-a {
  width: 360px; height: 360px;
  top: -120px; right: -100px;
  background: radial-gradient(circle, rgba(111, 148, 184, 0.10), transparent 60%);
}
.decor-b {
  width: 240px; height: 240px;
  top: 220px; left: -80px;
  background: radial-gradient(circle, rgba(200, 157, 150, 0.10), transparent 60%);
}
.decor-c {
  width: 280px; height: 280px;
  bottom: -100px; right: -60px;
  background: radial-gradient(circle, rgba(168, 181, 184, 0.10), transparent 60%);
}

/* 品牌区 */
.brand-zone {
  position: relative;
  text-align: center;
  padding: 80px 16px 36px;
  z-index: 1;
}
.brand-mark {
  width: 72px; height: 72px;
  margin: 0 auto 18px;
  position: relative;
  display: flex; align-items: center; justify-content: center;
  background: var(--brand-soft);
  border-radius: 18px;
  box-shadow: 0 8px 24px rgba(74, 106, 135, 0.10);
}
.brand-mark .star {
  width: 32px; height: 32px;
  background: var(--brand-deep);
  clip-path: polygon(50% 0%, 61% 35%, 98% 35%, 68% 57%, 79% 91%, 50% 70%, 21% 91%, 32% 57%, 2% 35%, 39% 35%);
  position: relative; z-index: 1;
}
.brand-mark .orbit {
  position: absolute; inset: 0;
  border: 1px dashed rgba(111, 148, 184, 0.35);
  border-radius: 50%;
  margin: -8px;
  animation: spin 20s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
.brand-name {
  font-size: 22px;
  font-weight: 600;
  color: var(--ink);
  letter-spacing: 0.08em;
  font-family: 'Songti SC', 'STSong', 'SimSun', serif;
}
.brand-en {
  font-size: 10.5px;
  color: var(--muted);
  letter-spacing: 0.36em;
  margin-top: 4px;
  font-weight: 500;
}
.brand-slogan {
  margin-top: 16px;
  font-size: 12.5px;
  color: var(--ink-2);
  letter-spacing: 0.16em;
  font-family: 'Songti SC', serif;
  opacity: 0.85;
}

/* 登录卡片 */
.login-card {
  position: relative;
  z-index: 1;
  margin: 0 24px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-xl);
  padding: 28px 22px 22px;
  box-shadow: 0 8px 32px rgba(60, 70, 68, 0.08);
}
.card-head { margin-bottom: 22px; }
.head-title {
  font-size: 19px; font-weight: 600; color: var(--ink);
  letter-spacing: 0.04em;
  font-family: 'Songti SC', 'STSong', serif;
}
.head-sub {
  font-size: 12px; color: var(--muted);
  margin-top: 6px; letter-spacing: 0.04em;
}

.form { display: flex; flex-direction: column; gap: 16px; }
.form-item { display: flex; flex-direction: column; gap: 6px; }
.form-label {
  font-size: 11px; color: var(--muted);
  letter-spacing: 0.12em;
  text-transform: uppercase;
  font-weight: 500;
}
.form-input {
  width: 100%;
  height: 44px;
  padding: 0 14px;
  background: var(--surface-2);
  border: 1px solid var(--line);
  border-radius: var(--r-md);
  font-size: 14.5px;
  color: var(--ink);
  letter-spacing: 0.04em;
  outline: none;
  transition: border-color var(--dur) var(--ease), background-color var(--dur) var(--ease);
  font-family: inherit;
}
.form-input:focus {
  border-color: var(--brand);
  background: var(--surface);
}
.form-input::placeholder { color: var(--muted-2); }
.form-row { display: flex; gap: 10px; }
.form-row .form-input { flex: 1; }
.code-btn {
  height: 44px;
  padding: 0 14px;
  background: var(--surface-2);
  border: 1px solid var(--line);
  border-radius: var(--r-md);
  color: var(--muted);
  font-size: 13px;
  cursor: not-allowed;
  white-space: nowrap;
  transition: all var(--dur) var(--ease);
  font-family: inherit;
}
.code-btn.active {
  background: var(--brand-soft);
  border-color: var(--brand-soft);
  color: var(--brand-deep);
  cursor: pointer;
}
.code-btn.active:active { transform: scale(0.98); }

.submit-btn {
  height: 46px;
  margin-top: 6px;
  background: var(--brand-deep);
  color: #fff;
  border: none;
  border-radius: var(--r-md);
  font-size: 15px;
  font-weight: 500;
  letter-spacing: 0.32em;
  cursor: pointer;
  transition: all var(--dur) var(--ease);
  font-family: inherit;
}
.submit-btn:hover:not(:disabled) { transform: scale(1.01); background: #3d5b76; }
.submit-btn:active:not(:disabled) { transform: scale(0.99); }
.submit-btn:disabled { background: var(--muted-2); cursor: not-allowed; }

.agreement {
  font-size: 11px; color: var(--muted);
  text-align: center; margin-top: 12px;
  letter-spacing: 0.02em;
}
.agree-link { color: var(--brand-deep); margin: 0 2px; }

/* 底部小字 */
.footer-poem {
  text-align: center;
  margin-top: 32px;
  position: relative;
  z-index: 1;
}
.poem-line {
  font-size: 11px;
  color: var(--muted);
  letter-spacing: 0.32em;
  font-family: 'Songti SC', serif;
  opacity: 0.7;
}
</style>
