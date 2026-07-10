<template>
  <div class="page home">
    <!-- 顶部 blog header -->
    <BlogHeader :slogan="slogan" />

    <!-- 会员卡 -->
    <div class="card-wrap">
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
    <div class="grid-card ui-card">
      <div class="grid-item" v-for="(g, i) in grids" :key="i" @click="router.push(g.path)">
        <div class="grid-icon" :class="`ic-${g.tone}`">
          <van-icon :name="g.icon" size="20" />
        </div>
        <div class="grid-label">{{ g.label }}</div>
      </div>
    </div>

    <!-- 今日小语 -->
    <div class="quote-card">
      <div class="quote-mark">「</div>
      <div class="quote-body">{{ quote.text }}</div>
      <div class="quote-author">— {{ quote.from }}</div>
    </div>

    <!-- 活动 banner -->
    <div class="section-title">
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
          <van-icon :name="b.icon" size="36" />
        </div>
      </div>
    </div>

    <!-- 会员权益 -->
    <div class="section-title">
      <span>会员权益</span>
      <span class="st-tip">你的待解锁</span>
    </div>
    <div class="ui-card benefits">
      <div class="ben-row" v-for="(b, i) in benefits" :key="i" @click="b.path && router.push(b.path)">
        <div class="ben-icon" :class="`b-${b.tone}`">
          <van-icon :name="b.icon" size="18" />
        </div>
        <div class="ben-text">
          <div class="ben-title">{{ b.title }}</div>
          <div class="ben-sub">{{ b.sub }}</div>
        </div>
        <van-icon name="arrow" class="ben-arrow" />
      </div>
    </div>

    <!-- 快捷链接 -->
    <div class="section-title">
      <span>关于我们</span>
    </div>
    <div class="link-card ui-card">
      <div class="link-row" @click="router.push('/about')">
        <div class="lk-left">
          <van-icon name="info-o" />
          <span>关于星河</span>
        </div>
        <span class="lk-arrow">›</span>
      </div>
      <div class="link-row" @click="router.push('/help')">
        <div class="lk-left">
          <van-icon name="question-o" />
          <span>帮助中心</span>
        </div>
        <span class="lk-arrow">›</span>
      </div>
      <div class="link-row" @click="router.push('/profile')">
        <div class="lk-left">
          <van-icon name="setting-o" />
          <span>账号设置</span>
        </div>
        <span class="lk-arrow">›</span>
      </div>
    </div>

    <!-- 底部 tabbar -->
    <TabBar :items="tabItems" />

    <div class="bottom-placeholder"></div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { h5Api } from '@/api/h5'
import { useMemberStore } from '@/stores/member'
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
  { label: '消费记录', path: '/transactions', icon: 'balance-list-o', tone: 'mist' },
  { label: '附近门店', path: '/stores', icon: 'shop-o', tone: 'clay' },
  { label: '我的订单', path: '/my-orders', icon: 'orders-o', tone: 'brand' },
  { label: '邀请有礼', path: '/referral', icon: 'share-o', tone: 'rose' },
  { label: '积分商城', path: '/mall', icon: 'star-o', tone: 'mist' },
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

onMounted(loadProfile)
</script>

<style scoped>
.home { padding: 0 0 24px; }

.card-wrap { padding: 8px 16px 12px; }

/* 4 宫格 */
.grid-card {
  margin: 0 16px;
  padding: 18px 8px;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 4px 0;
}
.grid-item {
  text-align: center;
  padding: 6px 0;
  cursor: pointer;
  transition: transform var(--dur) var(--ease);
}
.grid-item:active { transform: scale(0.97); }
.grid-icon {
  width: 40px; height: 40px;
  border-radius: 12px;
  margin: 0 auto 6px;
  display: flex; align-items: center; justify-content: center;
}
.grid-icon.ic-brand { background: var(--brand-soft); color: var(--brand-deep); }
.grid-icon.ic-rose { background: var(--accent-rose-soft); color: #8a5a52; }
.grid-icon.ic-mist { background: rgba(168, 181, 184, 0.18); color: #5d6e72; }
.grid-icon.ic-clay { background: var(--accent-clay-soft); color: #8a5a32; }
.grid-label { font-size: 12px; color: var(--ink-2); letter-spacing: 0.02em; }

/* 今日小语 */
.quote-card {
  margin: 14px 16px 0;
  padding: 14px 16px;
  background: var(--surface-2);
  border: 1px dashed var(--line-2);
  border-radius: var(--r-md);
  position: relative;
  color: var(--ink-2);
}
.quote-mark {
  position: absolute; top: 4px; left: 10px;
  font-size: 26px; line-height: 1;
  color: var(--brand);
  font-family: 'Songti SC', serif;
  opacity: 0.5;
}
.quote-body {
  font-size: 13.5px; line-height: 1.7;
  letter-spacing: 0.04em;
  padding-left: 12px;
  font-family: 'Songti SC', 'STSong', serif;
}
.quote-author {
  font-size: 11px; color: var(--muted);
  margin-top: 6px; padding-left: 12px;
  letter-spacing: 0.1em;
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
  transition: transform var(--dur) var(--ease);
  min-height: 90px;
}
.banner-item:active { transform: scale(0.99); }
.banner-item::before {
  content: '';
  position: absolute; right: -30px; top: -30px;
  width: 120px; height: 120px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
}
.banner-rose { background: linear-gradient(135deg, #b88780 0%, #a8736a 100%); }
.banner-brand { background: linear-gradient(135deg, #5a7d9f 0%, #4a6a87 100%); }
.banner-clay { background: linear-gradient(135deg, #b8825a 0%, #9c6a45 100%); }
.b-tag {
  display: inline-block;
  font-size: 10.5px;
  padding: 2px 8px;
  background: rgba(255, 255, 255, 0.18);
  border-radius: 999px;
  margin-bottom: 6px;
  letter-spacing: 0.08em;
}
.b-title { font-size: 17px; font-weight: 600; letter-spacing: 0.02em; line-height: 1.3; }
.b-sub { font-size: 12px; opacity: 0.85; margin-top: 4px; letter-spacing: 0.04em; }
.b-right { opacity: 0.5; z-index: 1; }

/* 会员权益 */
.benefits { margin: 0 16px; }
.ben-row {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 0;
  border-bottom: 1px dashed var(--line);
  cursor: pointer;
  transition: background-color var(--dur) var(--ease);
}
.ben-row:last-child { border-bottom: none; }
.ben-row:active { background: var(--surface-2); margin: 0 -16px; padding: 12px 16px; }
.ben-icon {
  width: 32px; height: 32px;
  border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.ben-icon.b-brand { background: var(--brand-soft); color: var(--brand-deep); }
.ben-icon.b-rose { background: var(--accent-rose-soft); color: #8a5a52; }
.ben-icon.b-mist { background: rgba(168, 181, 184, 0.18); color: #5d6e72; }
.ben-icon.b-clay { background: var(--accent-clay-soft); color: #8a5a32; }
.ben-text { flex: 1; min-width: 0; }
.ben-title { font-size: 13.5px; color: var(--ink); font-weight: 500; letter-spacing: 0.02em; }
.ben-sub { font-size: 11.5px; color: var(--muted); margin-top: 2px; letter-spacing: 0.02em; }
.ben-arrow { color: var(--muted-2); font-size: 14px; }

/* 链接 */
.link-card { margin: 0 16px; }
.link-row {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px dashed var(--line);
  cursor: pointer;
  transition: opacity var(--dur) var(--ease);
}
.link-row:last-child { border-bottom: none; }
.link-row:active { opacity: 0.6; }
.lk-left { display: flex; align-items: center; gap: 10px; font-size: 13.5px; color: var(--ink-2); }
.lk-left .van-icon { color: var(--brand-deep); font-size: 16px; }
.lk-arrow { color: var(--muted-2); font-size: 16px; }

.bottom-placeholder { height: 70px; }
</style>
