<template>
  <div class="page mall-orders">
    <NavBar title="我的订单" back />

    <van-pull-refresh v-model="refreshing" class="pr-wrap" @refresh="onRefresh">
    <div class="page-padding">
      <div class="tip">每一笔订单，都是一份心意的托付。</div>

      <!-- 搜索 -->
      <div class="search-box">
        <van-icon name="search" class="s-ic" />
        <input
          v-model="keyword"
          type="search"
          placeholder="按订单号搜索"
          class="s-input"
          @keyup.enter="onSearch"
        />
        <span v-if="keyword" class="s-clear" @click="onClear">×</span>
      </div>

      <!-- 状态 tab -->
      <div class="status-tabs">
        <div
          v-for="t in tabs"
          :key="t.value"
          class="tab"
          :class="{ active: status === t.value }"
          @click="onTab(t.value)"
        >
          {{ t.label }}
        </div>
      </div>

      <van-list
        v-model:loading="loading"
        :finished="finished"
        :finished-text="list.length ? '没有更多了' : ''"
        @load="load"
      >
        <div v-if="loading && !list.length" class="loading"><van-loading color="#6f94b8" /></div>
        <EmptyState
          v-else-if="!list.length && finished"
          title="暂无订单"
          sub="下单后会在这里显示"
          art="box"
        >
          <van-button round type="primary" size="small" class="go-btn" @click="goMall">去逛逛</van-button>
        </EmptyState>
        <div v-else class="order-list">
          <div
            v-for="o in list"
            :key="o.id"
            class="order-card ui-card hoverable"
            :class="{ highlight: String(highlightId) === String(o.id) }"
            @click="openDetail(o)"
          >
            <div class="o-head">
              <span class="o-no val">{{ o.orderNo }}</span>
              <span class="chip" :class="statusChipClass(o.status)">{{ statusText(o.status) }}</span>
            </div>
            <div v-if="o.items?.length" class="o-items">
              <div v-for="(it, i) in o.items.slice(0, 3)" :key="i" class="o-item">
                <span class="o-item-name">{{ it.productName }}</span>
                <span class="o-item-qty muted">x{{ it.quantity }}</span>
                <span class="o-item-sub val">¥{{ yuan(it.subtotal) }}</span>
              </div>
              <div v-if="o.items.length > 3" class="o-more muted">还有 {{ o.items.length - 3 }} 件...</div>
            </div>
            <div v-if="o.trackingNo" class="o-tracking">
              <van-icon name="logistics" size="14" />
              <span class="val">{{ o.trackingNo }}</span>
              <span v-if="o.trackingCompany" class="muted">（{{ o.trackingCompany }}）</span>
            </div>
            <div class="o-foot">
              <div class="o-time muted">{{ fmt(o.createdAt) }}</div>
              <div class="o-amount">
                <span class="muted">合计</span>
                <span class="val strong">¥{{ yuan(o.totalAmount) }}</span>
              </div>
            </div>
          </div>
        </div>
      </van-list>
    </div>
    </van-pull-refresh>

    <!-- 订单详情弹层 -->
    <van-popup v-model:show="detailVisible" position="bottom" round :style="{ maxHeight: '80%' }">
      <div class="detail-popup">
        <div class="dp-header">
          <span class="dp-title">订单详情</span>
          <van-icon name="cross" size="18" class="dp-close" @click="detailVisible = false" />
        </div>
        <div v-if="detailLoading" class="loading"><van-loading color="#6f94b8" /></div>
        <template v-else-if="detail">
          <div class="dp-section">
            <div class="dp-row">
              <span class="muted">订单号</span>
              <span class="val">{{ detail.orderNo }}</span>
            </div>
            <div class="dp-row">
              <span class="muted">状态</span>
              <span class="chip" :class="statusChipClass(detail.status)">{{ statusText(detail.status) }}</span>
            </div>
            <div class="dp-row">
              <span class="muted">下单时间</span>
              <span>{{ fmt(detail.createdAt) }}</span>
            </div>
            <div class="dp-row">
              <span class="muted">配送方式</span>
              <span>{{ detail.extend?.deliveryType === 'PICKUP' ? '到店自提' : '配送到家' }}</span>
            </div>
          </div>

          <div v-if="detail.extend?.deliveryType !== 'PICKUP' && detail.extend?.receiverName" class="dp-section">
            <div class="section-title">收货信息</div>
            <div class="dp-addr">
              <div>{{ detail.extend?.receiverName }} {{ detail.extend?.receiverPhone }}</div>
              <div class="muted">{{ detail.extend?.receiverProvince }}{{ detail.extend?.receiverCity }}{{ detail.extend?.receiverDistrict }}{{ detail.extend?.receiverAddress }}</div>
            </div>
          </div>

          <div v-if="detail.extend?.trackingNo" class="dp-section">
            <div class="section-title">物流信息</div>
            <div class="dp-addr">
              <span class="val">{{ detail.extend?.trackingNo }}</span>
              <span v-if="detail.extend?.trackingCompany" class="muted">（{{ detail.extend?.trackingCompany }}）</span>
            </div>
          </div>

          <div class="dp-section">
            <div class="section-title">商品清单</div>
            <div v-for="(it, i) in detail.items" :key="i" class="dp-item">
              <span class="di-name">{{ it.productName }}</span>
              <span class="muted">x{{ it.quantity }}</span>
              <span class="val">¥{{ yuan(it.subtotal) }}</span>
            </div>
          </div>

          <div class="dp-section">
            <div class="dp-row">
              <span class="muted">商品总额</span>
              <span class="val">¥{{ yuan(detail.totalAmount) }}</span>
            </div>
            <div v-if="detail.discountAmount" class="dp-row">
              <span class="muted">优惠</span>
              <span class="neg val">-¥{{ yuan(detail.discountAmount) }}</span>
            </div>
            <div class="dp-row">
              <span class="muted">实付</span>
              <span class="val strong dp-paid">¥{{ yuan(detail.paidAmount ?? detail.totalAmount) }}</span>
            </div>
          </div>

          <div v-if="detail.remark" class="dp-section">
            <div class="section-title">备注</div>
            <div class="dp-remark muted">{{ detail.remark }}</div>
          </div>

          <div class="dp-actions">
            <template v-if="detail.status === 'PENDING'">
              <van-button plain round size="small" class="dp-btn" @click="onCancelOrder(detail)">取消订单</van-button>
              <van-button type="primary" round size="small" color="var(--brand-deep)" class="dp-btn" @click="onPayOrder(detail)">去支付</van-button>
            </template>
            <template v-else-if="detail.status === 'PAID' && (detail.extend?.trackingNo || detail.trackingNo)">
              <van-button plain round size="small" class="dp-btn" @click="onCopyTracking(detail)">查看物流</van-button>
            </template>
            <template v-else-if="detail.status === 'SHIPPED'">
              <van-button type="primary" round size="small" color="var(--brand-deep)" class="dp-btn" @click="onConfirmReceipt(detail)">确认收货</van-button>
            </template>
          </div>
        </template>
      </div>
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { onActivated, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showConfirmDialog, showToast, showSuccessToast } from 'vant'
import { mallApi } from '@/api/h5'
import NavBar from '@/components/NavBar.vue'
import EmptyState from '@/components/EmptyState.vue'
import { fenToYuan, formatDateTime } from '@/utils/format'

const route = useRoute()
const router = useRouter()

interface OrderItem {
  productId: number | string
  productName: string
  unitPrice?: number
  quantity: number
  subtotal: number
}
interface MallOrder {
  id: number | string
  orderNo: string
  totalAmount: number
  paidAmount?: number
  discountAmount?: number
  status: string
  deliveryType?: string
  trackingNo?: string
  trackingCompany?: string
  receiverName?: string
  receiverPhone?: string
  receiverProvince?: string
  receiverCity?: string
  receiverDistrict?: string
  receiverAddress?: string
  remark?: string
  createdAt: string
  items?: OrderItem[]
  extend?: {
    deliveryType?: string
    trackingNo?: string
    trackingCompany?: string
    receiverName?: string
    receiverPhone?: string
    receiverProvince?: string
    receiverCity?: string
    receiverDistrict?: string
    receiverAddress?: string
  }
}

const loading = ref(false)
const finished = ref(false)
const page = ref(1)
const list = ref<MallOrder[]>([])
const status = ref('')
const keyword = ref('')
const highlightId = ref<number | string>('')
const refreshing = ref(false)

const tabs = [
  { label: '全部', value: '' },
  { label: '待付款', value: 'PENDING' },
  { label: '已付款', value: 'PAID' },
  { label: '已发货', value: 'SHIPPED' },
  { label: '已退款', value: 'REFUNDED' },
  { label: '已作废', value: 'VOID' }
]

let pending = false
async function load() {
  if (pending) return
  pending = true
  loading.value = true
  const isFirstPage = page.value === 1
  try {
    const data: any = await mallApi.myOrders({
      status: status.value || undefined,
      keyword: keyword.value.trim() || undefined,
      page: page.value,
      size: 20
    })
    const items = data?.records || data?.list || (Array.isArray(data) ? data : [])
    list.value.push(...items)
    page.value++
    const total = data?.total
    if (items.length < 20 || (total != null && list.value.length >= total)) {
      finished.value = true
    }
    // 高亮刚下单的订单（仅在首页加载时）
    if (isFirstPage && highlightId.value) {
      setTimeout(() => { highlightId.value = '' }, 2000)
    }
  } catch {
    // 加载失败: 不标 finished(避免伪装成"暂无订单")
  } finally {
    pending = false
    loading.value = false
  }
}

function reset() {
  page.value = 1
  list.value = []
  finished.value = false
  loading.value = false
}

function onTab(v: string) {
  status.value = v
  reset()
  load()
}

function onSearch() { reset(); load() }
function onClear() { keyword.value = ''; reset(); load() }

async function onRefresh() {
  reset()
  try {
    await load()
    showToast({ message: '已刷新', position: 'top' })
  } catch {/* 静默 */}
  finally { refreshing.value = false }
}

// 订单详情弹层
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<MallOrder | null>(null)

async function openDetail(o: MallOrder) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    const data: any = await mallApi.orderDetail(o.id)
    detail.value = data || o
  } catch { detail.value = o }
  finally { detailLoading.value = false }
}

function goMall() { router.push('/mall') }

async function onPayOrder(o: MallOrder) {
  try {
    const res: any = await mallApi.payOrder(o.id)
    if (res?.timeStamp && res?.paySign && typeof (window as any).WeixinJSBridge !== 'undefined') {
      ;(window as any).WeixinJSBridge.invoke(
        'getBrandWCPayRequest',
        {
          appId: res.appId,
          timeStamp: res.timeStamp,
          nonceStr: res.nonceStr,
          package: res.package,
          signType: res.signType,
          paySign: res.paySign
        },
        (r: any) => {
          if (r.err_msg === 'get_brand_wcpay_request:ok') {
            showSuccessToast('支付成功')
            detailVisible.value = false
            reset(); load()
          } else {
            showToast('支付未完成')
          }
        }
      )
    } else {
      showToast({ message: '支付已发起', position: 'top' })
      detailVisible.value = false
      reset(); load()
    }
  } catch (e: any) {
    showToast(e?.message || '支付失败')
  }
}

async function onCancelOrder(o: MallOrder) {
  try {
    await showConfirmDialog({ title: '取消订单', message: '确定要取消该订单吗？' })
  } catch { return }
  try {
    await mallApi.cancelOrder(o.id)
    showSuccessToast('已取消')
    detailVisible.value = false
    reset(); load()
  } catch (e: any) {
    showToast(e?.message || '取消失败')
  }
}

async function onConfirmReceipt(o: MallOrder) {
  try {
    await showConfirmDialog({ title: '确认收货', message: '确认已收到商品吗？' })
  } catch { return }
  try {
    await mallApi.confirmOrder(o.id)
    showSuccessToast('已确认收货')
    detailVisible.value = false
    reset(); load()
  } catch (e: any) {
    showToast(e?.message || '操作失败')
  }
}

function onCopyTracking(o: MallOrder) {
  const no = o.extend?.trackingNo || o.trackingNo || ''
  if (!no) { showToast('暂无物流单号'); return }
  try {
    navigator.clipboard.writeText(no)
    showSuccessToast('已复制物流单号：' + no)
  } catch {
    showToast('物流单号：' + no)
  }
}

function statusText(s: string) {
  return ({
    PENDING: '待付款',
    PAID: '已付款',
    SHIPPED: '已发货',
    REFUNDED: '已退款',
    VOID: '已作废'
  } as any)[s] || s || '-'
}
function statusChipClass(s: string) {
  return ({
    PENDING: 'warning',
    PAID: 'success',
    SHIPPED: 'info',
    REFUNDED: 'danger',
    VOID: 'muted'
  } as any)[s] || 'mist'
}

const yuan = fenToYuan
const fmt = formatDateTime

onMounted(() => {
  if (route.query.highlight) highlightId.value = String(route.query.highlight)
})

onActivated(() => {
  reset(); load()
})
</script>

<style scoped>
.tip {
  font-size: 12px; color: var(--muted);
  letter-spacing: 0.04em; margin-bottom: 14px;
  font-family: var(--font-serif); opacity: 0.85;
  padding-left: 2px;
}
.pr-wrap { min-height: 60vh; }

.status-tabs {
  display: flex; gap: 6px;
  margin-bottom: 14px;
  padding: 0 2px;
  overflow-x: auto;
}
.status-tabs::-webkit-scrollbar { display: none; }

/* 搜索框 */
.search-box {
  display: flex; align-items: center; gap: 8px;
  height: 38px;
  padding: 0 14px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: 999px;
  margin-bottom: 12px;
  transition: border-color var(--dur) var(--ease);
}
.search-box:focus-within { border-color: var(--brand); }
.s-ic { color: var(--muted); font-size: 14px; }
.s-input {
  flex: 1; border: none; background: transparent; outline: none;
  font-size: 13px; color: var(--ink);
  font-family: inherit; letter-spacing: 0.02em;
}
.s-input::placeholder { color: var(--muted-2); }
.s-clear {
  width: 18px; height: 18px; border-radius: 50%;
  background: var(--surface-3); color: var(--muted);
  display: flex; align-items: center; justify-content: center;
  font-size: 13px; line-height: 1; cursor: pointer;
}
.tab {
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
  letter-spacing: 0.04em;
}
.tab.active {
  background: var(--brand-deep);
  color: #fff;
  border-color: var(--brand-deep);
}

.loading { display: flex; justify-content: center; padding: 40px 0; }

.order-list { display: flex; flex-direction: column; gap: 12px; }
.order-card {
  padding: 14px 16px;
  cursor: pointer;
  transition: box-shadow var(--dur) var(--ease-out),
              transform var(--dur) var(--ease-out),
              border-color var(--dur) var(--ease-out);
}
.order-card.highlight {
  border-color: var(--brand);
  box-shadow: 0 0 0 3px var(--brand-soft);
}
.o-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.o-no { font-size: 11.5px; color: var(--muted); letter-spacing: 0.04em; }
.chip {
  display: inline-flex; align-items: center;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 11px;
  letter-spacing: 0.04em;
}
.chip-warning { background: var(--warning-soft); color: var(--warning-deep); }
.chip-success { background: var(--success-soft); color: var(--success-deep); }
.chip-info, .chip-mist { background: rgba(138, 142, 133, 0.14); color: var(--muted); }
.chip-danger { background: var(--danger-soft); color: var(--danger-deep); }
.chip-brand { background: var(--brand-soft); color: var(--brand-deep); }
.chip-muted { background: var(--surface-3); color: var(--muted); }

.o-items {
  padding: 8px 0;
  border-top: 1px dashed var(--line);
  border-bottom: 1px dashed var(--line);
}
.o-item { display: flex; align-items: center; gap: 10px; padding: 4px 0; font-size: 13px; }
.o-item-name { flex: 1; color: var(--ink); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.o-item-qty { font-size: 12px; }
.o-item-sub { color: var(--ink-2); font-size: 13px; }
.o-more { font-size: 11.5px; padding: 4px 0; }

.o-tracking {
  display: flex; align-items: center; gap: 6px;
  margin-top: 10px;
  font-size: 12px;
  color: var(--brand-deep);
  background: var(--brand-softer);
  padding: 6px 10px;
  border-radius: var(--r-sm);
}

.o-foot { display: flex; justify-content: space-between; align-items: center; margin-top: 10px; }
.o-time { font-size: 11.5px; }
.o-amount { font-size: 13px; color: var(--muted); display: flex; align-items: baseline; gap: 4px; }
.o-amount .strong { color: var(--brand-deep); font-weight: 600; font-size: 16px; }

.go-btn {
  margin-top: 14px;
  background: var(--brand-deep);
  border-color: var(--brand-deep);
}

/* 订单详情弹层 */
.detail-popup {
  padding: 16px 16px 24px;
  max-height: 80vh;
  overflow-y: auto;
}
.dp-header {
  display: flex; align-items: center; justify-content: space-between;
  padding-bottom: 12px;
  border-bottom: 1px dashed var(--line-2);
  margin-bottom: 8px;
}
.dp-title {
  font-family: var(--font-serif);
  font-size: 15px; font-weight: 500; color: var(--ink);
  letter-spacing: 0.06em;
}
.dp-close { color: var(--muted); cursor: pointer; }
.dp-section {
  padding: 10px 0;
  border-bottom: 1px dashed var(--line);
}
.dp-section:last-child { border-bottom: none; }
.dp-row {
  display: flex; align-items: center; justify-content: space-between;
  padding: 5px 0;
  font-size: 13px;
  color: var(--ink-2);
}
.dp-addr { font-size: 13px; color: var(--ink); line-height: 1.7; }
.dp-item {
  display: flex; align-items: center; gap: 10px;
  padding: 5px 0;
  font-size: 13px;
}
.di-name { flex: 1; color: var(--ink); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.dp-paid { color: var(--brand-deep); font-size: 16px; font-weight: 600; }
.dp-remark { font-size: 12.5px; line-height: 1.7; }

.dp-actions {
  display: flex; justify-content: flex-end; gap: 10px;
  padding-top: 14px;
}
.dp-btn { min-width: 84px; }
</style>
