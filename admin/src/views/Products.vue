<template>
  <div class="page">
    <div class="page-header is-enhanced">
      <div class="header-left">
        <div class="header-icon"><el-icon><Goods /></el-icon></div>
        <div class="header-text">
          <h2 class="page-title">商品服务</h2>
          <div class="page-sub">{{ productSlogan }}</div>
        </div>
      </div>
      <div class="header-actions">
        <el-radio-group v-model="view" size="small">
          <el-radio-button value="card">卡片</el-radio-button>
          <el-radio-button value="table">表格</el-radio-button>
        </el-radio-group>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openEdit()" class="btn-scale">新建商品</el-button>
      </div>
    </div>

    <div class="x-card filter-card">
      <div class="filter-bar">
        <el-input v-model="query.keyword" placeholder="按名称搜索" clearable :prefix-icon="Search" />
        <el-select v-model="query.category" placeholder="类型" clearable>
          <el-option label="服务 SERVICE" value="SERVICE" />
          <el-option label="商品 GOODS" value="GOODS" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable>
          <el-option label="上架" value="ACTIVE" />
          <el-option label="下架" value="DISABLED" />
        </el-select>
        <el-button @click="onSearch">查询</el-button>
      </div>
    </div>

    <div v-if="view === 'card'" v-loading="loading" class="product-grid">
      <div v-for="p in list" :key="p.id" class="x-card hoverable product-card">
        <div class="cover" :class="`cover-${p.category}`">
          <el-icon class="cover-icon"><component :is="p.category === 'GOODS' ? Box : MagicStick" /></el-icon>
        </div>
        <div class="card-body">
          <div class="row-1">
            <span class="name">{{ p.name }}</span>
            <span class="dot" :class="p.status === 'ACTIVE' ? 'success' : 'info'" />
          </div>
          <div class="row-2">
            <span class="price val">¥{{ yuan(p.price) }}</span>
            <span class="meta">{{ p.category === 'GOODS' ? '库存 ' + (p.stock ?? 0) : '服务' }}</span>
          </div>
          <div class="row-3">
            <el-button size="small" :icon="Edit" link type="primary" @click="openEdit(p)">编辑</el-button>
            <el-button size="small" :icon="p.status === 'ACTIVE' ? Bottom : Top" link @click="toggleStatus(p)">
              {{ p.status === 'ACTIVE' ? '下架' : '上架' }}
            </el-button>
            <el-button v-if="p.category === 'GOODS'" size="small" :icon="More" link @click="openStock(p)">库存</el-button>
            <el-button size="small" :icon="Delete" link type="danger" @click="remove(p)">删除</el-button>
          </div>
        </div>
      </div>
      <div v-if="!loading && list.length === 0" class="empty-state">
        <el-icon><DocumentRemove /></el-icon>
        <div>暂无商品, 先添加一个吧</div>
      </div>
      <el-pagination
        v-if="!loading && total > size"
        class="pager"
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="load"
        @size-change="onSizeChange"
      />
    </div>

    <div v-else class="x-card">
      <el-table v-loading="loading" :data="list" stripe size="small">
        <el-table-column label="名称" prop="name" min-width="160" />
        <el-table-column label="类型" width="80">
          <template #default="{ row }">
            <el-tag :type="row.category === 'GOODS' ? 'warning' : 'primary'" size="small">{{ row.category }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="售价" width="100">
          <template #default="{ row }">¥ <span class="val">{{ yuan(row.price) }}</span></template>
        </el-table-column>
        <el-table-column label="成本" width="100">
          <template #default="{ row }">¥ <span class="val">{{ yuan(row.costPrice) }}</span></template>
        </el-table-column>
        <el-table-column label="库存" width="80" prop="stock" />
        <el-table-column label="已售" width="80" prop="soldCount" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <span class="dot" :class="row.status === 'ACTIVE' ? 'success' : 'info'" />
            {{ row.status === 'ACTIVE' ? '上架' : '下架' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <div class="row-actions">
              <el-button link type="primary" :icon="Edit" @click="openEdit(row)">编辑</el-button>
              <el-button link :icon="row.status === 'ACTIVE' ? Bottom : Top" @click="toggleStatus(row)">
                {{ row.status === 'ACTIVE' ? '下架' : '上架' }}
              </el-button>
              <el-button v-if="row.category === 'GOODS'" link :icon="More" @click="openStock(row)">库存</el-button>
              <el-button link type="danger" :icon="Delete" @click="remove(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        class="pager"
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="load"
        @size-change="onSizeChange"
      />
    </div>

    <el-dialog v-model="editVisible" :title="form.id ? '编辑商品' : '新建商品'" width="540px" destroy-on-close>
      <el-form :model="form" label-width="80px" size="default">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="类型" required>
          <el-radio-group v-model="form.category" :disabled="!!form.id">
            <el-radio-button value="SERVICE">服务</el-radio-button>
            <el-radio-button value="GOODS">商品</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="售价(元)">
          <el-input-number v-model="form.priceYuan" :min="0" :precision="2" :step="1" style="width: 180px" />
        </el-form-item>
        <el-form-item label="成本(元)">
          <el-input-number v-model="form.costPriceYuan" :min="0" :precision="2" :step="1" style="width: 180px" />
        </el-form-item>
        <el-form-item v-if="form.category === 'GOODS'" label="初始库存">
          <el-input-number v-model="form.stock" :min="0" :step="1" style="width: 180px" />
        </el-form-item>
        <el-form-item label="封面 URL">
          <el-input v-model="form.cover" placeholder="可选" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio-button value="ACTIVE">上架</el-radio-button>
            <el-radio-button value="DISABLED">下架</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="适用门店">
          <el-select v-model="form.storeIds" multiple clearable collapse-tags collapse-tags-tooltip placeholder="不选表示全店适用" style="width: 100%">
            <el-option v-for="s in stores" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
          <div class="form-hint">不选任何门店表示全店适用</div>
        </el-form-item>
        <el-form-item label="商城展示">
          <el-switch v-model="form.mallVisible" />
          <span class="form-hint">开启后商品将在线上商城展示</span>
        </el-form-item>
        <el-form-item v-if="form.mallVisible" label="商城分类">
          <el-select v-model="form.mallCategoryId" clearable placeholder="选择商城分类" style="width: 100%">
            <el-option v-for="c in mallCategories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save" class="btn-scale">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="stockVisible" title="库存调整" width="420px" destroy-on-close>
      <div class="stock-target">商品: <b>{{ stockForm.name }}</b> · 当前库存 <span class="val">{{ stockForm.stock ?? 0 }}</span></div>
      <el-form label-width="100px">
        <el-form-item label="调整方式">
          <el-radio-group v-model="stockForm.mode">
            <el-radio-button value="SET">设为</el-radio-button>
            <el-radio-button value="INC">增减</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="数量">
          <el-input-number v-model="stockForm.value" :step="1" style="width: 180px" />
          <span class="muted-hint">负数为扣减</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stockVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveStock" class="btn-scale">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search, Edit, Delete, Bottom, Top, More, Box, MagicStick, Goods, DocumentRemove } from '@element-plus/icons-vue'
import { productsApi, mallApi, storesApi } from '@/api'
import { fenToYuan, yuanToFen } from '@/utils/format'
import type { Store } from '@/types'

const productSlogan = [
  '每一件商品，都该被认真地呈现',
  '把用心装进商品，把商品交给懂得的人',
  '好的陈列，是把心意陈列清楚'
][Math.floor(Math.random() * 3)]

const view = ref<'card' | 'table'>('card')
const loading = ref(false)
const saving = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)

const query = reactive({ keyword: '', category: '', status: '' })

const editVisible = ref(false)
const form = reactive<any>({
  id: null,
  name: '',
  category: 'SERVICE',
  priceYuan: 0,
  costPriceYuan: 0,
  stock: 0,
  cover: '',
  description: '',
  status: 'ACTIVE',
  mallVisible: false,
  mallCategoryId: null,
  storeIds: [] as number[]
})

const mallCategories = ref<any[]>([])
const stores = ref<Store[]>([])

const stockVisible = ref(false)
const stockForm = reactive<any>({ id: null, name: '', stock: 0, mode: 'INC', value: 0 })

async function load() {
  loading.value = true
  try {
    const data: any = await productsApi.list({
      keyword: query.keyword || undefined,
      category: query.category || undefined,
      status: query.status || undefined,
      page: page.value,
      size: size.value
    })
    list.value = data?.records || data?.list || data?.content || []
    total.value = data?.total || data?.totalElements || 0
  } finally {
    loading.value = false
  }
}
function onSearch() {
  page.value = 1
  load()
}
function onSizeChange() {
  page.value = 1
  load()
}

function openEdit(row?: any) {
  Object.assign(form, {
    id: null, name: '', category: 'SERVICE', priceYuan: 0, costPriceYuan: 0,
    stock: 0, cover: '', description: '', status: 'ACTIVE',
    mallVisible: false, mallCategoryId: null, storeIds: []
  })
  if (row) {
    form.id = row.id
    form.name = row.name
    form.category = row.category
    form.priceYuan = Number(fenToYuan(row.price)) || 0
    form.costPriceYuan = Number(fenToYuan(row.costPrice)) || 0
    form.stock = row.stock ?? 0
    form.cover = row.cover || ''
    form.description = row.description || ''
    form.status = row.status || 'ACTIVE'
    form.mallVisible = !!row.mallVisible
    form.mallCategoryId = row.mallCategoryId || null
    form.storeIds = Array.isArray(row.storeIds) ? [...row.storeIds] : []
  }
  editVisible.value = true
}

async function save() {
  if (!form.name.trim()) { ElMessage.warning('请填写名称'); return }
  const payload: any = {
    name: form.name,
    category: form.category,
    price: yuanToFen(form.priceYuan),
    costPrice: yuanToFen(form.costPriceYuan),
    stock: form.category === 'GOODS' ? (Number(form.stock) || 0) : null,
    cover: form.cover,
    description: form.description,
    status: form.status,
    mallVisible: form.mallVisible,
    mallCategoryId: form.mallVisible ? form.mallCategoryId : null,
    storeIds: Array.isArray(form.storeIds) ? form.storeIds : []
  }
  saving.value = true
  try {
    if (form.id) {
      await productsApi.update(form.id, payload)
      ElMessage.success('已更新')
    } else {
      await productsApi.create(payload)
      ElMessage.success('已创建')
    }
    editVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row: any) {
  const next = row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  await productsApi.changeStatus(row.id, next)
  ElMessage.success(next === 'ACTIVE' ? '已上架' : '已下架')
  load()
}

async function remove(row: any) {
  try {
    await ElMessageBox.confirm(`确定删除「${row.name}」?`, '提示', { type: 'warning' })
  } catch { return }
  await productsApi.remove(row.id)
  ElMessage.success('已删除')
  load()
}

function openStock(row: any) {
  stockForm.id = row.id
  stockForm.name = row.name
  stockForm.stock = row.stock ?? 0
  stockForm.mode = 'INC'
  stockForm.value = 0
  stockVisible.value = true
}

async function saveStock() {
  if (stockForm.mode === 'SET' && (Number(stockForm.value) || 0) < 0) {
    ElMessage.warning('设为模式库存不能为负数')
    return
  }
  if (stockForm.mode === 'INC' && !Number(stockForm.value)) {
    ElMessage.warning('请输入增减数量')
    return
  }
  saving.value = true
  try {
    await productsApi.stock(stockForm.id, stockForm.mode, Number(stockForm.value) || 0)
    ElMessage.success('库存已调整')
    stockVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

const yuan = fenToYuan

onMounted(() => {
  load()
  mallApi.categories().then((d: any) => { mallCategories.value = d || [] }).catch(() => {})
  storesApi.list().then((d: any) => { stores.value = d || [] }).catch(() => {})
})
</script>

<style scoped>
.filter-card { padding: 12px 16px; margin-bottom: 14px; }
.pager { display: flex; justify-content: flex-end; padding: 12px 6px 0; }
.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 14px;
}
.product-card { overflow: hidden; display: flex; flex-direction: column; }
.cover {
  height: 96px;
  display: flex; align-items: center; justify-content: center;
  color: rgba(255,255,255,0.92);
}
.cover-GOODS { background: var(--accent-clay); }
.cover-SERVICE { background: var(--brand); }
.cover-icon { font-size: 36px; }
.card-body { padding: 12px 14px 14px; display: flex; flex-direction: column; gap: 6px; }
.row-1 { display: flex; align-items: center; justify-content: space-between; }
.row-1 .name { font-weight: 600; color: var(--ink); font-size: 14px; }
.row-2 { display: flex; align-items: baseline; gap: 10px; }
.row-2 .price { font-size: 18px; color: var(--primary-action); font-weight: 600; }
.row-2 .meta { color: var(--muted); font-size: 12px; }
.row-3 { display: flex; gap: 4px; margin-top: 4px; }
.stock-target { color: var(--ink-2); margin-bottom: 8px; }
.muted-hint { color: var(--muted); font-size: 12px; margin-left: 8px; }
.form-hint { margin-left: 8px; color: var(--muted); font-size: 12px; }
</style>
