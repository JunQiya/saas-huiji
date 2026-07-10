<template>
  <div class="page mall">
    <NavBar title="积分商城" back />

    <!-- 积分卡 -->
    <div class="points-card">
      <div class="pc-bg pc-bg-1"></div>
      <div class="pc-bg pc-bg-2"></div>
      <div class="pc-shine"></div>
      <div class="pc-content">
        <div class="pc-top">
          <div class="pc-brand">
            <div class="brand-mark"><span class="star"></span></div>
            <div>
              <div class="pc-name">星河好物</div>
              <div class="pc-sub">积分兑换 · 到店核销</div>
            </div>
          </div>
          <div class="pc-level">{{ memberInfo?.levelName || '普通会员' }}</div>
        </div>
        <div class="pc-points">
          <div class="pp-label">我的积分</div>
          <div class="pp-val">{{ displayPoints }}</div>
        </div>
        <div class="pc-foot">
          <div class="pf-item" @click="showRecords = true">
            <van-icon name="records-o" />
            <span>兑换记录</span>
            <em v-if="records.length">{{ records.length }}</em>
          </div>
          <div class="pf-item" @click="showRule = true">
            <van-icon name="question-o" />
            <span>兑换说明</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 分类 -->
    <div class="cat-bar">
      <div
        v-for="c in cats"
        :key="c.value"
        class="cat-chip"
        :class="{ active: cat === c.value }"
        @click="cat = c.value"
      >
        {{ c.label }}
      </div>
    </div>

    <div class="page-tip">用积分换一份小小心意。</div>

    <!-- 商品列表 -->
    <div v-if="loading" class="loading"><van-loading color="#6f94b8" /></div>
    <EmptyState v-else-if="!filteredList.length" :title="emptyTitle" sub="更多好礼即将上架" art="box" />

    <div v-else class="grid">
      <div v-for="p in filteredList" :key="p.id" class="card ui-card hoverable">
        <div class="cover" :class="`tone-${p.category}`">
          <van-icon :name="p.category === 'GOODS' ? 'gift-o' : 'gem-o'" size="32" color="rgba(255,255,255,0.9)" />
          <div v-if="p.category === 'GOODS' && (p.stock ?? 0) <= 5" class="stock-flag">仅剩 {{ p.stock }}</div>
        </div>
        <div class="info">
          <div class="name">{{ p.name }}</div>
          <div class="desc">{{ p.description || (p.category === 'GOODS' ? '实物商品' : '到店体验服务') }}</div>
          <div class="meta">
            <div class="price">
              <span class="num val">{{ pointsFor(p) }}</span>
              <span class="unit">积分</span>
            </div>
            <button
              class="ex-btn"
              :class="{ disabled: !canExchange(p) }"
              :disabled="!canExchange(p)"
              @click="onExchange(p)"
            >
              {{ canExchange(p) ? '兑换' : '积分不足' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 兑换确认 -->
    <van-dialog
      v-model:show="confirmVisible"
      title="确认兑换"
      show-cancel-button
      :confirm-button-text="exchanging ? '兑换中…' : '确认兑换'"
      :confirm-button-disabled="exchanging"
      @confirm="doExchange"
    >
      <div class="confirm-body">
        <div class="conf-product">
          <div class="cp-cover" :class="`tone-${target?.category || ''}`">
            <van-icon :name="target?.category === 'GOODS' ? 'gift-o' : 'gem-o'" size="26" color="rgba(255,255,255,0.9)" />
          </div>
          <div class="cp-info">
            <div class="cp-name">{{ target?.name }}</div>
            <div class="cp-desc">{{ target?.description || (target?.category === 'GOODS' ? '实物商品' : '到店体验服务') }}</div>
          </div>
        </div>
        <div class="conf-rows">
          <div class="conf-row"><span>消耗积分</span><span class="val">{{ target ? pointsFor(target) : 0 }}</span></div>
          <div class="conf-row"><span>当前积分</span><span>{{ memberInfo?.points || 0 }}</span></div>
          <div class="conf-row"><span>兑换后剩余</span><span class="val strong">{{ remainingAfter }}</span></div>
        </div>
        <div class="conf-tip">兑换后将生成核销码，请到店出示核销。</div>
      </div>
    </van-dialog>

    <!-- 核销码 -->
    <van-dialog v-model:show="codeVisible" title="兑换成功" :show-cancel-button="false" confirm-button-text="我知道了">
      <div class="code-body">
        <div class="code-success">
          <van-icon name="checked" size="34" color="#fff" />
        </div>
        <div class="code-name">{{ redeemed?.name }}</div>
        <div class="code-box">
          <div class="qr-mock">
            <div
              v-for="i in 49"
              :key="i"
              class="qr-cell"
              :class="{ on: cellOn(redeemed?.code, i) }"
            ></div>
          </div>
        </div>
        <div class="code-text">核销码</div>
        <div class="code-no">{{ redeemed?.code }}</div>
        <div class="code-hint">请到门店出示此码完成核销</div>
      </div>
    </van-dialog>

    <!-- 兑换说明 -->
    <van-dialog v-model:show="showRule" title="兑换说明" :show-cancel-button="false" confirm-button-text="知道了">
      <div class="rule-body">
        <p>· 积分按 1 元 = 10 积分换算商品所需积分。</p>
        <p>· 兑换成功后生成专属核销码，请到门店出示核销。</p>
        <p>· 实物商品库存有限，兑完即止。</p>
        <p>· 积分不可转让、不可折现，兑换后不退回。</p>
        <p>· 如有疑问，请联系门店或拨打客服电话。</p>
      </div>
    </van-dialog>

    <!-- 兑换记录 -->
    <van-popup v-model:show="showRecords" position="right" :style="{ width: '88%', height: '100%' }">
      <div class="records-page">
        <NavBar title="兑换记录" back @click="showRecords = false" />
        <EmptyState v-if="!records.length" title="暂无兑换记录" sub="你兑换的每一份心意，都会留在这里" art="box" />
        <div v-else class="rec-list">
          <div v-for="r in records" :key="r.code" class="rec-item">
            <div class="ri-left">
              <div class="ri-name">{{ r.name }}</div>
              <div class="ri-meta">
                <span class="ri-cost">{{ r.cost }} 积分</span>
                <span class="ri-time">{{ fmtTime(r.time) }}</span>
              </div>
              <div class="ri-code">核销码 {{ r.code }}</div>
            </div>
            <div class="chip" :class="r.status === 'PENDING' ? 'warning' : r.status === 'USED' ? 'success' : 'danger'">
              {{ statusText(r.status) }}
            </div>
          </div>
          <div v-if="records.length" class="rec-clear" @click="onClearRecords">清空记录</div>
        </div>
      </div>
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { showToast, showConfirmDialog } from 'vant'
import { h5Api, type Product } from '@/api/h5'
import { useMemberStore } from '@/stores/member'
import NavBar from '@/components/NavBar.vue'
import EmptyState from '@/components/EmptyState.vue'

const memberStore = useMemberStore()
const memberInfo = computed(() => memberStore.memberInfo)

const products = ref<Product[]>([])
const loading = ref(false)
const cat = ref<'' | 'SERVICE' | 'GOODS'>('')

const cats = [
  { label: '全部', value: '' as const },
  { label: '服务', value: 'SERVICE' as const },
  { label: '实物', value: 'GOODS' as const }
]
const catLabel = computed(() => cats.find(c => c.value === cat.value)?.label || '')
const emptyTitle = computed(() => cat.value ? `暂无${catLabel.value}好礼` : '暂无可兑换好礼')

const filteredList = computed(() => {
  if (!cat.value) return products.value
  return products.value.filter(p => p.category === cat.value)
})

function pointsFor(p: Product) {
  return Math.max(1, Math.round((Number(p.price) || 0) / 100 * 10))
}
function canExchange(p: Product) {
  return (memberInfo.value?.points || 0) >= pointsFor(p)
}
const displayPoints = computed(() => memberInfo.value?.points || 0)
const remainingAfter = computed(() => {
  if (!target.value) return 0
  return Math.max(0, (memberInfo.value?.points || 0) - pointsFor(target.value))
})

async function load() {
  loading.value = true
  try {
    const data: any = await h5Api.activeProducts()
    products.value = Array.isArray(data) ? data : []
  } catch { products.value = [] }
  finally { loading.value = false }
}

// 兑换流程
const confirmVisible = ref(false)
const target = ref<Product | null>(null)
const exchanging = ref(false)
const codeVisible = ref(false)
const redeemed = ref<{ name: string; code: string; cost: number } | null>(null)

function onExchange(p: Product) {
  if (!canExchange(p)) { showToast('积分不足'); return }
  target.value = p
  confirmVisible.value = true
}

async function doExchange() {
  if (!target.value) return
  exchanging.value = true
  await new Promise(r => setTimeout(r, 480))
  const p = target.value
  const cost = pointsFor(p)
  const code = genCode()
  if (memberInfo.value) {
    memberStore.patchMember({ points: Math.max(0, (memberInfo.value.points || 0) - cost) })
  }
  records.value.unshift({ code, name: p.name, cost, category: p.category, time: Date.now(), status: 'PENDING' })
  saveRecords()
  redeemed.value = { name: p.name, code, cost }
  confirmVisible.value = false
  exchanging.value = false
  codeVisible.value = true
}

function genCode() {
  const t = Date.now().toString(36).toUpperCase().slice(-5)
  const r = Math.random().toString(36).toUpperCase().slice(2, 5)
  return `XH${t}${r}`
}
function cellOn(code: string | undefined, i: number) {
  if (!code) return false
  const seed = code.split('').reduce((s, c) => s + c.charCodeAt(0), 0)
  return ((seed * (i + 3)) % 7) < 3
}

// 兑换记录
interface Record { code: string; name: string; cost: number; category: string; time: number; status: 'PENDING' | 'USED' | 'EXPIRED' }
const RECORD_KEY = 'huiji_mall_records'
const records = ref<Record[]>([])
const showRecords = ref(false)
const showRule = ref(false)

function loadRecords() {
  try {
    const raw = localStorage.getItem(RECORD_KEY)
    records.value = raw ? JSON.parse(raw) : []
  } catch { records.value = [] }
}
function saveRecords() {
  try { localStorage.setItem(RECORD_KEY, JSON.stringify(records.value)) } catch {}
}
async function onClearRecords() {
  try {
    await showConfirmDialog({ title: '提示', message: '确认清空兑换记录？' })
  } catch { return }
  records.value = []
  saveRecords()
}
function statusText(s: string) {
  return ({ PENDING: '待核销', USED: '已核销', EXPIRED: '已失效' } as any)[s] || s
}
function fmtTime(t: number) {
  try { return new Date(t).toLocaleString('zh-CN', { hour12: false }) } catch { return '-' }
}

onMounted(() => { load(); loadRecords() })
</script>

<style scoped>
.mall { padding-bottom: 24px; }

.page-tip {
  font-size: 12px;
  color: var(--muted);
  letter-spacing: 0.04em;
  margin: 0 16px 12px;
  font-family: 'Songti SC', serif;
  opacity: 0.85;
  padding-left: 2px;
}

/* 积分卡 */
.points-card {
  position: relative;
  margin: 12px 16px 14px;
  border-radius: var(--r-lg);
  overflow: hidden;
  background: linear-gradient(135deg, #6f94b8 0%, #5a7d9f 55%, #4a6a87 100%);
  color: #fff;
  box-shadow: 0 6px 22px rgba(74, 106, 135, 0.22);
}
.pc-bg { position: absolute; border-radius: 50%; pointer-events: none; }
.pc-bg-1 { width: 200px; height: 200px; top: -80px; right: -60px; background: radial-gradient(circle, rgba(255, 255, 255, 0.14), transparent 60%); }
.pc-bg-2 { width: 120px; height: 120px; bottom: -50px; left: -30px; background: radial-gradient(circle, rgba(255, 255, 255, 0.08), transparent 60%); }
.pc-shine { position: absolute; top: 0; left: 0; right: 0; height: 1px; background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.6), transparent); }
.pc-content { position: relative; z-index: 1; padding: 16px 20px; }
.pc-top { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; }
.pc-brand { display: flex; align-items: center; gap: 10px; }
.brand-mark { width: 30px; height: 30px; border-radius: 8px; background: rgba(255, 255, 255, 0.20); display: flex; align-items: center; justify-content: center; }
.star { width: 14px; height: 14px; background: #fff; clip-path: polygon(50% 0%, 61% 35%, 98% 35%, 68% 57%, 79% 91%, 50% 70%, 21% 91%, 32% 57%, 2% 35%, 39% 35%); }
.pc-name { font-size: 14px; font-weight: 600; letter-spacing: 0.04em; }
.pc-sub { font-size: 10.5px; opacity: 0.78; margin-top: 1px; letter-spacing: 0.5px; }
.pc-level { font-size: 11px; padding: 3px 10px; background: rgba(255, 255, 255, 0.22); border-radius: 999px; letter-spacing: 0.04em; }
.pc-points { margin-bottom: 14px; }
.pp-label { font-size: 11.5px; opacity: 0.82; margin-bottom: 2px; letter-spacing: 0.04em; }
.pp-val { font-size: 30px; font-weight: 600; letter-spacing: 0.5px; font-variant-numeric: tabular-nums; line-height: 1.2; }
.pc-foot { display: flex; gap: 18px; padding-top: 12px; border-top: 1px dashed rgba(255, 255, 255, 0.18); }
.pf-item { display: flex; align-items: center; gap: 5px; font-size: 12px; opacity: 0.92; cursor: pointer; transition: opacity var(--dur) var(--ease); }
.pf-item:active { opacity: 0.6; }
.pf-item em { font-style: normal; background: rgba(255, 255, 255, 0.22); padding: 0 6px; border-radius: 999px; font-size: 10.5px; margin-left: 2px; }

/* 分类 */
.cat-bar { display: flex; gap: 8px; padding: 0 16px 4px; overflow-x: auto; }
.cat-chip {
  padding: 6px 14px;
  font-size: 12.5px;
  color: var(--ink-2);
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: 999px;
  white-space: nowrap;
  flex-shrink: 0;
  cursor: pointer;
  transition: all var(--dur) var(--ease);
  letter-spacing: 0.02em;
}
.cat-chip.active { background: var(--brand-deep); color: #fff; border-color: var(--brand-deep); }

.loading { display: flex; justify-content: center; padding: 40px 0; }

/* 商品网格 */
.grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; padding: 0 16px; }
.card { padding: 0; overflow: hidden; }
.cover {
  position: relative;
  height: 100px;
  display: flex; align-items: center; justify-content: center;
}
.cover.tone-GOODS { background: linear-gradient(135deg, #5a7d9f, #4a6a87); }
.cover.tone-SERVICE { background: linear-gradient(135deg, #c89d96, #a8736a); }
.stock-flag { position: absolute; top: 8px; right: 8px; font-size: 10px; background: rgba(192, 133, 116, 0.92); color: #fff; padding: 2px 8px; border-radius: 999px; letter-spacing: 0.02em; }
.info { padding: 10px 12px 12px; }
.name { font-size: 13px; color: var(--ink); font-weight: 500; line-height: 1.4; min-height: 36px; letter-spacing: 0.02em; }
.desc { font-size: 11px; color: var(--muted); margin: 3px 0 8px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.meta { display: flex; align-items: center; justify-content: space-between; }
.price { color: var(--brand-deep); }
.price .num { font-size: 16px; font-weight: 600; font-variant-numeric: tabular-nums; }
.price .unit { font-size: 10.5px; margin-left: 2px; opacity: 0.85; }
.ex-btn {
  font-size: 12px;
  height: 26px; padding: 0 12px;
  background: var(--brand-deep);
  color: #fff;
  border: none;
  border-radius: 999px;
  font-family: inherit;
  cursor: pointer;
  letter-spacing: 0.04em;
  transition: all var(--dur) var(--ease);
}
.ex-btn:active { transform: scale(0.96); }
.ex-btn.disabled { background: var(--muted-2); cursor: not-allowed; }

/* 兑换确认 */
.confirm-body { padding: 4px 20px 8px; }
.conf-product { display: flex; align-items: center; gap: 12px; padding: 10px 0 14px; border-bottom: 1px dashed var(--line); }
.cp-cover { width: 52px; height: 52px; border-radius: 10px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.cp-cover.tone-GOODS { background: linear-gradient(135deg, #5a7d9f, #4a6a87); }
.cp-cover.tone-SERVICE { background: linear-gradient(135deg, #c89d96, #a8736a); }
.cp-info { flex: 1; min-width: 0; }
.cp-name { font-size: 14px; color: var(--ink); font-weight: 500; }
.cp-desc { font-size: 11.5px; color: var(--muted); margin-top: 2px; }
.conf-rows { padding: 10px 0; }
.conf-row { display: flex; align-items: center; justify-content: space-between; padding: 5px 0; font-size: 13px; color: var(--ink-2); }
.conf-row .val { color: var(--brand-deep); font-weight: 600; font-variant-numeric: tabular-nums; }
.conf-row .strong { font-size: 14px; }
.conf-tip { font-size: 11.5px; color: var(--muted); background: var(--brand-softer); padding: 8px 12px; border-radius: 8px; line-height: 1.6; }

/* 核销码 */
.code-body { padding: 16px 24px 20px; text-align: center; }
.code-success { width: 56px; height: 56px; border-radius: 50%; background: var(--success); display: flex; align-items: center; justify-content: center; margin: 0 auto 12px; }
.code-name { font-size: 15px; color: var(--ink); font-weight: 500; margin-bottom: 14px; }
.code-box { display: flex; justify-content: center; margin-bottom: 12px; }
.qr-mock {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 2px;
  width: 140px; height: 140px;
  padding: 8px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 8px;
}
.qr-cell { background: transparent; border-radius: 1px; }
.qr-cell.on { background: var(--ink); }
.code-text { font-size: 11.5px; color: var(--muted); letter-spacing: 0.08em; }
.code-no { font-family: 'SF Mono', Menlo, monospace; font-size: 16px; letter-spacing: 2px; color: var(--brand-deep); font-weight: 600; margin: 4px 0 8px; }
.code-hint { font-size: 11.5px; color: var(--muted); }

/* 兑换说明 */
.rule-body { padding: 8px 20px 16px; }
.rule-body p { font-size: 12.5px; color: var(--ink-2); line-height: 1.8; margin: 6px 0; }

/* 兑换记录 */
.records-page { height: 100%; display: flex; flex-direction: column; background: var(--page-bg); }
.rec-list { flex: 1; overflow-y: auto; padding: 12px 16px; }
.rec-item {
  display: flex; align-items: center; gap: 10px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-md);
  padding: 12px 14px;
  margin-bottom: 10px;
}
.ri-left { flex: 1; min-width: 0; }
.ri-name { font-size: 13.5px; color: var(--ink); font-weight: 500; }
.ri-meta { display: flex; gap: 10px; margin: 4px 0; font-size: 11.5px; color: var(--muted); }
.ri-cost { color: var(--brand-deep); }
.ri-code { font-size: 11px; color: var(--muted); font-family: 'SF Mono', monospace; letter-spacing: 0.04em; }
.rec-clear { text-align: center; font-size: 12.5px; color: var(--muted); padding: 16px 0 8px; cursor: pointer; letter-spacing: 0.04em; }
.rec-clear:active { color: var(--danger); }
</style>
