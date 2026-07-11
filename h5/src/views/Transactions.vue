<template>
  <div class="page transactions">
    <NavBar title="消费记录" back />
    <div class="page-padding">
      <div class="tip">每一笔流水都是一段日常。</div>

      <div class="type-tabs">
        <div v-for="t in typeTabs" :key="t.value"
             class="tab" :class="{ active: currentType === t.value }"
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
        <EmptyState v-else-if="!list.length && finished" title="暂无流水" sub="下一次到店，会出现在这里" art="leaf" />
        <div v-else class="tx-list">
          <div v-for="tx in list" :key="tx.id" class="tx-item">
            <div class="tx-icon" :class="`t-${tx.type}`">
              <van-icon :name="icon(tx.type)" size="20" />
            </div>
            <div class="tx-content">
              <div class="tx-top">
                <span class="tx-type">{{ typeText(tx.type) }}</span>
                <span v-if="tx.type === 'POINT'" class="tx-amount pos">+{{ tx.amount }} 分</span>
                <span v-else class="tx-amount" :class="tx.amount >= 0 ? 'pos' : 'neg'">
                  {{ tx.amount >= 0 ? '+' : '' }}¥{{ Math.abs(tx.amount / 100).toFixed(2) }}
                </span>
              </div>
              <div class="tx-meta">
                <span>{{ formatDateTime(tx.createdAt) }}</span>
                <span v-if="tx.storeName">· {{ tx.storeName }}</span>
              </div>
              <div v-if="tx.remark" class="tx-remark">{{ tx.remark }}</div>
            </div>
          </div>
        </div>
      </van-list>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onActivated, onMounted, ref } from 'vue'
import { h5Api, type TransactionRecord } from '@/api/h5'
import { formatDateTime } from '@/utils/format'
import NavBar from '@/components/NavBar.vue'
import EmptyState from '@/components/EmptyState.vue'

const loading = ref(false)
const finished = ref(false)
const page = ref(1)
const list = ref<TransactionRecord[]>([])
const currentType = ref('')
const typeTabs = [
  { label: '全部', value: '' },
  { label: '充值', value: 'RECHARGE' },
  { label: '消费', value: 'CONSUME' },
  { label: '赠送', value: 'GIFT' },
  { label: '积分', value: 'POINT' },
  { label: '退款', value: 'REFUND' }
]

let pending = false
async function load() {
  if (pending) return
  pending = true
  loading.value = true
  try {
    const res = await h5Api.transactions({ type: currentType.value || undefined, page: page.value, size: 20 })
    const items = (res as any)?.list || (res as any)?.records || (Array.isArray(res) ? res : [])
    const total = (res as any)?.total
    list.value.push(...items)
    page.value++
    if (items.length < 20 || (total != null && list.value.length >= total)) {
      finished.value = true
    }
  } catch {
    finished.value = true
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

function onTab(t: string) { currentType.value = t; reset(); load() }
function typeText(t: string) {
  return ({ RECHARGE: '充值', CONSUME: '消费', GIFT: '赠送', POINT: '积分', REFUND: '退款' } as any)[t] || t
}
function icon(t: string) {
  return ({ RECHARGE: 'plus', CONSUME: 'minus', GIFT: 'gift-card-o', POINT: 'star-o', REFUND: 'revoke' } as any)[t] || 'records'
}

onMounted(load)
onActivated(() => { reset(); load() })
</script>

<style scoped>
.tip {
  font-size: 12px; color: var(--muted);
  letter-spacing: 0.04em; margin-bottom: 14px;
  font-family: 'Songti SC', serif; opacity: 0.85;
  padding-left: 2px;
}

.type-tabs { display: flex; gap: 6px; margin-bottom: 14px; padding: 0 2px; overflow-x: auto; }
.type-tabs::-webkit-scrollbar { display: none; }
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
.tx-list { display: flex; flex-direction: column; gap: 10px; }
.tx-item {
  display: flex; align-items: flex-start; gap: 12px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-md);
  padding: 13px 14px;
  transition: transform var(--dur) var(--ease), box-shadow var(--dur) var(--ease);
}
.tx-item:active { transform: scale(0.998); }
.tx-icon {
  width: 36px; height: 36px;
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.tx-icon.t-RECHARGE { background: var(--brand-soft); color: var(--brand-deep); }
.tx-icon.t-CONSUME { background: var(--warning-soft); color: #8a6f3a; }
.tx-icon.t-GIFT { background: var(--success-soft); color: #5b7868; }
.tx-icon.t-POINT { background: var(--accent-twilight-soft); color: #6b6080; }
.tx-icon.t-REFUND { background: var(--danger-soft); color: #8a4d3f; }
.tx-content { flex: 1; min-width: 0; }
.tx-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px; }
.tx-type { font-size: 13.5px; font-weight: 500; color: var(--ink); }
.tx-amount { font-size: 15px; font-weight: 600; font-variant-numeric: tabular-nums; }
.tx-amount.pos { color: #5b7868; }
.tx-amount.neg { color: #8a4d3f; }
.tx-meta { font-size: 11.5px; color: var(--muted); }
.tx-remark { font-size: 11.5px; color: var(--muted); margin-top: 4px; }
</style>
