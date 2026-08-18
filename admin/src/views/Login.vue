<template>
  <div class="login-page">
    <!-- 左侧品牌区 -->
    <div class="login-left">
      <div class="left-content">
        <div class="brand-row">
          <div class="brand-mark">星</div>
          <div class="brand-text">
            <div class="brand-name">星河·会记</div>
            <div class="brand-sub">HUIJI · 会员经营</div>
          </div>
        </div>

        <h1 class="slogan">{{ slogan }}</h1>
        <p class="slogan-sub">在细水长流的经营里，记住每一位会员的故事。</p>

        <div v-if="!isProd" class="bottom-tip">
          <span class="dot"></span>
          演示账号 admin / 123456
        </div>
      </div>
    </div>

    <!-- 右侧登录区 -->
    <div class="login-right">
      <div class="login-card">
        <div class="login-title">欢迎回来</div>
        <div class="login-sub">登录后继续今天的经营。</div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          size="large"
          @keyup.enter="submit"
        >
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="账号" :prefix-icon="User" clearable />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" placeholder="密码" :prefix-icon="Lock" show-password clearable />
          </el-form-item>
          <el-button type="primary" class="submit-btn" :loading="loading" @click="submit">
            登 录
          </el-button>
        </el-form>

        <div class="tip-card">
          <div class="tip-label">今日营业小贴士</div>
          <div class="tip-text">{{ todayTip }}</div>
        </div>
      </div>

      <div class="login-foot">© 星河·会记 · 让会员管理有温度</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const form = reactive({ username: '', password: '' })
const rules: FormRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}
const loading = ref(false)
const isProd = import.meta.env.PROD

const slogans = [
  '记住每一位会员',
  '细水长流的经营',
  '让回访有迹可循'
]
const slogan = slogans[Math.floor(Math.random() * slogans.length)]

const tips = [
  '节后 7 天内回访的会员，复购率比平时高出 32%',
  '沉睡 60 天以上的会员，小额代金券往往能唤醒',
  '生日券提前 3 天发放，更显心意',
  '下午 4-6 点的优惠券领取率高于早高峰'
]
const todayTip = tips[Math.floor(Math.random() * tips.length)]

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await userStore.login(form.username.trim(), form.password)
    ElMessage.success('登录成功，欢迎回来')
    const redirect = (route.query.redirect as string) || '/dashboard'
    router.replace(redirect)
  } catch {} finally { loading.value = false }
}

// 登录过期自动退出: 若携带 reason=expired, 提示已过期
onMounted(() => {
  if (route.query.reason === 'expired') {
    ElMessage.warning('登录已过期，请重新登录')
  }
})
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex; flex-direction: row;
  background: var(--page-bg);
  overflow: hidden;
}

/* 左侧 */
.login-left {
  position: relative;
  width: 48%;
  background: var(--page-bg);
  border-right: 1px solid var(--line);
  display: flex; align-items: center; justify-content: center;
  overflow: hidden;
}

.left-content {
  position: relative; z-index: 2;
  max-width: 420px; padding: 0 36px;
  animation: x-fade-in 0.6s var(--ease-out) both;
}
.brand-row {
  display: flex; align-items: center; gap: 12px;
  margin-bottom: 64px;
}
.brand-mark {
  width: 38px; height: 38px;
  background: var(--brand);
  color: #fff;
  border-radius: var(--r-md);
  font-family: var(--font-serif);
  font-size: 18px;
  display: flex; align-items: center; justify-content: center;
  letter-spacing: 0.04em;
}
.brand-name {
  font-family: var(--font-serif);
  font-size: 18px; font-weight: 500; color: var(--ink);
  letter-spacing: 0.08em;
}
.brand-sub {
  font-family: var(--font-num);
  font-size: 10px; color: var(--muted);
  letter-spacing: 0.28em;
  margin-top: 4px;
}
.slogan {
  font-family: var(--font-serif);
  font-size: 30px; font-weight: 500; color: var(--ink);
  letter-spacing: 0.04em;
  line-height: 1.35;
  margin: 0 0 18px;
  animation: x-fade-in 0.7s var(--ease-out) 0.1s both;
}
.slogan-divider {
  width: 36px; height: 1px;
  background: var(--brand);
  margin: 20px 0 16px;
  animation: x-fade-in 0.7s var(--ease-out) 0.2s both;
}
.slogan-sub {
  font-family: var(--font-serif);
  font-size: 14px; color: var(--ink-3); line-height: 1.8;
  letter-spacing: 0.04em;
  margin: 0 0 32px;
  animation: x-fade-in 0.7s var(--ease-out) 0.25s both;
}
.bottom-tip {
  font-family: var(--font-num);
  font-size: 11.5px; color: var(--muted);
  display: flex; align-items: center; gap: 6px;
  letter-spacing: 0.08em;
  animation: x-fade-in 0.7s var(--ease-out) 0.35s both;
}
.bottom-tip .dot { width: 5px; height: 5px; border-radius: 50%; background: var(--brand); opacity: 0.7; }

/* 右侧 */
.login-right {
  flex: 1;
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  padding: 24px;
  background: var(--surface);
  position: relative;
}
.login-card {
  width: 100%; max-width: 380px;
  padding: 8px 4px 4px;
  animation: x-fade-in 0.6s var(--ease-out) 0.15s both;
}
.login-title {
  font-family: var(--font-serif);
  font-size: 22px; font-weight: 500; color: var(--ink);
  margin-bottom: 6px; letter-spacing: 0.06em;
}
.login-sub {
  font-family: var(--font-serif);
  font-size: 13px; color: var(--muted); margin-bottom: 28px;
  letter-spacing: 0.04em;
}
.submit-btn {
  width: 100%; height: 42px; font-size: 14px;
  letter-spacing: 0.32em;
  font-family: var(--font-serif);
  margin-top: 6px;
  border-radius: var(--r);
}
.tip-card {
  margin-top: 20px;
  padding: 12px 14px;
  background: var(--brand-softer);
  border: 1px dashed var(--line-2);
  border-left: 2px solid var(--brand);
  border-radius: 0 var(--r) var(--r) 0;
}
.tip-label {
  display: flex; align-items: center; gap: 6px;
  font-family: var(--font-serif);
  font-size: 11.5px; color: var(--brand-ink);
  letter-spacing: 0.10em; font-weight: 500;
  margin-bottom: 4px;
}
.tip-icon { font-size: 10px; color: var(--brand); }
.tip-text {
  font-family: var(--font-serif);
  font-size: 12px; color: var(--ink-2);
  line-height: 1.7; letter-spacing: 0.04em;
}
.login-foot {
  margin-top: 28px;
  font-family: var(--font-serif);
  font-size: 11px; color: var(--muted-2);
  letter-spacing: 0.18em;
  text-align: center;
}

/* 移动端单列 */
@media (max-width: 820px) {
  .login-page { flex-direction: column; }
  .login-left { width: 100%; height: 200px; }
  .slogan { font-size: 20px; }
  .brand-row { margin-bottom: 16px; }
  .bottom-tip { margin-top: 12px; }
  .login-right { padding: 16px; }
}
</style>
