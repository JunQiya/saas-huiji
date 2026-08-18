<template>
  <div class="page mall-cart">
    <NavBar title="购物车" back />

    <div v-if="loading" class="loading"><van-loading color="#6f94b8" /></div>

    <EmptyState
      v-else-if="!list.length"
      title="购物车空空如也"
      sub="去商城挑几件好物放进购物车吧"
      art="box"
    >
      <van-button round type="primary" size="small" class="go-btn" @click="goMall">去逛逛</van-button>
    </EmptyState>

    <template v-else>
      <div class="page-padding">
        <div class="tip">已选 {{ selectedCount }} 件，合计 ¥{{ yuan(totalAmount) }}</div>

        <div class="cart-list">
          <div v-for="item in list" :key="item.id" class="cart-card ui-card">
            <van-checkbox
              :model-value="!!item.selected"
              @update:model-value="(v: any) => onToggleSelected(item, v)"
              class="cc-check"
            />
            <div class="cc-cover" :class="`tone-${item.category || 'GOODS'}`" @click="openProduct(item)">
              <van-icon :name="item.category === 'SERVICE' ? 'gem-o' : 'gift-o'" size="22" color="rgba(255,255,255,0.9)" />
            </div>
            <div class="cc-info" @click="openProduct(item)">
              <div class="cc-name">{{ item.productName || item.name }}</div>
              <div class="cc-price">¥<span class="val">{{ yuan(item.unitPrice ?? item.price) }}</span></div>
              <div class="cc-stepper">
                <van-stepper
                  :model-value="item.quantity"
                  :min="0"
                  :max="99"
                  integer
                  button-size="24"
                  @change="(v: any) => onQtyChange(item, v)"
                />
              </div>
            </div>
            <div class="cc-del" @click="onRemove(item)">
              <van-icon name="delete-o" size="18" />
            </div>
          </div>
        </div>

        <div class="actions">
          <van-button plain size="small" @click="onClear">清空购物车</van-button>
        </div>
      </div>

      <!-- 底部结算栏 -->
      <van-submit-bar
        :price="totalAmount"
        button-text="去结算"
        :disabled="selectedCount === 0"
        button-color="var(--brand-deep)"
        @submit="onCheckout"
      >
        <van-checkbox :model-value="allSelected" @update:model-value="onToggleAll">全选</van-checkbox>
        <template #tip>
          <span v-if="selectedCount > 0" class="bar-tip">已选 {{ selectedCount }} 件</span>
          <span v-else class="bar-tip muted">请选择要结算的商品</span>
        </template>
      </van-submit-bar>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onActivated, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
import { mallApi } from '@/api/h5'
import NavBar from '@/components/NavBar.vue'
import EmptyState from '@/components/EmptyState.vue'
import { fenToYuan } from '@/utils/format'

const router = useRouter()

interface CartItem {
  id: number | string
  productId: number | string
  productName?: string
  name?: string
  category?: string
  unitPrice?: number
  price?: number
  quantity: number
  selected?: boolean
  stock?: number
}

const loading = ref(false)
const list = ref<CartItem[]>([])

const selectedCount = computed(() =>
  list.value.filter(i => i.selected).reduce((s, i) => s + i.quantity, 0)
)
const totalAmount = computed(() =>
  list.value
    .filter(i => i.selected)
    .reduce((s, i) => s + (Number(i.unitPrice ?? i.price) || 0) * i.quantity, 0)
)
const allSelected = computed({
  get: () => list.value.length > 0 && list.value.every(i => i.selected),
  set: () => {/* 由 onToggleAll 处理 */}
})

async function load() {
  loading.value = true
  try {
    const data: any = await mallApi.cart()
    list.value = (data?.list || data || []).map((i: any) => ({ ...i }))
  } catch { list.value = [] }
  finally { loading.value = false }
}

async function onQtyChange(item: CartItem, v: any) {
  const qty = Number(v) || 0
  if (qty <= 0) {
    // 减到 0 视为移除, 先确认避免误删
    await onRemove(item, false)
    return
  }
  try {
    await mallApi.updateCart(item.id, { quantity: qty })
    item.quantity = qty
  } catch (e: any) {
    showToast(e?.message || '更新数量失败')
  }
}

async function onToggleSelected(item: CartItem, v: any) {
  const selected = !!v
  try {
    await mallApi.updateCart(item.id, { selected })
    item.selected = selected
  } catch (e: any) {
    showToast(e?.message || '操作失败')
  }
}

async function onToggleAll(v: any) {
  const selected = !!v
  try {
    await Promise.all(
      list.value
        .filter(i => !!i.selected !== selected)
        .map(i => mallApi.updateCart(i.id, { selected }))
    )
    list.value.forEach(i => { i.selected = selected })
  } catch (e: any) {
    showToast(e?.message || '操作失败')
    load()
  }
}

async function onRemove(item: CartItem, silent = false) {
  if (!silent) {
    try {
      await showConfirmDialog({ title: '提示', message: `从购物车移除「${item.productName || item.name}」？` })
    } catch { return }
  }
  try {
    await mallApi.removeFromCart(item.productId)
    list.value = list.value.filter(i => i.id !== item.id)
    if (!silent) showToast('已移除')
  } catch (e: any) {
    showToast(e?.message || '移除失败')
  }
}

async function onClear() {
  if (!list.value.length) return
  try {
    await showConfirmDialog({ title: '提示', message: '确认清空购物车？' })
  } catch { return }
  try {
    await mallApi.clearCart()
    list.value = []
    showToast('已清空')
  } catch (e: any) {
    showToast(e?.message || '清空失败')
  }
}

function onCheckout() {
  const selected = list.value.filter(i => i.selected)
  if (!selected.length) { showToast('请选择要结算的商品'); return }
  const itemIds = selected.map(i => i.id).join(',')
  router.push({ path: '/mall/checkout', query: { itemIds } })
}

function openProduct(item: CartItem) {
  router.push(`/mall/product/${item.productId}`)
}
function goMall() { router.push('/mall') }

const yuan = fenToYuan

onMounted(load)
onActivated(load)
</script>

<style scoped>
.mall-cart { padding-bottom: 96px; }

.tip {
  font-size: 12px; color: var(--muted);
  letter-spacing: 0.04em; margin-bottom: 12px;
  font-family: var(--font-serif); opacity: 0.85;
  padding-left: 2px;
}

.loading { display: flex; justify-content: center; padding: 60px 0; }

.cart-list { display: flex; flex-direction: column; gap: 10px; }
.cart-card {
  display: flex; align-items: center; gap: 10px;
  padding: 12px;
}
.cc-check { flex-shrink: 0; }
.cc-cover {
  width: 64px; height: 64px;
  border-radius: var(--r);
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
  cursor: pointer;
}
.cc-cover.tone-GOODS { background: var(--brand); }
.cc-cover.tone-SERVICE { background: var(--accent-rose); }
.cc-info { flex: 1; min-width: 0; cursor: pointer; }
.cc-name {
  font-size: 13.5px; color: var(--ink); font-weight: 500;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
  letter-spacing: 0.02em;
}
.cc-price {
  font-size: 14px; color: var(--brand-deep);
  font-weight: 600; margin: 4px 0;
}
.cc-stepper { margin-top: 2px; }
.cc-del {
  color: var(--muted);
  cursor: pointer;
  padding: 8px;
  transition: color var(--dur) var(--ease);
}
.cc-del:active { color: var(--danger); }

.actions {
  display: flex; justify-content: center;
  margin-top: 16px;
}

.go-btn {
  margin-top: 14px;
  background: var(--brand-deep);
  border-color: var(--brand-deep);
}

.bar-tip { font-size: 12px; color: var(--ink-2); }
.bar-tip.muted { color: var(--muted); }

:deep(.van-submit-bar) {
  position: fixed;
  left: 0; right: 0; bottom: 0;
  max-width: 480px;
  margin: 0 auto;
}
:deep(.van-submit-bar__bar) {
  border-top: 1px dashed var(--line-2);
}
</style>
