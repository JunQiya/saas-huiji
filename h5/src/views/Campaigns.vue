<template>
  <div class="page campaigns">
    <NavBar title="全部活动" back />
    <div class="page-padding">
      <div class="page-tip">每一场活动，都是一次被认真对待的邀约。</div>

      <div v-if="loading" class="loading"><van-loading /></div>
      <EmptyState v-else-if="!list.length" title="暂无进行中的活动" sub="新活动正在路上，敬请期待" art="star" />

      <div v-else class="camp-list">
        <div
          v-for="(c, i) in list"
          :key="c.id"
          class="camp-card"
          :class="`camp-${toneOf(i)}`"
          @click="goDetail(c)"
        >
          <div class="cc-left">
            <div class="cc-tag">{{ tagText(c) }}</div>
            <div class="cc-title">{{ c.name }}</div>
            <div class="cc-sub">{{ c.subtitle || c.timeText || '点击查看活动详情' }}</div>
          </div>
          <div class="cc-right">
            <van-icon :name="iconOf(c)" size="30" />
            <span class="cc-arrow">›</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { h5Api, type CampaignDetail } from '@/api/h5'
import NavBar from '@/components/NavBar.vue'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()
const loading = ref(false)
const list = ref<CampaignDetail[]>([])

const tones = ['brand', 'rose', 'clay', 'sage', 'twilight']

function toneOf(i: number) { return tones[i % tones.length] }
function tagText(c: CampaignDetail) {
  return ({ BIRTHDAY: '生日关怀', DORMANT: '沉睡唤醒', REPURCHASE: '回购刺激', MANUAL: '人工活动' } as any)[c.tag || ''] || c.tag || '活动'
}
function iconOf(c: CampaignDetail) {
  return ({ BIRTHDAY: 'gem-o', DORMANT: 'moon-o', REPURCHASE: 'replay', MANUAL: 'gift-o' } as any)[c.tag || ''] || 'gift-o'
}

function goDetail(c: CampaignDetail) {
  router.push(`/promotion/${c.id}`)
}

async function load() {
  loading.value = true
  try { list.value = await h5Api.campaigns() }
  catch (e: any) { showToast(e?.message || '加载失败') }
  finally { loading.value = false }
}

onMounted(load)
</script>

<style scoped>
.campaigns { padding-bottom: 32px; }
.page-tip {
  font-size: 12px; color: var(--muted);
  letter-spacing: 0.04em; margin-bottom: 14px;
  font-family: var(--font-serif); opacity: 0.85;
  padding-left: 2px;
}
.loading { display: flex; justify-content: center; padding: 40px 0; }

.camp-list { display: flex; flex-direction: column; gap: 10px; }
.camp-card {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 18px;
  border-radius: var(--r-md);
  color: #fff;
  min-height: 84px;
  cursor: pointer;
  transition: transform var(--dur) var(--ease-out);
}
.camp-card:active { transform: scale(0.99); }
.camp-brand { background: var(--brand); }
.camp-rose { background: var(--accent-rose); }
.camp-clay { background: var(--accent-clay); }
.camp-sage { background: var(--accent-sage); }
.camp-twilight { background: var(--accent-twilight); }

.cc-left { flex: 1; min-width: 0; }
.cc-tag {
  display: inline-block;
  font-family: var(--font-serif);
  font-size: 10.5px;
  padding: 2px 8px;
  background: rgba(255, 255, 255, 0.18);
  border-radius: 2px;
  margin-bottom: 6px;
  letter-spacing: 0.16em;
}
.cc-title {
  font-family: var(--font-serif);
  font-size: 16px; font-weight: 500;
  letter-spacing: 0.04em; line-height: 1.3;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.cc-sub {
  font-family: var(--font-serif);
  font-size: 12px; opacity: 0.85; margin-top: 4px;
  letter-spacing: 0.06em;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.cc-right {
  display: flex; align-items: center; gap: 10px;
  margin-left: 14px;
  opacity: 0.75;
}
.cc-arrow { font-family: var(--font-serif); font-size: 20px; }
</style>
