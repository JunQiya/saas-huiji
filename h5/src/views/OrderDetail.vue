<template>
  <div class="page order-detail">
    <NavBar title="订单详情" back />

    <div v-if="loading" class="loading"><van-loading color="#5a7a9c" /></div>
    <EmptyState v-else-if="!order" title="订单不存在" sub="订单可能已被清理或链接有误" art="box" />

    <div v-else>
      <!-- 状态卡 -->
      <div class="status-card" :class="statusClass(order.status)">
        <svg class="st-stars" viewBox="0 0 200 80" aria-hidden="true">
          <circle cx="170" cy="20" r="0.8" fill="#fff" opacity="0.4" />
          <circle cx="180" cy="50" r="0.7" fill="#fff" opacity="0.3" />
          <circle cx="150" cy="65" r="0.6" fill="#fff" opacity="0.25" />
          <circle cx="20" cy="20" r="0.7" fill="#fff" opacity="0.3" />
        </svg>
        <div class="st-content">
          <div class="st-name">{{ statusLabel(order.status) }}</div>
          <div class="st-sub">订单号 {{ order.orderNo }}</div>
        </div>
      </div>

      <!-- 订单时间轴 -->
      <div class="section-title">
        <span>订单进度</span>
        <span class="st-tip">{{ progress.length }} 个节点</span>
      </div>
      <div class="ui-card block timeline">
        <div
          v-for="(p, i) in progress"
          :key="i"
          class="tl-row"
          :class="[p.state, { last: i === progress.length - 1 }]"
        >
          <div class="tl-dot">
            <div class="td-inner"></div>
          </div>
          <div class="tl-content">
            <div class="tl-title">{{ p.title }}</div>
            <div class="tl-time">{{ p.time }}</div>
            <div v-if="p.desc" class="tl-desc">{{ p.desc }}</div>
          </div>
        </div>
      </div>

      <!-- 订单信息 -->
      <div class="section-title"><span>订单信息</span></div>
      <div class="ui-card block">
        <div class="kv"><span class="k">下单时间</span><span class="v">{{ fmt(order.createdAt) }}</span></div>
        <div class="kv"><span class="k">门店</span><span class="v">{{ order.storeName || order.storeId || '-' }}</span></div>
        <div class="kv" v-if="order.cashierName || order.cashierId"><span class="k">收银员</span><span class="v">{{ order.cashierName || order.cashierId }}</span></div>
        <div class="kv" v-if="order.remark"><span class="k">备注</span><span class="v">{{ order.remark }}</span></div>
      </div>

      <!-- 商品明细 -->
      <div class="section-title">
        <span>商品明细</span>
        <span class="st-tip">{{ order.items?.length || 0 }} 项</span>
      </div>
      <div class="ui-card block">
        <div v-for="it in (order.items || [])" :key="it.id" class="item-row">
          <div class="it-name">{{ it.productName }}</div>
          <div class="it-qty">x{{ it.quantity }}</div>
          <div class="it-sub val">¥{{ yuan(it.subtotal) }}</div>
        </div>
        <div v-if="!order.items || order.items.length === 0" class="muted">无明细</div>
      </div>

      <!-- 结算 -->
      <div class="section-title"><span>结算</span></div>
      <div class="ui-card block">
        <div class="kv"><span class="k">商品总额</span><span class="v val">¥{{ yuan(order.totalAmount) }}</span></div>
        <div class="kv"><span class="k">优惠</span><span class="v val neg">- ¥{{ yuan(order.discountAmount) }}</span></div>
        <div class="kv kv-strong"><span class="k">实付</span><span class="v val strong">¥{{ yuan(order.paidAmount) }}</span></div>
        <div class="kv" v-if="order.payMethod"><span class="k">支付方式</span><span class="v">{{ payLabel(order.payMethod) }}</span></div>
        <div class="kv" v-if="order.paidAt"><span class="k">支付时间</span><span class="v">{{ fmt(order.paidAt) }}</span></div>
        <div class="kv" v-if="order.refundedAt"><span class="k">退款时间</span><span class="v">{{ fmt(order.refundedAt) }}</span></div>
        <div class="kv" v-if="order.refundReason"><span class="k">退款原因</span><span class="v">{{ order.refundReason }}</span></div>
      </div>

      <!-- 联系客服 -->
      <div class="footer-actions">
        <button class="action-btn primary" @click="onContact">
          <van-icon name="service-o" size="14" /> 联系门店
        </button>
        <button class="action-btn" @click="onCopy">
          <van-icon name="orders-o" size="14" /> 复制单号
        </button>
      </div>
    </div>

    <div class="footnote">记得星河 也记得这次到店</div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { showToast } from 'vant'
import { h5Api } from '@/api/h5'
import NavBar from '@/components/NavBar.vue'
import EmptyState from '@/components/EmptyState.vue'

const route = useRoute()
const loading = ref(false)
const order = ref<any>(null)

function statusLabel(s: string) { return ({ PENDING: '待支付', PAID: '已支付', REFUNDED: '已退款', VOID: '已作废' } as any)[s] || s }
function statusClass(s: string) { if (s === 'PAID') return 'success'; if (s === 'PENDING') return 'warning'; if (s === 'VOID') return 'danger'; if (s === 'REFUNDED') return 'info'; return '' }
function payLabel(m: string) { return ({ CASH: '现金', WECHAT: '微信', ALIPAY: '支付宝', BALANCE: '余额', MIXED: '混合' } as any)[m] || m }
function yuan(f: any) { if (f == null) return '0.00'; return (Number(f) / 100).toFixed(2) }
function fmt(t: any) { if (!t) return '-'; try { return new Date(t).toLocaleString('zh-CN', { hour12: false }) } catch { return String(t) } }

// 进度时间轴
interface Node { state: 'done' | 'current' | 'pending'; title: string; time: string; desc?: string }
const progress = computed<Node[]>(() => {
  const o = order.value
  if (!o) return []
  const list: Node[] = []
  list.push({ state: 'done', title: '订单创建', time: fmt(o.createdAt), desc: '门店已收到你的订单' })

  if (o.status === 'PENDING') {
    list.push({ state: 'current', title: '等待支付', time: '请尽快完成支付', desc: '超时将自动取消' })
    list.push({ state: 'pending', title: '支付完成', time: '-' })
    if (o.status === 'REFUNDED' || o.status === 'VOID') {
      list.push({ state: 'done', title: '订单关闭', time: fmt(o.refundedAt), desc: o.refundReason || '已关闭' })
    }
  } else {
    list.push({ state: 'done', title: '支付完成', time: fmt(o.paidAt || o.createdAt), desc: o.payMethod ? `支付方式：${payLabel(o.payMethod)}` : '' })
    if (o.status === 'REFUNDED') {
      list.push({ state: 'done', title: '已退款', time: fmt(o.refundedAt), desc: o.refundReason || '退款完成' })
    } else if (o.status === 'VOID') {
      list.push({ state: 'done', title: '订单作废', time: fmt(o.refundedAt), desc: o.refundReason || '已作废' })
    } else {
      list.push({ state: 'done', title: '订单完成', time: fmt(o.paidAt || o.createdAt), desc: '感谢你的到店' })
    }
  }
  return list
})

async function load() {
  loading.value = true
  try {
    const id = String(route.params.id)
    order.value = await h5Api.orderDetail(id)
  } catch { order.value = null }
  finally { loading.value = false }
}

function onContact() { showToast('门店热线：详见门店页') }
function onCopy() {
  if (!order.value?.orderNo) return
  if (navigator.clipboard) {
    navigator.clipboard.writeText(order.value.orderNo)
      .then(() => showToast({ type: 'success', message: '已复制订单号' }))
      .catch(() => fallbackCopy(order.value.orderNo))
  } else { fallbackCopy(order.value.orderNo) }
}
function fallbackCopy(text: string) {
  const ta = document.createElement('textarea')
  ta.value = text
  ta.style.position = 'fixed'
  ta.style.left = '-9999px'
  document.body.appendChild(ta)
  ta.select()
  try { document.execCommand('copy'); showToast({ type: 'success', message: '已复制订单号' }) }
  catch { showToast('复制失败，请手动选择') }
  document.body.removeChild(ta)
}

onMounted(load)
</script>

<style scoped>
.order-detail { padding-bottom: 40px; }
.loading { display: flex; justify-content: center; padding: 50px 0; }

/* 状态卡（去渐变，使用低饱和纯色） */
.status-card {
  position: relative;
  margin: 12px 16px 14px;
  padding: 22px 22px;
  border-radius: var(--r-lg);
  color: #fff;
  background: #4a6583;
  box-shadow: 0 6px 20px rgba(74, 101, 131, 0.18);
  overflow: hidden;
}
.status-card.success { background: #6a8273; }
.status-card.warning { background: #a08558; }
.status-card.danger { background: #a06b5a; }
.status-card.info { background: #767570; }
.st-stars { position: absolute; inset: 0; width: 100%; height: 100%; pointer-events: none; }
.st-content { position: relative; z-index: 1; }
.st-name {
  font-family: var(--font-serif);
  font-size: 22px; font-weight: 500;
  letter-spacing: 0.08em;
}
.st-sub {
  font-family: var(--font-num);
  font-size: 12px; opacity: 0.78;
  margin-top: 6px; letter-spacing: 0.08em;
}

.block { margin: 0 16px 14px; }

/* 时间轴 */
.timeline { padding: 6px 16px; }
.tl-row {
  position: relative;
  display: flex; align-items: flex-start; gap: 12px;
  padding: 12px 0;
}
.tl-row + .tl-row { border-top: 1px dashed var(--line); }
.tl-row:not(.last)::before {
  content: '';
  position: absolute;
  left: 5px; top: 32px; bottom: -2px;
  width: 1px;
  background: var(--line-2);
}
.tl-row.done:not(.last)::before { background: var(--success); opacity: 0.5; }
.tl-dot {
  width: 11px; height: 11px;
  border-radius: 50%;
  background: var(--surface);
  border: 1.5px solid var(--muted-2);
  flex-shrink: 0;
  margin-top: 5px;
  position: relative; z-index: 1;
}
.tl-row.done .tl-dot { border-color: var(--success); background: var(--success-soft); }
.tl-row.done .tl-dot .td-inner { display: block; width: 5px; height: 5px; border-radius: 50%; background: var(--success); margin: 1.5px auto 0; }
.tl-row.current .tl-dot { border-color: var(--brand); background: var(--brand-soft); animation: x-twinkle 1.6s ease-in-out infinite; }
.tl-row.current .tl-dot .td-inner { display: block; width: 5px; height: 5px; border-radius: 50%; background: var(--brand); margin: 1.5px auto 0; }
.tl-row.pending .tl-dot { border-color: var(--muted-2); background: var(--surface); }
.tl-row.pending .td-inner { display: none; }
.tl-content { flex: 1; min-width: 0; }
.tl-title {
  font-family: var(--font-serif);
  font-size: 13.5px; color: var(--ink);
  font-weight: 500; letter-spacing: 0.04em;
}
.tl-row.pending .tl-title { color: var(--muted); }
.tl-time {
  font-family: var(--font-num);
  font-size: 11.5px; color: var(--muted);
  margin-top: 2px; letter-spacing: 0.02em;
}
.tl-desc {
  font-family: var(--font-serif);
  font-size: 11.5px; color: var(--ink-3);
  margin-top: 3px; letter-spacing: 0.02em;
}

/* kv */
.kv { display: flex; align-items: center; justify-content: space-between; padding: 8px 0; font-size: 13px; border-bottom: 1px dashed var(--line); }
.kv:last-child { border-bottom: none; }
.kv .k { color: var(--muted); }
.kv .v { color: var(--ink); }
.kv .v.neg { color: #8a4d3f; }
.kv-strong { padding-top: 12px !important; margin-top: 4px; border-top: 1px solid var(--line-2); }
.kv-strong .k { color: var(--ink-2) !important; font-weight: 500; }
.kv-strong .v.strong { color: var(--brand-deep); font-size: 17px; font-weight: 600; }
.item-row { display: flex; align-items: center; padding: 6px 0; font-size: 13px; border-bottom: 1px dashed var(--line); }
.item-row:last-child { border-bottom: none; }
.it-name { flex: 1; color: var(--ink); }
.it-qty { color: var(--muted); margin: 0 12px; font-size: 12.5px; }
.it-sub { color: var(--ink-2); }
.muted { color: var(--muted); font-size: 12.5px; padding: 6px 0; text-align: center; }

/* 操作按钮 */
.footer-actions {
  display: flex; gap: 10px;
  margin: 16px 16px 0;
  padding-top: 14px;
  border-top: 1px dashed var(--line);
}
.action-btn {
  flex: 1;
  height: 38px;
  border-radius: var(--r);
  background: var(--surface-2);
  border: 1px solid var(--line);
  color: var(--ink-2);
  font-size: 12.5px;
  font-family: var(--font-serif);
  letter-spacing: 0.06em;
  display: flex; align-items: center; justify-content: center; gap: 5px;
  cursor: pointer;
  transition: all var(--dur) var(--ease-out);
}
.action-btn:active { transform: scale(0.98); }
.action-btn.primary {
  background: var(--brand-deep);
  color: #fff;
  border-color: transparent;
}
.action-btn.primary:active { background: #2e4863; }
</style>
