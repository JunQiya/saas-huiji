<template>
  <div class="page home">
    <!-- 顶部 blog header -->
    <BlogHeader :slogan="slogan" />

    <!-- 会员卡 -->
    <div class="card-wrap x-fade">
      <MemberCard
        :name="memberStore.memberInfo?.name || '游客'"
        :phone="memberStore.memberInfo?.phone || ''"
        :level="memberStore.memberInfo?.level || 1"
        :level-name="memberStore.memberInfo?.levelName || '普通会员'"
        :balance="memberStore.memberInfo?.balance || 0"
        :points="memberStore.memberInfo?.points || 0"
        :consume-count="memberStore.memberInfo?.consumeCount || 0"
      />
    </div>

    <!-- 4 宫格快捷 -->
    <div class="grid-card ui-card x-stagger">
      <div class="grid-item" v-for="(g, i) in grids" :key="i" @click="router.push(g.path)">
        <div class="grid-icon" :class="`ic-${g.tone}`">
          <van-icon :name="g.icon" size="20" />
        </div>
        <div class="grid-label">{{ g.label }}</div>
      </div>
    </div>

    <!-- 今日小语 -->
    <div class="quote-card x-fade">
      <div class="quote-mark">「</div>
      <div class="quote-body">{{ quote.text }}</div>
      <div class="quote-author">— {{ quote.from }}</div>
    </div>

    <!-- 活动 banner -->
    <div class="section-title x-fade">
      <span>精彩活动</span>
      <span class="st-tip">轻触查看</span>
    </div>
    <div class="banner-list">
      <div
        v-for="(b, i) in banners"
        :key="i"
        class="banner-item"
        :class="`banner-${b.tone}`"
        @click="router.push(b.path || '/promotion/1')"
      >
        <div class="b-left">
          <div class="b-tag">{{ b.tag }}</div>
          <div class="b-title">{{ b.title }}</div>
          <div class="b-sub">{{ b.sub }}</div>
        </div>
        <div class="b-right">
          <van-icon :name="b.icon" size="34" />
        </div>
      </div>
    </div>

    <!-- 会员权益 -->
    <div class="section-title x-fade">
      <span>会员权益</span>
      <span class="st-tip">你的待解锁</span>
    </div>
    <div class="ui-card benefits x-fade">
      <div class="ben-row" v-for="(b, i) in benefits" :key="i" @click="b.path && router.push(b.path)">
        <div class="ben-icon" :class="`b-${b.tone}`">
          <van-icon :name="b.icon" size="18" />
        </div>
        <div class="ben-text">
          <div class="ben-title">{{ b.title }}</div>
          <div class="ben-sub">{{ b.sub }}</div>
        </div>
        <span class="ben-arrow">›</span>
      </div>
    </div>

    <!-- 快捷链接 -->
    <div class="section-title x-fade">
      <span>关于与帮助</span>
    </div>
    <div class="link-card ui-card x-fade">
      <div class="link-row" @click="router.push('/about')">
        <div class="lk-left">
          <span class="lk-dot dot-mist"></span>
          <span>关于星河</span>
        </div>
        <span class="lk-arrow">›</span>
      </div>
      <div class="link-row" @click="router.push('/help')">
        <div class="lk-left">
          <span class="lk-dot dot-clay"></span>
          <span>帮助中心</span>
        </div>
        <span class="lk-arrow">›</span>
      </div>
      <div class="link-row" @click="router.push('/profile')">
        <div class="lk-left">
          <span class="lk-dot dot-twilight"></span>
          <span>账号设置</span>
        </div>
        <span class="lk-arrow">›</span>
      </div>
    </div>

    <div class="footnote">把每一位会员 都当作一段值得悉心维护的关系</div>

    <!-- 底部 tabbar -->
    <TabBar :items="tabItems" />

    <div class="bottom-placeholder"></div>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { h5Api } from '@/api/h5'
import { useMemberStore } from '@/stores/member'
import { initWxSdk, wxShare } from '@/utils/wx-sdk'
import BlogHeader from '@/components/BlogHeader.vue'
import MemberCard from '@/components/MemberCard.vue'
import TabBar from '@/components/TabBar.vue'

const router = useRouter()
const memberStore = useMemberStore()

const slogans = [
  '慢慢来，会员值得被认真对待',
  '把每一次到店，妥帖安放',
  '记得星河，也记得你',
  '细水长流，是最好的生意',
  '今天的小心意，明天的大回响'
]
const slogan = slogans[Math.floor(Math.random() * slogans.length)]

const quotes = [
  { text: '所有温柔的事物，都在缓慢地生长。', from: '今日小语' },
  { text: '愿你到店时的心情，如你钟爱的那杯茶。', from: '星河·会记' },
  { text: '细碎日子里的小惊喜，是被记得的证据。', from: '今日小语' },
  { text: '一束光落在卡面上，正好是你今天的样子。', from: '星河·会记' }
]
const quote = quotes[Math.floor(Math.random() * quotes.length)]

const grids = [
  { label: '我的券', path: '/my-coupons', icon: 'coupon-o', tone: 'brand' },
  { label: '领券中心', path: '/coupon-center', icon: 'gift-card-o', tone: 'rose' },
  { label: '门店点餐', path: '/dining', icon: 'point-gift-o', tone: 'sage' },
  { label: '赢奖游戏', path: '/games', icon: 'point-gift-o', tone: 'twilight' },
  { label: '消费记录', path: '/transactions', icon: 'balance-list-o', tone: 'mist' },
  { label: '附近门店', path: '/stores', icon: 'shop-o', tone: 'clay' },
  { label: '我的订单', path: '/my-orders', icon: 'orders-o', tone: 'twilight' },
  { label: '邀请有礼', path: '/referral', icon: 'share-o', tone: 'sage' },
  { label: '积分商城', path: '/mall', icon: 'star-o', tone: 'brand' },
  { label: '联系客服', path: '/help', icon: 'service-o', tone: 'clay' }
]

const banners = [
  { tag: '生日月', title: '50 元代金券', sub: '给你 30 天的温柔', icon: 'gem-o', tone: 'rose', path: '/promotion/1' },
  { tag: '新朋友', title: '欢迎礼 30 元', sub: '首次注册即得', icon: 'like-o', tone: 'brand', path: '/coupon-center' },
  { tag: '沉睡唤醒', title: '50 元代金券', sub: '我们想你了', icon: 'moon-o', tone: 'clay', path: '/promotion/3' }
]

const benefits = [
  { title: '生日礼遇', sub: '生日月自动发放 50 元代金券', icon: 'gem-o', tone: 'rose' },
  { title: '等级折扣', sub: '金卡 9 折 / 钻石 8.5 折', icon: 'medal-o', tone: 'brand' },
  { title: '积分商城', sub: '用积分兑一份心意', icon: 'star-o', tone: 'mist', path: '/mall' },
  { title: '推荐有礼', sub: '老带新双向各得 30 元券', icon: 'share-o', tone: 'clay', path: '/referral' }
]

const tabItems = [
  { path: '/', label: '首页', icon: 'wap-home-o' },
  { path: '/mall', label: '商城', icon: 'gift-card-o' },
  { path: '/my-orders', label: '订单', icon: 'orders-o' },
  { path: '/profile', label: '我的', icon: 'user-o' }
]

async function loadProfile() {
  try {
    const p = await h5Api.profile()
    memberStore.setMember(p)
  } catch {/* */}
}

// 初始化微信 JS-SDK 并设置默认分享内容（非微信环境静默失败）
async function initWx() {
  try {
    await initWxSdk()
    wxShare('星河·会记', '会员卡 · 优惠券 · 积分商城', window.location.href, '')
  } catch {
    // 非微信环境或签名失败，不影响页面正常使用
  }
}

onMounted(() => {
  loadProfile()
  initWx()
})
</script>

<style scoped>
.home { padding: 0 0 24px; }
.card-wrap { padding: 8px 16px 12px; }

/* 4 宫格 */
.grid-card {
  margin: 0 16px;
  padding: 16px 8px 14px;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 6px 0;
}
.grid-item {
  text-align: center;
  padding: 4px 0;
  cursor: pointer;
  transition: transform var(--dur) var(--ease-out);
}
.grid-item:active { transform: scale(0.96); }
.grid-icon {
  width: 38px; height: 38px;
  border-radius: 9px;
  margin: 0 auto 6px;
  display: flex; align-items: center; justify-content: center;
  transition: transform var(--dur) var(--ease-out);
}
.grid-item:hover .grid-icon { transform: scale(1.04); }
.grid-icon.ic-brand { background: var(--brand-soft); color: var(--brand-deep); }
.grid-icon.ic-rose { background: var(--accent-rose-soft); color: #8a5a52; }
.grid-icon.ic-mist { background: var(--accent-mist-soft); color: #4d5e68; }
.grid-icon.ic-clay { background: var(--accent-clay-soft); color: #8a5a3a; }
.grid-icon.ic-twilight { background: var(--accent-twilight-soft); color: #5e5278; }
.grid-icon.ic-sage { background: var(--accent-sage-soft); color: #4a6655; }
.grid-label { font-size: 12px; color: var(--ink-2); letter-spacing: 0.02em; }

/* 今日小语 */
.quote-card {
  margin: 14px 16px 0;
  padding: 14px 16px 12px;
  background: var(--surface-2);
  border: 1px dashed var(--line-2);
  border-left: 2px solid var(--brand);
  border-radius: 0 var(--r-md) var(--r-md) 0;
  position: relative;
  color: var(--ink-2);
}
.quote-mark {
  position: absolute; top: 4px; left: 8px;
  font-size: 26px; line-height: 1;
  color: var(--brand);
  font-family: var(--font-serif);
  opacity: 0.45;
}
.quote-body {
  font-family: var(--font-serif);
  font-size: 13.5px; line-height: 1.8;
  letter-spacing: 0.04em;
  padding-left: 14px;
  color: var(--ink);
}
.quote-author {
  font-family: var(--font-serif);
  font-size: 11px; color: var(--muted);
  margin-top: 6px; padding-left: 14px;
  letter-spacing: 0.18em;
}

/* 活动列表 */
.banner-list { padding: 0 16px; display: flex; flex-direction: column; gap: 10px; }
.banner-item {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 18px;
  border-radius: var(--r-md);
  color: #fff;
  position: relative;
  overflow: hidden;
  cursor: pointer;
  transition: transform var(--dur) var(--ease-out);
  min-height: 88px;
}
.banner-item:active { transform: scale(0.99); }
.banner-item::before {
  content: '';
  position: absolute; right: -30px; top: -30px;
  width: 120px; height: 120px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.06);
}
.banner-item::after {
  content: '';
  position: absolute; left: -40px; bottom: -40px;
  width: 100px; height: 100px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.04);
}
/* 去除高饱和渐变，使用低饱和纯色 */
.banner-rose { background: #a88580; }
.banner-brand { background: #5a7d9f; }
.banner-clay { background: #a88366; }
.b-tag {
  display: inline-block;
  font-family: var(--font-serif);
  font-size: 10.5px;
  padding: 2px 8px;
  background: rgba(255, 255, 255, 0.16);
  border-radius: 2px;
  margin-bottom: 6px;
  letter-spacing: 0.16em;
}
.b-title {
  font-family: var(--font-serif);
  font-size: 17px; font-weight: 500;
  letter-spacing: 0.04em; line-height: 1.3;
}
.b-sub { font-family: var(--font-serif); font-size: 12px; opacity: 0.85; margin-top: 4px; letter-spacing: 0.06em; }
.b-right { opacity: 0.5; z-index: 1; }

/* 会员权益 */
.benefits { margin: 0 16px; padding: 4px 16px; }
.ben-row {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 0;
  border-bottom: 1px dashed var(--line);
  cursor: pointer;
  transition: background-color var(--dur) var(--ease-out);
}
.ben-row:last-child { border-bottom: none; }
.ben-row:active { background: var(--surface-2); }
.ben-icon {
  width: 32px; height: 32px;
  border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.ben-icon.b-brand { background: var(--brand-soft); color: var(--brand-deep); }
.ben-icon.b-rose { background: var(--accent-rose-soft); color: #8a5a52; }
.ben-icon.b-mist { background: var(--accent-mist-soft); color: #4d5e68; }
.ben-icon.b-clay { background: var(--accent-clay-soft); color: #8a5a3a; }
.ben-text { flex: 1; min-width: 0; }
.ben-title { font-family: var(--font-serif); font-size: 13.5px; color: var(--ink); font-weight: 500; letter-spacing: 0.04em; }
.ben-sub { font-size: 11.5px; color: var(--muted); margin-top: 2px; letter-spacing: 0.02em; }
.ben-arrow { color: var(--muted-2); font-size: 16px; line-height: 1; font-family: var(--font-serif); }

/* 链接 */
.link-card { margin: 0 16px; padding: 4px 16px; }
.link-row {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px dashed var(--line);
  cursor: pointer;
  transition: opacity var(--dur) var(--ease-out);
}
.link-row:last-child { border-bottom: none; }
.link-row:active { opacity: 0.6; }
.lk-left { display: flex; align-items: center; gap: 10px; font-size: 13.5px; color: var(--ink-2); font-family: var(--font-serif); letter-spacing: 0.04em; }
.lk-dot { display: inline-block; width: 6px; height: 6px; border-radius: 50%; }
.lk-arrow { color: var(--muted-2); font-size: 16px; line-height: 1; font-family: var(--font-serif); }

.bottom-placeholder { height: 70px; }
</style>
