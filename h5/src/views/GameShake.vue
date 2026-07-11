<template>
  <div class="page game-shake" :style="bgStyle">
    <NavBar :title="game?.name || '摇一摇'" back />
    <div class="page-padding">
      <!-- 游戏标题区 -->
      <div class="game-head" v-if="game">
        <div class="gh-sub" v-if="game.subtitle">{{ game.subtitle }}</div>
        <div class="gh-meta">
          <span class="chip twilight">摇一摇</span>
          <span class="gh-remain">今日剩余 <em class="val">{{ remaining }}</em> 次</span>
        </div>
      </div>

      <!-- 规则 -->
      <div class="rules-box" v-if="game?.rules">
        <div class="rules-title">活动规则</div>
        <div class="rules-text">{{ game.rules }}</div>
      </div>

      <!-- 摇一摇区 -->
      <div class="shake-area" v-if="prizes.length">
        <div class="shake-icon" :class="{ shaking }">
          <van-icon name="shake-o" size="72" />
        </div>
        <div class="shake-tip">{{ tipText }}</div>
        <!-- 降级按钮（非移动端或权限拒绝时） -->
        <button v-if="!motionEnabled" class="btn-shake" :disabled="playing" @click="onShake">
          {{ playing ? '摇动中…' : '点击抽奖' }}
        </button>
        <button v-else class="btn-shake" :disabled="playing" @click="onShake">
          {{ playing ? '摇动中…' : '也可以点击抽奖' }}
        </button>
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

      <div class="footnote">摇一摇 让好运 恰好降临</div>
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
import { computed, onMounted, onUnmounted, ref } from 'vue'
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
const shaking = ref(false)

// 是否已启用 DeviceMotion 监听
const motionEnabled = ref(false)
// 上次摇动时间（节流）
let lastShakeAt = 0
// 摇动阈值
const SHAKE_THRESHOLD = 14

const bgStyle = computed(() => {
  if (game.value?.bgImage) {
    return { backgroundImage: `url(${game.value.bgImage})` }
  }
  return {}
})

const tipText = computed(() => {
  if (playing.value) return '正在抽取你的奖品…'
  if (motionEnabled.value) return '摇动手机开始抽奖'
  return '点击下方按钮开始抽奖'
})

// 摇动检测回调
function onDeviceMotion(e: DeviceMotionEvent) {
  if (playing.value) return
  const acc = e.accelerationIncludingGravity
  if (!acc) return
  const x = acc.x || 0
  const y = acc.y || 0
  const z = acc.z || 0
  const delta = Math.sqrt(x * x + y * y + z * z)
  const now = Date.now()
  if (delta > SHAKE_THRESHOLD && now - lastShakeAt > 1500) {
    lastShakeAt = now
    onShake()
  }
}

// 启用摇动监听（iOS 13+ 需用户手势触发权限请求）
async function enableMotion() {
  // 非微信浏览器 / iOS 13+ 需要 requestPermission
  const DME = window.DeviceMotionEvent as any
  if (typeof DME === 'undefined') {
    motionEnabled.value = false
    return
  }
  if (typeof DME.requestPermission === 'function') {
    try {
      const res = await DME.requestPermission()
      if (res === 'granted') {
        window.addEventListener('devicemotion', onDeviceMotion)
        motionEnabled.value = true
      } else {
        motionEnabled.value = false
      }
    } catch {
      motionEnabled.value = false
    }
  } else {
    // Android 等无需权限
    window.addEventListener('devicemotion', onDeviceMotion)
    motionEnabled.value = true
  }
}

// 摇一摇 / 点击抽奖
async function onShake() {
  if (playing.value) return
  if (remaining.value <= 0) {
    showToast('今日次数已用完，明日再来')
    return
  }
  playing.value = true
  shaking.value = true
  // 震动反馈
  if (navigator.vibrate) navigator.vibrate([40, 30, 60])
  try {
    const res = await gameApi.play(gameId)
    result.value = res
    // 摇动动画持续 1s 后展示结果
    setTimeout(() => {
      shaking.value = false
      playing.value = false
      resultVisible.value = true
      remaining.value = Math.max(0, remaining.value - 1)
      loadRecords()
    }, 1000)
  } catch {
    shaking.value = false
    playing.value = false
  }
}

function onContinue() {
  resultVisible.value = false
  result.value = null
}

async function loadDetail() {
  loading.value = true
  try {
    const d = await gameApi.detail(gameId)
    game.value = d
    prizes.value = d?.prizes || []
    remaining.value = d?.remaining ?? d?.dailyLimit ?? 0
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

onMounted(async () => {
  loadDetail()
  loadRecords()
  // 尝试启用摇动监听（非微信环境会降级为点击）
  await enableMotion()
})

onUnmounted(() => {
  window.removeEventListener('devicemotion', onDeviceMotion)
})
</script>

<style scoped>
.game-shake {
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

/* 摇一摇区 */
.shake-area {
  margin: 40px 0 24px;
  text-align: center;
  display: flex; flex-direction: column;
  align-items: center; gap: 18px;
}
.shake-icon {
  width: 120px; height: 120px;
  border-radius: 50%;
  background: var(--accent-twilight-soft);
  color: #5e5278;
  display: flex; align-items: center; justify-content: center;
  transition: transform var(--dur) var(--ease-out);
}
.shake-icon.shaking {
  animation: shake-anim 0.5s var(--ease) infinite;
}
@keyframes shake-anim {
  0%, 100% { transform: rotate(0) translateX(0); }
  20% { transform: rotate(-12deg) translateX(-6px); }
  40% { transform: rotate(10deg) translateX(6px); }
  60% { transform: rotate(-8deg) translateX(-4px); }
  80% { transform: rotate(6deg) translateX(4px); }
}
.shake-tip {
  font-family: var(--font-serif);
  font-size: 14px; color: var(--ink-2);
  letter-spacing: 0.08em;
}
.btn-shake {
  padding: 10px 36px;
  border: none;
  border-radius: 999px;
  background: var(--brand-deep);
  color: #fff;
  font-size: 14px;
  font-family: var(--font-serif);
  letter-spacing: 0.1em;
  cursor: pointer;
  transition: transform var(--dur) var(--ease-out), opacity var(--dur) var(--ease-out);
  box-shadow: 0 4px 14px rgba(61, 89, 122, 0.3);
}
.btn-shake:active { transform: scale(0.97); }
.btn-shake:disabled { opacity: 0.55; cursor: not-allowed; }

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
