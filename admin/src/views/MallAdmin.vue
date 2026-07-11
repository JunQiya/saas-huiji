<template>
  <div class="page">
    <div class="page-header is-enhanced">
      <div class="header-left">
        <div class="header-icon"><el-icon><Shop /></el-icon></div>
        <div class="header-text">
          <h2 class="page-title">线上商城</h2>
          <div class="page-sub">{{ mallSlogan }}</div>
        </div>
      </div>
      <div class="header-actions">
        <el-button :icon="Refresh" @click="reload">刷新</el-button>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="mall-tabs">
      <!-- 商品分类 -->
      <el-tab-pane label="商品分类" name="categories">
        <div class="x-card filter-card">
          <div class="filter-bar">
            <el-button type="primary" :icon="Plus" @click="openCategoryEdit()" class="btn-scale">新建分类</el-button>
            <div class="filter-spacer"></div>
            <el-button :icon="Refresh" link @click="loadCategories">刷新分类</el-button>
          </div>
        </div>

        <div class="x-card">
          <el-table v-loading="catLoading" :data="categories" stripe size="small">
            <el-table-column label="排序" prop="sortOrder" width="70" />
            <el-table-column label="分类名称" prop="name" min-width="140" />
            <el-table-column label="图标" width="90">
              <template #default="{ row }">
                <el-tag v-if="row.icon" size="small" type="info">{{ row.icon }}</el-tag>
                <span v-else class="muted">—</span>
              </template>
            </el-table-column>
            <el-table-column label="商品数" width="90">
              <template #default="{ row }">
                <span class="val">{{ row.productIds?.length || row.productCount || 0 }}</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <span class="dot" :class="row.status === 'ACTIVE' ? 'success' : 'muted'"></span>
                {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="240" fixed="right">
              <template #default="{ row }">
                <div class="row-actions">
                  <el-button link type="primary" :icon="Edit" @click="openCategoryEdit(row)">编辑</el-button>
                  <el-button link :icon="Connection" @click="openBindProducts(row)">绑定商品</el-button>
                  <el-button link type="danger" :icon="Delete" @click="removeCategory(row)">删除</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- 商城订单 -->
      <el-tab-pane label="商城订单" name="orders">
        <div class="x-card filter-card">
          <div class="filter-bar">
            <el-select v-model="orderQuery.status" placeholder="订单状态" clearable style="width: 160px">
              <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
            </el-select>
            <el-input v-model="orderQuery.keyword" placeholder="订单号/收件人" clearable style="width: 200px" />
            <el-button :icon="Search" @click="loadOrders">查询</el-button>
            <div class="filter-spacer"></div>
            <el-button :icon="Refresh" link @click="loadOrders">刷新</el-button>
          </div>
        </div>

        <div class="x-card">
          <el-table v-loading="orderLoading" :data="orders" stripe size="small">
            <el-table-column label="订单号" prop="orderNo" min-width="160">
              <template #default="{ row }">
                <span class="val">{{ row.orderNo }}</span>
              </template>
            </el-table-column>
            <el-table-column label="收件人" min-width="120">
              <template #default="{ row }">
                <div>{{ row.receiverName || '-' }}</div>
                <div class="muted" style="font-size: 11px">{{ row.receiverPhone || '' }}</div>
              </template>
            </el-table-column>
            <el-table-column label="配送方式" width="90">
              <template #default="{ row }">
                <el-tag size="small" :type="row.deliveryType === 'PICKUP' ? 'info' : 'primary'">
                  {{ row.deliveryType === 'PICKUP' ? '自提' : '配送' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="金额" width="100">
              <template #default="{ row }">¥ <span class="val">{{ yuan(row.totalAmount) }}</span></template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <span class="chip" :class="statusChipClass(row.status)">{{ statusText(row.status) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="物流" min-width="160">
              <template #default="{ row }">
                <div v-if="row.trackingNo" class="val" style="font-size: 12px">{{ row.trackingNo }}</div>
                <div v-if="row.trackingCompany" class="muted" style="font-size: 11px">{{ row.trackingCompany }}</div>
                <span v-if="!row.trackingNo" class="muted">未填写</span>
              </template>
            </el-table-column>
            <el-table-column label="下单时间" width="150">
              <template #default="{ row }">
                <span class="muted" style="font-size: 11.5px">{{ fmtTime(row.createdAt) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" :icon="Edit" @click="openTracking(row)">更新物流</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pager">
            <el-pagination
              v-model:current-page="orderQuery.page"
              v-model:page-size="orderQuery.size"
              :total="orderTotal"
              :page-sizes="[10, 20, 50]"
              layout="total, prev, pager, next, sizes"
              background
              @current-change="loadOrders"
              @size-change="loadOrders"
            />
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 分类编辑弹窗 -->
    <el-dialog v-model="catEditVisible" :title="catForm.id ? '编辑分类' : '新建分类'" width="460px" destroy-on-close>
      <el-form :model="catForm" label-width="80px" size="default">
        <el-form-item label="名称" required>
          <el-input v-model="catForm.name" maxlength="20" show-word-limit placeholder="如：精选好物" />
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="catForm.icon" placeholder="图标标识（可选）" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="catForm.sortOrder" :min="0" :step="1" style="width: 180px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="catForm.status">
            <el-radio-button value="ACTIVE">启用</el-radio-button>
            <el-radio-button value="DISABLED">停用</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="catEditVisible = false">取消</el-button>
        <el-button type="primary" :loading="catSaving" @click="saveCategory" class="btn-scale">保存</el-button>
      </template>
    </el-dialog>

    <!-- 绑定商品弹窗 -->
    <el-dialog v-model="bindVisible" title="绑定商品到分类" width="620px" destroy-on-close>
      <div class="bind-head">
        <span class="muted">当前分类：</span>
        <b>{{ bindForm.categoryName }}</b>
      </div>
      <div v-loading="productLoading" class="bind-list">
        <el-checkbox v-model="productCheckAll" :indeterminate="productIndeterminate" @change="onProductCheckAll">
          全选 / 取消
        </el-checkbox>
        <el-checkbox-group v-model="bindForm.productIds" class="bind-grid">
          <el-checkbox v-for="p in productList" :key="p.id" :label="p.id" class="bind-item">
            <div class="bind-prod">
              <span class="bp-name">{{ p.name }}</span>
              <span class="bp-meta muted">¥{{ yuan(p.price) }} · {{ p.category === 'GOODS' ? '商品' : '服务' }}</span>
            </div>
          </el-checkbox>
        </el-checkbox-group>
        <div v-if="!productList.length && !productLoading" class="empty-state">
          <div>暂无可绑定商品</div>
        </div>
      </div>
      <template #footer>
        <el-button @click="bindVisible = false">取消</el-button>
        <el-button type="primary" :loading="bindSaving" @click="saveBind" class="btn-scale">
          确认绑定 ({{ bindForm.productIds.length }})
        </el-button>
      </template>
    </el-dialog>

    <!-- 物流更新弹窗 -->
    <el-dialog v-model="trackingVisible" title="更新物流信息" width="460px" destroy-on-close>
      <div class="bind-head">
        <span class="muted">订单号：</span>
        <b class="val">{{ trackingForm.orderNo }}</b>
      </div>
      <el-form :model="trackingForm" label-width="90px" size="default" style="margin-top: 12px">
        <el-form-item label="物流公司">
          <el-select v-model="trackingForm.trackingCompany" filterable allow-create placeholder="选择或输入物流公司" style="width: 100%">
            <el-option v-for="c in expressCompanies" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="物流单号" required>
          <el-input v-model="trackingForm.trackingNo" placeholder="请输入物流单号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="trackingVisible = false">取消</el-button>
        <el-button type="primary" :loading="trackingSaving" @click="saveTracking" class="btn-scale">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, Refresh, Search, Edit, Delete, Connection, Shop
} from '@element-plus/icons-vue'
import { mallApi, productsApi } from '@/api'

const mallSlogan = [
  '把好物陈列清楚，让顾客挑得安心',
  '线上商城，是门店之外的一份延展',
  '每一件商品，都通向一次心意'
][Math.floor(Math.random() * 3)]

const activeTab = ref<'categories' | 'orders'>('categories')

// ===== 商品分类 =====
const catLoading = ref(false)
const categories = ref<any[]>([])

async function loadCategories() {
  catLoading.value = true
  try {
    const data = await mallApi.categories()
    categories.value = Array.isArray(data) ? data : []
  } catch { categories.value = [] }
  finally { catLoading.value = false }
}

const catEditVisible = ref(false)
const catSaving = ref(false)
const catForm = reactive<any>({
  id: null, name: '', icon: '', sortOrder: 0, status: 'ACTIVE'
})

function openCategoryEdit(row?: any) {
  Object.assign(catForm, { id: null, name: '', icon: '', sortOrder: 0, status: 'ACTIVE' })
  if (row) {
    catForm.id = row.id
    catForm.name = row.name
    catForm.icon = row.icon || ''
    catForm.sortOrder = row.sortOrder ?? 0
    catForm.status = row.status || 'ACTIVE'
  }
  catEditVisible.value = true
}

async function saveCategory() {
  if (!catForm.name?.trim()) { ElMessage.warning('请填写分类名称'); return }
  catSaving.value = true
  try {
    await mallApi.saveCategory({
      id: catForm.id || undefined,
      name: catForm.name.trim(),
      icon: catForm.icon || '',
      sortOrder: Number(catForm.sortOrder) || 0,
      status: catForm.status
    })
    ElMessage.success(catForm.id ? '已更新' : '已创建')
    catEditVisible.value = false
    loadCategories()
  } finally { catSaving.value = false }
}

async function removeCategory(row: any) {
  try {
    await ElMessageBox.confirm(`确定删除分类「${row.name}」？`, '提示', { type: 'warning' })
  } catch { return }
  await mallApi.removeCategory(row.id)
  ElMessage.success('已删除')
  loadCategories()
}

// ===== 绑定商品 =====
const bindVisible = ref(false)
const bindSaving = ref(false)
const productLoading = ref(false)
const productList = ref<any[]>([])
const bindForm = reactive<{ categoryId: number | null; categoryName: string; productIds: number[] }>({
  categoryId: null, categoryName: '', productIds: []
})

const productCheckAll = ref(false)
const productIndeterminate = computed(() => {
  const total = productList.value.length
  const checked = bindForm.productIds.length
  return checked > 0 && checked < total
})
watch(() => bindForm.productIds, () => {
  const total = productList.value.length
  productCheckAll.value = total > 0 && bindForm.productIds.length === total
}, { deep: true })

function onProductCheckAll(val: any) {
  const checked = val instanceof Event ? (val.target as HTMLInputElement).checked : val
  bindForm.productIds = checked ? productList.value.map((p: any) => Number(p.id)) : []
}

async function openBindProducts(row: any) {
  bindForm.categoryId = row.id
  bindForm.categoryName = row.name
  bindForm.productIds = Array.isArray(row.productIds)
    ? row.productIds.map((id: any) => Number(id))
    : []
  bindVisible.value = true
  productLoading.value = true
  try {
    const data: any = await productsApi.active()
    productList.value = Array.isArray(data) ? data : (data?.list || [])
  } catch { productList.value = [] }
  finally { productLoading.value = false }
}

async function saveBind() {
  if (bindForm.categoryId == null) { ElMessage.warning('分类缺失'); return }
  bindSaving.value = true
  try {
    await mallApi.bindProducts(bindForm.categoryId, bindForm.productIds)
    ElMessage.success('已更新绑定')
    bindVisible.value = false
    loadCategories()
  } finally { bindSaving.value = false }
}

// ===== 商城订单 =====
const orderLoading = ref(false)
const orders = ref<any[]>([])
const orderTotal = ref(0)
const orderQuery = reactive<any>({ status: '', keyword: '', page: 1, size: 20 })

const statusOptions = [
  { label: '待付款', value: 'PENDING_PAY' },
  { label: '待发货', value: 'PENDING_SHIP' },
  { label: '待收货', value: 'PENDING_RECEIVE' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '已取消', value: 'CANCELLED' }
]

async function loadOrders() {
  orderLoading.value = true
  try {
    const data: any = await mallApi.orders({
      status: orderQuery.status || undefined,
      keyword: orderQuery.keyword || undefined,
      page: orderQuery.page,
      size: orderQuery.size
    })
    orders.value = data?.records || data?.list || data?.content || (Array.isArray(data) ? data : [])
    orderTotal.value = data?.total || orders.value.length
  } catch { orders.value = [] }
  finally { orderLoading.value = false }
}

function statusText(s: string) {
  return ({
    PENDING_PAY: '待付款',
    PENDING_SHIP: '待发货',
    PENDING_RECEIVE: '待收货',
    COMPLETED: '已完成',
    CANCELLED: '已取消',
    PAID: '已付款',
    SHIPPED: '已发货',
    REFUNDED: '已退款'
  } as any)[s] || s || '-'
}
function statusChipClass(s: string) {
  return ({
    PENDING_PAY: 'warning',
    PENDING_SHIP: 'warning',
    PENDING_RECEIVE: 'mist',
    COMPLETED: 'success',
    CANCELLED: 'muted',
    PAID: 'success',
    SHIPPED: 'brand',
    REFUNDED: 'danger'
  } as any)[s] || 'mist'
}

// ===== 物流更新 =====
const trackingVisible = ref(false)
const trackingSaving = ref(false)
const trackingForm = reactive<any>({
  orderId: null, orderNo: '', trackingNo: '', trackingCompany: ''
})
const expressCompanies = ['顺丰速运', '中通快递', '圆通速递', '韵达快递', '申通快递', '京东物流', '邮政EMS', '极兔速递']

function openTracking(row: any) {
  trackingForm.orderId = row.id
  trackingForm.orderNo = row.orderNo
  trackingForm.trackingNo = row.trackingNo || ''
  trackingForm.trackingCompany = row.trackingCompany || ''
  trackingVisible.value = true
}

async function saveTracking() {
  if (!trackingForm.trackingNo?.trim()) { ElMessage.warning('请填写物流单号'); return }
  trackingSaving.value = true
  try {
    await mallApi.updateTracking(trackingForm.orderId, {
      trackingNo: trackingForm.trackingNo.trim(),
      trackingCompany: trackingForm.trackingCompany || ''
    })
    ElMessage.success('物流已更新')
    trackingVisible.value = false
    loadOrders()
  } finally { trackingSaving.value = false }
}

function yuan(f: any) {
  if (f == null) return '0.00'
  return (Number(f) / 100).toFixed(2)
}
function fmtTime(t: any) {
  if (!t) return '-'
  try { return new Date(t).toLocaleString('zh-CN', { hour12: false }) } catch { return String(t) }
}

function reload() {
  if (activeTab.value === 'categories') loadCategories()
  else loadOrders()
}

onMounted(() => {
  loadCategories()
  loadOrders()
})
</script>

<style scoped>
.mall-tabs { margin-top: 4px; }
.filter-card { padding: 12px 16px; margin-bottom: 14px; }

.bind-head { padding: 4px 2px 12px; font-size: 13px; color: var(--ink-2); }
.bind-list { max-height: 380px; overflow-y: auto; }
.bind-grid { display: flex; flex-direction: column; gap: 6px; margin-top: 10px; }
.bind-item {
  display: flex; align-items: center;
  padding: 8px 12px;
  border: 1px solid var(--line);
  border-radius: var(--r-sm);
  transition: background var(--dur) var(--ease-out);
  margin-right: 0 !important;
}
.bind-item:hover { background: var(--surface-2); }
.bind-item :deep(.el-checkbox__label) { flex: 1; padding-left: 10px; }
.bind-prod { display: flex; align-items: center; justify-content: space-between; gap: 10px; width: 100%; }
.bp-name { color: var(--ink); font-size: 13px; }
.bp-meta { font-size: 11.5px; }
</style>
