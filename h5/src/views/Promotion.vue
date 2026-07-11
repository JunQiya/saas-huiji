<template>
  <div class="page promotion">
    <NavBar title="活动详情" back />

    <div v-if="loading" class="loading"><van-loading color="#5a7a9c" /></div>
    <EmptyState v-else-if="!detail" title="活动不存在" sub="活动可能已结束或链接有误" art="box" />

    <div v-else class="page-padding">
      <div class="hero" :class="`hero-${id}`">
        <div class="hero-content">
          <div class="hero-tag">{{ tagText }}</div>
          <h1 class="hero-title">{{ name }}</h1>
          <div class="hero-sub">{{ subText }}</div>
        </div>
      </div>

      <div class="ui-card block">
        <div class="block-title">
          <span class="dot"></span>活动规则
        </div>
        <div class="rule-text">{{ ruleText }}</div>
      </div>

      <div class="ui-card block">
        <div class="block-title">
          <span class="dot"></span>活动时间
        </div>
        <div class="rule-text">{{ timeText }}</div>
      </div>

      <div class="ui-card block">
        <div class="block-title">
          <span class="dot"></span>温馨提示
        </div>
        <ul class="tip-list">
          <li>领取的券可在「我的券」中查看</li>
          <li>请到店出示券码使用</li>
          <li>最终解释权归门店所有</li>
        </ul>
      </div>

      <button class="join-btn" @click="onJoin">立即参与</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { h5Api, type CampaignDetail } from '@/api/h5'
import NavBar from '@/components/NavBar.vue'
import EmptyState from '@/components/EmptyState.vue'

const route = useRoute()
const router = useRouter()
const id = computed(() => String(route.params.id || ''))
const loading = ref(false)
const detail = ref<CampaignDetail | null>(null)

const name = computed(() => detail.value?.name || '限时活动')
const tagText = computed(() => detail.value?.tag || '活动')
const subText = computed(() => detail.value?.subtitle || '请到店出示券码使用')
const ruleText = computed(() => detail.value?.rules || '')
const timeText = computed(() => {
  const d = detail.value
  if (!d) return ''
  if (d.timeText) return d.timeText
  if (d.startTime && d.endTime) return `${fmtDate(d.startTime)} ~ ${fmtDate(d.endTime)}`
  return ''
})

function fmtDate(t: string) {
  try {
    const d = new Date(t)
    return `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, '0')}.${String(d.getDate()).padStart(2, '0')}`
  } catch { return String(t) }
}

async function load() {
  if (!id.value) return
  loading.value = true
  try {
    detail.value = await h5Api.campaignDetail(id.value)
  } catch {
    detail.value = null
  } finally {
    loading.value = false
  }
}

async function onJoin() {
  const d = detail.value
  if (!d) return
  if (d.couponId) {
    try {
      await h5Api.claimCoupon(d.couponId)
      showToast('领取成功，请到「我的券」查看')
    } catch {
      showToast('领取失败，请稍后再试')
    }
  } else if (d.link) {
    router.push(d.link)
  } else {
    router.push('/coupon-center')
  }
}

onMounted(load)
</script>

<style scoped>
.promotion { padding-bottom: 60px; }
.loading { display: flex; justify-content: center; padding: 50px 0; }

.hero {
  position: relative;
  height: 180px;
  border-radius: var(--r-lg);
  overflow: hidden;
  margin-bottom: 16px;
  color: #fff;
  background: var(--brand);
}
.hero-1 { background: var(--accent-rose); }
.hero-2 { background: var(--success); }
.hero-3 { background: var(--accent-clay); }
.hero-content { position: relative; padding: 24px 22px; z-index: 1; }
.hero-tag {
  display: inline-block;
  font-size: 11px;
  padding: 2px 10px;
  background: rgba(255, 255, 255, 0.20);
  border-radius: 999px;
  margin-bottom: 10px;
  letter-spacing: 0.12em;
  font-weight: 500;
}
.hero-title {
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 6px;
  letter-spacing: 0.04em;
  font-family: 'Songti SC', 'STSong', serif;
}
.hero-sub { font-size: 13px; opacity: 0.85; letter-spacing: 0.04em; }

.block { margin-bottom: 12px; padding: 16px 18px; }
.block-title {
  font-size: 13px; font-weight: 600; color: var(--ink);
  margin-bottom: 8px;
  display: flex; align-items: center; gap: 8px;
  letter-spacing: 0.04em;
}
.block-title .dot { width: 5px; height: 5px; background: var(--brand); border-radius: 50%; }
.rule-text { font-size: 12.5px; color: var(--ink-2); line-height: 1.85; letter-spacing: 0.02em; }
.tip-list { margin: 0; padding-left: 18px; color: var(--ink-2); font-size: 12.5px; line-height: 1.85; }
.tip-list li { margin: 2px 0; }

.join-btn {
  position: fixed; left: 0; right: 0; bottom: 0;
  max-width: 480px; margin: 0 auto;
  width: calc(100% - 32px);
  height: 48px;
  margin-bottom: 16px;
  background: var(--brand-deep);
  color: #fff;
  border: none;
  border-radius: 999px;
  font-size: 15px;
  font-weight: 500;
  letter-spacing: 0.32em;
  cursor: pointer;
  font-family: inherit;
  z-index: 5;
}
</style>
