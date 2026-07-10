<template>
  <div class="page coupon-center">
    <NavBar title="领券中心" back />
    <div class="page-padding">
      <div class="page-tip">把好券先收下，到店时正好用上。</div>

      <div v-if="loading" class="loading"><van-loading color="#6f94b8" /></div>
      <EmptyState v-else-if="!list.length" title="暂无可领取的券" sub="更多好券正在路上" art="box" />

      <div v-else class="grid">
        <div v-for="c in list" :key="c.id" class="grid-card">
          <div class="grid-top" :class="`type-${c.type}`">
            <div class="top-amount">
              <template v-if="c.type === 'PERCENT'">
                {{ c.faceValue }}<span class="unit">折</span>
              </template>
              <template v-else-if="c.type === 'EXPERIENCE' || c.type === 'BIRTHDAY'">
                <span class="unit gift">免费</span>
              </template>
              <template v-else>
                <span class="unit">¥</span>{{ ((c.faceValue || 0) / 100).toFixed(0) }}
              </template>
            </div>
            <div class="top-cond">
              <template v-if="c.type === 'FULL_CUT'">满 {{ ((c.threshold || 0) / 100).toFixed(0) }} 元可用</template>
              <template v-else-if="c.type === 'PERCENT'">{{ ((c.faceValue || 0) / 10).toFixed(1) }} 折</template>
              <template v-else-if="c.type === 'EXPERIENCE'">到店体验</template>
              <template v-else>生日专享</template>
            </div>
          </div>
          <div class="grid-body">
            <div class="name">{{ c.name }}</div>
            <div class="valid">
              <van-icon name="clock-o" />
              <span v-if="c.validType === 'DAYS'">领取后 {{ c.validDays }} 天有效</span>
              <span v-else>{{ formatDate(c.validStart) }} ~ {{ formatDate(c.validEnd) }}</span>
            </div>
            <div class="remain" v-if="c.total">
              <span>剩余 {{ c.remain ?? 0 }} / {{ c.total }}</span>
              <div class="remain-bar"><div class="remain-fill" :style="{ width: percent(c) + '%' }"></div></div>
            </div>
            <button
              class="claim-btn"
              :class="{ claimed: c.claimed, empty: c.remain === 0 }"
              :disabled="c.claimed || (c.remain === 0)"
              @click="onClaim(c)"
            >
              {{ c.claimed ? '已领取' : (c.remain === 0 ? '已抢完' : '一键领取') }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { showToast } from 'vant'
import { h5Api, type AvailableCoupon } from '@/api/h5'
import { formatDate } from '@/utils/format'
import NavBar from '@/components/NavBar.vue'
import EmptyState from '@/components/EmptyState.vue'

const loading = ref(false)
const list = ref<AvailableCoupon[]>([])

async function load() {
  loading.value = true
  try { list.value = await h5Api.availableCoupons() } catch {/* */}
  finally { loading.value = false }
}

function percent(c: AvailableCoupon) {
  if (!c.total) return 100
  return Math.round((c.remain ?? 0) / c.total * 100)
}

async function onClaim(c: AvailableCoupon) {
  if (c.claimed || c.remain === 0) return
  try {
    await h5Api.claimCoupon(c.id)
    c.claimed = true
    showToast('领取成功')
  } catch { showToast('领取失败') }
}

onMounted(load)
</script>

<style scoped>
.page-tip {
  font-size: 12px; color: var(--muted);
  letter-spacing: 0.04em; margin-bottom: 14px;
  font-family: 'Songti SC', serif; opacity: 0.85;
  padding-left: 2px;
}

.loading { display: flex; justify-content: center; padding: 40px 0; }
.grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
.grid-card {
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-md);
  overflow: hidden;
  transition: transform var(--dur) var(--ease), box-shadow var(--dur) var(--ease);
}
.grid-card:hover { transform: scale(1.01); box-shadow: var(--shadow-md); }
.grid-top {
  padding: 18px 8px;
  color: #fff;
  text-align: center;
  position: relative;
}
.grid-top::after {
  content: ''; position: absolute; bottom: -4px; left: 0; right: 0; height: 6px;
  background-image:
    linear-gradient(135deg, transparent 50%, var(--surface) 50%),
    linear-gradient(225deg, transparent 50%, var(--surface) 50%);
  background-size: 8px 100%;
  background-repeat: repeat-x;
}
.grid-top.type-FULL_CUT { background: linear-gradient(135deg, #5a7d9f, #4a6a87); }
.grid-top.type-PERCENT { background: linear-gradient(135deg, #b8a16a, #9b8550); }
.grid-top.type-EXPERIENCE { background: linear-gradient(135deg, #7e9a8a, #5b7868); }
.grid-top.type-BIRTHDAY { background: linear-gradient(135deg, #c89d96, #a8736a); }
.top-amount { font-size: 30px; font-weight: 600; line-height: 1.2; font-variant-numeric: tabular-nums; }
.top-amount .unit { font-size: 12px; font-weight: 400; margin-left: 2px; }
.top-amount .unit.gift { font-size: 18px; }
.top-cond { font-size: 11.5px; margin-top: 4px; opacity: 0.92; letter-spacing: 0.04em; }
.grid-body { padding: 14px 12px; }
.name { font-size: 13.5px; font-weight: 500; color: var(--ink); margin-bottom: 6px; line-height: 1.5; }
.valid { font-size: 11.5px; color: var(--muted); display: flex; align-items: center; gap: 3px; margin-bottom: 8px; }
.remain { font-size: 11px; color: var(--muted); margin-bottom: 12px; }
.remain-bar { height: 3px; background: var(--line); border-radius: 2px; margin-top: 4px; overflow: hidden; }
.remain-fill { height: 100%; background: var(--brand); border-radius: 2px; transition: width var(--dur-slow) var(--ease); }
.claim-btn {
  width: 100%;
  background: var(--brand-deep);
  color: #fff;
  border: none;
  padding: 9px 0;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  letter-spacing: 0.16em;
  cursor: pointer;
  font-family: inherit;
  transition: all var(--dur) var(--ease);
}
.claim-btn:active:not(:disabled) { transform: scale(0.98); }
.claim-btn.claimed, .claim-btn.empty { background: var(--muted-2); cursor: not-allowed; }
</style>
