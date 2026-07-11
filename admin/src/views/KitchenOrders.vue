<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2 class="page-title">厨房工单</h2>
        <div class="page-sub">每一道菜都值得被认真对待，从下单到出餐</div>
      </div>
      <el-button :icon="Refresh" @click="loadList" class="btn-scale">刷新</el-button>
    </div>

    <div class="filter-bar">
      <el-select v-model="storeId" placeholder="选择门店" style="width: 180px" @change="loadList">
        <el-option v-for="s in stores" :key="s.id" :label="s.name" :value="s.id" />
      </el-select>
      <el-radio-group v-model="statusFilter" @change="loadList">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="PENDING">待制作</el-radio-button>
        <el-radio-button value="COOKING">制作中</el-radio-button>
        <el-radio-button value="SERVED">已出餐</el-radio-button>
      </el-radio-group>
      <div class="filter-spacer"></div>
      <span class="stat-text">共 {{ list.length }} 单</span>
    </div>

    <div v-loading="loading" class="order-list">
      <div v-for="o in list" :key="o.id" class="order-card x-card btn-scale" :class="`st-${(o.status || 'PENDING').toLowerCase()}`">
        <div class="oc-head">
          <div class="oc-no">
            <span class="num-bubble">{{ orderIndex(o) }}</span>
            <div class="oc-no-text">
              <div class="oc-order-no">{{ o.orderNo || `#${o.id}` }}</div>
              <div class="oc-time">{{ fmtTime(o.createdAt) }}</div>
            </div>
          </div>
          <span class="chip" :class="statusChip(o.status)">{{ statusText(o.status) }}</span>
        </div>

        <div class="oc-tags">
          <span class="chip brand">
            <el-icon><PlaceHolder /></el-icon>
            {{ o.tableName || `桌台 ${o.tableId || '-'}` }}
          </span>
          <span class="chip" :class="o.orderType === 'TAKEOUT' ? 'clay' : 'mist'">
            {{ o.orderType === 'TAKEOUT' ? '外带' : '堂食' }}
          </span>
        </div>

        <div class="oc-items">
          <div v-for="(it, i) in (o.items || [])" :key="i" class="item-row">
            <div class="ir-name">
              <span class="ir-qty">×{{ it.quantity }}</span>
              {{ it.productName || `商品 ${it.productId}` }}
            </div>
            <div v-if="it.remark" class="ir-remark">{{ it.remark }}</div>
          </div>
          <div v-if="o.remark" class="oc-remark">订单备注：{{ o.remark }}</div>
        </div>

        <div class="oc-actions">
          <el-button
            v-if="o.status === 'PENDING'"
            type="primary"
            size="small"
            class="btn-scale"
            @click="onUpdateStatus(o, 'COOKING')"
          >开始制作</el-button>
          <el-button
            v-if="o.status === 'COOKING'"
            type="success"
            size="small"
            class="btn-scale"
            @click="onUpdateStatus(o, 'SERVED')"
          >完成出餐</el-button>
          <span v-if="o.status === 'SERVED'" class="oc-done">已完成</span>
        </div>
      </div>

      <div v-if="!loading && !list.length" class="empty">
        <el-empty :description="storeId ? '暂无工单' : '请先选择门店'" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { diningApi, storesApi } from '@/api'

// PlaceHolder 图标在 element-plus 中不存在时用兜底，这里用 Location 代替
import { Location as PlaceHolder } from '@element-plus/icons-vue'

const loading = ref(false)
const list = ref<any[]>([])
const stores = ref<any[]>([])
const storeId = ref<number | undefined>(undefined)
const statusFilter = ref('')

async function loadStores() {
  try { stores.value = (await storesApi.list()) || [] } catch {}
}
async function loadList() {
  if (!storeId.value) { list.value = []; return }
  loading.value = true
  try {
    list.value = (await diningApi.kitchenOrders(storeId.value, statusFilter.value || undefined)) || []
  } catch {
    list.value = []
  } finally {
    loading.value = false
  }
}

function statusText(s: string) {
  return ({ PENDING: '待制作', COOKING: '制作中', SERVED: '已出餐' } as any)[s] || s
}
function statusChip(s: string) {
  return ({ PENDING: 'warning', COOKING: 'brand', SERVED: 'success' } as any)[s] || 'muted'
}
function orderIndex(o: any) {
  const i = list.value.findIndex(x => x.id === o.id)
  return i < 0 ? '-' : i + 1
}
function fmtTime(t?: string) {
  if (!t) return ''
  try {
    const d = new Date(t)
    return d.toLocaleString('zh-CN', { hour12: false, month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
  } catch { return t }
}

async function onUpdateStatus(o: any, status: string) {
  try {
    await diningApi.updateKitchenStatus(o.id, status)
    ElMessage.success(status === 'COOKING' ? '已开始制作' : '已出餐')
    o.status = status
    // 如果在筛选状态下，更新后从列表移除（保持筛选一致性）
    if (statusFilter.value && statusFilter.value !== status) {
      list.value = list.value.filter(x => x.id !== o.id)
    }
  } catch {
    ElMessage.error('操作失败')
  }
}

onMounted(async () => {
  await loadStores()
  if (stores.value.length && !storeId.value) {
    storeId.value = stores.value[0].id
    loadList()
  }
})
</script>

<style scoped>
.stat-text {
  font-size: 12.5px; color: var(--muted);
  font-family: var(--font-serif); letter-spacing: 0.04em;
}

.order-list {
  display: flex; flex-direction: column;
  gap: 12px;
}
.order-card {
  padding: 14px 18px;
  border-left: 3px solid var(--line-2);
}
.order-card.st-pending { border-left-color: var(--warning); }
.order-card.st-cooking { border-left-color: var(--brand); }
.order-card.st-served { border-left-color: var(--success); }

.oc-head {
  display: flex; align-items: flex-start; justify-content: space-between;
  margin-bottom: 10px;
}
.oc-no { display: flex; align-items: center; gap: 10px; }
.oc-no-text { line-height: 1.3; }
.oc-order-no {
  font-size: 14px; font-weight: 500; color: var(--ink);
  font-family: var(--font-num); letter-spacing: 0.02em;
}
.oc-time {
  font-size: 11.5px; color: var(--muted);
  margin-top: 2px;
}

.oc-tags {
  display: flex; gap: 6px; flex-wrap: wrap;
  margin-bottom: 10px;
}
.oc-tags .chip .el-icon { font-size: 12px; }

.oc-items {
  background: var(--surface-2);
  border-radius: var(--r-md);
  padding: 10px 12px;
  margin-bottom: 10px;
}
.item-row {
  display: flex; align-items: center; justify-content: space-between;
  padding: 4px 0;
  font-size: 13px; color: var(--ink-2);
  border-bottom: 1px dashed var(--line-soft);
}
.item-row:last-child { border-bottom: none; }
.ir-name {
  display: flex; align-items: center; gap: 8px;
  flex: 1;
}
.ir-qty {
  display: inline-flex; align-items: center; justify-content: center;
  min-width: 28px; height: 18px;
  background: var(--brand-soft); color: var(--brand-ink);
  border-radius: 4px;
  font-size: 11px; font-weight: 500;
  font-family: var(--font-num);
  padding: 0 4px;
}
.ir-remark {
  font-size: 11.5px; color: var(--danger-deep);
  background: var(--danger-soft);
  padding: 1px 8px; border-radius: 4px;
}
.oc-remark {
  margin-top: 6px; padding-top: 6px;
  border-top: 1px dashed var(--line);
  font-size: 12px; color: var(--ink-3);
  font-family: var(--font-serif); letter-spacing: 0.02em;
}

.oc-actions {
  display: flex; justify-content: flex-end; align-items: center;
  gap: 8px;
}
.oc-done {
  font-size: 12.5px; color: var(--success-deep);
  font-family: var(--font-serif); letter-spacing: 0.04em;
}
.empty { padding: 20px 0; }
</style>
