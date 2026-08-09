<template>
  <div class="page game-list">
    <NavBar title="赢奖小游戏" back />
    <div class="page-padding">
      <div class="page-tip">把好运，轻轻递到手里。</div>

      <div v-if="loading" class="loading"><van-loading color="#5a7a9c" /></div>
      <EmptyState v-else-if="!list.length" title="暂无可玩的游戏" sub="更多惊喜正在路上" art="star" />

      <div v-else class="game-cards x-stagger">
        <div
          v-for="g in list"
          :key="g.id"
          class="game-card ui-card"
          :class="`type-${g.type}`"
          @click="enter(g)"
        >
          <div class="gc-cover">
            <img v-if="g.coverImage" :src="g.coverImage" :alt="g.name" />
            <van-icon v-else :name="typeIcon(g.type)" size="36" />
          </div>
          <div class="gc-body">
            <div class="gc-name">{{ g.name }}</div>
            <div class="gc-sub" v-if="g.subtitle">{{ g.subtitle }}</div>
            <div class="gc-meta">
              <span class="chip brand">{{ typeText(g.type) }}</span>
              <span class="gc-cost" v-if="g.pointsCost > 0">{{ g.pointsCost }} 积分/次</span>
              <span class="gc-cost" v-else>免费</span>
              <span class="gc-remaining" v-if="remainingText(g)">{{ remainingText(g) }}</span>
            </div>
          </div>
          <div class="gc-arrow">›</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onActivated, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { gameApi } from '@/api/h5'
import NavBar from '@/components/NavBar.vue'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()
const loading = ref(false)
const list = ref<any[]>([])

// 游戏类型文案
function typeText(t: string) {
  return ({ WHEEL: '大转盘', SCRATCH: '刮刮乐', EGG: '砸金蛋', SHAKE: '摇一摇' } as any)[t] || '游戏'
}
// 游戏类型图标
function typeIcon(t: string) {
  return ({ WHEEL: 'point-gift-o', SCRATCH: 'gold-coin-o', EGG: 'gift-o', SHAKE: 'shake-o' } as any)[t] || 'gift-o'
}

// 剩余次数展示
function remainingText(g: any): string {
  if (typeof g.remaining === 'number') {
    if (g.remaining <= 0) return '今日已用完'
    return `今日剩余 ${g.remaining} 次`
  }
  if (typeof g.plays === 'number') {
    if (g.plays <= 0) return '今日已用完'
    return `今日剩余 ${g.plays} 次`
  }
  if (typeof g.todayLimit === 'number') {
    if (g.todayLimit <= 0) return '今日已用完'
    return `今日剩余 ${g.todayLimit} 次`
  }
  return ''
}

// 根据游戏类型跳转对应页面
function enter(g: any) {
  const pathMap: Record<string, string> = {
    WHEEL: `/games/wheel/${g.id}`,
    SCRATCH: `/games/scratch/${g.id}`,
    EGG: `/games/egg/${g.id}`,
    SHAKE: `/games/shake/${g.id}`
  }
  const fallback = pathMap[g.type] || `/games/wheel/${g.id}`
  router.push(fallback)
}

async function load() {
  loading.value = true
  try { list.value = (await gameApi.list()) || [] } catch (e: any) { showToast(e?.message || '加载游戏失败') }
  finally { loading.value = false }
}

onMounted(load)
onActivated(load)
</script>

<style scoped>
.game-list { padding-bottom: 24px; }
.page-tip {
  font-family: var(--font-serif);
  font-size: 12.5px; color: var(--muted);
  letter-spacing: 0.06em; margin-bottom: 14px;
}
.loading { padding: 60px 0; text-align: center; }

.game-cards { display: flex; flex-direction: column; gap: 12px; }
.game-card {
  display: flex; align-items: center; gap: 14px;
  padding: 14px;
  cursor: pointer;
  transition: transform var(--dur) var(--ease-out);
}
.game-card:active { transform: scale(0.99); }
.gc-cover {
  width: 64px; height: 64px;
  border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
  color: #fff;
}
.game-card.type-WHEEL .gc-cover { background: var(--brand); }
.game-card.type-SCRATCH .gc-cover { background: var(--accent-clay); }
.game-card.type-EGG .gc-cover { background: var(--warning); }
.game-card.type-SHAKE .gc-cover { background: var(--accent-twilight); }
.gc-cover img { width: 100%; height: 100%; object-fit: cover; }
.gc-body { flex: 1; min-width: 0; }
.gc-name {
  font-family: var(--font-serif);
  font-size: 15.5px; font-weight: 500; color: var(--ink);
  letter-spacing: 0.04em;
}
.gc-sub {
  font-size: 12px; color: var(--muted);
  margin-top: 3px; letter-spacing: 0.02em;
}
.gc-meta {
  display: flex; align-items: center; gap: 8px;
  margin-top: 8px;
}
.gc-cost {
  font-size: 11.5px; color: var(--ink-3);
  font-family: var(--font-num);
}
.gc-remaining {
  font-size: 11px;
  color: var(--warning);
  background: rgba(184, 132, 92, 0.10);
  padding: 1px 8px;
  border-radius: 999px;
  letter-spacing: 0.04em;
  font-family: var(--font-num);
}
.gc-arrow {
  color: var(--muted-2); font-size: 18px;
  font-family: var(--font-serif);
  flex-shrink: 0;
}
</style>
