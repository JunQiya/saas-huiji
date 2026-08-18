<template>
  <div class="page my-orders">
    <NavBar title="我的订单" back />
    <van-pull-refresh v-model="refreshing" class="pr-wrap" @refresh="onRefresh">
    <div class="page-padding">
      <div class="tip">每张订单都是一次到访的注脚。</div>

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

      <div class="status-tabs">
        <div v-for="t in tabs" :key="t.value"
             class="tab" :class="{ active: status === t.value }"
             @click="onTab(t.value)">
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
        <EmptyState v-else-if="!list.length && finished" title="暂无该状态订单" sub="到店消费后会自动出现在这里" art="box" />
        <div v-else class="order-list">
          <div v-for="o in list" :key="o.id" class="order-card ui-card hoverable" @click="open(o)">
            <div class="o-head">
              <span class="o-no">订单号 {{ o.orderNo }}</span>
              <div class="chip" :class="`chip-${statusClass(o.status)}`">{{ statusLabel(o.status) }}</div>
            </div>
            <div v-if="o.items?.length" class="o-items">
              <div class="o-item" v-for="(it, i) in o.items.slice(0, 2)" :key="i">
                <span class="o-item-name">{{ it.productName }}</span>
                <span class="o-item-qty">x{{ it.quantity }}</span>
                <span class="o-item-sub val">¥{{ yuan(it.subtotal) }}</span>
              </div>
              <div v-if="o.items.length > 2" class="o-more">还有 {{ o.items.length - 2 }} 项...</div>
            </div>
            <div class="o-foot">
              <div class="o-time">{{ fmt(o.createdAt) }}</div>
              <div class="o-amount">实付 <span class="val strong">¥{{ yuan(o.paidAmount) }}</span></div>
            </div>
          </div>
        </div>
      </van-list>
    </div>
    </van-pull-refresh>
  </div>
</template>

<script setup lang="ts">
import { onActivated, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { h5Api, type OrderInfo as Order } from '@/api/h5'
import NavBar from '@/components/NavBar.vue'
import EmptyState from '@/components/EmptyState.vue'
import { fenToYuan, formatDateTime } from '@/utils/format'

const router = useRouter()
const loading = ref(false)
const finished = ref(false)
const page = ref(1)
const list = ref<Order[]>([])
const status = ref('')
const keyword = ref('')
const refreshing = ref(false)
const tabs = [
  { label: '全部', value: '' },
  { label: '待支付', value: 'PENDING' },
  { label: '已支付', value: 'PAID' },
  { label: '已退款', value: 'REFUNDED' },
  { label: '已作废', value: 'VOID' }
]

let pending = false
async function load() {
  if (pending) return
  pending = true
  loading.value = true
  try {
    const r: any = await h5Api.myOrders(status.value || undefined, keyword.value.trim() || undefined, page.value, 20)
    const items = Array.isArray(r) ? r : (r?.list || r?.records || [])
    list.value.push(...items)
    page.value++
    const total = r?.total
    if (items.length < 20 || (total != null && list.value.length >= total)) {
      finished.value = true
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

function onTab(v: string) { status.value = v; reset(); load() }
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
function open(o: any) { router.push(`/order/${o.id}`) }
function statusLabel(s: string) {
  return ({ PENDING: '待支付', PAID: '已支付', REFUNDED: '已退款', VOID: '已作废' } as any)[s] || s
}
function statusClass(s: string) {
  return ({ PENDING: 'warning', PAID: 'success', REFUNDED: 'info', VOID: 'danger' } as any)[s] || 'mist'
}
const yuan = fenToYuan
const fmt = formatDateTime

onMounted(load)
onActivated(() => { reset(); load() })
</script>

<style scoped>
.tip {
  font-size: 12px; color: var(--muted);
  letter-spacing: 0.04em; margin-bottom: 14px;
  font-family: var(--font-serif); opacity: 0.85;
  padding-left: 2px;
}

.status-tabs { display: flex; gap: 6px; margin-bottom: 14px; padding: 0 2px; overflow-x: auto; }
.status-tabs::-webkit-scrollbar { display: none; }
.pr-wrap { min-height: 60vh; }

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
.tab.active { background: var(--brand-deep); color: #fff; border-color: var(--brand-deep); }

.loading { display: flex; justify-content: center; padding: 40px 0; }
.order-list { display: flex; flex-direction: column; gap: 12px; }
.order-card { padding: 14px 16px; cursor: pointer; }
.o-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.o-no { font-size: 11.5px; color: var(--muted); font-family: 'SF Mono', monospace; letter-spacing: 0.04em; }
.chip { display: inline-flex; align-items: center; padding: 2px 8px; border-radius: 999px; font-size: 11px; letter-spacing: 0.04em; }
.chip-warning { background: var(--warning-soft); color: #8a6f3a; }
.chip-success { background: var(--success-soft); color: #5b7868; }
.chip-info { background: rgba(138, 142, 133, 0.14); color: var(--muted); }
.chip-danger { background: var(--danger-soft); color: #8a4d3f; }
.chip-mist { background: var(--surface-3); color: var(--ink-2); }
.o-items { padding: 8px 0; border-top: 1px dashed var(--line); border-bottom: 1px dashed var(--line); }
.o-item { display: flex; align-items: center; gap: 10px; padding: 4px 0; font-size: 13px; }
.o-item-name { flex: 1; color: var(--ink); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.o-item-qty { color: var(--muted); font-size: 12px; }
.o-item-sub { color: var(--ink-2); font-size: 13px; }
.o-more { font-size: 11.5px; color: var(--muted); padding: 4px 0; }
.o-foot { display: flex; justify-content: space-between; align-items: center; margin-top: 10px; }
.o-time { font-size: 11.5px; color: var(--muted); }
.o-amount { font-size: 13px; color: var(--muted); }
.o-amount .strong { color: var(--brand-deep); font-weight: 600; font-size: 16px; margin-left: 4px; }
</style>
