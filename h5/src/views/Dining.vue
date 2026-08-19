<template>
  <div class="page dining">
    <NavBar title="扫码点餐" back />

    <!-- 桌台信息 -->
    <div v-if="table" class="table-card">
      <div class="tc-content">
        <div class="tc-top">
          <div class="tc-brand">
            <div class="brand-mark"><van-icon name="location-o" size="16" color="#fff" /></div>
            <div>
              <div class="tc-name">{{ table?.name || '桌台' }}</div>
              <div class="tc-sub">{{ table?.area || '未分区' }} · {{ table?.seats || 0 }} 座</div>
            </div>
          </div>
          <span class="chip" :class="table?.status === 'OCCUPIED' ? 'warning' : 'success'">
            {{ table?.status === 'OCCUPIED' ? '占用中' : '空闲' }}
          </span>
        </div>
      </div>
    </div>
    <!-- 门店信息（无桌台时） -->
    <div v-else-if="selectedStoreId" class="table-card">
      <div class="tc-content">
        <div class="tc-top">
          <div class="tc-brand">
            <div class="brand-mark"><van-icon name="shop-o" size="16" color="#fff" /></div>
            <div>
              <div class="tc-name">{{ selectedStore?.name || '门店' }}</div>
              <div class="tc-sub">{{ selectedStore?.address || '地址未填' }}</div>
            </div>
          </div>
          <span v-if="stores.length > 1" class="chip" @click="resetStore">更换</span>
        </div>
      </div>
    </div>

    <!-- 门店选择（无 tableId 且未选门店） -->
    <div v-if="needStorePicker" class="store-picker">
      <div class="sp-head">
        <div class="sp-title">选择门店</div>
        <div class="sp-sub">请选择就餐门店后查看菜单</div>
      </div>
      <div v-if="storesLoading" class="loading"><van-loading /></div>
      <EmptyState v-else-if="!stores.length" title="暂无门店" sub="门店正陆续上线中" art="leaf" />
      <div v-else class="sp-list">
        <div
          v-for="s in stores"
          :key="s.id"
          class="sp-card ui-card hoverable"
          @click="selectStore(s.id)"
        >
          <div class="sp-head-row">
            <div class="sp-cover"><van-icon name="shop-o" size="20" color="#fff" /></div>
            <div class="sp-meta">
              <div class="sp-name">{{ s.name }}</div>
              <div class="sp-addr">{{ s.address || '地址未填' }}</div>
            </div>
            <van-icon name="arrow" size="14" color="var(--muted)" />
          </div>
          <div v-if="s.businessHours || s.phone" class="sp-foot">
            <span v-if="s.businessHours" class="sp-tag"><van-icon name="clock-o" /> {{ s.businessHours }}</span>
            <span v-if="s.phone" class="sp-tag"><van-icon name="phone-o" /> {{ s.phone }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 菜单区域 -->
    <div v-if="activeStoreId" class="menu-wrap">
      <!-- 左侧分类 -->
      <div class="cat-nav">
        <div
          v-for="(c, i) in menu"
          :key="c.id ?? i"
          class="cat-item"
          :class="{ active: activeCat === i }"
          @click="activeCat = i"
        >
          {{ c.name || `分类${i + 1}` }}
        </div>
        <div v-if="!menu.length && !loading" class="cat-empty">无分类</div>
      </div>

      <!-- 右侧商品 -->
      <div class="prod-list">
        <div v-if="loading" class="loading"><van-loading /></div>
        <EmptyState v-else-if="!currentProducts.length" title="暂无菜品" sub="该分类下还没有商品" art="box" />
        <div v-else>
          <div class="prod-cat-title">{{ currentCat?.name }}</div>
          <div v-for="p in currentProducts" :key="p.id" class="prod-card">
            <div class="pc-cover" :class="`tone-${(p.category || 'GOODS').toLowerCase()}`">
              <van-icon :name="p.category === 'SERVICE' ? 'gem-o' : 'goods-collect-o'" size="22" color="rgba(255,255,255,0.92)" />
            </div>
            <div class="pc-info">
              <div class="pc-name">{{ p.name }}</div>
              <div class="pc-desc">{{ p.description || (p.category === 'SERVICE' ? '到店体验' : '店内好物') }}</div>
              <div class="pc-bottom">
                <div class="pc-price">
                  <span class="num val">{{ (Number(p.price || 0) / 100).toFixed(2) }}</span>
                  <span class="unit">元</span>
                </div>
                <div class="pc-stepper">
                  <button
                    v-if="qty(p.id) > 0"
                    class="step-btn minus"
                    @click="changeQty(p.id, -1)"
                  >−</button>
                  <span v-if="qty(p.id) > 0" class="step-num val">{{ qty(p.id) }}</span>
                  <button class="step-btn plus" @click="changeQty(p.id, 1)">+</button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部购物车栏 -->
    <div v-if="totalCount > 0" class="cart-bar safe-bottom">
      <div class="cb-icon" @click="showCart = true">
        <van-icon name="shopping-cart-o" size="24" color="#fff" />
        <span class="cb-badge val">{{ totalCount }}</span>
      </div>
      <div class="cb-info">
        <div class="cb-total val">¥{{ (totalPrice / 100).toFixed(2) }}</div>
        <div class="cb-count">共 {{ totalCount }} 件</div>
      </div>
      <button class="cb-btn" @click="openCheckout">去结算</button>
    </div>

    <!-- 购物车明细 -->
    <van-popup v-model:show="showCart" position="bottom" :style="{ maxHeight: '60%' }" round>
      <div class="cart-popup">
        <div class="cp-head">
          <span class="cp-title">已选菜品</span>
          <span class="cp-clear" @click="clearCart">清空</span>
        </div>
        <div class="cp-list">
          <div v-for="it in cartList" :key="it.id" class="cp-item">
            <div class="ci-name">{{ it.name }}</div>
            <div class="ci-right">
              <span class="ci-price val">¥{{ (it.price * it.qty / 100).toFixed(2) }}</span>
              <div class="pc-stepper">
                <button class="step-btn minus" @click="changeQty(it.id, -1)">−</button>
                <span class="step-num val">{{ it.qty }}</span>
                <button class="step-btn plus" @click="changeQty(it.id, 1)">+</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </van-popup>

    <!-- 结算确认 -->
    <van-dialog
      v-model:show="checkoutVisible"
      title="确认订单"
      show-cancel-button
      :confirm-button-text="submitting ? '提交中…' : '提交订单'"
      :confirm-button-disabled="submitting"
      @confirm="submitOrder"
    >
      <div class="checkout-body">
        <div class="ck-row">
          <span class="ck-label">就餐方式</span>
          <div class="ck-types">
            <div
              class="ck-type"
              :class="{ active: orderType === 'DINE_IN' }"
              @click="orderType = 'DINE_IN'"
            >堂食</div>
            <div
              class="ck-type"
              :class="{ active: orderType === 'TAKEOUT' }"
              @click="orderType = 'TAKEOUT'"
            >外带</div>
          </div>
        </div>
        <div class="ck-summary">
          <div class="ck-s-row"><span>桌台</span><span>{{ table?.name || '-' }}</span></div>
          <div class="ck-s-row"><span>菜品</span><span>{{ totalCount }} 件</span></div>
          <div class="ck-s-row"><span>合计</span><span class="val strong">¥{{ (totalPrice / 100).toFixed(2) }}</span></div>
        </div>
        <van-field
          v-model="remark"
          type="textarea"
          placeholder="口味偏好、忌口等备注（选填）"
          rows="2"
          autosize
          class="ck-remark"
        />
      </div>
    </van-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onActivated, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { diningApi, h5Api } from '@/api/h5'
import { useMemberStore } from '@/stores/member'
import NavBar from '@/components/NavBar.vue'
import EmptyState from '@/components/EmptyState.vue'

defineOptions({ name: 'Dining' })

const route = useRoute()
const router = useRouter()
const memberStore = useMemberStore()

const tableId = computed(() => route.query.tableId as string || route.params.tableId as string || '')
const table = ref<any>(null)
const menu = ref<any[]>([])
const loading = ref(false)
const activeCat = ref(0)

// 门店选择（无 tableId 时）
const stores = ref<any[]>([])
const storesLoading = ref(false)
const selectedStoreId = ref<string | number>('')
const selectedStore = computed(() => stores.value.find(s => String(s.id) === String(selectedStoreId.value)) || null)
const needStorePicker = computed(() => !tableId.value && !selectedStoreId.value)
const activeStoreId = computed(() => table.value?.storeId || selectedStoreId.value || '')

// 购物车：productId -> { id, name, price, qty }
const cart = ref<Record<string, { id: string | number; name: string; price: number; qty: number }>>({})

const currentCat = computed(() => menu.value[activeCat.value] || null)
const currentProducts = computed(() => currentCat.value?.products || [])

const cartList = computed(() => Object.values(cart.value).filter(it => it.qty > 0))
const totalCount = computed(() => cartList.value.reduce((s, it) => s + it.qty, 0))
const totalPrice = computed(() => cartList.value.reduce((s, it) => s + it.price * it.qty, 0))

function qty(id: string | number) {
  return cart.value[id]?.qty || 0
}
function changeQty(id: string | number, delta: number) {
  const p = findProduct(id)
  if (!p) return
  const cur = cart.value[id]?.qty || 0
  const next = Math.max(0, cur + delta)
  // 库存上限
  const stock = p.stock != null ? Number(p.stock) : null
  if (stock != null && next > stock) {
    showToast(`库存仅 ${stock}`)
    return
  }
  if (next === 0) {
    delete cart.value[id]
  } else {
    cart.value[id] = {
      id,
      name: p.name,
      price: Number(p.price || 0),
      qty: next
    }
  }
  // 触发响应式
  cart.value = { ...cart.value }
}
function findProduct(id: string | number) {
  for (const c of menu.value) {
    const p = (c.products || []).find((x: any) => String(x.id) === String(id))
    if (p) return p
  }
  return null
}
function clearCart() {
  cart.value = {}
}

// 购物车弹窗
const showCart = ref(false)

// 结算
const checkoutVisible = ref(false)
const submitting = ref(false)
const orderType = ref<'DINE_IN' | 'TAKEOUT'>('DINE_IN')
const remark = ref('')

function openCheckout() {
  if (!totalCount.value) { showToast('请先选择菜品'); return }
  if (!memberStore.isLogin) {
    showToast('请先登录后点餐')
    const redirect = tableId.value ? `/dining?tableId=${tableId.value}` : '/dining'
    setTimeout(() => {
      router.push({ path: '/login', query: { redirect } })
    }, 600)
    return
  }
  remark.value = ''
  orderType.value = 'DINE_IN'
  checkoutVisible.value = true
}

async function submitOrder() {
  const storeId = table.value?.storeId || selectedStoreId.value
  if (!storeId) { showToast('请选择门店'); return }
  if (tableId.value && !table.value) { showToast('桌台信息缺失'); return }
  submitting.value = true
  try {
    const res: any = await diningApi.order({
      tableId: table.value?.id || tableId.value,
      storeId,
      orderType: orderType.value,
      items: cartList.value.map(it => ({
        productId: it.id,
        quantity: it.qty,
        remark: ''
      })),
      remark: remark.value || undefined
    })
    showToast({ type: 'success', message: '点餐成功' })
    clearCart()
    checkoutVisible.value = false
    // 跳转订单详情
    const orderId = res?.order?.id || res?.id || res?.orderId || (typeof res === 'number' ? res : null)
    if (orderId) {
      setTimeout(() => router.replace(`/order/${orderId}`), 500)
    }
  } catch (e: any) {
    showToast(e?.message || '点餐失败，请稍后再试')
  } finally {
    submitting.value = false
  }
}

async function loadTable() {
  if (!tableId.value) return
  try {
    table.value = await diningApi.table(tableId.value)
  } catch {
    table.value = null
  }
}
async function loadMenu() {
  const storeId = table.value?.storeId || selectedStoreId.value
  if (!storeId) return
  loading.value = true
  try {
    const data = await diningApi.menu(storeId)
    menu.value = Array.isArray(data) ? data : []
    activeCat.value = 0
  } catch {
    menu.value = []
  } finally {
    loading.value = false
  }
}
async function loadStores() {
  if (tableId.value) return // 有 tableId 走桌台流程
  storesLoading.value = true
  try {
    const data = await h5Api.stores()
    stores.value = Array.isArray(data) ? data : []
    if (stores.value.length === 1) {
      // 仅一家门店，自动选中
      selectedStoreId.value = stores.value[0].id
      await loadMenu()
    }
  } catch {
    stores.value = []
  } finally {
    storesLoading.value = false
  }
}
async function selectStore(id: string | number) {
  selectedStoreId.value = id
  activeCat.value = 0
  cart.value = {}
  await loadMenu()
}
function resetStore() {
  selectedStoreId.value = ''
  menu.value = []
  cart.value = {}
  activeCat.value = 0
  loadStores()
}

async function initPage() {
  if (tableId.value) {
    await loadTable()
    if (table.value?.storeId) {
      await loadMenu()
    }
  } else {
    await loadStores()
  }
}

onMounted(() => { initPage() })

let firstActivated = true
onActivated(() => {
  if (firstActivated) { firstActivated = false; return } // 首次由 onMounted 处理
  initPage()
})
</script>

<style scoped>
.dining { padding-bottom: 80px; }

/* 桌台信息卡 */
.table-card {
  position: relative;
  margin: 12px 16px 12px;
  border-radius: var(--r-lg);
  overflow: hidden;
  background: var(--brand);
  color: #fff;
}
.tc-content { position: relative; z-index: 1; padding: 14px 18px; }
.tc-top { display: flex; align-items: center; justify-content: space-between; }
.tc-brand { display: flex; align-items: center; gap: 10px; }
.brand-mark {
  width: 30px; height: 30px; border-radius: 8px;
  background: rgba(255,255,255,0.22);
  display: flex; align-items: center; justify-content: center;
}
.tc-name { font-size: 15px; font-weight: 600; letter-spacing: 0.04em; }
.tc-sub { font-size: 11px; opacity: 0.85; margin-top: 2px; letter-spacing: 0.04em; }
.table-card .chip { background: rgba(255,255,255,0.22); color: #fff; border-color: transparent; cursor: pointer; }

/* 门店选择 */
.store-picker {
  margin: 12px 16px;
}
.sp-head { padding: 4px 2px 12px; }
.sp-title {
  font-family: var(--font-serif);
  font-size: 14px; font-weight: 500; color: var(--ink);
  letter-spacing: 0.06em;
}
.sp-sub {
  font-size: 11.5px; color: var(--muted);
  margin-top: 4px; letter-spacing: 0.04em;
}
.sp-list { display: flex; flex-direction: column; gap: 10px; }
.sp-card {
  padding: 12px 14px;
  cursor: pointer;
  transition: transform var(--dur) var(--ease);
}
.sp-card:active { transform: scale(0.98); }
.sp-head-row { display: flex; align-items: center; gap: 10px; }
.sp-cover {
  width: 36px; height: 36px;
  border-radius: 10px;
  background: var(--brand);
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.sp-meta { flex: 1; min-width: 0; }
.sp-name {
  font-size: 14px; font-weight: 600; color: var(--ink);
  letter-spacing: 0.02em;
}
.sp-addr {
  font-size: 11.5px; color: var(--muted);
  margin-top: 3px;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.sp-foot {
  display: flex; flex-wrap: wrap; gap: 10px;
  margin-top: 10px; padding-top: 10px;
  border-top: 1px dashed var(--line);
}
.sp-tag {
  display: inline-flex; align-items: center; gap: 4px;
  font-size: 11px; color: var(--ink-2);
  letter-spacing: 0.02em;
}
.sp-tag .van-icon { color: var(--muted); font-size: 12px; }

/* 菜单区域 */
.menu-wrap {
  display: flex;
  margin: 0 16px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-lg);
  overflow: hidden;
  min-height: 360px;
}
.cat-nav {
  width: 84px;
  flex-shrink: 0;
  background: var(--surface-2);
  border-right: 1px solid var(--line);
  overflow-y: auto;
}
.cat-item {
  padding: 14px 8px;
  font-size: 12.5px;
  color: var(--ink-2);
  text-align: center;
  cursor: pointer;
  position: relative;
  font-family: var(--font-serif);
  letter-spacing: 0.02em;
  transition: all var(--dur) var(--ease);
  border-bottom: 1px solid var(--line-soft);
}
.cat-item.active {
  background: var(--surface);
  color: var(--brand-deep);
  font-weight: 500;
}
.cat-item.active::before {
  content: '';
  position: absolute;
  left: 0; top: 50%;
  transform: translateY(-50%);
  width: 3px; height: 18px;
  background: var(--brand);
  border-radius: 0 2px 2px 0;
}
.cat-empty {
  padding: 30px 8px;
  text-align: center;
  font-size: 11.5px;
  color: var(--muted-2);
}

.prod-list {
  flex: 1;
  padding: 0 12px;
  overflow-y: auto;
  min-width: 0;
}
.prod-cat-title {
  font-family: var(--font-serif);
  font-size: 13px; font-weight: 500;
  color: var(--ink);
  letter-spacing: 0.06em;
  padding: 12px 0 8px;
  border-bottom: 1px dashed var(--line);
  margin-bottom: 8px;
}
.prod-card {
  display: flex; gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid var(--line-soft);
}
.prod-card:last-child { border-bottom: none; }
.pc-cover {
  width: 56px; height: 56px;
  border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.pc-cover.tone-goods { background: var(--brand); }
.pc-cover.tone-service { background: var(--accent-rose); }
.pc-info { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.pc-name {
  font-size: 13px; font-weight: 500; color: var(--ink);
  line-height: 1.4; letter-spacing: 0.02em;
}
.pc-desc {
  font-size: 11px; color: var(--muted);
  margin: 2px 0;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.pc-bottom {
  display: flex; align-items: center; justify-content: space-between;
  margin-top: auto;
}
.pc-price { color: var(--brand-deep); }
.pc-price .num { font-size: 15px; font-weight: 600; }
.pc-price .unit { font-size: 10px; margin-left: 1px; opacity: 0.85; }

/* 步进器 */
.pc-stepper { display: flex; align-items: center; gap: 6px; }
.step-btn {
  width: 24px; height: 24px;
  border-radius: 50%;
  border: none;
  font-size: 16px;
  line-height: 1;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer;
  font-family: inherit;
  transition: transform var(--dur) var(--ease);
}
.step-btn.plus { background: var(--brand-deep); color: #fff; }
.step-btn.minus { background: var(--surface-3); color: var(--ink-2); border: 1px solid var(--line-2); }
.step-btn:active { transform: scale(0.88); }
.step-num { font-size: 13px; color: var(--ink); min-width: 16px; text-align: center; }

.loading { display: flex; justify-content: center; padding: 40px 0; }

/* 底部购物车栏 */
.cart-bar {
  position: fixed;
  left: 0; right: 0; bottom: 0;
  max-width: 480px;
  margin: 0 auto;
  background: rgba(31, 29, 24, 0.92);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  display: flex; align-items: center;
  padding: 10px 16px;
  z-index: 50;
  border-radius: var(--r-lg) var(--r-lg) 0 0;
}
.cb-icon {
  position: relative;
  width: 44px; height: 44px;
  border-radius: 50%;
  background: var(--brand-deep);
  display: flex; align-items: center; justify-content: center;
  cursor: pointer;
  margin-top: -16px;
  border: 3px solid rgba(255,255,255,0.15);
}
.cb-badge {
  position: absolute;
  top: -4px; right: -4px;
  min-width: 18px; height: 18px;
  padding: 0 4px;
  border-radius: 999px;
  background: var(--danger);
  color: #fff;
  font-size: 11px;
  display: flex; align-items: center; justify-content: center;
}
.cb-info { flex: 1; margin-left: 12px; color: #fff; }
.cb-total { font-size: 17px; font-weight: 600; letter-spacing: 0.02em; }
.cb-count { font-size: 11px; opacity: 0.7; margin-top: 1px; }
.cb-btn {
  height: 36px; padding: 0 22px;
  border: none;
  border-radius: 999px;
  background: var(--brand);
  color: #fff;
  font-size: 13.5px;
  font-weight: 500;
  letter-spacing: 0.04em;
  cursor: pointer;
  font-family: inherit;
  transition: transform var(--dur) var(--ease);
}
.cb-btn:active { transform: scale(0.96); }

/* 购物车弹窗 */
.cart-popup { padding: 4px 0 12px; }
.cp-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 16px 8px;
  border-bottom: 1px dashed var(--line);
}
.cp-title {
  font-family: var(--font-serif);
  font-size: 14px; font-weight: 500; color: var(--ink);
  letter-spacing: 0.06em;
}
.cp-clear { font-size: 12px; color: var(--muted); cursor: pointer; }
.cp-clear:active { color: var(--danger); }
.cp-list { padding: 4px 16px; }
.cp-item {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid var(--line-soft);
}
.cp-item:last-child { border-bottom: none; }
.ci-name { font-size: 13.5px; color: var(--ink); flex: 1; min-width: 0; }
.ci-right { display: flex; align-items: center; gap: 12px; }
.ci-price { font-size: 13px; color: var(--brand-deep); font-weight: 500; }

/* 结算弹窗 */
.checkout-body { padding: 4px 16px 8px; }
.ck-row {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px dashed var(--line);
}
.ck-label { font-size: 13px; color: var(--ink-2); }
.ck-types { display: flex; gap: 8px; }
.ck-type {
  padding: 5px 16px;
  font-size: 12.5px;
  border-radius: 999px;
  border: 1px solid var(--line-2);
  color: var(--ink-2);
  cursor: pointer;
  transition: all var(--dur) var(--ease);
}
.ck-type.active {
  background: var(--brand-deep);
  color: #fff;
  border-color: var(--brand-deep);
}
.ck-summary { padding: 10px 0; }
.ck-s-row {
  display: flex; align-items: center; justify-content: space-between;
  padding: 5px 0;
  font-size: 13px;
  color: var(--ink-2);
}
.ck-s-row .val { color: var(--brand-deep); font-weight: 600; }
.ck-s-row .strong { font-size: 14.5px; }
.ck-remark {
  margin-top: 6px;
  border: 1px solid var(--line-2);
  border-radius: var(--r-md);
  overflow: hidden;
}
</style>
