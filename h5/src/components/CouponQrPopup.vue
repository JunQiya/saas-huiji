<template>
  <van-popup v-model:show="visible" position="center" round :close-on-click-overlay="true">
    <div class="coupon-qr">
      <div class="cq-head">
        <span class="cq-title">{{ couponName }}</span>
        <span v-if="status" class="chip" :class="chipClass">{{ statusText }}</span>
      </div>
      <div class="cq-box">
        <canvas ref="qrCanvas" class="cq-canvas"></canvas>
      </div>
      <div class="cq-code val">{{ code }}</div>
      <div class="cq-tip">到店出示此券码，由收银员扫码核销</div>
      <div v-if="expireAt" class="cq-expire">有效期至 {{ fmtDate(expireAt) }}</div>
      <button class="cq-close" @click="visible = false">知道了</button>
    </div>
  </van-popup>
</template>

<script setup lang="ts">
import { computed, ref, watch, nextTick } from 'vue'
import QRCode from 'qrcode'
import { formatDate } from '@/utils/format'

interface Props {
  show: boolean
  couponName?: string
  code?: string
  status?: string
  expireAt?: string
}
const props = withDefaults(defineProps<Props>(), {
  show: false,
  couponName: '优惠券',
  code: '',
  status: '',
  expireAt: ''
})
const emit = defineEmits<{ (e: 'update:show', v: boolean): void }>()

const visible = computed({
  get: () => props.show,
  set: (v) => emit('update:show', v)
})

const qrCanvas = ref<HTMLCanvasElement>()

const statusText = computed(() => ({ UNUSED: '待使用', USED: '已使用', EXPIRED: '已过期' } as any)[props.status] || props.status || '')
const chipClass = computed(() => ({ UNUSED: 'success', USED: 'info', EXPIRED: 'danger' } as any)[props.status] || '')

async function drawQr() {
  if (!qrCanvas.value || !props.code) return
  try {
    await QRCode.toCanvas(qrCanvas.value, props.code, {
      width: 220,
      margin: 1,
      color: { dark: '#2a3a4a', light: '#ffffff' }
    })
  } catch (e: any) {
    console.warn('券码二维码生成失败', e)
  }
}

function fmtDate(iso: string) {
  return formatDate(iso)
}

watch(visible, (v) => {
  if (v) nextTick(drawQr)
})
</script>

<style scoped>
.coupon-qr {
  width: 300px;
  padding: 24px 24px 20px;
  text-align: center;
  background: var(--surface);
  border-radius: var(--r-lg);
}
.cq-head {
  display: flex; align-items: center; justify-content: center; gap: 8px;
  margin-bottom: 16px;
}
.cq-title {
  font-family: var(--font-serif);
  font-size: 16px; font-weight: 500; color: var(--ink);
  letter-spacing: 0.04em;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
  max-width: 190px;
}
.cq-box {
  display: inline-block;
  padding: 10px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: var(--r);
  box-shadow: var(--shadow-sm);
}
.cq-canvas { display: block; width: 220px; height: 220px; }
.cq-code {
  font-family: var(--font-num);
  font-size: 15px; font-weight: 600; color: var(--ink);
  margin-top: 14px;
  letter-spacing: 0.16em;
  font-variant-numeric: tabular-nums;
}
.cq-tip {
  font-size: 12px; color: var(--ink-2);
  margin-top: 6px; letter-spacing: 0.04em;
}
.cq-expire {
  font-size: 11.5px; color: var(--muted);
  margin-top: 4px; letter-spacing: 0.04em;
}
.cq-close {
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
  transition: transform var(--dur) var(--ease-out);
}
.cq-close:active { transform: scale(0.98); }
</style>
