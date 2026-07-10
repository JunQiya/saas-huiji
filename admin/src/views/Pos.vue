<template>
  <div class="page pos-page">
    <div class="page-header is-enhanced">
      <div class="header-left">
        <div class="header-icon"><el-icon><Money /></el-icon></div>
        <div class="header-text">
          <h2 class="page-title">收银台</h2>
          <div class="page-sub">{{ posSlogan }}</div>
        </div>
      </div>
      <div class="header-actions">
        <el-button :icon="Refresh" @click="loadAll">刷新</el-button>
        <el-button :icon="Printer" @click="printReceipt" class="btn-scale">打印小票</el-button>
      </div>
    </div>

    <div class="pos-body">
      <!-- 左侧商品 -->
      <div class="x-card left-pane">
        <div class="category-bar">
          <div v-for="c in categories" :key="c.value" class="cat-chip" :class="{ active: cat === c.value }" @click="cat = c.value">
            {{ c.label }}
          </div>
        </div>
        <el-input v-model="keyword" placeholder="按名称搜索" :prefix-icon="Search" clearable />
        <div v-loading="loadingProducts" class="product-grid">
          <div v-for="p in filteredProducts" :key="p.id" class="p-item x-card hoverable" @click="addToCart(p)">
            <div class="p-name">{{ p.name }}</div>
            <div class="p-meta">
              <span class="price val">¥{{ yuan(p.price) }}</span>
              <span v-if="p.category === 'GOODS'" class="stock">库存 {{ p.stock ?? 0 }}</span>
            </div>
          </div>
          <div v-if="filteredProducts.length === 0" class="empty-state">暂无商品</div>
        </div>
      </div>

      <!-- 右侧购物车 -->
      <div class="x-card right-pane">
        <div class="member-block">
          <div class="m-label">会员</div>
          <el-input v-model="memberKeyword" placeholder="手机号 / 姓名" clearable :prefix-icon="User" @keyup.enter="searchMember" />
          <el-button type="primary" plain @click="searchMember">查询</el-button>
        </div>
        <div v-if="member" class="member-card">
          <div class="m-row">
            <span class="m-name">{{ member.name }}</span>
            <span class="m-phone">{{ member.phone || '-' }}</span>
          </div>
          <div class="m-row">
            <span class="muted">储值余额</span>
            <span class="balance val">¥ {{ yuan(member.balance) }}</span>
          </div>
          <div class="m-row coupons">
            <el-select v-model="selectedCoupon" placeholder="选用券" clearable size="small" style="width: 200px">
              <el-option v-for="c in memberCoupons" :key="c.id" :label="`${c.couponName || ''} ¥${yuan(c.amount)}`" :value="c.code" />
            </el-select>
            <span class="muted">共 {{ memberCoupons.length }} 张可用</span>
          </div>
        </div>

        <div class="cart-title">购物车 ({{ cart.length }})</div>
        <div class="cart-list">
          <div v-for="(c, idx) in cart" :key="c.productId" class="cart-item">
            <div class="ci-name">{{ c.name }}</div>
            <el-input-number v-model="c.quantity" :min="1" size="small" style="width: 110px" />
            <div class="ci-sub val">¥ {{ yuan(c.subtotal) }}</div>
            <el-button link type="danger" :icon="Delete" @click="cart.splice(idx, 1)" />
          </div>
          <div v-if="cart.length === 0" class="empty-state">点击左侧商品加入购物车</div>
        </div>

        <div class="total-bar">
          <div class="t-row"><span>商品金额</span><span class="val">¥ {{ yuan(subtotalAmount) }}</span></div>
          <div class="t-row">
            <span>优惠</span>
            <span>
              <el-input-number v-model="discountYuan" :min="0" :max="subtotalAmountYuan" :precision="2" :step="1" size="small" style="width: 140px" />
            </span>
          </div>
          <div class="t-row total"><span>应付</span><span class="val">¥ {{ yuan(payable) }}</span></div>
        </div>

        <div class="pay-block">
          <el-radio-group v-model="payMethod">
            <el-radio-button value="CASH">现金</el-radio-button>
            <el-radio-button value="WECHAT">微信</el-radio-button>
            <el-radio-button value="ALIPAY">支付宝</el-radio-button>
            <el-radio-button value="BALANCE" :disabled="!member">余额</el-radio-button>
          </el-radio-group>
        </div>
        <div class="footer">
          <el-button @click="clearCart">清空</el-button>
          <el-button type="primary" :loading="submitting" :disabled="cart.length === 0" @click="checkout" class="btn-scale">
            结算 ¥{{ yuan(payable) }}
          </el-button>
        </div>
      </div>
    </div>

    <!-- 小票区 -->
    <div class="receipt-print">
      <div v-if="lastReceipt" class="receipt">
        <div class="r-store">{{ storeName }}</div>
        <div class="r-line">订单号: {{ lastReceipt.orderNo }}</div>
        <div class="r-line">时间: {{ fmtDate(lastReceipt.createdAt) }}</div>
        <div class="r-sep"></div>
        <div v-for="it in (lastReceipt.items || [])" :key="it.id" class="r-item">
          <span>{{ it.productName }} x{{ it.quantity }}</span>
          <span>¥{{ yuan(it.subtotal) }}</span>
        </div>
        <div class="r-sep"></div>
        <div class="r-line total"><span>合计</span><span>¥{{ yuan(lastReceipt.totalAmount) }}</span></div>
        <div class="r-line">优惠: -¥{{ yuan(lastReceipt.discountAmount) }}</div>
        <div class="r-line">实付: ¥{{ yuan(lastReceipt.paidAmount) }}</div>
        <div class="r-line">支付: {{ payMethodLabel(lastReceipt.payMethod) }}</div>
        <div class="r-thanks">— 星河会记 谢谢惠顾 —</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Search, User, Delete, Printer, Money } from '@element-plus/icons-vue'
import { productsApi, ordersApi, membersApi, settingsPlanApi } from '@/api'
import { fenToYuan, yuanToFen } from '@/utils/format'

const posSlogan = [
  '指尖轻点，是一次有温度的往来',
  '把每一次结账，都做成一束轻快的小事',
  '收下的是钱，递出的是心意'
][Math.floor(Math.random() * 3)]

const categories = [
  { label: '全部', value: '' },
  { label: '服务', value: 'SERVICE' },
  { label: '商品', value: 'GOODS' }
]
const cat = ref('')
const keyword = ref('')
const loadingProducts = ref(false)
const products = ref<any[]>([])

const memberKeyword = ref('')
const member = ref<any>(null)
const memberCoupons = ref<any[]>([])
const selectedCoupon = ref<string | undefined>(undefined)

const cart = ref<any[]>([])
const discountYuan = ref(0)
const payMethod = ref<'CASH' | 'WECHAT' | 'ALIPAY' | 'BALANCE'>('WECHAT')
const submitting = ref(false)

const storeName = ref('星河·会记')

const lastReceipt = ref<any>(null)

const filteredProducts = computed(() => {
  let list = products.value
  if (cat.value) list = list.filter(p => p.category === cat.value)
  if (keyword.value) {
    const k = keyword.value.toLowerCase()
    list = list.filter(p => (p.name || '').toLowerCase().includes(k))
  }
  return list
})

const subtotalAmount = computed(() => cart.value.reduce((s, c) => s + (c.subtotal || 0), 0))
const subtotalAmountYuan = computed(() => Number(fenToYuan(subtotalAmount.value)))
const discountFen = computed(() => yuanToFen(discountYuan.value || 0))
const payable = computed(() => Math.max(0, subtotalAmount.value - discountFen.value))

function yuan(f: any) { if (f == null) return '0.00'; return Number(fenToYuan(f)).toFixed(2) }
function payMethodLabel(m: string) {
  return ({ CASH: '现金', WECHAT: '微信', ALIPAY: '支付宝', BALANCE: '余额', MIXED: '混合' } as any)[m] || m
}
function fmtDate(t: any) { if (!t) return '-'; try { return new Date(t).toLocaleString() } catch { return String(t) } }

async function loadAll() {
  loadingProducts.value = true
  try {
    const data: any = await productsApi.active()
    products.value = Array.isArray(data) ? data : []
  } finally {
    loadingProducts.value = false
  }
  try {
    const cur: any = await settingsPlanApi.currentStore()
    if (cur?.name) storeName.value = cur.name
  } catch {}
}

function addToCart(p: any) {
  const exist = cart.value.find(c => c.productId === p.id)
  if (exist) {
    exist.quantity += 1
    exist.subtotal = (exist.unitPrice || 0) * exist.quantity
  } else {
    cart.value.push({
      productId: p.id,
      name: p.name,
      unitPrice: p.price || 0,
      quantity: 1,
      subtotal: (p.price || 0) * 1
    })
  }
}

function clearCart() {
  cart.value = []
  discountYuan.value = 0
  selectedCoupon.value = undefined
}

async function searchMember() {
  if (!memberKeyword.value) { member.value = null; memberCoupons.value = []; return }
  const data: any = await membersApi.list({ keyword: memberKeyword.value, page: 1, size: 5 })
  const records = data?.records || data?.list || data?.content || []
  if (records.length === 0) { ElMessage.warning('未找到会员'); return }
  member.value = records[0]
  try {
    memberCoupons.value = await membersApi.coupons(member.value.id) || []
  } catch { memberCoupons.value = [] }
}

async function checkout() {
  if (cart.value.length === 0) return
  if (payMethod.value === 'BALANCE' && !member.value) {
    ElMessage.warning('余额支付需先选择会员')
    return
  }
  submitting.value = true
  try {
    const payload: any = {
      memberId: member.value?.id,
      items: cart.value.map(c => ({ productId: c.productId, quantity: c.quantity })),
      discountAmount: discountFen.value,
      couponCode: selectedCoupon.value,
      payMethod: payMethod.value,
      remark: '收银台结算'
    }
    const order: any = await ordersApi.create(payload)
    ElMessage.success(`结算成功: ${order.orderNo}`)
    lastReceipt.value = order
    clearCart()
  } finally {
    submitting.value = false
  }
}

function printReceipt() {
  if (!lastReceipt.value) { ElMessage.warning('暂无可打印的小票'); return }
  window.print()
}

onMounted(loadAll)
</script>

<style scoped>
.pos-body { display: grid; grid-template-columns: minmax(0, 1fr) 420px; gap: 14px; align-items: start; }
.left-pane, .right-pane { padding: 14px; }
.category-bar { display: flex; gap: 6px; margin-bottom: 10px; }
.cat-chip { padding: 6px 12px; border: 1px solid var(--line); border-radius: 999px; cursor: pointer; font-size: 12px; color: var(--ink-2); }
.cat-chip.active { background: var(--primary-action-soft); color: var(--primary-action); border-color: var(--primary-action); }
.product-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(140px, 1fr)); gap: 10px; margin-top: 12px; }
.p-item { padding: 10px 12px; cursor: pointer; }
.p-name { font-size: 13px; color: var(--ink); font-weight: 500; line-height: 1.4; }
.p-meta { display: flex; align-items: baseline; justify-content: space-between; margin-top: 4px; }
.p-meta .price { color: var(--primary-action); font-weight: 600; }
.p-meta .stock { color: var(--muted); font-size: 12px; }
.right-pane { display: flex; flex-direction: column; }
.member-block { display: flex; align-items: center; gap: 8px; }
.member-block .m-label { color: var(--muted); font-size: 12px; width: 36px; }
.member-card { margin-top: 8px; padding: 10px 12px; background: #faf9f6; border-radius: 10px; display: flex; flex-direction: column; gap: 4px; }
.m-row { display: flex; align-items: center; justify-content: space-between; }
.m-name { font-weight: 600; color: var(--ink); }
.m-phone { color: var(--muted); font-size: 12px; }
.balance { color: var(--primary-action); font-weight: 600; }
.muted { color: var(--muted); font-size: 12px; }
.cart-title { margin-top: 14px; font-weight: 600; color: var(--ink); }
.cart-list { margin-top: 6px; flex: 1; min-height: 100px; max-height: 280px; overflow: auto; }
.cart-item { display: grid; grid-template-columns: 1fr 110px 90px 30px; align-items: center; gap: 6px; padding: 6px 0; border-bottom: 1px dashed var(--line); }
.ci-name { font-size: 13px; color: var(--ink); }
.ci-sub { color: var(--ink-2); font-size: 13px; text-align: right; }
.total-bar { margin-top: 10px; padding-top: 8px; border-top: 1px solid var(--line); }
.t-row { display: flex; align-items: center; justify-content: space-between; padding: 4px 0; font-size: 13px; color: var(--ink-2); }
.t-row.total { font-size: 16px; font-weight: 600; color: var(--primary-action); }
.pay-block { margin-top: 10px; }
.footer { display: flex; gap: 8px; justify-content: flex-end; margin-top: 10px; }

.receipt-print { display: none; }
@media print {
  .pos-page > *:not(.receipt-print) { display: none !important; }
  .receipt-print { display: block; }
  .receipt { font-family: monospace; padding: 20px; }
  .receipt .r-store { text-align: center; font-weight: 600; margin-bottom: 8px; }
  .receipt .r-line { display: flex; justify-content: space-between; padding: 2px 0; }
  .receipt .r-sep { border-top: 1px dashed #999; margin: 4px 0; }
  .receipt .r-item { display: flex; justify-content: space-between; }
  .receipt .total { font-weight: 600; }
  .receipt .r-thanks { text-align: center; margin-top: 12px; font-size: 12px; }
}
</style>
