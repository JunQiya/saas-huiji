<template>
  <div class="page profile">
    <!-- 顶部 blog header -->
    <BlogHeader :slogan="slogan" />

    <!-- 用户卡 -->
    <div class="user-wrap">
      <div class="user-card" :class="`lv-${memberInfo?.level || 1}`">
        <div class="card-decor decor-1"></div>
        <div class="card-decor decor-2"></div>
        <div class="uc-content">
          <div class="uc-top">
            <div class="uc-avatar">{{ avatarText }}</div>
            <div class="uc-info">
              <div class="uc-name">
                {{ memberInfo?.name || '未登录' }}
                <span class="uc-level">{{ memberInfo?.levelName || '普通会员' }}</span>
              </div>
              <div class="uc-phone">{{ memberInfo?.phone || '点击登录' }}</div>
            </div>
            <van-icon name="setting-o" class="uc-set" @click="router.push('/about')" />
          </div>
          <div class="uc-progress" v-if="memberInfo">
            <div class="prog-row">
              <span>成长值</span>
              <span class="prog-tip">距下一等级还差 ¥{{ gapToNext }}</span>
            </div>
            <div class="prog-bar">
              <div class="prog-fill" :style="{ width: progressPct + '%' }"></div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 数字三联 -->
    <div class="stat-row">
      <div class="stat-cell">
        <div class="stat-val">¥{{ ((memberInfo?.balance || 0) / 100).toFixed(2) }}</div>
        <div class="stat-lbl">储值</div>
      </div>
      <div class="stat-cell">
        <div class="stat-val">{{ memberInfo?.points || 0 }}</div>
        <div class="stat-lbl">积分</div>
      </div>
      <div class="stat-cell">
        <div class="stat-val">{{ memberInfo?.consumeCount || 0 }}</div>
        <div class="stat-lbl">到店</div>
      </div>
    </div>

    <!-- 我的功能 -->
    <div class="section-title">
      <span>我的</span>
    </div>
    <div class="menu-card ui-card">
      <div class="menu-row" v-for="(m, i) in mineMenus" :key="i" @click="onClick(m)">
        <div class="m-icon" :class="`ic-${m.tone}`">
          <van-icon :name="m.icon" size="18" />
        </div>
        <span class="m-text">{{ m.label }}</span>
        <span v-if="m.badge" class="m-badge">{{ m.badge }}</span>
        <van-icon name="arrow" class="m-arrow" />
      </div>
    </div>

    <!-- 增值服务 -->
    <div class="section-title">
      <span>增值服务</span>
    </div>
    <div class="menu-card ui-card">
      <div class="menu-row" v-for="(m, i) in serviceMenus" :key="i" @click="onClick(m)">
        <div class="m-icon" :class="`ic-${m.tone}`">
          <van-icon :name="m.icon" size="18" />
        </div>
        <span class="m-text">{{ m.label }}</span>
        <span v-if="m.badge" class="m-badge">{{ m.badge }}</span>
        <van-icon name="arrow" class="m-arrow" />
      </div>
    </div>

    <!-- 退出 -->
    <div class="logout-btn" @click="onLogout">
      <van-icon name="revoke" />
      <span>退出登录</span>
    </div>

    <div class="version">v 1.0 · 星河·会记</div>

    <TabBar :items="tabItems" />
    <div class="bottom-placeholder"></div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
import { useMemberStore } from '@/stores/member'
import BlogHeader from '@/components/BlogHeader.vue'
import TabBar from '@/components/TabBar.vue'

const router = useRouter()
const memberStore = useMemberStore()
const memberInfo = computed(() => memberStore.memberInfo)

const slogans = [
  '回到这里，就是回到日常',
  '会记里的你，正在被好好记得',
  '愿每段关系都被妥善安放'
]
const slogan = slogans[Math.floor(Math.random() * slogans.length)]

const avatarText = computed(() => memberInfo.value?.name?.charAt(0) || '星')

const progressPct = computed(() => {
  const v = (memberInfo.value?.totalAmount || 0) / 100
  return Math.min(100, Math.round((v / 10000) * 100))
})
const gapToNext = computed(() => {
  const v = (memberInfo.value?.totalAmount || 0) / 100
  return Math.max(0, 10000 - v).toFixed(0)
})

const stats = null

const mineMenus: { label: string; icon: string; tone: string; path: string; badge?: string }[] = [
  { label: '我的券', icon: 'coupon-o', tone: 'brand', path: '/my-coupons' },
  { label: '消费记录', icon: 'balance-list-o', tone: 'mist', path: '/transactions' },
  { label: '我的订单', icon: 'orders-o', tone: 'rose', path: '/my-orders' },
  { label: '商城订单', icon: 'gift-o', tone: 'brand', path: '/mall/orders' },
  { label: '附近门店', icon: 'shop-o', tone: 'clay', path: '/stores' }
]

const serviceMenus: { label: string; icon: string; tone: string; path: string; badge?: string }[] = [
  { label: '积分商城', icon: 'star-o', tone: 'brand', path: '/mall' },
  { label: '赢奖游戏', icon: 'point-gift-o', tone: 'twilight', path: '/games' },
  { label: '邀请有礼', icon: 'share-o', tone: 'rose', path: '/referral', badge: '双向 30' },
  { label: '关于星河', icon: 'info-o', tone: 'mist', path: '/about' },
  { label: '帮助中心', icon: 'question-o', tone: 'clay', path: '/help' }
]

const tabItems = [
  { path: '/', label: '首页', icon: 'wap-home-o' },
  { path: '/mall', label: '商城', icon: 'gift-card-o' },
  { path: '/my-orders', label: '订单', icon: 'orders-o' },
  { path: '/profile', label: '我的', icon: 'user-o' }
]

function onClick(m: any) {
  if (m.path) router.push(m.path)
}

async function onLogout() {
  try {
    await showConfirmDialog({ title: '确认退出', message: '真的要离开星河吗？' })
  } catch { return }
  memberStore.logout()
  showToast('已退出')
  router.replace('/login')
}
</script>

<style scoped>
.profile { padding-bottom: 24px; }

.user-wrap { padding: 0 16px 12px; }
.user-card {
  position: relative;
  background: linear-gradient(135deg, #5a7d9f 0%, #4a6a87 100%);
  color: #fff;
  border-radius: var(--r-lg);
  padding: 18px 20px 16px;
  overflow: hidden;
  box-shadow: 0 6px 24px rgba(74, 106, 135, 0.18);
}
.user-card.lv-1 { background: linear-gradient(135deg, #6c7066 0%, #8a8e85 100%); }
.user-card.lv-2 { background: linear-gradient(135deg, #5a7d9f 0%, #6f94b8 100%); }
.user-card.lv-3 { background: linear-gradient(135deg, #4a6a87 0%, #5a7d9f 100%); }
.user-card.lv-4 { background: linear-gradient(135deg, #3a5a76 0%, #4a6a87 100%); }
.card-decor { position: absolute; border-radius: 50%; pointer-events: none; }
.decor-1 { width: 200px; height: 200px; top: -80px; right: -60px; background: radial-gradient(circle, rgba(255, 255, 255, 0.12), transparent 60%); }
.decor-2 { width: 120px; height: 120px; bottom: -40px; left: -30px; background: radial-gradient(circle, rgba(255, 255, 255, 0.06), transparent 60%); }

.uc-content { position: relative; z-index: 1; }
.uc-top { display: flex; align-items: center; gap: 12px; }
.uc-avatar {
  width: 48px; height: 48px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.18);
  border: 2px solid rgba(255, 255, 255, 0.30);
  display: flex; align-items: center; justify-content: center;
  font-size: 22px; font-weight: 500;
  font-family: 'Songti SC', serif;
}
.uc-info { flex: 1; min-width: 0; }
.uc-name {
  font-size: 16px; font-weight: 500;
  display: flex; align-items: center; gap: 8px;
  letter-spacing: 0.02em;
}
.uc-level {
  font-size: 10.5px;
  background: rgba(255, 255, 255, 0.22);
  padding: 2px 8px;
  border-radius: 999px;
  font-weight: 400;
  letter-spacing: 0.04em;
}
.uc-phone {
  font-size: 11.5px;
  opacity: 0.8;
  margin-top: 4px;
  font-family: 'SF Mono', monospace;
  letter-spacing: 0.12em;
}
.uc-set {
  font-size: 20px;
  color: rgba(255, 255, 255, 0.85);
  padding: 4px;
}

.uc-progress { margin-top: 14px; }
.prog-row { display: flex; justify-content: space-between; font-size: 11px; opacity: 0.85; margin-bottom: 6px; }
.prog-tip { opacity: 0.7; }
.prog-bar { height: 4px; background: rgba(255, 255, 255, 0.18); border-radius: 2px; overflow: hidden; }
.prog-fill { height: 100%; background: rgba(255, 255, 255, 0.9); border-radius: 2px; transition: width var(--dur-slow) var(--ease); }

.stat-row {
  display: grid; grid-template-columns: repeat(3, 1fr);
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-lg);
  margin: 0 16px 0;
  padding: 14px 0;
  position: relative; z-index: 1;
}
.stat-cell { text-align: center; border-right: 1px solid var(--line); }
.stat-cell:last-child { border-right: none; }
.stat-val { font-size: 17px; font-weight: 600; color: var(--ink); font-variant-numeric: tabular-nums; letter-spacing: 0.01em; }
.stat-lbl { font-size: 11.5px; color: var(--muted); margin-top: 4px; letter-spacing: 0.08em; }

.menu-card { margin: 0 16px 0; }
.menu-row {
  display: flex; align-items: center; gap: 12px;
  padding: 13px 0;
  border-bottom: 1px dashed var(--line);
  cursor: pointer;
  transition: opacity var(--dur) var(--ease);
}
.menu-card .menu-row:last-child { border-bottom: none; }
.menu-row:active { opacity: 0.6; }
.m-icon {
  width: 30px; height: 30px;
  border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.m-icon.ic-brand { background: var(--brand-soft); color: var(--brand-deep); }
.m-icon.ic-rose { background: var(--accent-rose-soft); color: #8a5a52; }
.m-icon.ic-mist { background: rgba(168, 181, 184, 0.18); color: #5d6e72; }
.m-icon.ic-clay { background: var(--accent-clay-soft); color: #8a5a32; }
.m-text { flex: 1; font-size: 13.5px; color: var(--ink-2); letter-spacing: 0.02em; }
.m-badge {
  font-size: 10.5px;
  padding: 1px 7px;
  background: var(--accent-rose-soft);
  color: #8a5a52;
  border-radius: 999px;
  letter-spacing: 0.04em;
}
.m-arrow { color: var(--muted-2); font-size: 14px; }

.logout-btn {
  margin: 18px 16px 0;
  display: flex; align-items: center; justify-content: center; gap: 6px;
  height: 42px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-md);
  color: #8a5a52;
  font-size: 13.5px;
  letter-spacing: 0.16em;
  cursor: pointer;
  transition: all var(--dur) var(--ease);
}
.logout-btn:active { transform: scale(0.99); background: var(--surface-2); }
.logout-btn .van-icon { font-size: 16px; }

.version {
  text-align: center;
  font-size: 11px;
  color: var(--muted);
  margin: 24px 0 8px;
  letter-spacing: 0.16em;
  opacity: 0.7;
}
.bottom-placeholder { height: 70px; }
</style>
