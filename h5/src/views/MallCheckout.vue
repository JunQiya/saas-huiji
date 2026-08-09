<template>
  <div class="page mall-checkout">
    <NavBar title="确认订单" back />

    <div v-if="loading" class="loading"><van-loading color="#6f94b8" /></div>

    <template v-else>
      <div class="page-padding">
        <!-- 配送方式 -->
        <div class="section-title">配送方式</div>
        <div class="delivery-tabs ui-card">
          <div
            class="dt-item"
            :class="{ active: form.deliveryType === 'DELIVERY' }"
            @click="form.deliveryType = 'DELIVERY'"
          >
            <van-icon name="logistics" size="20" />
            <span>配送到家</span>
          </div>
          <div
            class="dt-item"
            :class="{ active: form.deliveryType === 'PICKUP' }"
            @click="form.deliveryType = 'PICKUP'"
          >
            <van-icon name="shop-o" size="20" />
            <span>到店自提</span>
          </div>
        </div>

        <!-- 收货地址（配送） -->
        <div v-if="form.deliveryType === 'DELIVERY'" class="address-card ui-card">
          <div class="section-title">收货信息</div>
          <van-cell-group :border="false" inset>
            <van-field v-model="form.receiverName" label="收件人" placeholder="请输入姓名" input-align="right" />
            <van-field v-model="form.receiverPhone" label="手机号" type="tel" placeholder="请输入手机号" input-align="right" />
            <van-field v-model="form.receiverProvince" label="省" placeholder="如：浙江省" input-align="right" />
            <van-field v-model="form.receiverCity" label="市" placeholder="如：杭州市" input-align="right" />
            <van-field v-model="form.receiverDistrict" label="区/县" placeholder="如：西湖区" input-align="right" />
            <van-field
              v-model="form.receiverAddress"
              label="详细地址"
              type="textarea"
              :rows="2"
              placeholder="街道、门牌号等"
              input-align="right"
            />
          </van-cell-group>
        </div>

        <!-- 自提门店（自提） -->
        <div v-else class="address-card ui-card">
          <div class="section-title">选择自提门店</div>
          <div v-if="storeLoading" class="loading-mini"><van-loading size="20" /></div>
          <div v-else class="store-pick">
            <div
              v-for="s in stores"
              :key="s.id"
              class="store-item"
              :class="{ active: form.storeId == s.id }"
              @click="form.storeId = s.id as any"
            >
              <div class="si-name">{{ s.name }}</div>
              <div class="si-addr muted">{{ s.address || '地址未填写' }}</div>
              <van-icon v-if="form.storeId == s.id" name="success" color="var(--brand-deep)" size="16" />
            </div>
            <div v-if="!stores.length" class="empty-mini muted">暂无可选门店</div>
          </div>
        </div>

        <!-- 商品清单 -->
        <div class="section-title">商品清单</div>
        <div class="goods-card ui-card">
          <div v-for="item in items" :key="item.id" class="gc-item">
            <div class="gc-cover" :class="`tone-${item.category || 'GOODS'}`">
              <van-icon :name="item.category === 'SERVICE' ? 'gem-o' : 'gift-o'" size="18" color="rgba(255,255,255,0.9)" />
            </div>
            <div class="gc-info">
              <div class="gc-name">{{ item.productName || item.name }}</div>
              <div class="gc-meta">
                <span class="gc-price">¥{{ yuan(item.unitPrice ?? item.price) }}</span>
                <span class="gc-qty muted">x{{ item.quantity }}</span>
              </div>
            </div>
            <div class="gc-sub val">¥{{ yuan((Number(item.unitPrice ?? item.price) || 0) * item.quantity) }}</div>
          </div>
          <div v-if="!items.length" class="empty-mini muted">暂无结算商品</div>
        </div>

        <!-- 优惠券 -->
        <div class="section-title">优惠券</div>
        <div class="coupon-card ui-card" @click="openCouponSheet">
          <div class="coup-left">
            <van-icon name="ticket-o" size="18" color="var(--brand-deep)" />
            <span>选择优惠券</span>
          </div>
          <div class="coup-right">
            <template v-if="selectedCoupon">
              <span class="coup-name">{{ selectedCoupon.couponName }}</span>
              <span class="coup-discount">-¥{{ yuan(couponDiscount) }}</span>
            </template>
            <template v-else-if="coupons.length">
              <span class="muted">{{ coupons.length }} 张可用</span>
            </template>
            <template v-else>
              <span class="muted">暂无可用</span>
            </template>
            <van-icon name="arrow" size="14" />
          </div>
        </div>

        <!-- 备注 -->
        <div class="section-title">订单备注</div>
        <div class="remark-card ui-card">
          <van-field
            v-model="form.remark"
            placeholder="选填，给商家留言（如配送时间、发票等）"
            type="textarea"
            :rows="2"
            maxlength="100"
            show-word-limit
            :border="false"
          />
        </div>

        <!-- 支付方式 -->
        <div class="section-title">支付方式</div>
        <div class="pay-card ui-card">
          <van-radio-group v-model="form.payMethod" direction="horizontal">
            <van-radio name="WECHAT">微信支付</van-radio>
            <van-radio name="BALANCE">余额支付</van-radio>
          </van-radio-group>
        </div>

        <div class="footnote">提交订单即同意商城服务条款</div>
      </div>

      <!-- 底部提交栏 -->
      <van-submit-bar
        :price="payable"
        button-text="提交订单"
        button-color="var(--brand-deep)"
        :disabled="submitting || !items.length"
        @submit="onSubmit"
      >
        <span class="bar-tip">
          <span v-if="couponDiscount > 0" class="bar-discount">已优惠 -¥{{ yuan(couponDiscount) }}</span>
          合计 <span class="val strong">¥{{ yuan(payable) }}</span>
        </span>
      </van-submit-bar>

      <!-- 优惠券选择弹窗 -->
      <van-popup v-model:show="couponVisible" position="bottom" round :style="{ maxHeight: '70%' }">
        <div class="coupon-sheet">
          <div class="cs-header">
            <span class="cs-title">选择优惠券</span>
            <van-icon name="cross" size="18" class="cs-close" @click="couponVisible = false" />
          </div>
          <div v-if="couponLoading" class="loading"><van-loading color="#6f94b8" /></div>
          <div v-else-if="!coupons.length" class="empty-mini muted">暂无可用优惠券</div>
          <div v-else class="cs-list">
            <div
              v-for="c in coupons"
              :key="c.id"
              class="cs-item"
              :class="{ active: form.couponId == c.id, disabled: !isCouponUsable(c) }"
              @click="onSelectCoupon(c)"
            >
              <div class="cs-val">
                <span class="cs-amount">¥{{ yuan(c.faceValue) }}</span>
                <span v-if="c.threshold" class="cs-thr muted">满¥{{ yuan(c.threshold) }}可用</span>
              </div>
              <div class="cs-info">
                <div class="cs-name">{{ c.couponName }}</div>
                <div class="cs-date muted">{{ c.expireAt ? '至 ' + fmtDate(c.expireAt) : '长期有效' }}</div>
              </div>
              <van-icon v-if="form.couponId == c.id" name="success" color="var(--brand-deep)" size="16" />
            </div>
          </div>
        </div>
      </van-popup>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { mallApi, h5Api, type CouponRecord } from '@/api/h5'
import NavBar from '@/components/NavBar.vue'
import { fenToYuan, formatDate } from '@/utils/format'

const route = useRoute()
const router = useRouter()

interface CheckItem {
  id: number | string
  productId: number | string
  productName?: string
  name?: string
  category?: string
  unitPrice?: number
  price?: number
  quantity: number
}

const loading = ref(false)
const storeLoading = ref(false)
const submitting = ref(false)
const items = ref<CheckItem[]>([])
const stores = ref<any[]>([])

const couponVisible = ref(false)
const couponLoading = ref(false)
const coupons = ref<CouponRecord[]>([])

const form = reactive<any>({
  deliveryType: 'DELIVERY',
  receiverName: '',
  receiverPhone: '',
  receiverProvince: '',
  receiverCity: '',
  receiverDistrict: '',
  receiverAddress: '',
  storeId: null,
  remark: '',
  couponId: null,
  payMethod: 'WECHAT'
})

const totalAmount = computed(() =>
  items.value.reduce((s, i) => s + (Number(i.unitPrice ?? i.price) || 0) * i.quantity, 0)
)

const selectedCoupon = computed(() =>
  coupons.value.find(c => c.id == form.couponId) || null
)

const couponDiscount = computed(() => {
  const c = selectedCoupon.value
  if (!c) return 0
  const total = totalAmount.value
  if (c.threshold && total < c.threshold) return 0
  return Math.min(c.faceValue || 0, total)
})

const payable = computed(() => Math.max(0, totalAmount.value - couponDiscount.value))

async function loadCartItems() {
  loading.value = true
  try {
    const data: any = await mallApi.cart()
    const all: any[] = data?.list || data || []
    const itemIds = String(route.query.itemIds || '').split(',').filter(Boolean)
    if (itemIds.length) {
      // 仅结算选中的购物车项
      items.value = all.filter(i => itemIds.includes(String(i.id)))
    } else if (route.query.buyNow === '1' && route.query.productId) {
      const pid = Number(route.query.productId)
      items.value = all.filter((it: any) => it.productId === pid)
      if (!items.value.length) {
        showToast('商品添加失败，请重试')
        setTimeout(() => router.back(), 600)
      }
      return
    } else {
      items.value = all.filter(i => i.selected)
    }
  } catch { items.value = [] }
  finally { loading.value = false }
}

async function loadStores() {
  storeLoading.value = true
  try {
    const data = await h5Api.stores()
    stores.value = Array.isArray(data) ? data : []
  } catch { stores.value = [] }
  finally { storeLoading.value = false }
}

async function loadCoupons() {
  couponLoading.value = true
  try {
    const data = await h5Api.myCoupons('UNUSED')
    const list = (data as any)?.list || (Array.isArray(data) ? data : [])
    coupons.value = list
  } catch { coupons.value = [] }
  finally { couponLoading.value = false }
}

function isCouponUsable(c: CouponRecord): boolean {
  if (c.threshold && totalAmount.value < c.threshold) return false
  return true
}

function openCouponSheet() {
  couponVisible.value = true
  if (!coupons.value.length) loadCoupons()
}

function onSelectCoupon(c: CouponRecord) {
  if (!isCouponUsable(c)) {
    showToast('订单金额不满足该券使用条件')
    return
  }
  if (form.couponId == c.id) {
    form.couponId = null
  } else {
    form.couponId = c.id
  }
  couponVisible.value = false
}

const fmtDate = formatDate

function validate(): boolean {
  if (!items.value.length) { showToast('没有可结算的商品'); return false }
  if (form.deliveryType === 'DELIVERY') {
    if (!form.receiverName?.trim()) { showToast('请填写收件人'); return false }
    if (!form.receiverPhone?.trim()) { showToast('请填写手机号'); return false }
    if (!/^1\d{10}$/.test(form.receiverPhone.trim())) { showToast('手机号格式不正确'); return false }
    if (!form.receiverAddress?.trim()) { showToast('请填写详细地址'); return false }
  } else {
    if (!form.storeId) { showToast('请选择自提门店'); return false }
  }
  return true
}

async function onSubmit() {
  if (!validate()) return
  submitting.value = true
  try {
    const payload: any = {
      itemIds: items.value.map(i => i.id),
      deliveryType: form.deliveryType,
      remark: form.remark || undefined,
      couponId: form.couponId,
      payMethod: form.payMethod
    }
    if (form.deliveryType === 'DELIVERY') {
      payload.receiverName = form.receiverName.trim()
      payload.receiverPhone = form.receiverPhone.trim()
      payload.receiverProvince = form.receiverProvince || ''
      payload.receiverCity = form.receiverCity || ''
      payload.receiverDistrict = form.receiverDistrict || ''
      payload.receiverAddress = form.receiverAddress.trim()
    } else {
      payload.storeId = form.storeId
    }
    const data: any = await mallApi.checkout(payload)
    const orderId = data?.id || data?.orderId
    showToast({ message: '订单已提交', position: 'top' })
    if (orderId) {
      router.replace(`/mall/orders?highlight=${orderId}`)
    } else {
      router.replace('/mall/orders')
    }
  } catch (e: any) {
    showToast(e?.message || '提交失败，请稍后再试')
  }
  finally { submitting.value = false }
}

const yuan = fenToYuan

onMounted(() => {
  loadCartItems()
  loadStores()
  loadCoupons()
})
</script>

<style scoped>
.mall-checkout { padding-bottom: 96px; }

.loading { display: flex; justify-content: center; padding: 60px 0; }
.loading-mini { display: flex; justify-content: center; padding: 20px 0; }

.delivery-tabs {
  display: flex; gap: 10px;
  padding: 10px;
}
.dt-item {
  flex: 1;
  display: flex; flex-direction: column; align-items: center; gap: 4px;
  padding: 12px 0;
  border: 1px solid var(--line);
  border-radius: var(--r);
  color: var(--ink-2);
  cursor: pointer;
  transition: all var(--dur) var(--ease);
  font-size: 13px;
  letter-spacing: 0.04em;
}
.dt-item.active {
  background: var(--brand-soft);
  border-color: var(--brand);
  color: var(--brand-deep);
}

.address-card { padding: 12px 0; }
.address-card :deep(.van-cell-group--inset) { margin: 0 8px; }
.address-card :deep(.van-cell) {
  background: transparent;
  padding: 10px 12px;
}

.store-pick { display: flex; flex-direction: column; gap: 8px; padding: 0 12px 12px; }
.store-item {
  display: flex; flex-direction: column; gap: 4px;
  padding: 10px 12px;
  border: 1px solid var(--line);
  border-radius: var(--r);
  cursor: pointer;
  position: relative;
  transition: all var(--dur) var(--ease);
}
.store-item.active {
  background: var(--brand-soft);
  border-color: var(--brand);
}
.si-name { font-size: 13.5px; color: var(--ink); font-weight: 500; }
.si-addr { font-size: 11.5px; }
.store-item.active .van-icon {
  position: absolute;
  top: 10px; right: 10px;
}
.empty-mini { padding: 16px; text-align: center; font-size: 12px; }

.goods-card { padding: 12px; display: flex; flex-direction: column; gap: 10px; }
.gc-item {
  display: flex; align-items: center; gap: 10px;
}
.gc-cover {
  width: 44px; height: 44px;
  border-radius: var(--r-sm);
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.gc-cover.tone-GOODS { background: var(--brand); }
.gc-cover.tone-SERVICE { background: var(--accent-rose); }
.gc-info { flex: 1; min-width: 0; }
.gc-name {
  font-size: 13px; color: var(--ink);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.gc-meta { display: flex; gap: 10px; margin-top: 2px; }
.gc-price { font-size: 12px; color: var(--brand-deep); }
.gc-qty { font-size: 11.5px; }
.gc-sub { font-size: 14px; color: var(--ink); font-weight: 500; }

.coupon-card {
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 16px;
  cursor: pointer;
}
.coup-left { display: flex; align-items: center; gap: 8px; font-size: 13.5px; color: var(--ink); }
.coup-right { display: flex; align-items: center; gap: 6px; font-size: 12px; }
.coup-name { color: var(--brand-deep); font-size: 12.5px; max-width: 140px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.coup-discount { color: var(--brand-deep); font-weight: 500; }

.bar-tip { font-size: 13px; color: var(--ink-2); }
.bar-tip .strong { color: var(--brand-deep); font-size: 16px; margin-left: 4px; }
.bar-discount { color: var(--muted); font-size: 11.5px; margin-right: 8px; }

/* 优惠券选择弹窗 */
.coupon-sheet {
  padding: 16px 16px 24px;
  max-height: 70vh;
  overflow-y: auto;
}
.cs-header {
  display: flex; align-items: center; justify-content: space-between;
  padding-bottom: 12px;
  border-bottom: 1px dashed var(--line-2);
  margin-bottom: 8px;
}
.cs-title {
  font-family: var(--font-serif);
  font-size: 15px; font-weight: 500; color: var(--ink);
  letter-spacing: 0.06em;
}
.cs-close { color: var(--muted); cursor: pointer; }
.cs-list { display: flex; flex-direction: column; gap: 10px; }
.cs-item {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 14px;
  border: 1px solid var(--line);
  border-radius: var(--r);
  cursor: pointer;
  transition: all var(--dur) var(--ease);
  position: relative;
}
.cs-item.active {
  background: var(--brand-soft);
  border-color: var(--brand);
}
.cs-item.disabled {
  opacity: 0.45;
}
.cs-val {
  display: flex; flex-direction: column; align-items: center;
  min-width: 64px;
  padding-right: 12px;
  border-right: 1px dashed var(--line);
}
.cs-amount {
  font-size: 17px; font-weight: 600;
  color: var(--brand-deep);
  font-family: var(--font-num);
}
.cs-thr { font-size: 10.5px; margin-top: 2px; }
.cs-info { flex: 1; min-width: 0; }
.cs-name { font-size: 13.5px; color: var(--ink); font-weight: 500; }
.cs-date { font-size: 11.5px; margin-top: 3px; }

.remark-card { padding: 4px 12px; }

.pay-card { padding: 14px 16px; }
.pay-card :deep(.van-radio-group) { gap: 18px; }
.pay-card :deep(.van-radio__label) { font-size: 13px; color: var(--ink-2); }

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
