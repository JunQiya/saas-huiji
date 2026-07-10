<template>
  <div class="login-page">
    <!-- 左侧装饰区 -->
    <div class="login-left">
      <div class="left-decor decor-1"></div>
      <div class="left-decor decor-2"></div>
      <div class="left-decor decor-3"></div>
      <div class="left-content">
        <div class="brand-row">
          <div class="brand-mark">
            <span class="star"></span>
          </div>
          <div class="brand-text">
            <div class="brand-name">星河·会记</div>
            <div class="brand-sub">会员营销管理后台</div>
          </div>
        </div>
        <div class="slogan">
          <div class="slogan-text">{{ slogan }}</div>
          <div class="slogan-divider"></div>
          <div class="slogan-sub">在细水长流的经营里，记住每一位会员的故事</div>
        </div>
        <div class="bottom-tip">
          <span class="dot"></span>
          演示账号 admin / 123456
        </div>
      </div>
    </div>

    <!-- 右侧登录区 -->
    <div class="login-right">
      <div class="login-card x-card">
        <div class="login-title">欢迎回来</div>
        <div class="login-sub">登录后继续您今天的经营</div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          size="large"
          @keyup.enter="submit"
        >
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              placeholder="账号"
              :prefix-icon="User"
              clearable
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="密码"
              :prefix-icon="Lock"
              show-password
              clearable
            />
          </el-form-item>
          <el-button
            type="primary"
            class="submit-btn btn-scale"
            :loading="loading"
            @click="submit"
          >
            登 录
          </el-button>
        </el-form>

        <!-- 今日小贴士 -->
        <div class="tip-card">
          <div class="tip-label">
            <el-icon><Sunny /></el-icon>
            <span>今日营业小贴士</span>
          </div>
          <div class="tip-text">{{ todayTip }}</div>
        </div>
      </div>

      <div class="login-footer">© 星河·会记 · 让会员管理有温度</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Lock, Sunny } from '@element-plus/icons-vue'
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

// 欢迎语 - 文学化、温暖
const slogans = [
  '记得星河，也记得你',
  '经营从每一次回访开始',
  '把每一位会员，妥帖地安放',
  '慢慢来，比较快',
  '细水长流，是最好的生意',
  '今天的回访，明天的回头客'
]
const slogan = slogans[Math.floor(Math.random() * slogans.length)]

// 今日小贴士
const tips = [
  '节后 7 天内回访的会员，复购率比平时高出 32%',
  '沉睡 60 天以上的会员，发一张小额代金券往往能唤醒',
  '生日券提前 3 天发放，比生日当天更显心意',
  '同一时段发送营销短信，打开率最高',
  '下午 4-6 点的优惠券领取率高于早高峰'
]
const todayTip = tips[Math.floor(Math.random() * tips.length)]

async function submit() {
  await formRef.value?.validate()
  loading.value = true
  try {
    await userStore.login(form.username.trim(), form.password)
    ElMessage.success('登录成功，欢迎回来')
    const redirect = (route.query.redirect as string) || '/dashboard'
    router.replace(redirect)
  } catch {
    /* 拦截器已提示 */
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  flex-direction: row;
  background: var(--page-bg);
  overflow: hidden;
}

/* 左侧装饰区 */
.login-left {
  position: relative;
  width: 50%;
  background:
    radial-gradient(circle at 18% 22%, rgba(111, 148, 184, 0.18), transparent 42%),
    radial-gradient(circle at 82% 78%, rgba(138, 130, 120, 0.12), transparent 42%),
    linear-gradient(140deg, #fbfaf6 0%, #eef0f2 60%, #e6e8ec 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}
.left-content {
  position: relative;
  z-index: 2;
  max-width: 420px;
  padding: 0 32px;
}
.left-decor {
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
  filter: blur(0.5px);
}
.decor-1 {
  width: 220px; height: 220px;
  top: 12%; right: 8%;
  background: radial-gradient(circle, rgba(111, 148, 184, 0.10), transparent 70%);
}
.decor-2 {
  width: 320px; height: 320px;
  bottom: -10%; left: -8%;
  background: radial-gradient(circle, rgba(138, 130, 120, 0.10), transparent 70%);
}
.decor-3 {
  width: 80px; height: 80px;
  top: 18%; left: 14%;
  border: 1px solid rgba(108, 120, 108, 0.18);
  background: rgba(255, 255, 255, 0.4);
}

.brand-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 56px;
}
.brand-mark {
  width: 44px; height: 44px;
  border-radius: 12px;
  background: linear-gradient(135deg, #6f94b8 0%, #4a6a87 100%);
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 4px 14px rgba(74, 106, 135, 0.25);
}
.star {
  width: 18px; height: 18px;
  background: #fff;
  clip-path: polygon(50% 0, 61% 35%, 98% 35%, 68% 57%, 79% 91%, 50% 70%, 21% 91%, 32% 57%, 2% 35%, 39% 35%);
}
.brand-name {
  font-size: 20px; font-weight: 600; color: var(--ink);
}
.brand-sub {
  font-size: 12px; color: var(--muted); margin-top: 2px;
}
.slogan {
  margin-bottom: 32px;
}
.slogan-text {
  font-size: 30px;
  font-weight: 600;
  color: var(--ink);
  letter-spacing: 2px;
  line-height: 1.3;
}
.slogan-divider {
  width: 36px; height: 2px;
  background: var(--primary-action);
  margin: 20px 0 16px;
  border-radius: 1px;
}
.slogan-sub {
  font-size: 14px; color: var(--muted); line-height: 1.7;
  letter-spacing: 0.5px;
}
.bottom-tip {
  margin-top: 64px;
  font-size: 12px; color: var(--muted-2);
  display: flex; align-items: center; gap: 6px;
}
.bottom-tip .dot {
  background: var(--success);
}

/* 右侧登录区 */
.login-right {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: var(--card-bg);
  position: relative;
}
.login-card {
  width: 100%;
  max-width: 380px;
  padding: 32px 32px 24px;
  box-shadow: var(--shadow);
}
.login-title {
  font-size: 22px; font-weight: 600; color: var(--ink);
  margin-bottom: 6px; letter-spacing: 0.5px;
}
.login-sub {
  font-size: 13px; color: var(--muted); margin-bottom: 24px;
}
.submit-btn {
  width: 100%; margin-top: 6px; height: 42px; font-size: 15px; letter-spacing: 4px;
}
.tip-card {
  margin-top: 18px;
  padding: 12px 14px;
  background: rgba(111, 148, 184, 0.06);
  border: 1px dashed rgba(111, 148, 184, 0.30);
  border-radius: var(--radius);
}
.tip-label {
  display: flex; align-items: center; gap: 6px;
  font-size: 12px; color: var(--primary-action); font-weight: 500;
  margin-bottom: 4px;
}
.tip-text {
  font-size: 12.5px; color: var(--ink-2); line-height: 1.6;
}
.login-footer {
  margin-top: 24px;
  font-size: 12px; color: var(--muted);
}

/* 输入框聚焦态：用冷蓝 */
.login-card :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--primary-action) inset !important;
}
.login-card :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(111, 148, 184, 0.4) inset !important;
}

/* 移动端单列 */
@media (max-width: 820px) {
  .login-page { flex-direction: column; }
  .login-left { width: 100%; height: 220px; }
  .slogan-text { font-size: 22px; }
  .brand-row { margin-bottom: 20px; }
  .bottom-tip { margin-top: 16px; }
  .login-right { padding: 16px; }
}
</style>
