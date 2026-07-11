<template>
  <van-popup v-model:show="visible" position="center" :close-on-click-overlay="false" round>
    <div class="result-box" :class="{ win: isWin, miss: !isWin }">
      <!-- 彩带（中奖时） -->
      <div v-if="isWin" class="confetti">
        <span v-for="i in 14" :key="i" class="cf" :style="confettiStyle(i)"></span>
      </div>
      <div class="result-icon">
        <van-icon v-if="isWin" name="gift-o" size="48" />
        <van-icon v-else name="info-o" size="48" />
      </div>
      <div class="result-title">{{ isWin ? '恭喜中奖' : '再接再厉' }}</div>
      <div class="result-prize" v-if="isWin">{{ prizeName }}</div>
      <div class="result-desc">{{ isWin ? '奖品已发放至账户，请留意到账通知' : '本次未中奖，下次还有机会' }}</div>
      <div class="result-actions">
        <button class="btn-continue" @click="onContinue">{{ continueText }}</button>
      </div>
    </div>
  </van-popup>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { showToast } from 'vant'

interface Props {
  // 是否显示
  show: boolean
  // 中奖结果对象（后端返回），含 prize/prizeType/prizeName 等
  result?: any
  // 剩余次数
  remaining?: number
  // 继续按钮文案
  continueText?: string
}
const props = withDefaults(defineProps<Props>(), {
  show: false,
  result: undefined,
  remaining: 0,
  continueText: '再来一次'
})
const emit = defineEmits<{ (e: 'update:show', v: boolean): void; (e: 'continue'): void }>()

const visible = computed({
  get: () => props.show,
  set: (v) => emit('update:show', v)
})

// 是否中奖：EMPTY 视为未中奖
const isWin = computed(() => {
  const r = props.result
  if (!r) return false
  const t = r.prizeType || r.type
  return t !== 'EMPTY' && !r.miss
})

const prizeName = computed(() => {
  const r = props.result
  return r?.prizeName || r?.name || r?.prize || ''
})

// 彩带样式
function confettiStyle(i: number) {
  const colors = ['#5a7a9c', '#b89692', '#b89a5a', '#94a89a', '#8b7ea3', '#b8845c']
  const left = (i * 7 + 5) % 100
  const delay = (i % 7) * 0.18
  const dur = 2.4 + (i % 5) * 0.4
  const color = colors[i % colors.length]
  return {
    left: left + '%',
    background: color,
    animationDelay: delay + 's',
    animationDuration: dur + 's'
  }
}

function onContinue() {
  if (props.remaining <= 0) {
    showToast('今日次数已用完，明日再来')
    return
  }
  emit('continue')
}
</script>

<style scoped>
.result-box {
  width: 280px;
  padding: 32px 24px 22px;
  text-align: center;
  position: relative;
  overflow: hidden;
}
.result-box.win {
  background: var(--surface);
  border: 1px solid var(--accent-clay);
}
.result-box.miss {
  background: var(--surface);
}

/* 彩带 */
.confetti {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}
.cf {
  position: absolute;
  top: -12px;
  width: 6px; height: 10px;
  border-radius: 1px;
  opacity: 0;
  animation: cf-fall linear infinite;
}
@keyframes cf-fall {
  0% { transform: translateY(-12px) rotate(0deg); opacity: 0; }
  10% { opacity: 0.9; }
  90% { opacity: 0.7; }
  100% { transform: translateY(320px) rotate(540deg); opacity: 0; }
}

.result-icon {
  color: var(--warning-deep);
  margin-bottom: 10px;
}
.result-box.miss .result-icon { color: var(--muted); }
.result-title {
  font-family: var(--font-serif);
  font-size: 19px; font-weight: 500;
  color: var(--ink);
  letter-spacing: 0.08em;
}
.result-prize {
  font-family: var(--font-serif);
  font-size: 15px; color: var(--warning-deep);
  margin-top: 8px; letter-spacing: 0.04em;
  font-weight: 500;
}
.result-desc {
  font-size: 12px; color: var(--muted);
  margin-top: 8px; line-height: 1.7;
  letter-spacing: 0.02em;
}
.result-actions { margin-top: 18px; }
.btn-continue {
  width: 100%;
  height: 40px;
  border: none;
  border-radius: 999px;
  background: var(--brand-deep);
  color: #fff;
  font-size: 13.5px;
  font-family: var(--font-serif);
  letter-spacing: 0.08em;
  cursor: pointer;
  transition: transform var(--dur) var(--ease-out);
}
.btn-continue:active { transform: scale(0.98); }
</style>
