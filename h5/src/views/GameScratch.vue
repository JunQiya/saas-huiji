<template>
  <div class="page game-scratch" :style="bgStyle">
    <NavBar :title="game?.name || '刮刮乐'" back />
    <div class="page-padding">
      <!-- 游戏标题区 -->
      <div class="game-head" v-if="game">
        <div class="gh-sub" v-if="game.subtitle">{{ game.subtitle }}</div>
        <div class="gh-meta">
          <span class="chip clay">刮刮乐</span>
          <span class="gh-remain">今日剩余 <em class="val">{{ remaining }}</em> 次</span>
        </div>
      </div>

      <!-- 规则 -->
      <div class="rules-box" v-if="game?.rules">
        <div class="rules-title">活动规则</div>
        <div class="rules-text">{{ game.rules }}</div>
      </div>

      <!-- 刮奖区 -->
      <div class="scratch-wrap" v-if="prizes.length">
        <div class="scratch-card">
          <!-- 底层：中奖结果 -->
          <div class="scratch-prize" :class="{ revealed }">
            <div class="prize-icon">
              <van-icon :name="isWin ? 'gift-o' : 'info-o'" size="42" />
            </div>
            <div class="prize-text">{{ revealed ? prizeText : '刮开查看奖品' }}</div>
          </div>
          <!-- 顶层：Canvas 刮奖层 -->
          <canvas
            ref="canvasRef"
            class="scratch-canvas"
            @touchstart.prevent="onTouchStart"
            @touchmove.prevent="onTouchMove"
            @touchend="onTouchEnd"
            @mousedown="onMouseDown"
            @mousemove="onMouseMove"
            @mouseup="onMouseUp"
            @mouseleave="onMouseUp"
          ></canvas>
        </div>
        <div class="scratch-tip">用手指刮开灰色区域</div>
        <button class="btn-reset" v-if="revealed" @click="onReset">再刮一次</button>
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

      <div class="footnote">轻轻一刮 好运自来</div>
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
import { computed, nextTick, onActivated, onMounted, ref } from 'vue'
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
const revealed = ref(false)

// Canvas
const canvasRef = ref<HTMLCanvasElement | null>(null)
let ctx: CanvasRenderingContext2D | null = null
let isDrawing = false
let played = false // 本次是否已调用 play 接口

const bgStyle = computed(() => {
  if (game.value?.bgImage) {
    return { backgroundImage: `url(${game.value.bgImage})` }
  }
  return {}
})

const isWin = computed(() => {
  const r = result.value
  if (!r) return false
  const t = r.prizeType || r.type
  return t !== 'EMPTY' && !r.miss
})

const prizeText = computed(() => {
  const r = result.value
  if (!r) return '未中奖'
  if (isWin.value) return r.prizeName || r.name || '神秘奖品'
  return '谢谢参与'
})

// 初始化 Canvas 刮奖层
function initCanvas() {
  const canvas = canvasRef.value
  if (!canvas) return
  // 适配高分屏
  const rect = canvas.getBoundingClientRect()
  const dpr = window.devicePixelRatio || 1
  canvas.width = rect.width * dpr
  canvas.height = rect.height * dpr
  ctx = canvas.getContext('2d')
  if (!ctx) return
  ctx.scale(dpr, dpr)
  // 绘制灰色覆盖层
  ctx.fillStyle = '#b3ad9f'
  ctx.fillRect(0, 0, rect.width, rect.height)
  // 提示文字
  ctx.fillStyle = '#6a655c'
  ctx.font = '14px "PingFang SC", sans-serif'
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  ctx.fillText('刮开此处', rect.width / 2, rect.height / 2)
  revealed.value = false
  played = false
}

// 获取坐标
function getPos(e: TouchEvent | MouseEvent) {
  const canvas = canvasRef.value
  if (!canvas) return { x: 0, y: 0 }
  const rect = canvas.getBoundingClientRect()
  let clientX = 0, clientY = 0
  if ('touches' in e) {
    const t = e.touches[0] || e.changedTouches[0]
    clientX = t.clientX
    clientY = t.clientY
  } else {
    clientX = (e as MouseEvent).clientX
    clientY = (e as MouseEvent).clientY
  }
  return { x: clientX - rect.left, y: clientY - rect.top }
}

// 刮开（圆形擦除）
function scratch(x: number, y: number) {
  if (!ctx) return
  ctx.globalCompositeOperation = 'destination-out'
  ctx.beginPath()
  ctx.arc(x, y, 18, 0, Math.PI * 2)
  ctx.fill()
}

// 触摸事件
function onTouchStart(e: TouchEvent) {
  isDrawing = true
  const { x, y } = getPos(e)
  scratch(x, y)
  // 首次刮开时调用 play 接口
  if (!played) {
    played = true
    playOnce()
  }
}
function onTouchMove(e: TouchEvent) {
  if (!isDrawing) return
  const { x, y } = getPos(e)
  scratch(x, y)
}
function onTouchEnd() {
  if (!isDrawing) return
  isDrawing = false
  checkReveal()
}

// 鼠标事件（PC 调试）
function onMouseDown(e: MouseEvent) {
  isDrawing = true
  const { x, y } = getPos(e)
  scratch(x, y)
  if (!played) {
    played = true
    playOnce()
  }
}
function onMouseMove(e: MouseEvent) {
  if (!isDrawing) return
  const { x, y } = getPos(e)
  scratch(x, y)
}
function onMouseUp() {
  if (!isDrawing) return
  isDrawing = false
  checkReveal()
}

// 检查刮开面积比例
function checkReveal() {
  const canvas = canvasRef.value
  if (!canvas || !ctx) return
  const w = canvas.width, h = canvas.height
  const data = ctx.getImageData(0, 0, w, h).data
  let cleared = 0
  const step = 4 * 16 // 采样降低开销
  let total = 0
  for (let i = 3; i < data.length; i += step) {
    total++
    if (data[i] === 0) cleared++
  }
  const ratio = cleared / total
  // 刮开超过 50% 自动揭示
  if (ratio > 0.5 && !revealed.value) {
    reveal()
  }
}

// 揭示结果
function reveal() {
  revealed.value = true
  // 清空 canvas 剩余部分
  const canvas = canvasRef.value
  if (canvas && ctx) {
    ctx.globalCompositeOperation = 'destination-out'
    ctx.fillRect(0, 0, canvas.width, canvas.height)
  }
  // 延迟展示弹窗
  setTimeout(() => {
    if (result.value) {
      resultVisible.value = true
      remaining.value = Math.max(0, remaining.value - 1)
      loadRecords()
    }
  }, 600)
}

// 调用 play 接口
async function playOnce() {
  if (remaining.value <= 0) {
    showToast('今日次数已用完，明日再来')
    return
  }
  try {
    const res = await gameApi.play(gameId)
    result.value = res
  } catch {
    result.value = { prizeType: 'EMPTY', prizeName: '未中奖' }
  }
}

// 重置：再刮一次
function onReset() {
  if (remaining.value <= 0) {
    showToast('今日次数已用完，明日再来')
    return
  }
  result.value = null
  nextTick(() => initCanvas())
}

function onContinue() {
  resultVisible.value = false
  result.value = null
  nextTick(() => initCanvas())
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

onMounted(async () => {
  await loadDetail()
  loadRecords()
  await nextTick()
  initCanvas()
})
// 从其他页面返回时重新拉取剩余次数
onActivated(() => {
  loadDetail()
})
</script>

<style scoped>
.game-scratch {
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

/* 刮奖区 */
.scratch-wrap {
  display: flex; flex-direction: column;
  align-items: center;
  margin: 18px 0 24px;
}
.scratch-card {
  position: relative;
  width: 280px; height: 160px;
  border-radius: var(--r-lg);
  overflow: hidden;
  box-shadow: var(--shadow-md);
  background: var(--surface);
  border: 1px solid var(--line-2);
}
.scratch-prize {
  position: absolute;
  inset: 0;
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  gap: 10px;
  background: linear-gradient(135deg, #faf3e8, #f0e6d6);
}
.scratch-prize.revealed {
  background: linear-gradient(135deg, rgba(184, 154, 90, 0.10), rgba(184, 132, 92, 0.08));
}
.prize-icon {
  color: var(--warning-deep);
}
.prize-text {
  font-family: var(--font-serif);
  font-size: 17px; font-weight: 500;
  color: var(--ink);
  letter-spacing: 0.06em;
}
.scratch-canvas {
  position: absolute;
  inset: 0;
  width: 100%; height: 100%;
  touch-action: none;
  cursor: pointer;
}
.scratch-tip {
  font-size: 12px; color: var(--muted);
  margin-top: 12px; letter-spacing: 0.04em;
}
.btn-reset {
  margin-top: 14px;
  padding: 8px 24px;
  border: 1px solid var(--brand);
  border-radius: 999px;
  background: transparent;
  color: var(--brand-deep);
  font-size: 13px;
  font-family: var(--font-serif);
  letter-spacing: 0.08em;
  cursor: pointer;
  transition: transform var(--dur) var(--ease-out);
}
.btn-reset:active { transform: scale(0.97); }

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
