<template>
  <div class="page my-coupons">
    <NavBar title="我的券" back />

    <van-pull-refresh v-model="refreshing" class="pr-wrap" @refresh="onRefresh">
    <div class="page-padding">
      <div class="tab-bar">
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

      <div v-if="loading" class="loading"><van-loading color="#6f94b8" /></div>
      <EmptyState v-else-if="!list.length" title="暂无该状态券" sub="领几张好券，下次到店就有小惊喜" art="leaf" />

      <div v-else class="coupon-list">
        <div
          v-for="c in list"
          :key="c.id"
          class="coupon-row"
          :class="[`s-${c.status}`, { clickable: c.status === 'UNUSED' }]"
          @click="onUse(c)"
        >
          <div class="left-stripe" :class="`type-${c.type}`">
            <div class="stripe-val">
              <template v-if="c.type === 'PERCENT'">
                {{ ((c.faceValue || 0) / 10).toFixed(1).replace(/\.0$/, '') }}<span class="small">折</span>
              </template>
              <template v-else-if="c.type === 'EXPERIENCE' || c.type === 'BIRTHDAY'">
                <span class="small gift">免费</span>
              </template>
              <template v-else>
                <span class="small">¥</span>{{ ((c.faceValue || 0) / 100).toFixed(0) }}
              </template>
            </div>
            <div class="stripe-cond" v-if="c.type === 'FULL_CUT'">满 {{ ((c.threshold || 0) / 100).toFixed(0) }} 可用</div>
            <div class="stripe-cond" v-else>无门槛</div>
          </div>
          <div class="right-body">
            <div class="c-name">{{ c.couponName }}</div>
            <div class="c-meta">
              <van-icon name="clock-o" />
              <span v-if="c.status === 'UNUSED'">{{ formatDateTime(c.expireAt) }} 到期</span>
              <span v-else-if="c.status === 'USED'">{{ formatDateTime(c.usedAt) }} 已使用</span>
              <span v-else>{{ formatDateTime(c.expireAt) }} 已过期</span>
            </div>
            <div class="c-code">券码 {{ c.code }}</div>
            <div class="c-status" :class="`chip-${dotClass(c.status)}`">
              <span class="dot" :class="dotClass(c.status)"></span>
              {{ statusText(c.status) }}
            </div>
            <div v-if="c.status === 'UNUSED'" class="c-use-btn" @click.stop="onUse(c)">出示券码 ›</div>
          </div>
        </div>
      </div>
    </div>
    </van-pull-refresh>

    <!-- 券码核销弹窗 -->
    <CouponQrPopup
      v-model:show="qrVisible"
      :coupon-name="qrCoupon?.couponName"
      :code="qrCoupon?.code"
      :status="qrCoupon?.status"
      :expire-at="qrCoupon?.expireAt"
    />
  </div>
</template>

<script setup lang="ts">
import { onActivated, onMounted, ref } from 'vue'
import { showToast } from 'vant'
import { h5Api, type CouponRecord } from '@/api/h5'
import { formatDateTime } from '@/utils/format'
import NavBar from '@/components/NavBar.vue'
import EmptyState from '@/components/EmptyState.vue'
import CouponQrPopup from '@/components/CouponQrPopup.vue'

const tabs = [
  { label: '未使用', value: 'UNUSED' as const },
  { label: '已使用', value: 'USED' as const },
  { label: '已过期', value: 'EXPIRED' as const }
]
const status = ref<'UNUSED' | 'USED' | 'EXPIRED'>('UNUSED')
const loading = ref(false)
const list = ref<CouponRecord[]>([])
const refreshing = ref(false)

async function load() {
  loading.value = true
  try { list.value = await h5Api.myCoupons(status.value) }
  catch { showToast('加载失败') }
  finally { loading.value = false }
}

async function onRefresh() {
  try {
    await load()
    showToast({ message: '已刷新', position: 'top' })
  } catch {/* 静默 */}
  finally { refreshing.value = false }
}

function onTab(v: 'UNUSED' | 'USED' | 'EXPIRED') {
  status.value = v
  load()
}
function statusText(s: string) {
  return ({ UNUSED: '待使用', USED: '已使用', EXPIRED: '已过期' } as any)[s] || s
}
function dotClass(s: string) {
  return ({ UNUSED: 'success', USED: 'info', EXPIRED: 'danger' } as any)[s] || 'info'
}

// 出示券码：未使用券点击后弹二维码，供门店核销
const qrVisible = ref(false)
const qrCoupon = ref<CouponRecord | null>(null)
function onUse(c: CouponRecord) {
  if (c.status !== 'UNUSED') return
  qrCoupon.value = c
  qrVisible.value = true
}

onMounted(load)
onActivated(load)
</script>

<style scoped>
.tab-bar { display: flex; gap: 6px; margin-bottom: 14px; padding: 0 2px; }
.pr-wrap { min-height: 60vh; }
.tab {
  padding: 6px 14px;
  font-size: 12.5px;
  color: var(--ink-2);
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: 999px;
  cursor: pointer;
  transition: all var(--dur) var(--ease);
  letter-spacing: 0.04em;
}
.tab.active { background: var(--brand-deep); color: #fff; border-color: var(--brand-deep); }

.loading { display: flex; justify-content: center; padding: 40px 0; }
.coupon-list { display: flex; flex-direction: column; gap: 10px; }
.coupon-row {
  display: flex;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-md);
  overflow: hidden;
  position: relative;
  transition: transform var(--dur) var(--ease), box-shadow var(--dur) var(--ease);
}
.coupon-row:hover { transform: scale(1.005); box-shadow: var(--shadow-sm); }
.coupon-row.s-USED, .coupon-row.s-EXPIRED { opacity: 0.7; }
.coupon-row.clickable { cursor: pointer; }
.coupon-row.clickable:hover { transform: translateY(-1px); box-shadow: var(--shadow-md); }
.c-use-btn {
  align-self: flex-end;
  margin-top: 4px;
  font-size: 12px;
  color: var(--brand-deep);
  letter-spacing: 0.06em;
  padding: 2px 0;
  font-weight: 500;
}

.left-stripe {
  width: 100px; flex-shrink: 0;
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  padding: 16px 8px;
  color: #fff;
  position: relative;
}
.left-stripe::after {
  content: ''; position: absolute; right: 0; top: 0; bottom: 0;
  border-right: 2px dashed rgba(255, 255, 255, 0.5);
}
.left-stripe.type-FULL_CUT { background: var(--brand); }
.left-stripe.type-PERCENT { background: var(--warning); }
.left-stripe.type-EXPERIENCE { background: var(--success); }
.left-stripe.type-BIRTHDAY { background: var(--accent-rose); }
.stripe-val { font-size: 26px; font-weight: 600; line-height: 1.2; font-variant-numeric: tabular-nums; }
.stripe-val .small { font-size: 12px; font-weight: 400; margin-left: 2px; }
.stripe-val .small.gift { font-size: 16px; }
.stripe-cond { font-size: 11px; margin-top: 4px; opacity: 0.92; letter-spacing: 0.04em; }

.right-body { flex: 1; padding: 12px 14px; display: flex; flex-direction: column; gap: 4px; }
.c-name { font-size: 14px; color: var(--ink); font-weight: 500; }
.c-meta { font-size: 11.5px; color: var(--muted); display: flex; align-items: center; gap: 3px; }
.c-code { font-size: 11px; color: var(--muted); font-family: 'SF Mono', monospace; letter-spacing: 0.04em; }
.c-status {
  display: inline-flex; align-items: center; gap: 4px;
  font-size: 11px; margin-top: 4px;
  align-self: flex-start;
  padding: 2px 8px;
  background: var(--surface-3);
  border-radius: 999px;
  color: var(--ink-2);
  letter-spacing: 0.04em;
}
.c-status.chip-success { background: var(--success-soft); color: #5b7868; }
.c-status.chip-info { background: rgba(138, 142, 133, 0.14); color: var(--muted); }
.c-status.chip-danger { background: var(--danger-soft); color: #8a4d3f; }
.dot { width: 5px; height: 5px; border-radius: 50%; }
.dot.success { background: var(--success); }
.dot.info { background: var(--muted); }
.dot.danger { background: var(--danger); }
</style>
