<template>
  <div class="page game-egg" :style="bgStyle">
    <NavBar :title="game?.name || '砸金蛋'" back />
    <div class="page-padding">
      <!-- 游戏标题区 -->
      <div class="game-head" v-if="game">
        <div class="gh-sub" v-if="game.subtitle">{{ game.subtitle }}</div>
        <div class="gh-meta">
          <span class="chip warning">砸金蛋</span>
          <span class="gh-remain">今日剩余 <em class="val">{{ remaining }}</em> 次</span>
        </div>
      </div>

      <!-- 规则 -->
      <div class="rules-box" v-if="game?.rules">
        <div class="rules-title">活动规则</div>
        <div class="rules-text">{{ game.rules }}</div>
      </div>

      <!-- 金蛋区 -->
      <div class="egg-area" v-if="prizes.length">
        <div class="egg-tip">{{ playing ? '砸开中…' : '选一颗金蛋砸开' }}</div>
        <div class="eggs">
          <div
            v-for="i in eggCount"
            :key="i"
            class="egg"
            :class="{ broken: brokenIndex === i, hitting: hittingIndex === i }"
            @click="onSmash(i)"
          >
            <div class="egg-body">
              <div class="egg-shine"></div>
              <div class="egg-zip"></div>
            </div>
            <!-- 碎片 -->
            <div v-if="brokenIndex === i" class="fragments">
              <span v-for="f in 6" :key="f" class="frag" :style="fragStyle(f)"></span>
            </div>
          </div>
        </div>
      </div>

      <div v-else-if="!loading" class="empty-box">
        <div class="empty-text">暂未配置奖品</div>
      </div>

      <!-- 我的游戏记录 -->
      <div class="section-title" @click="showRecords = !showRecords">
        <span>我的游戏记录</span>
        <span class="st-tip">{{ showRecords ? '收起' : '展开' }}</span>
      </div>
      <div class="records ui-card" v-if="showRecords">
        <div v-if="!records.length" class="rec-empty">暂无记录</div>
        <div v-for="(r, i) in records" :key="i" class="rec-row">
          <div class="rec-left">
            <span class="rec-dot" :class="r.win ? 'win' : 'miss'"></span>
            <span class="rec-name">{{ r.prizeName || '未中奖' }}</span>
          </div>
          <span class="rec-time">{{ formatTime(r.playedAt || r.createdAt) }}</span>
        </div>
      </div>

      <div class="footnote">一锤定音 好运落袋</div>
    </div>

    <!-- 中奖结果弹窗 -->
    <GameResult
      v-model:show="resultVisible"
      :result="result"
      :remaining="remaining"
      @continue="onContinue"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onActivated, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { showToast } from 'vant'
import { gameApi } from '@/api/h5'
import { formatDateTime } from '@/utils/format'
import NavBar from '@/components/NavBar.vue'
import GameResult from '@/components/GameResult.vue'

const route = useRoute()
const gameId = route.params.id as string

const loading = ref(false)
const game = ref<any>(null)
const prizes = ref<any[]>([])
const records = ref<any[]>([])
const showRecords = ref(false)

const remaining = ref(0)
const resultVisible = ref(false)
const result = ref<any>(null)
const playing = ref(false)

// 金蛋数量：3-6 个
const eggCount = ref(3)
const brokenIndex = ref(0) // 被砸碎的金蛋序号
const hittingIndex = ref(0) // 当前敲击中

const bgStyle = computed(() => {
  if (game.value?.bgImage) {
    return { backgroundImage: `url(${game.value.bgImage})` }
  }
  return {}
})

// 碎片样式
function fragStyle(i: number) {
  const angle = (i * 60) * Math.PI / 180
  const dist = 50 + (i % 3) * 14
  const x = Math.cos(angle) * dist
  const y = Math.sin(angle) * dist - 20
  return {
    transform: `translate(${x}px, ${y}px) rotate(${i * 70}deg)`,
    animationDelay: (i * 0.04) + 's'
  }
}

// 砸金蛋
async function onSmash(index: number) {
  if (playing.value) return
  if (remaining.value <= 0) {
    showToast('今日次数已用完，明日再来')
    return
  }
  playing.value = true
  hittingIndex.value = index
  try {
    const res = await gameApi.play(gameId)
    result.value = res
    // 敲击动画 → 碎裂
    setTimeout(() => {
      hittingIndex.value = 0
      brokenIndex.value = index
      // 震动反馈
      if (navigator.vibrate) navigator.vibrate(60)
    }, 300)
    // 展示结果
    setTimeout(() => {
      resultVisible.value = true
      remaining.value = Math.max(0, remaining.value - 1)
      loadRecords()
    }, 900)
  } catch {
    playing.value = false
    hittingIndex.value = 0
  }
}

// 继续：重置金蛋
function onContinue() {
  resultVisible.value = false
  result.value = null
  brokenIndex.value = 0
  hittingIndex.value = 0
  playing.value = false
  // 随机刷新金蛋数量
  eggCount.value = 3 + Math.floor(Math.random() * 4)
}

async function loadDetail() {
  loading.value = true
  try {
    const d = await gameApi.detail(gameId)
    game.value = d?.game ?? d
    prizes.value = d?.prizes || []
    remaining.value = d?.game?.remaining ?? d?.game?.dailyLimit ?? d?.remaining ?? d?.dailyLimit ?? 0
  } catch {/* */}
  finally { loading.value = false }
}

async function loadRecords() {
  try { records.value = (await gameApi.myPlays(gameId)) || [] } catch {/* */}
}

function formatTime(t?: string) {
  if (!t) return ''
  return formatDateTime(t)
}

onMounted(() => {
  loadDetail()
  loadRecords()
})
// 从其他页面返回时重新拉取剩余次数
onActivated(() => {
  loadDetail()
})
</script>

<style scoped>
.game-egg {
  padding-bottom: 32px;
  background-size: cover;
  background-position: center;
}
.game-head { margin-bottom: 14px; }
.gh-sub {
  font-family: var(--font-serif);
  font-size: 13px; color: var(--muted);
  letter-spacing: 0.04em; margin-bottom: 8px;
}
.gh-meta {
  display: flex; align-items: center; gap: 10px;
}
.gh-remain {
  font-size: 12px; color: var(--ink-3);
}
.gh-remain em {
  color: var(--warning-deep); font-weight: 500;
  font-style: normal; font-size: 14px;
}

/* 规则 */
.rules-box {
  background: var(--surface-2);
  border: 1px dashed var(--line-2);
  border-radius: var(--r-md);
  padding: 12px 14px;
  margin-bottom: 18px;
}
.rules-title {
  font-family: var(--font-serif);
  font-size: 12px; color: var(--ink-2);
  letter-spacing: 0.08em; margin-bottom: 6px;
}
.rules-text {
  font-size: 12px; color: var(--ink-3);
  line-height: 1.7; white-space: pre-wrap;
  letter-spacing: 0.02em;
}

/* 金蛋区 */
.egg-area {
  margin: 24px 0;
  text-align: center;
}
.egg-tip {
  font-family: var(--font-serif);
  font-size: 13px; color: var(--ink-2);
  letter-spacing: 0.06em;
  margin-bottom: 20px;
}
.eggs {
  display: flex; flex-wrap: wrap;
  justify-content: center;
  gap: 18px 22px;
  padding: 12px 0;
}
.egg {
  position: relative;
  width: 86px; height: 110px;
  cursor: pointer;
  transition: transform var(--dur) var(--ease-out);
}
.egg:active { transform: scale(0.96); }
.egg.hitting .egg-body {
  animation: egg-shake 0.3s var(--ease);
}
@keyframes egg-shake {
  0%, 100% { transform: rotate(0); }
  25% { transform: rotate(-8deg); }
  75% { transform: rotate(8deg); }
}
.egg.broken .egg-body {
  animation: egg-break 0.5s var(--ease-out) forwards;
}
@keyframes egg-break {
  0% { transform: scale(1); opacity: 1; }
  40% { transform: scale(1.15); opacity: 1; }
  100% { transform: scale(0); opacity: 0; }
}

.egg-body {
  position: absolute;
  inset: 0;
  /* 椭圆 + 径向渐变 */
  border-radius: 50% 50% 48% 48% / 56% 56% 44% 44%;
  background: radial-gradient(circle at 35% 30%, #f5d99a 0%, #d9a85a 45%, #b8845c 100%);
  box-shadow:
    0 6px 14px rgba(184, 132, 92, 0.35),
    inset -8px -10px 18px rgba(120, 76, 40, 0.25),
    inset 6px 8px 14px rgba(255, 240, 200, 0.4);
}
.egg-shine {
  position: absolute;
  top: 14px; left: 18px;
  width: 22px; height: 30px;
  border-radius: 50%;
  background: radial-gradient(ellipse, rgba(255,255,255,0.7), transparent 70%);
}
.egg-zip {
  position: absolute;
  top: 50%; left: 10%; right: 10%;
  height: 2px;
  background: linear-gradient(90deg, transparent, rgba(120, 76, 40, 0.4) 30%, rgba(120, 76, 40, 0.4) 70%, transparent);
  transform: translateY(-50%);
}

/* 碎片 */
.fragments {
  position: absolute;
  top: 50%; left: 50%;
  width: 0; height: 0;
  pointer-events: none;
}
.frag {
  position: absolute;
  top: 0; left: 0;
  width: 14px; height: 18px;
  background: #d9a85a;
  border-radius: 40% 60% 50% 50%;
  opacity: 0;
  animation: frag-fly 0.6s var(--ease-out) forwards;
  box-shadow: 0 2px 4px rgba(120, 76, 40, 0.3);
}
@keyframes frag-fly {
  0% { opacity: 1; transform: translate(0, 0) rotate(0); }
  100% { opacity: 0; }
}

/* 记录 */
.records { padding: 6px 14px; }
.rec-empty {
  padding: 18px 0; text-align: center;
  color: var(--muted); font-size: 12px;
}
.rec-row {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px dashed var(--line);
  font-size: 12.5px;
}
.rec-row:last-child { border-bottom: none; }
.rec-left { display: flex; align-items: center; gap: 8px; }
.rec-dot {
  width: 6px; height: 6px; border-radius: 50%;
}
.rec-dot.win { background: var(--warning); }
.rec-dot.miss { background: var(--muted-2); }
.rec-name { color: var(--ink-2); }
.rec-time { color: var(--muted); font-family: var(--font-num); font-size: 11px; }
</style>
