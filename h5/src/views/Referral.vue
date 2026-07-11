<template>
  <div class="page referral">
    <NavBar title="邀请有礼" back />
    <div v-if="loading" class="loading"><van-loading color="#6f94b8" /></div>

    <div v-else class="ref-page">
      <!-- 推荐码卡 -->
      <div class="code-card">
        <div class="cc-content">
          <div class="cc-label">我的专属邀请码</div>
          <div class="cc-code">{{ info.code || '—' }}</div>
          <button class="cc-copy" @click="onCopy">复制邀请码</button>
          <div class="cc-tip">分享给朋友，注册后双向各得 30 元券</div>
        </div>
      </div>

      <!-- 二维码 -->
      <div class="ui-card qr-card">
        <div class="qr-title">扫码邀请</div>
        <div class="qr-box">
          <canvas ref="qrCanvas" class="qr-canvas"></canvas>
        </div>
        <div class="qr-tip">长按识别 / 截图保存</div>
      </div>

      <!-- 统计 -->
      <div class="ui-card stat-card">
        <div class="stat-row">
          <div class="stat-item">
            <div class="stat-num val">{{ stats.total || 0 }}</div>
            <div class="stat-lbl">已邀请</div>
          </div>
          <div class="stat-item">
            <div class="stat-num val">{{ stats.active || 0 }}</div>
            <div class="stat-lbl">已活跃</div>
          </div>
          <div class="stat-item">
            <div class="stat-num val">{{ stats.rewarded || 0 }}</div>
            <div class="stat-lbl">已奖励</div>
          </div>
          <div class="stat-item">
            <div class="stat-num val">¥{{ formatMoney(stats.totalReward) }}</div>
            <div class="stat-lbl">累计收益</div>
          </div>
        </div>
      </div>

      <!-- 绑定我的推荐人 -->
      <div class="section-title">
        <span>绑定我的推荐人</span>
        <span class="st-tip">领新人礼</span>
      </div>
      <div class="ui-card bind-card">
        <div class="bind-title">填写邀请码，领取新人 30 元券</div>
        <input
          v-model="bindCode"
          type="text"
          maxlength="6"
          placeholder="请输入 6 位邀请码"
          class="bind-input"
        />
        <button class="bind-btn" :disabled="!bindCode" @click="onBind">立即绑定</button>
        <div class="bind-tip">每位用户仅可绑定一次，不可更改</div>
      </div>

      <!-- 我的邀请 -->
      <div class="section-title">
        <span>我的邀请</span>
        <span class="st-tip">{{ list.length }} 人</span>
      </div>
      <div class="ui-card list-card">
        <div v-if="!list.length" class="empty">
          <van-icon name="user-o" size="32" color="#b4b7b1" />
          <div class="empty-text">还没有邀请记录，快去分享吧</div>
        </div>
        <div v-else class="list">
          <div v-for="r in list" :key="r.id" class="list-item">
            <div class="li-avatar">{{ avatarChar(r.refereeName) }}</div>
            <div class="li-main">
              <div class="li-name">{{ r.refereeName || '匿名' }}</div>
              <div class="li-time">{{ maskPhone(r.refereePhone) }} · {{ formatDate(r.createdAt) }}</div>
            </div>
            <div class="chip" :class="chipClass(r.status)">{{ statusText(r.status) }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch, nextTick } from 'vue'
import { showSuccessToast, showFailToast, showToast } from 'vant'
import QRCode from 'qrcode'
import { referralApi } from '@/api/h5'
import { formatDate, fenToYuan } from '@/utils/format'

function formatMoney(fen?: number) { return fenToYuan(fen || 0) }

const loading = ref(true)
const info = ref<any>({})
const stats = ref<any>({ total: 0, active: 0, rewarded: 0, totalReward: 0 })
const list = ref<any[]>([])
const bindCode = ref('')
const qrCanvas = ref<HTMLCanvasElement>()

async function drawQr() {
  if (!qrCanvas.value || !info.value.code) return
  const url = `${window.location.origin}/#/login?ref=${info.value.code}`
  try {
    await QRCode.toCanvas(qrCanvas.value, url, {
      width: 200,
      margin: 1,
      color: { dark: '#2a3a4a', light: '#ffffff' }
    })
  } catch (e: any) { console.warn('QR draw failed', e) }
}

watch(() => info.value.code, () => nextTick(drawQr))

async function loadAll() {
  loading.value = true
  try {
    const r: any = await referralApi.me()
    info.value = r || {}
    stats.value = r?.stats || stats.value
    const lr: any = await referralApi.list()
    list.value = lr || []
  } catch (e: any) { console.warn('loadAll failed', e) }
  finally { loading.value = false }
}

async function onCopy() {
  if (!info.value.code) return
  try {
    await navigator.clipboard.writeText(info.value.code)
    showSuccessToast('已复制: ' + info.value.code)
  } catch {
    showSuccessToast('请长按复制: ' + info.value.code)
  }
}

async function onBind() {
  if (!bindCode.value) { showToast('请输入邀请码'); return }
  try {
    await referralApi.bind(bindCode.value.trim().toUpperCase())
    showSuccessToast('绑定成功')
    bindCode.value = ''
    loadAll()
  } catch (e: any) {
    showFailToast(e?.message || '绑定失败')
  }
}

function avatarChar(name?: string) {
  if (!name) return '友'
  return name.slice(0, 1)
}
function maskPhone(p?: string) {
  if (!p) return ''
  const s = String(p)
  if (s.length < 7) return s
  return s.slice(0, 3) + '****' + s.slice(-4)
}
function statusText(s: string) {
  return ({ REGISTERED: '已注册', ACTIVE: '已活跃', REWARDED: '已奖励' } as any)[s] || s
}
function chipClass(s: string) {
  return ({ REGISTERED: 'mist', ACTIVE: 'warning', REWARDED: 'success' } as any)[s] || 'mist'
}

onMounted(async () => {
  await loadAll()
  nextTick(drawQr)
})
</script>

<style scoped>
.loading { padding: 60px 0; display: flex; justify-content: center; }
.ref-page { padding: 12px 16px 30px; }

/* 推荐码卡 */
.code-card {
  position: relative;
  border-radius: var(--r-lg);
  overflow: hidden;
  margin-bottom: 14px;
  background: var(--surface);
  border: 1px solid var(--line);
}
.cc-content { position: relative; padding: 22px 20px 20px; text-align: center; }
.cc-label { font-size: 11.5px; color: var(--brand-ink); letter-spacing: 0.16em; font-weight: 500; }
.cc-code {
  font-size: 32px; font-weight: 600; color: var(--brand-deep);
  letter-spacing: 6px; margin: 10px 0 16px;
  font-family: 'SF Mono', Menlo, monospace;
  font-variant-numeric: tabular-nums;
}
.cc-copy {
  background: var(--brand-deep);
  color: #fff;
  border: none;
  border-radius: 999px;
  padding: 9px 26px;
  font-size: 13px;
  cursor: pointer;
  font-family: inherit;
  letter-spacing: 0.12em;
}
.cc-tip { font-size: 11.5px; color: var(--muted); margin-top: 12px; letter-spacing: 0.04em; }

/* 二维码 */
.qr-card { padding: 18px; text-align: center; margin-bottom: 14px; }
.qr-title { font-size: 13px; color: var(--ink-2); margin-bottom: 12px; letter-spacing: 0.04em; }
.qr-box { display: inline-block; padding: 10px; background: var(--surface); border: 1px solid var(--line); border-radius: 10px; }
.qr-canvas { display: block; width: 200px; height: 200px; border-radius: 4px; }
.qr-tip { font-size: 12px; color: var(--muted); margin-top: 12px; }

/* 统计 */
.stat-card { padding: 14px 0; margin-bottom: 14px; }
.stat-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 4px; }
.stat-item { text-align: center; }
.stat-num { font-size: 18px; font-weight: 600; color: var(--brand-deep); }
.stat-lbl { font-size: 11px; color: var(--muted); margin-top: 4px; letter-spacing: 0.04em; }

/* 绑定 */
.bind-card { padding: 16px 18px 14px; }
.bind-title { font-size: 13.5px; color: var(--ink); margin-bottom: 10px; font-weight: 500; }
.bind-input {
  width: 100%;
  height: 40px;
  padding: 0 14px;
  background: var(--surface-2);
  border: 1px solid var(--line);
  border-radius: 8px;
  font-size: 14px;
  color: var(--ink);
  font-family: 'SF Mono', monospace;
  letter-spacing: 0.16em;
  outline: none;
  transition: border-color var(--dur) var(--ease);
}
.bind-input:focus { border-color: var(--brand); }
.bind-input::placeholder { color: var(--muted-2); }
.bind-btn {
  width: 100%;
  background: var(--brand-deep);
  color: #fff;
  border: none;
  padding: 11px 0;
  border-radius: 8px;
  font-size: 13.5px;
  font-weight: 500;
  letter-spacing: 0.16em;
  margin-top: 10px;
  cursor: pointer;
  font-family: inherit;
  transition: all var(--dur) var(--ease);
}
.bind-btn:disabled { background: var(--muted-2); cursor: not-allowed; }
.bind-tip { font-size: 11px; color: var(--muted); text-align: center; margin-top: 8px; letter-spacing: 0.04em; }

/* 列表 */
.list-card { padding: 6px 16px 6px; }
.list-item { display: flex; align-items: center; gap: 10px; padding: 12px 0; border-bottom: 1px dashed var(--line); }
.list-item:last-child { border-bottom: none; }
.li-avatar {
  width: 36px; height: 36px; border-radius: 50%;
  background: linear-gradient(135deg, #f4f2ec, #e8e3d6);
  color: var(--brand-deep); font-weight: 500;
  display: flex; align-items: center; justify-content: center;
  font-family: 'Songti SC', serif; font-size: 15px;
  flex-shrink: 0;
}
.li-main { flex: 1; min-width: 0; }
.li-name { font-size: 13.5px; color: var(--ink); font-weight: 500; }
.li-time { font-size: 11.5px; color: var(--muted); margin-top: 2px; }

.empty { text-align: center; padding: 30px 0; color: var(--muted); font-size: 12px; }
.empty .van-icon { display: block; margin: 0 auto 8px; }
.empty-text { font-size: 12.5px; }
</style>
