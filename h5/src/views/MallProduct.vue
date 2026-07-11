<template>
  <div class="page mall-product">
    <NavBar title="商品详情" back />

    <div v-if="loading" class="loading"><van-loading color="#6f94b8" /></div>
    <EmptyState v-else-if="!product" title="商品不存在或已下架" sub="返回商城再逛逛" art="box" />

    <template v-else>
      <!-- 商品主图 -->
      <div class="hero" :class="`tone-${product.category || 'GOODS'}`">
        <van-icon
          :name="product.category === 'SERVICE' ? 'gem-o' : 'gift-o'"
          size="64"
          color="rgba(255,255,255,0.92)"
        />
        <div v-if="product.stock === 0" class="sold-out-mask">已售罄</div>
      </div>

      <!-- 价格与名称 -->
      <div class="info-card ui-card">
        <div class="price-row">
          <div class="price">
            <span class="unit">¥</span><span class="num val">{{ yuan(product.price) }}</span>
          </div>
          <div v-if="product.stock != null" class="stock muted">库存 {{ product.stock }}</div>
        </div>
        <div class="name">{{ product.name }}</div>
        <div v-if="product.description" class="desc">{{ product.description }}</div>
      </div>

      <!-- 数量选择 -->
      <div class="qty-card ui-card">
        <div class="qty-label">购买数量</div>
        <van-stepper
          v-model="quantity"
          :min="1"
          :max="maxQty"
          :disable-input="false"
          integer
          button-size="28"
          @change="onQtyChange"
        />
      </div>

      <!-- 商品参数（占位） -->
      <div class="param-card ui-card">
        <div class="section-title">商品参数</div>
        <div class="param-row">
          <span class="muted">类型</span>
          <span>{{ product.category === 'SERVICE' ? '到店服务' : '实物商品' }}</span>
        </div>
        <div class="param-row">
          <span class="muted">编号</span>
          <span class="val">{{ product.id }}</span>
        </div>
      </div>

      <div class="footnote">每一件好物，都值得被认真挑选</div>

      <!-- 底部操作栏 -->
      <van-action-bar class="action-bar">
        <div class="cart-icon" @click="goCart">
          <van-icon name="shopping-cart-o" size="22" />
          <div v-if="cartCount > 0" class="ci-badge val">{{ cartCount }}</div>
        </div>
        <van-action-bar-icon icon="chat-o" text="客服" @click="onService" />
        <van-action-bar-button
          type="warning"
          text="加入购物车"
          :disabled="product.stock === 0 || adding"
          @click="onAddCart"
        />
        <van-action-bar-button
          type="danger"
          text="立即购买"
          :disabled="product.stock === 0 || buying"
          @click="onBuyNow"
        />
      </van-action-bar>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onActivated, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { mallApi } from '@/api/h5'
import { useMemberStore } from '@/stores/member'
import NavBar from '@/components/NavBar.vue'
import EmptyState from '@/components/EmptyState.vue'

const route = useRoute()
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
const product = ref<MallProduct | null>(null)
const quantity = ref(1)
const adding = ref(false)
const buying = ref(false)

const maxQty = computed(() => {
  if (product.value?.stock == null) return 99
  return Math.max(1, product.value.stock)
})

function productId() {
  return route.params.id as string
}

async function load() {
  const id = productId()
  if (!id) return
  loading.value = true
  try {
    const data: any = await mallApi.productDetail(id)
    product.value = data || null
  } catch { product.value = null }
  finally { loading.value = false }
}

function onQtyChange(v: any) {
  const n = Number(v) || 1
  if (product.value?.stock != null && n > product.value.stock) {
    showToast('超出库存')
    quantity.value = product.value.stock
  }
}

function ensureLogin(redirect?: string) {
  if (!isLogin.value) {
    showToast('请先登录')
    setTimeout(() => router.push({ path: '/login', query: { redirect: redirect || '/mall' } }), 600)
    return false
  }
  return true
}

async function onAddCart() {
  if (!product.value) return
  if (!ensureLogin('/mall')) return
  adding.value = true
  try {
    await mallApi.addToCart({ productId: product.value.id, quantity: quantity.value })
    showToast({ message: '已加入购物车', position: 'top' })
    loadCartCount()
  } catch {/* */}
  finally { adding.value = false }
}

async function onBuyNow() {
  if (!product.value) return
  if (!ensureLogin(`/mall/product/${product.value.id}`)) return
  buying.value = true
  try {
    // 立即购买：先加入购物车并选中，然后跳转到结算页（带单品标识）
    await mallApi.addToCart({ productId: product.value.id, quantity: quantity.value })
    router.push({
      path: '/mall/checkout',
      query: { buyNow: '1', productId: String(product.value.id), quantity: String(quantity.value) }
    })
  } catch {/* */}
  finally { buying.value = false }
}

function onService() {
  router.push('/help')
}

const cartCount = ref(0)
async function loadCartCount() {
  if (!isLogin.value) { cartCount.value = 0; return }
  try {
    const data: any = await mallApi.cartSummary()
    cartCount.value = Number(data?.selectedCount ?? data?.totalCount ?? 0)
  } catch { cartCount.value = 0 }
}

function goCart() {
  if (!ensureLogin('/mall/cart')) return
  router.push('/mall/cart')
}

function yuan(f: any) {
  if (f == null) return '0.00'
  return (Number(f) / 100).toFixed(2)
}

watch(() => route.params.id, () => {
  if (route.name === 'MallProduct') {
    quantity.value = 1
    load()
  }
})

onMounted(() => { load(); loadCartCount() })
// 返回时刷新商品详情（库存可能变化）和购物车件数
onActivated(() => { load(); loadCartCount() })
</script>

<style scoped>
.mall-product { padding-bottom: 88px; }

.loading { display: flex; justify-content: center; padding: 60px 0; }

.hero {
  position: relative;
  height: 280px;
  display: flex; align-items: center; justify-content: center;
  margin: 0 16px;
  border-radius: var(--r-lg);
  overflow: hidden;
}
.hero.tone-GOODS { background: var(--brand); }
.hero.tone-SERVICE { background: var(--accent-rose); }
.hero img { width: 100%; height: 100%; object-fit: cover; }
.sold-out-mask {
  position: absolute;
  inset: 0;
  background: rgba(31, 29, 24, 0.55);
  color: #fff;
  font-family: var(--font-serif);
  font-size: 22px;
  letter-spacing: 0.16em;
  display: flex; align-items: center; justify-content: center;
}

.info-card { margin: 12px 16px; padding: 16px; }
.price-row { display: flex; align-items: baseline; justify-content: space-between; }
.price { color: var(--brand-deep); }
.price .unit { font-size: 14px; opacity: 0.85; margin-right: 2px; }
.price .num { font-size: 26px; font-weight: 600; }
.stock { font-size: 12px; }
.name {
  font-size: 16px; color: var(--ink); font-weight: 500;
  margin-top: 10px; letter-spacing: 0.04em;
  font-family: var(--font-serif);
}
.desc {
  font-size: 12.5px; color: var(--ink-3);
  margin-top: 8px; line-height: 1.7;
  white-space: pre-wrap;
}

.qty-card {
  margin: 12px 16px;
  padding: 14px 16px;
  display: flex; align-items: center; justify-content: space-between;
}
.qty-label { font-size: 14px; color: var(--ink-2); letter-spacing: 0.04em; }

.param-card { margin: 12px 16px; padding: 14px 16px; }
.param-row {
  display: flex; justify-content: space-between;
  padding: 6px 0;
  font-size: 13px;
  border-bottom: 1px dashed var(--line);
}
.param-row:last-child { border-bottom: none; }

.action-bar {
  position: fixed;
  left: 0; right: 0; bottom: 0;
  max-width: 480px;
  margin: 0 auto;
  border-top: 1px dashed var(--line-2);
}
.cart-icon {
  position: relative;
  width: 48px;
  display: flex; align-items: center; justify-content: center;
  color: var(--ink-2);
  cursor: pointer;
}
.ci-badge {
  position: absolute;
  top: 2px; right: 2px;
  min-width: 16px; height: 16px;
  padding: 0 4px;
  border-radius: 8px;
  background: var(--danger);
  color: #fff;
  font-size: 10px;
  display: flex; align-items: center; justify-content: center;
}
</style>
