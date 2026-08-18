<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2 class="page-title">储值流水</h2>
        <div class="page-sub">{{ walletSlogan }}</div>
      </div>
      <el-button :icon="Download" @click="exportCsv" class="btn-scale">导出 CSV</el-button>
    </div>

    <div class="x-card table-wrap">
      <div class="filter-bar">
        <el-input v-model="query.keyword" placeholder="会员姓名/手机号" clearable @keyup.enter="onSearch" />
        <el-select v-model="query.type" placeholder="类型" clearable @change="onSearch">
          <el-option label="充值" value="RECHARGE" />
          <el-option label="消费" value="CONSUME" />
          <el-option label="赠送" value="GIFT" />
          <el-option label="退款" value="REFUND" />
        </el-select>
        <el-select v-model="query.storeId" placeholder="门店" clearable @change="onSearch">
          <el-option v-for="s in stores" :key="s.id" :label="s.name" :value="s.id" />
        </el-select>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="—"
          start-placeholder="开始"
          end-placeholder="结束"
          value-format="YYYY-MM-DD"
          @change="onSearch"
        />
        <el-button type="primary" :icon="Search" @click="onSearch">查询</el-button>
        <el-button :icon="RefreshLeft" @click="onReset">重置</el-button>
      </div>

      <!-- 汇总 -->
      <div class="summary">
        <div class="sum-item">
          <div class="sum-label">充值合计</div>
          <div class="sum-val pos">+¥{{ formatMoney(summary.recharge) }}</div>
        </div>
        <div class="sum-item">
          <div class="sum-label">消费合计</div>
          <div class="sum-val neg">-¥{{ formatMoney(summary.consume) }}</div>
        </div>
        <div class="sum-item">
          <div class="sum-label">赠送合计</div>
          <div class="sum-val pos">+¥{{ formatMoney(summary.gift) }}</div>
        </div>
        <div class="sum-item">
          <div class="sum-label">退款合计</div>
          <div class="sum-val neg">-¥{{ formatMoney(summary.refund) }}</div>
        </div>
        <div class="sum-item">
          <div class="sum-label">笔数</div>
          <div class="sum-val">{{ summary.total }}</div>
        </div>
      </div>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column label="时间" width="155">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="txTypeType(row.type)" effect="light">{{ txTypeText(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="会员" min-width="150">
          <template #default="{ row }">
            <span class="link" @click="goMember(row)">{{ row.memberName || `#${row.memberId}` }}</span>
          </template>
        </el-table-column>
        <el-table-column label="门店" width="130" prop="storeName">
          <template #default="{ row }">{{ row.storeName || '—' }}</template>
        </el-table-column>
        <el-table-column label="金额" width="120" align="right">
          <template #default="{ row }">
            <span :class="row.amount >= 0 ? 'pos' : 'neg'">{{ row.amount >= 0 ? '+' : '-' }}¥{{ formatMoney(Math.abs(row.amount)) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="交易后余额" width="120" align="right">
          <template #default="{ row }">¥{{ formatMoney(row.balanceAfter) }}</template>
        </el-table-column>
        <el-table-column label="备注" prop="remark" show-overflow-tooltip min-width="140" />
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="loadList"
          @size-change="loadList"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, RefreshLeft, Download } from '@element-plus/icons-vue'
import { walletApi, storesApi } from '@/api'
import { formatMoney, formatDateTime, fenToYuan } from '@/utils/format'
import { quickCsv } from '@/utils/csv'
import type { Transaction, Store } from '@/types'

const walletSlogan = [
  '把每一笔储值，都看作一份信赖',
  '资金在流动，信赖在生长',
  '账目清晰，是最朴素的责任'
][Math.floor(Math.random() * 3)]

const router = useRouter()
const loading = ref(false)
const list = ref<Transaction[]>([])
const total = ref(0)
const stores = ref<Store[]>([])
const dateRange = ref<[string, string] | null>(null)
// 后端返回的全局汇总(基于全部筛选条件, 非当前页近似)
const summaryData = ref({ recharge: 0, consume: 0, gift: 0, refund: 0, total: 0 })

const query = reactive({
  keyword: '',
  type: '',
  storeId: undefined as number | undefined,
  start: '',
  end: '',
  page: 1,
  size: 20
})

// 直接使用接口返回的汇总, 不再基于当前页近似计算
const summary = computed(() => summaryData.value)

async function loadList() {
  loading.value = true
  try {
    const params: any = { page: query.page, size: query.size }
    if (query.keyword) params.keyword = query.keyword
    if (query.type) params.type = query.type
    if (query.storeId) params.storeId = query.storeId
    if (dateRange.value && dateRange.value.length === 2) {
      params.start = dateRange.value[0]
      params.end = dateRange.value[1]
    }
    const res = await walletApi.transactions(params)
    list.value = res.list || []
    total.value = res.total || 0
    summaryData.value = res.summary || { recharge: 0, consume: 0, gift: 0, refund: 0, total: 0 }
  } finally {
    loading.value = false
  }
}

function onSearch() {
  query.page = 1
  loadList()
}
function onReset() {
  query.keyword = ''
  query.type = ''
  query.storeId = undefined
  dateRange.value = null
  query.page = 1
  loadList()
}
function goMember(row: Transaction) {
  // 跳到会员管理（带 memberId，可扩展为直接打开详情）
  router.push({ path: '/members', query: { id: String(row.memberId) } })
}
function exportCsv() {
  if (!list.value.length) {
    ElMessage.info('暂无数据可导出')
    return
  }
  quickCsv(`储值流水-${new Date().toLocaleDateString('zh-CN')}`, [
    '时间', '类型', '会员', '门店', '金额(元)', '余额(元)', '备注'
  ], list.value.map(t => [
    formatDateTime(t.createdAt),
    txTypeText(t.type),
    t.memberName || `#${t.memberId}`,
    t.storeName || '',
    fenToYuan(t.amount),
    t.balanceAfter != null ? fenToYuan(t.balanceAfter) : '',
    t.remark || ''
  ]))
  ElMessage.success('已导出 CSV')
}

function txTypeText(t: string) {
  return ({ RECHARGE: '充值', CONSUME: '消费', GIFT: '赠送', REFUND: '退款' } as any)[t] || t
}
function txTypeType(t: string) {
  return ({ RECHARGE: 'success', CONSUME: 'warning', GIFT: 'info', REFUND: 'danger' } as any)[t] || 'info'
}

onMounted(async () => {
  stores.value = await storesApi.list().catch(() => [])
  loadList()
})
</script>

<style scoped>
.table-wrap {
  padding: 16px 18px;
}
.summary {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 10px;
  margin-bottom: 14px;
}
.sum-item {
  background: var(--surface-2);
  border: 1px solid var(--card-border);
  border-radius: 8px;
  padding: 10px 12px;
}
.sum-label {
  font-size: 12px;
  color: var(--muted);
}
.sum-val {
  font-size: 15px;
  font-weight: 600;
  color: var(--ink);
  margin-top: 4px;
}
.pos {
  color: var(--success);
}
.neg {
  color: var(--danger);
}
.link {
  color: var(--primary);
  cursor: pointer;
}
.link:hover {
  text-decoration: underline;
}
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}
@media (max-width: 900px) {
  .summary {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
