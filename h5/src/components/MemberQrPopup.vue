<template>
  <van-popup v-model:show="visible" position="center" round :close-on-click-overlay="true">
    <div class="member-qr">
      <div class="mq-head">
        <div class="mq-avatar">{{ avatarText }}</div>
        <div class="mq-info">
          <div class="mq-name">
            {{ name }}
            <span v-if="levelName" class="mq-level">{{ levelName }}</span>
          </div>
          <div class="mq-phone">{{ maskPhone(phone) }}</div>
        </div>
      </div>
      <div class="mq-box">
        <canvas ref="qrCanvas" class="mq-canvas"></canvas>
      </div>
      <div class="mq-code val">会员号 {{ memberId }}</div>
      <div class="mq-tip">到店出示此码，核销会员身份与储值</div>
      <button class="mq-close" @click="visible = false">知道了</button>
    </div>
  </van-popup>
</template>

<script setup lang="ts">
import { computed, ref, watch, nextTick } from 'vue'
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

function maskPhone(p: string) {
  if (!p || p.length !== 11) return p || '—'
  return p.slice(0, 3) + '****' + p.slice(-4)
}

async function drawQr() {
  if (!qrCanvas.value || !props.memberId) return
  const payload = `星河会记会员 ${props.name} 会员号:${props.memberId} 手机:${props.phone}`
  try {
    await QRCode.toCanvas(qrCanvas.value, payload, {
      width: 220,
      margin: 1,
      color: { dark: '#2a3a4a', light: '#ffffff' }
    })
  } catch (e: any) {
    console.warn('会员码生成失败', e)
  }
}

watch(visible, (v) => {
  if (v) nextTick(drawQr)
})
</script>

<style scoped>
.member-qr {
  width: 300px;
  padding: 24px 24px 20px;
  text-align: center;
  background: var(--surface);
  border-radius: var(--r-lg);
}
.mq-head {
  display: flex; align-items: center; gap: 12px;
  text-align: left;
  margin-bottom: 18px;
}
.mq-avatar {
  width: 44px; height: 44px;
  border-radius: 50%;
  background: var(--brand);
  color: #fff;
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
  display: flex; align-items: center; gap: 6px;
}
.mq-level {
  font-size: 10px;
  background: var(--brand-soft);
  color: var(--brand-ink);
  padding: 2px 8px;
  border-radius: 999px;
  font-family: var(--font-ui);
  font-weight: 400;
}
.mq-phone {
  font-family: var(--font-num);
  font-size: 12px; color: var(--muted);
  margin-top: 4px; letter-spacing: 0.1em;
}
.mq-box {
  display: inline-block;
  padding: 10px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: var(--r);
  box-shadow: var(--shadow-sm);
}
.mq-canvas { display: block; width: 220px; height: 220px; }
.mq-code {
  font-family: var(--font-num);
  font-size: 11.5px; color: var(--muted);
  margin-top: 12px; letter-spacing: 0.08em;
}
.mq-tip {
  font-size: 12px; color: var(--ink-2);
  margin-top: 6px; letter-spacing: 0.04em;
}
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
  transition: transform var(--dur) var(--ease-out);
}
.mq-close:active { transform: scale(0.98); }
</style>
