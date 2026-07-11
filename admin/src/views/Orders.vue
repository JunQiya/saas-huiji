<template>
  <div class="page">
    <div class="page-header is-enhanced">
      <div class="header-left">
        <div class="header-icon"><el-icon><List /></el-icon></div>
        <div class="header-text">
          <h2 class="page-title">订单流水</h2>
          <div class="page-sub">{{ orderSlogan }}</div>
        </div>
      </div>
      <div class="header-actions">
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button :icon="Download" @click="onExport" :disabled="!list.length">导出</el-button>
        <el-button type="primary" :icon="Plus" @click="$router.push('/pos')" class="btn-scale">去收银</el-button>
      </div>
    </div>

    <div class="x-card filter-card">
      <div class="filter-bar">
        <el-input v-model="query.keyword" placeholder="订单号 / 会员" clearable :prefix-icon="Search" />
        <el-select v-model="query.status" placeholder="状态" clearable>
          <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
        <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始" end-placeholder="结束" />
        <el-button @click="load">查询</el-button>
      </div>
      <div class="kpi-strip">
        <div class="kpi-item"><div class="k">今日订单</div><div class="v val">{{ todayCount }}</div></div>
        <div class="kpi-item"><div class="k">今日收入</div><div class="v val">¥ {{ todayAmount }}</div></div>
        <div class="kpi-item"><div class="k">筛选笔数</div><div class="v val">{{ total }}</div></div>
      </div>
    </div>

    <div class="x-card">
      <el-table v-loading="loading" :data="list" stripe size="small" @row-click="openDetail">
        <el-table-column label="订单号" prop="orderNo" min-width="180" />
        <el-table-column label="门店" width="100" prop="storeId" />
        <el-table-column label="会员" width="100" prop="memberId" />
        <el-table-column label="总额" width="120">
          <template #default="{ row }">¥ <span class="val">{{ yuan(row.totalAmount) }}</span></template>
        </el-table-column>
        <el-table-column label="实付" width="120">
          <template #default="{ row }">¥ <span class="val">{{ yuan(row.paidAmount) }}</span></template>
        </el-table-column>
        <el-table-column label="支付方式" width="100">
          <template #default="{ row }">{{ payMethodLabel(row.payMethod) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <span class="dot" :class="statusDot(row.status)" />
            {{ statusLabel(row.status) }}
          </template>
        </el-table-column>
        <el-table-column label="下单时间" width="170">
          <template #default="{ row }">{{ fmtDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <div class="row-actions">
              <el-button link type="primary" :icon="View" @click.stop="openDetail(row)">详情</el-button>
              <el-button v-if="row.status === 'PENDING'" link type="primary" @click.stop="quickPay(row)">收款</el-button>
              <el-button v-if="row.status === 'PAID'" link type="danger" @click.stop="refund(row)">退款</el-button>
              <el-button v-if="row.status === 'PENDING'" link type="warning" @click.stop="voidOrder(row)">作废</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        class="pager"
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        layout="total, prev, pager, next, jumper"
        @current-change="load"
      />
    </div>

    <el-drawer v-model="drawer" title="订单详情" size="420px">
      <div v-if="current" class="detail">
        <div class="row"><span class="k">订单号</span><span class="v">{{ current.orderNo }}</span></div>
        <div class="row"><span class="k">状态</span><span class="v"><span class="dot" :class="statusDot(current.status)" />{{ statusLabel(current.status) }}</span></div>
        <div class="row"><span class="k">门店</span><span class="v">{{ current.storeId || '-' }}</span></div>
        <div class="row"><span class="k">会员</span><span class="v">{{ current.memberId || '-' }}</span></div>
        <div class="row"><span class="k">收银员</span><span class="v">{{ current.cashierId || '-' }}</span></div>
        <div class="row"><span class="k">备注</span><span class="v">{{ current.remark || '-' }}</span></div>
        <div class="divider">商品明细</div>
        <div v-for="it in (current.items || [])" :key="it.id" class="item">
          <span class="it-name">{{ it.productName }}</span>
          <span class="it-qty">x{{ it.quantity }}</span>
          <span class="it-sub val">¥ {{ yuan(it.subtotal) }}</span>
        </div>
        <div class="totals">
          <div class="row"><span class="k">总额</span><span class="v val">¥ {{ yuan(current.totalAmount) }}</span></div>
          <div class="row"><span class="k">优惠</span><span class="v val">- ¥ {{ yuan(current.discountAmount) }}</span></div>
          <div class="row"><span class="k">实付</span><span class="v val">¥ {{ yuan(current.paidAmount) }}</span></div>
        </div>
        <div class="footer-actions">
          <el-button v-if="current.status === 'PENDING'" type="primary" @click="quickPay(current)" class="btn-scale">收款</el-button>
          <el-button v-if="current.status === 'PAID'" type="danger" @click="refund(current)" class="btn-scale">退款</el-button>
          <el-button v-if="current.status === 'PENDING'" type="warning" @click="voidOrder(current)" class="btn-scale">作废</el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { List, Refresh, Plus, Search, Download, User } from '@element-plus/icons-vue'
import { ordersApi, statsApi, storesApi } from '@/api'
import { fenToYuan } from '@/utils/format'
import { exportCsv } from '@/utils/csv'

const orderSlogan = [
  '把每一笔流水，都记成一段值得回看的故事',
  '认真对待每一笔，好生意自然生长',
  '把数字读成故事，把数据写成温度'
][Math.floor(Math.random() * 3)]

const statusOptions = [
  { value: 'PENDING', label: '待支付' },
  { value: 'PAID', label: '已支付' },
  { value: 'REFUNDED', label: '已退款' },
  { value: 'VOID', label: '已作废' }
]
function statusLabel(s: string) { return statusOptions.find(o => o.value === s)?.label || s }
function statusDot(s: string) {
  if (s === 'PAID') return 'success'
  if (s === 'PENDING') return 'warning'
  if (s === 'REFUNDED') return 'info'
  if (s === 'VOID') return 'danger'
  return 'info'
}
function payMethodLabel(m: string) {
  if (!m) return '-'
  return { CASH: '现金', WECHAT: '微信', ALIPAY: '支付宝', BALANCE: '余额', MIXED: '混合' }[m] || m
}
function fmtDate(t: any) { if (!t) return '-'; try { return new Date(t).toLocaleString() } catch { return String(t) } }
function yuan(f: any) { if (f == null) return '0.00'; return Number(fenToYuan(f)).toFixed(2) }

const storeMap = reactive<Record<number, string>>({})
async function loadStores() {
  try {
    const s: any[] = await storesApi.list()
    s.forEach((x: any) => { storeMap[x.id] = x.name })
  } catch {}
}
function storeName(id: any) {
  if (!id) return '-'
  return storeMap[id] || `门店${id}`
}

function onExport() {
  if (!list.value.length) return
  exportCsv(`订单列表-${new Date().toLocaleDateString('zh-CN')}`, list.value, [
    { key: 'orderNo', header: '订单号' },
    { key: 'storeId', header: '门店', format: r => storeName(r.storeId) },
    { key: 'memberId', header: '会员', format: r => r.memberName || r.memberPhone || r.memberId },
    { key: 'totalAmount', header: '订单金额(元)', format: r => Number(fenToYuan(r.totalAmount || 0)).toFixed(2) },
    { key: 'paidAmount', header: '实付金额(元)', format: r => Number(fenToYuan(r.paidAmount || 0)).toFixed(2) },
    { key: 'discountAmount', header: '优惠(元)', format: r => Number(fenToYuan(r.discountAmount || 0)).toFixed(2) },
    { key: 'payMethod', header: '支付方式', format: r => payMethodLabel(r.payMethod) },
    { key: 'status', header: '状态', format: r => statusLabel(r.status) },
    { key: 'createdAt', header: '创建时间', format: r => fmtDate(r.createdAt) }
  ])
  ElMessage.success(`已导出 ${list.value.length} 条订单`)
}

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const query = reactive({ keyword: '', status: '' })
const dateRange = ref<[Date, Date] | null>(null)
const todayCount = ref(0)
const todayAmount = ref('0.00')

const drawer = ref(false)
const current = ref<any>(null)

async function load() {
  loading.value = true
  try {
    const params: any = {
      keyword: query.keyword || undefined,
      status: query.status || undefined,
      page: page.value,
      size: size.value
    }
    if (dateRange.value && dateRange.value.length === 2) {
      params.start = dateRange.value[0].toISOString().slice(0, 10)
      params.end = dateRange.value[1].toISOString().slice(0, 10)
    }
    const data: any = await ordersApi.list(params)
    list.value = data?.records || data?.list || data?.content || []
    total.value = data?.total || data?.totalElements || 0
  } finally {
    loading.value = false
  }
  loadToday()
}

async function loadToday() {
  try {
    const t: any = await statsApi.ordersToday()
    todayCount.value = t.count || 0
    todayAmount.value = Number(fenToYuan(t.amount || 0)).toFixed(2)
  } catch (e) { /* 静默失败 */ }
}

async function openDetail(row: any) {
  const data: any = await ordersApi.detail(row.id)
  current.value = data
  drawer.value = true
}

async function quickPay(row: any) {
  const { value } = await ElMessageBox.prompt('选择支付方式(输入: CASH/WECHAT/ALIPAY/BALANCE)', '收款', {
    inputValue: 'WECHAT',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  })
  await ordersApi.pay(row.id, { payMethod: value.toUpperCase() })
  ElMessage.success('已收款')
  load()
}

async function refund(row: any) {
  await ElMessageBox.confirm(`确认退款订单 ${row.orderNo}?`, '提示', { type: 'warning' })
  await ordersApi.refund(row.id, { reason: '管理员操作' })
  ElMessage.success('已退款')
  load()
}

async function voidOrder(row: any) {
  await ElMessageBox.confirm(`确认作废订单 ${row.orderNo}?`, '提示', { type: 'warning' })
  await ordersApi.void(row.id, { reason: '管理员操作' })
  ElMessage.success('已作废')
  load()
}

onMounted(() => { load(); loadStores() })
</script>

<style scoped>
.filter-card { padding: 12px 16px; margin-bottom: 14px; }
.kpi-strip { display: flex; gap: 18px; padding-top: 6px; }
.kpi-item { display: flex; flex-direction: column; gap: 2px; }
.kpi-item .k { color: var(--muted); font-size: 12px; }
.kpi-item .v { font-size: 18px; color: var(--ink); font-weight: 600; }
.pager { display: flex; justify-content: flex-end; padding: 12px 6px 0; }
.row { display: flex; align-items: center; justify-content: space-between; padding: 6px 0; }
.row .k { color: var(--muted); font-size: 13px; }
.row .v { color: var(--ink); font-size: 13px; }
.divider { color: var(--muted); font-size: 12px; padding: 12px 0 6px; border-top: 1px dashed var(--line); margin-top: 8px; }
.item { display: flex; align-items: center; justify-content: space-between; padding: 4px 0; font-size: 13px; }
.totals { padding-top: 8px; border-top: 1px solid var(--line); margin-top: 6px; }
.footer-actions { display: flex; gap: 8px; justify-content: flex-end; padding-top: 14px; }
</style>
