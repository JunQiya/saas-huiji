<template>
  <div class="page game-wheel" :style="bgStyle">
    <NavBar :title="game?.name || '大转盘'" back />
    <div class="page-padding">
      <!-- 游戏标题区 -->
      <div class="game-head" v-if="game">
        <div class="gh-sub" v-if="game.subtitle">{{ game.subtitle }}</div>
        <div class="gh-meta">
          <span class="chip brand">大转盘</span>
          <span class="gh-remain">今日剩余 <em class="val">{{ remaining }}</em> 次</span>
        </div>
      </div>

      <!-- 规则 -->
      <div class="rules-box" v-if="game?.rules">
        <div class="rules-title">活动规则</div>
        <div class="rules-text">{{ game.rules }}</div>
      </div>

      <!-- 大转盘 -->
      <div class="wheel-wrap" v-if="prizes.length">
        <div class="wheel-pointer" @click="onStart">
          <div class="ptr-text">{{ playing ? '转动中' : '开始' }}</div>
        </div>
        <div class="wheel-outer" :style="wheelStyle">
          <div class="wheel-inner" :style="innerStyle">
            <!-- conic-gradient 扇形 -->
            <div class="wheel-bg" :style="conicStyle"></div>
            <!-- 扇形文字 -->
            <div
              v-for="(p, i) in prizes"
              :key="i"
              class="sector-label"
              :style="labelStyle(i)"
            >{{ prizeLabel(p) }}</div>
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

      <div class="footnote">愿好运 落在转动的每一格</div>
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

const playing = ref(false)
// 当前旋转角度（累计）
const rotateDeg = ref(0)
const resultVisible = ref(false)
const result = ref<any>(null)

// 剩余次数：由后端返回或本地递减
const remaining = ref(0)

// 背景图
const bgStyle = computed(() => {
  if (game.value?.bgImage) {
    return { backgroundImage: `url(${game.value.bgImage})` }
  }
  return {}
})

// 扇形颜色（低饱和调色板）
const sectorColors = [
  '#5a7a9c', '#b89692', '#b89a5a', '#94a89a',
  '#8b7ea3', '#b8845c', '#8a9aa3', '#a88366'
]

// conic-gradient 扇形背景
const conicStyle = computed(() => {
  const n = prizes.value.length
  if (!n) return {}
  const step = 360 / n
  const stops: string[] = []
  for (let i = 0; i < n; i++) {
    const color = sectorColors[i % sectorColors.length]
    stops.push(`${color} ${i * step}deg ${(i + 1) * step}deg`)
  }
  return { background: `conic-gradient(${stops.join(',')})` }
})

// 转盘旋转样式：缓动停止
const innerStyle = computed(() => ({
  transform: `rotate(${rotateDeg.value}deg)`,
  transition: 'transform 4s cubic-bezier(0.17, 0.67, 0.12, 0.99)'
}))

// 外层固定
const wheelStyle = computed(() => ({}))

// 扇形文字位置：用三角函数计算每个扇形中心的 x/y 坐标
function labelStyle(index: number) {
  const n = prizes.value.length
  if (!n) return {}
  const step = 360 / n
  // 扇形中心角度（0deg 在顶部 12 点方向）
  const angleDeg = index * step + step / 2
  const angleRad = angleDeg * Math.PI / 180
  // 标签到圆心的距离（转盘半径 140，文字放在 0.62 处）
  const radius = 86
  const x = Math.sin(angleRad) * radius
  const y = -Math.cos(angleRad) * radius
  return {
    left: `calc(50% + ${x}px)`,
    top: `calc(50% + ${y}px)`,
    transform: 'translate(-50%, -50%)'
  }
}

function prizeLabel(p: any) {
  if (p.type === 'EMPTY') return '谢谢参与'
  if (p.type === 'POINTS') return `${p.refId || p.amount || 0} 积分`
  return p.name || '奖品'
}

// 加载游戏详情
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

// 加载我的记录
async function loadRecords() {
  try { records.value = (await gameApi.myPlays(gameId)) || [] } catch {/* */}
}

// 开始抽奖
async function onStart() {
  if (playing.value) return
  if (remaining.value <= 0) {
    showToast('今日次数已用完，明日再来')
    return
  }
  playing.value = true
  try {
    // 调用 play 接口获取结果
    const res = await gameApi.play(gameId)
    // 根据中奖奖品索引计算停止角度
    const targetIndex = findPrizeIndex(res)
    const n = prizes.value.length || 1
    const step = 360 / n
    // 扇形中心角度（指针在顶部正上方，即 0deg/360deg 位置）
    // 需让 targetIndex 扇形的中心对准 0deg
    const targetCenter = targetIndex * step + step / 2
    // 转盘需要旋转的角度 = 至少 5 圈 + 校正到目标位置
    // 因为转盘顺时针旋转，指针在顶部，需让目标扇形转到顶部
    const baseRotations = 360 * 5
    // 当前角度对 360 取余
    const currentMod = rotateDeg.value % 360
    // 需要再旋转的角度，使最终角度 % 360 === (360 - targetCenter)
    const desiredMod = (360 - targetCenter + 360) % 360
    let delta = desiredMod - currentMod
    if (delta <= 0) delta += 360
    rotateDeg.value += baseRotations + delta

    // 旋转动画结束后展示结果
    setTimeout(() => {
      playing.value = false
      result.value = res
      resultVisible.value = true
      remaining.value = Math.max(0, remaining.value - 1)
      loadRecords()
    }, 4200)
  } catch (e) {
    playing.value = false
  }
}

// 根据后端返回结果匹配奖品索引
function findPrizeIndex(res: any): number {
  if (!res) return 0
  const prizeId = res.prizeId || res.id
  const idx = prizes.value.findIndex(p => p.id === prizeId)
  if (idx >= 0) return idx
  // 按名称匹配
  const name = res.prizeName || res.name
  if (name) {
    const idxByName = prizes.value.findIndex(p => p.name === name)
    if (idxByName >= 0) return idxByName
  }
  // 按类型匹配
  const type = res.prizeType || res.type
  if (type) {
    const idxByType = prizes.value.findIndex(p => p.type === type)
    if (idxByType >= 0) return idxByType
  }
  return 0
}

// 继续抽奖
function onContinue() {
  resultVisible.value = false
  result.value = null
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
.game-wheel {
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

/* 大转盘 */
.wheel-wrap {
  position: relative;
  width: 280px; height: 280px;
  margin: 16px auto 24px;
}
.wheel-outer {
  width: 100%; height: 100%;
  border-radius: 50%;
  background: #fff;
  border: 4px solid var(--surface-3);
  box-shadow: 0 6px 24px rgba(31, 29, 24, 0.10);
  overflow: hidden;
  position: relative;
}
.wheel-inner {
  position: absolute;
  inset: 0;
  border-radius: 50%;
}
.wheel-bg {
  position: absolute;
  inset: 0;
  border-radius: 50%;
}
.sector-label {
  position: absolute;
  font-family: var(--font-serif);
  font-size: 11px;
  color: #fff;
  font-weight: 500;
  letter-spacing: 0.04em;
  white-space: nowrap;
  text-shadow: 0 1px 2px rgba(0,0,0,0.18);
  pointer-events: none;
}

/* 指针（中心按钮） */
.wheel-pointer {
  position: absolute;
  top: 50%; left: 50%;
  transform: translate(-50%, -50%);
  width: 64px; height: 64px;
  border-radius: 50%;
  background: var(--brand-deep);
  color: #fff;
  display: flex; align-items: center; justify-content: center;
  z-index: 5;
  box-shadow: 0 4px 14px rgba(61, 89, 122, 0.35);
  cursor: pointer;
  transition: transform var(--dur) var(--ease-out);
  border: 3px solid #fff;
}
.wheel-pointer:active { transform: translate(-50%, -50%) scale(0.94); }
.ptr-text {
  font-family: var(--font-serif);
  font-size: 13px; font-weight: 500;
  letter-spacing: 0.08em;
}
/* 指针三角 */
.wheel-pointer::before {
  content: '';
  position: absolute;
  top: -14px; left: 50%;
  transform: translateX(-50%);
  width: 0; height: 0;
  border-left: 10px solid transparent;
  border-right: 10px solid transparent;
  border-bottom: 16px solid var(--brand-deep);
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
