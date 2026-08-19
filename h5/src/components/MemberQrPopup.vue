<template>
  <van-popup v-model:show="visible" position="center" round :close-on-click-overlay="true">
    <div class="member-qr">
      <!-- 顶部品牌 -->
      <div class="mq-brand">
        <div class="mq-brand-mark" aria-hidden="true">星</div>
        <div class="mq-brand-text">
          <div class="mq-brand-name">星河·会记</div>
          <div class="mq-brand-sub">MEMBER CARD</div>
        </div>
        <span v-if="levelName" class="mq-level chip">{{ levelName }}</span>
      </div>

      <!-- 用户信息 -->
      <div class="mq-user">
        <div class="mq-avatar">{{ avatarText }}</div>
        <div class="mq-info">
          <div class="mq-name">{{ name }}</div>
          <div class="mq-phone">{{ maskPhone(phone) }}</div>
        </div>
      </div>

      <!-- 二维码卡片 -->
      <div class="mq-box">
        <div class="mq-body">
          <div class="mq-corner tl"></div>
          <div class="mq-corner tr"></div>
          <div class="mq-corner bl"></div>
          <div class="mq-corner br"></div>
          <canvas ref="qrCanvas" class="mq-canvas"></canvas>
          <div class="mq-center" aria-hidden="true">星</div>
        </div>
        <div class="mq-expire" :class="{ warn: qrRemain <= 30 }">
          <van-icon name="clock-o" size="11" />
          <span>二维码有效期 {{ remainText }}，自动刷新</span>
          <van-icon name="replay" class="mq-refresh" size="14" @click="onManualRefresh" />
        </div>
      </div>

      <!-- 会员号 -->
      <div class="mq-no val">{{ formatNo(memberId) }}</div>
      <div class="mq-tip">到店出示此码，核销会员身份与储值</div>

      <button class="mq-close" @click="visible = false">知道了</button>
    </div>
  </van-popup>
</template>

<script setup lang="ts">
import { computed, ref, watch, nextTick, onBeforeUnmount } from 'vue'
import { showToast } from 'vant'
import QRCode from 'qrcode'

interface Props {
  show: boolean
  memberId?: number | string
  name?: string
  phone?: string
  levelName?: string
}
const props = withDefaults(defineProps<Props>(), {
  show: false,
  memberId: '',
  name: '会员',
  phone: '',
  levelName: ''
})
const emit = defineEmits<{ (e: 'update:show', v: boolean): void }>()

const visible = computed({
  get: () => props.show,
  set: (v) => emit('update:show', v)
})

const qrCanvas = ref<HTMLCanvasElement>()
const avatarText = computed(() => props.name?.charAt(0) || '星')

// 二维码有效期(秒): 到期自动重绘, 防截图盗用
const QR_TTL = 120
const qrRemain = ref(QR_TTL)
let qrTimer: number | null = null
let tickTimer: number | null = null

const remainText = computed(() => {
  const m = Math.floor(qrRemain.value / 60)
  const s = qrRemain.value % 60
  return m > 0 ? `${m}分${s}秒` : `${s}秒`
})

function maskPhone(p: string) {
  if (!p || p.length !== 11) return p || '—'
  return p.slice(0, 3) + '****' + p.slice(-4)
}

// 会员号分组展示: 12345678 -> 1234 5678
function formatNo(id: any) {
  const s = String(id ?? '')
  if (!s) return ''
  return s.replace(/\B(?=(\d{4})+(?!\d))/g, ' ')
}

async function drawQr() {
  if (!qrCanvas.value || !props.memberId) return
  const ts = Date.now()
  // 二维码仅承载会员号 + 时间戳, 不嵌入明文手机号等敏感信息
  const payload = `星河会记会员 ${props.name} 会员号:${props.memberId} 时效:${ts}`
  try {
    await QRCode.toCanvas(qrCanvas.value, payload, {
      width: 240,
      margin: 2,
      color: { dark: '#2a3a4a', light: '#ffffff' }
    })
  } catch (e: any) {
    console.warn('会员码生成失败', e)
  }
}

async function refreshQr() {
  qrRemain.value = QR_TTL
  await drawQr()
}

function onManualRefresh() {
  if (qrRemain.value >= QR_TTL - 3) return
  showToast({ message: '已刷新会员码', position: 'top' })
  refreshQr()
}

function startQrCycle() {
  stopQrCycle()
  qrRemain.value = QR_TTL
  nextTick(() => {
    drawQr()
    tickTimer = window.setInterval(() => {
      qrRemain.value = Math.max(0, qrRemain.value - 1)
      if (qrRemain.value <= 0) refreshQr()
    }, 1000)
    qrTimer = window.setInterval(refreshQr, QR_TTL * 1000)
  })
}

function stopQrCycle() {
  if (tickTimer) { clearInterval(tickTimer); tickTimer = null }
  if (qrTimer) { clearInterval(qrTimer); qrTimer = null }
}

watch(visible, (v) => {
  if (v) startQrCycle()
  else stopQrCycle()
})

onBeforeUnmount(stopQrCycle)
</script>

<style scoped>
.member-qr {
  width: 304px;
  padding: 18px 20px 18px;
  text-align: center;
  background: var(--surface);
  border-radius: var(--r-lg);
}

/* 顶部品牌 */
.mq-brand {
  display: flex; align-items: center; gap: 8px;
  text-align: left;
  padding-bottom: 14px;
  border-bottom: 1px dashed var(--line);
}
.mq-brand-mark {
  width: 28px; height: 28px;
  border-radius: 8px;
  background: var(--brand);
  color: #fff;
  font-family: var(--font-serif);
  font-size: 14px;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.mq-brand-text { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 1px; }
.mq-brand-name {
  font-family: var(--font-serif);
  font-size: 14px; font-weight: 500; color: var(--ink);
  letter-spacing: 0.08em;
}
.mq-brand-sub {
  font-family: var(--font-num);
  font-size: 9px; color: var(--muted);
  letter-spacing: 0.22em;
}
.mq-level.chip { margin-left: auto; }

/* 用户信息 */
.mq-user {
  display: flex; align-items: center; gap: 12px;
  text-align: left;
  padding: 14px 2px;
}
.mq-avatar {
  width: 44px; height: 44px;
  border-radius: 50%;
  background: var(--brand-soft);
  color: var(--brand-deep);
  border: 1px solid var(--brand-soft);
  display: flex; align-items: center; justify-content: center;
  font-family: var(--font-serif);
  font-size: 19px;
  flex-shrink: 0;
}
.mq-info { flex: 1; min-width: 0; }
.mq-name {
  font-family: var(--font-serif);
  font-size: 15px; font-weight: 500; color: var(--ink);
  letter-spacing: 0.04em;
}
.mq-phone {
  font-family: var(--font-num);
  font-size: 12px; color: var(--muted);
  margin-top: 4px; letter-spacing: 0.1em;
}

/* 二维码卡片 */
.mq-box {
  position: relative;
  width: 100%;
  padding: 14px;
  background: #fff;
  border: 1px solid var(--brand-soft);
  border-radius: var(--r-md);
  box-shadow: var(--shadow-sm);
}
.mq-body {
  position: relative;
  width: 100%;
  aspect-ratio: 1 / 1;
}
.mq-canvas {
  display: block;
  width: 100%;
  height: 100%;
  border-radius: var(--r-sm);
}
.mq-center {
  position: absolute; top: 50%; left: 50%;
  transform: translate(-50%, -50%);
  width: 26px; height: 26px;
  background: #fff;
  border-radius: 6px;
  display: flex; align-items: center; justify-content: center;
  color: var(--brand-deep);
  font-family: var(--font-serif);
  font-size: 14px;
  box-shadow: 0 0 0 3px #fff, 0 1px 6px rgba(20,30,50,0.14);
}
/* 四角装饰 */
.mq-corner { position: absolute; width: 16px; height: 16px; border-color: var(--brand); border-style: solid; opacity: 0.7; }
.mq-corner.tl { top: 4px; left: 4px; border-width: 2px 0 0 2px; border-top-left-radius: 6px; }
.mq-corner.tr { top: 4px; right: 4px; border-width: 2px 2px 0 0; border-top-right-radius: 6px; }
.mq-corner.bl { bottom: 4px; left: 4px; border-width: 0 0 2px 2px; border-bottom-left-radius: 6px; }
.mq-corner.br { bottom: 4px; right: 4px; border-width: 0 2px 2px 0; border-bottom-right-radius: 6px; }

/* 有效期 */
.mq-expire {
  display: flex; align-items: center; justify-content: center; gap: 5px;
  margin-top: 10px;
  font-family: var(--font-serif);
  font-size: 10.5px; color: var(--muted);
  letter-spacing: 0.04em;
}
.mq-expire.warn { color: var(--warning-deep); }
.mq-refresh { color: var(--brand); cursor: pointer; padding: 2px; }

/* 会员号 */
.mq-no {
  font-family: var(--font-num);
  font-size: 16px; font-weight: 600; color: var(--brand-deep);
  margin-top: 14px; letter-spacing: 0.14em;
}
.mq-tip {
  font-size: 12px; color: var(--ink-2);
  margin-top: 6px; letter-spacing: 0.04em;
}

/* 关闭 */
.mq-close {
  margin-top: 16px;
  width: 100%;
  height: 40px;
  border: none;
  border-radius: 999px;
  background: var(--brand-deep);
  color: #fff;
  font-size: 13.5px;
  font-family: var(--font-serif);
  letter-spacing: 0.16em;
  cursor: pointer;
  transition: transform var(--dur) var(--ease-out), opacity var(--dur) var(--ease-out);
}
.mq-close:active { transform: scale(0.98); opacity: 0.85; }
</style>
