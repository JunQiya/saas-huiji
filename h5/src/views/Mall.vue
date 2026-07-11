<template>
  <div class="page mall">
    <NavBar title="线上商城" back />

    <!-- 搜索栏 -->
    <div class="search-bar">
      <van-search
        v-model="keyword"
        placeholder="搜索商品名称"
        shape="round"
        :show-action="false"
        @search="onSearch"
      />
    </div>

    <!-- 分类横向滚动 -->
    <div class="cat-bar">
      <div
        v-for="c in categories"
        :key="c.id ?? 'all'"
        class="cat-chip"
        :class="{ active: activeCat === c.id }"
        @click="onCat(c.id)"
      >
        {{ c.name }}
      </div>
    </div>

    <div class="page-tip">把好物放进购物车，把心意交给收件人。</div>

    <!-- 商品网格 -->
    <div v-if="loading" class="loading"><van-loading color="#6f94b8" /></div>
    <EmptyState
      v-else-if="!list.length"
      :title="emptyTitle"
      sub="更多好物正在上架中"
      art="box"
    />

    <div v-else class="grid">
      <div
        v-for="p in list"
        :key="p.id"
        class="card ui-card hoverable"
        @click="openDetail(p)"
      >
        <div class="cover" :class="`tone-${p.category || 'GOODS'}`">
          <van-icon :name="p.category === 'SERVICE' ? 'gem-o' : 'gift-o'" size="32" color="rgba(255,255,255,0.92)" />
          <div v-if="p.stock != null && p.stock <= 5 && p.stock > 0" class="stock-flag">仅剩 {{ p.stock }}</div>
          <div v-else-if="p.stock === 0" class="stock-flag sold-out">已售罄</div>
        </div>
        <div class="info">
          <div class="name">{{ p.name }}</div>
          <div class="desc">{{ p.description || (p.category === 'SERVICE' ? '到店体验服务' : '实物商品') }}</div>
          <div class="meta">
            <div class="price">
              <span class="unit">¥</span><span class="num val">{{ yuan(p.price) }}</span>
            </div>
            <button
              class="add-btn"
              :class="{ disabled: p.stock === 0 }"
              :disabled="p.stock === 0"
              @click.stop="onAddCart(p)"
            >
              <van-icon name="cart-circle-o" size="16" />
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 浮动购物车入口 -->
    <div class="cart-fab" @click="goCart">
      <van-icon name="shopping-cart-o" size="24" color="#fff" />
      <div v-if="cartCount > 0" class="fab-badge val">{{ cartCount }}</div>
    </div>

    <!-- 我的订单入口 -->
    <div class="orders-fab" @click="goOrders">
      <van-icon name="orders-o" size="22" color="var(--brand-deep)" />
    </div>

    <div class="footnote">星河好物 · 一份心意</div>
  </div>
</template>

<script setup lang="ts">
import { computed, onActivated, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { mallApi } from '@/api/h5'
import { useMemberStore } from '@/stores/member'
import NavBar from '@/components/NavBar.vue'
import EmptyState from '@/components/EmptyState.vue'

const router = useRouter()
const memberStore = useMemberStore()
const isLogin = computed(() => memberStore.isLogin)

interface MallProduct {
  id: number | string
  name: string
  category?: string
  cover?: string
  price: number
  stock?: number
  description?: string
}

const loading = ref(false)
const list = ref<MallProduct[]>([])
const keyword = ref('')
const activeCat = ref<number | string>('')
const categories = ref<any[]>([{ id: '', name: '全部' }])

const emptyTitle = computed(() => keyword.value ? `未找到「${keyword.value}」相关商品` : '暂无商品')

async function loadCategories() {
  try {
    const data: any = await mallApi.categories()
    const arr = Array.isArray(data) ? data : (data?.list || [])
    categories.value = [{ id: '', name: '全部' }, ...arr]
  } catch {/* */}
}

async function loadProducts() {
  loading.value = true
  try {
    const data: any = await mallApi.products({
      categoryId: activeCat.value || undefined,
      keyword: keyword.value || undefined,
      page: 1,
      size: 50
    })
    list.value = data?.records || data?.list || (Array.isArray(data) ? data : [])
  } catch { list.value = [] }
  finally { loading.value = false }
}

function onCat(id: number | string) {
  activeCat.value = id
  loadProducts()
}
function onSearch() { loadProducts() }

function openDetail(p: MallProduct) {
  router.push(`/mall/product/${p.id}`)
}

async function onAddCart(p: MallProduct) {
  if (p.stock === 0) { showToast('已售罄'); return }
  if (!isLogin.value) {
    showToast('请先登录')
    setTimeout(() => router.push({ path: '/login', query: { redirect: '/mall' } }), 600)
    return
  }
  try {
    await mallApi.addToCart({ productId: p.id, quantity: 1 })
    showToast({ message: '已加入购物车', position: 'top' })
    loadCartCount()
  } catch {/* */}
}

// 购物车件数
const cartCount = ref(0)
async function loadCartCount() {
  if (!isLogin.value) { cartCount.value = 0; return }
  try {
    const data: any = await mallApi.cartSummary()
    cartCount.value = Number(data?.selectedCount ?? data?.totalCount ?? 0)
  } catch { cartCount.value = 0 }
}

function goCart() {
  if (!isLogin.value) {
    showToast('请先登录')
    setTimeout(() => router.push({ path: '/login', query: { redirect: '/mall/cart' } }), 600)
    return
  }
  router.push('/mall/cart')
}
function goOrders() {
  if (!isLogin.value) {
    showToast('请先登录')
    setTimeout(() => router.push({ path: '/login', query: { redirect: '/mall/orders' } }), 600)
    return
  }
  router.push('/mall/orders')
}

function yuan(f: any) {
  if (f == null) return '0.00'
  return (Number(f) / 100).toFixed(2)
}

onMounted(() => {
  loadCategories()
  loadProducts()
  loadCartCount()
})
// 从详情/购物车返回时刷新件数
onActivated(() => { loadCartCount() })
</script>

<style scoped>
.mall { padding-bottom: 96px; }

.search-bar { padding: 6px 12px 0; }
.search-bar :deep(.van-search) { padding: 0; background: transparent; }
.search-bar :deep(.van-search__content) {
  background: var(--surface);
  border: 1px solid var(--line);
  box-shadow: var(--shadow-xs);
}

/* 分类 */
.cat-bar {
  display: flex; gap: 8px;
  padding: 8px 16px 4px;
  overflow-x: auto;
}
.cat-bar::-webkit-scrollbar { display: none; }
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
  letter-spacing: 0.04em;
}
.cat-chip.active {
  background: var(--brand-deep);
  color: #fff;
  border-color: var(--brand-deep);
}

.page-tip {
  font-size: 12px;
  color: var(--muted);
  letter-spacing: 0.04em;
  margin: 4px 16px 12px;
  font-family: var(--font-serif);
  opacity: 0.85;
  padding-left: 2px;
}

.loading { display: flex; justify-content: center; padding: 40px 0; }

/* 商品网格 */
.grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  padding: 0 16px;
}
.card {
  padding: 0;
  overflow: hidden;
  cursor: pointer;
}
.cover {
  position: relative;
  height: 108px;
  display: flex; align-items: center; justify-content: center;
}
.cover.tone-GOODS { background: linear-gradient(135deg, #6f94b8, #4a6a87); }
.cover.tone-SERVICE { background: linear-gradient(135deg, #c89d96, #a8736a); }
.cover img { width: 100%; height: 100%; object-fit: cover; }
.stock-flag {
  position: absolute; top: 8px; right: 8px;
  font-size: 10px;
  background: rgba(192, 133, 116, 0.92);
  color: #fff;
  padding: 2px 8px;
  border-radius: 999px;
  letter-spacing: 0.02em;
}
.stock-flag.sold-out { background: rgba(31, 29, 24, 0.7); }
.info { padding: 10px 12px 12px; }
.name {
  font-size: 13px; color: var(--ink); font-weight: 500;
  line-height: 1.4; min-height: 36px;
  letter-spacing: 0.02em;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.desc {
  font-size: 11px; color: var(--muted);
  margin: 3px 0 8px;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.meta { display: flex; align-items: center; justify-content: space-between; }
.price { color: var(--brand-deep); }
.price .unit { font-size: 11px; margin-right: 1px; opacity: 0.85; }
.price .num { font-size: 17px; font-weight: 600; }
.add-btn {
  width: 28px; height: 28px;
  border-radius: 50%;
  border: none;
  background: var(--brand-deep);
  color: #fff;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer;
  transition: transform var(--dur) var(--ease);
}
.add-btn:active { transform: scale(0.92); }
.add-btn.disabled { background: var(--muted-2); cursor: not-allowed; }

/* 浮动入口 */
.cart-fab, .orders-fab {
  position: fixed;
  right: calc(50% - 230px);
  width: 48px; height: 48px;
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  z-index: 50;
  box-shadow: 0 4px 14px rgba(31, 29, 24, 0.18);
  cursor: pointer;
  transition: transform var(--dur) var(--ease);
}
.cart-fab {
  bottom: 28px;
  background: linear-gradient(135deg, #6f94b8, var(--brand-deep));
}
.orders-fab {
  bottom: 88px;
  background: var(--surface);
  border: 1px solid var(--line);
}
.cart-fab:active, .orders-fab:active { transform: scale(0.92); }
.fab-badge {
  position: absolute;
  top: -4px; right: -4px;
  min-width: 18px; height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  background: var(--danger);
  color: #fff;
  font-size: 11px;
  display: flex; align-items: center; justify-content: center;
  border: 2px solid var(--page-bg);
}

@media (max-width: 480px) {
  .cart-fab, .orders-fab { right: 16px; }
}
</style>
