<template>
  <div class="page order-detail">
    <NavBar title="订单详情" back />
    <div v-if="loading" class="loading"><van-loading color="#6f94b8" /></div>
    <EmptyState v-else-if="!order" title="订单不存在" sub="订单可能已被清理" art="box" />

    <div v-else>
      <div class="status-card" :class="statusClass(order.status)">
        <div class="st-decor decor-1"></div>
        <div class="st-decor decor-2"></div>
        <div class="st-content">
          <div class="st-name">{{ statusLabel(order.status) }}</div>
          <div class="st-sub">订单号 {{ order.orderNo }}</div>
        </div>
      </div>

      <div class="section-title">
        <span>订单信息</span>
      </div>
      <div class="ui-card block">
        <div class="kv"><span class="k">下单时间</span><span class="v">{{ fmt(order.createdAt) }}</span></div>
        <div class="kv"><span class="k">门店</span><span class="v">{{ order.storeName || order.storeId || '-' }}</span></div>
        <div class="kv" v-if="order.cashierName || order.cashierId"><span class="k">收银员</span><span class="v">{{ order.cashierName || order.cashierId }}</span></div>
        <div class="kv" v-if="order.remark"><span class="k">备注</span><span class="v">{{ order.remark }}</span></div>
      </div>

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

      <div class="section-title">
        <span>结算</span>
      </div>
      <div class="ui-card block">
        <div class="kv"><span class="k">商品总额</span><span class="v val">¥{{ yuan(order.totalAmount) }}</span></div>
        <div class="kv"><span class="k">优惠</span><span class="v val neg">- ¥{{ yuan(order.discountAmount) }}</span></div>
        <div class="kv kv-strong"><span class="k">实付</span><span class="v val strong">¥{{ yuan(order.paidAmount) }}</span></div>
        <div class="kv" v-if="order.payMethod"><span class="k">支付方式</span><span class="v">{{ payLabel(order.payMethod) }}</span></div>
        <div class="kv" v-if="order.paidAt"><span class="k">支付时间</span><span class="v">{{ fmt(order.paidAt) }}</span></div>
        <div class="kv" v-if="order.refundedAt"><span class="k">退款时间</span><span class="v">{{ fmt(order.refundedAt) }}</span></div>
        <div class="kv" v-if="order.refundReason"><span class="k">退款原因</span><span class="v">{{ order.refundReason }}</span></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
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

async function load() {
  loading.value = true
  try {
    const id = String(route.params.id)
    order.value = await h5Api.orderDetail(id)
  } catch { order.value = null }
  finally { loading.value = false }
}

onMounted(load)
</script>

<style scoped>
.order-detail { padding-bottom: 40px; }
.loading { display: flex; justify-content: center; padding: 50px 0; }

.status-card {
  position: relative;
  margin: 12px 16px 14px;
  padding: 22px 22px;
  border-radius: var(--r-lg);
  color: #fff;
  background: linear-gradient(135deg, #5a7d9f, #4a6a87);
  box-shadow: 0 6px 20px rgba(74, 106, 135, 0.22);
  overflow: hidden;
}
.status-card.success { background: linear-gradient(135deg, #7e9a8a, #5b7868); box-shadow: 0 6px 20px rgba(126, 154, 138, 0.22); }
.status-card.warning { background: linear-gradient(135deg, #b8a16a, #9b8550); box-shadow: 0 6px 20px rgba(184, 161, 106, 0.22); }
.status-card.danger { background: linear-gradient(135deg, #c0897a, #a56e5f); box-shadow: 0 6px 20px rgba(192, 133, 116, 0.22); }
.status-card.info { background: linear-gradient(135deg, #8a8e85, #6c7066); }
.st-decor { position: absolute; border-radius: 50%; pointer-events: none; }
.decor-1 { width: 160px; height: 160px; top: -60px; right: -40px; background: radial-gradient(circle, rgba(255, 255, 255, 0.14), transparent 60%); }
.decor-2 { width: 100px; height: 100px; bottom: -40px; left: -20px; background: radial-gradient(circle, rgba(255, 255, 255, 0.08), transparent 60%); }
.st-content { position: relative; z-index: 1; }
.st-name { font-size: 22px; font-weight: 600; letter-spacing: 0.04em; font-family: 'Songti SC', serif; }
.st-sub { font-size: 12px; opacity: 0.85; margin-top: 4px; font-family: 'SF Mono', monospace; letter-spacing: 0.04em; }

.block { margin: 0 16px 14px; }
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
</style>
