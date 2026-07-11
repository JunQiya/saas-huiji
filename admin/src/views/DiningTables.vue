<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2 class="page-title">门店点餐</h2>
        <div class="page-sub">把每一张桌台都安排妥当，是店里最朴素的秩序</div>
      </div>
      <div class="header-actions">
        <el-select v-model="storeId" placeholder="选择门店" style="width: 180px" @change="onStoreChange">
          <el-option v-for="s in stores" :key="s.id" :label="s.name" :value="s.id" />
        </el-select>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="dining-tabs">
      <el-tab-pane label="桌台管理" name="tables">
        <div class="header-actions" style="margin-bottom: 12px">
          <el-button type="primary" :icon="Plus" :disabled="!storeId" @click="openCreate" class="btn-scale">新增桌台</el-button>
        </div>
        <div class="filter-bar">
      <el-input v-model="keyword" placeholder="搜索桌名/区域" clearable style="width: 220px" />
      <el-radio-group v-model="statusFilter">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="IDLE">空闲</el-radio-button>
        <el-radio-button value="OCCUPIED">占用中</el-radio-button>
      </el-radio-group>
    </div>

    <div v-loading="loading" class="table-grid">
      <div v-for="t in filteredList" :key="t.id" class="table-card x-card btn-scale">
        <div class="tc-head">
          <div class="tc-mark" :class="t.status === 'OCCUPIED' ? 'is-busy' : ''">{{ (t.name || '').charAt(0) }}</div>
          <div class="tc-info">
            <div class="tc-name">{{ t.name }}</div>
            <div class="tc-area">
              <span class="chip" :class="t.status === 'OCCUPIED' ? 'warning' : 'success'">
                {{ t.status === 'OCCUPIED' ? '占用中' : '空闲' }}
              </span>
            </div>
          </div>
        </div>
        <div class="tc-meta">
          <div class="meta-row"><el-icon><Location /></el-icon><span>{{ t.area || '未分区' }}</span></div>
          <div class="meta-row"><el-icon><User /></el-icon><span>{{ t.seats }} 座</span></div>
          <div class="meta-row"><el-icon><Rank /></el-icon><span>排序 {{ t.sortOrder ?? 0 }}</span></div>
        </div>
        <div class="tc-actions">
          <el-button link type="primary" @click="onQrcode(t)">二维码</el-button>
          <el-button v-if="t.status !== 'OCCUPIED'" link type="warning" @click="onOccupy(t)">占用</el-button>
          <el-button v-else link type="success" @click="onFree(t)">释放</el-button>
          <el-button link type="primary" @click="openEdit(t)">编辑</el-button>
          <el-button link type="danger" @click="onRemove(t)">删除</el-button>
        </div>
      </div>
      <div v-if="!loading && !filteredList.length" class="empty">
        <el-empty :description="storeId ? '暂无桌台' : '请先选择门店'" />
      </div>
    </div>
      </el-tab-pane>

      <el-tab-pane label="菜单分类" name="categories">
        <div class="header-actions" style="margin-bottom: 12px">
          <el-button type="primary" :icon="Plus" :disabled="!storeId" @click="openCategoryCreate" class="btn-scale">新增分类</el-button>
        </div>
        <div class="x-card">
          <el-table :data="categories" stripe size="small">
            <el-table-column label="排序" prop="sortOrder" width="80" />
            <el-table-column label="分类名称" prop="name" min-width="160" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'" size="small">{{ row.status === 'ENABLED' ? '启用' : '禁用' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200">
              <template #default="{ row }">
                <el-button link type="primary" @click="openCategoryEdit(row)">编辑</el-button>
                <el-button link type="primary" @click="openBindProducts(row)">绑定商品</el-button>
                <el-button link type="danger" @click="removeCategory(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 新增/编辑桌台 -->
    <el-dialog v-model="formVisible" :title="editing ? '编辑桌台' : '新增桌台'" width="460px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
        <el-form-item label="桌台名称" prop="name">
          <el-input v-model="form.name" placeholder="如 A1、大厅 3 号" />
        </el-form-item>
        <el-form-item label="区域" prop="area">
          <el-input v-model="form.area" placeholder="如 大厅、包间、露台" />
        </el-form-item>
        <el-form-item label="座位数" prop="seats">
          <el-input-number v-model="form.seats" :min="1" :max="99" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <!-- 二维码弹窗 -->
    <el-dialog v-model="qrVisible" title="桌台二维码" width="320px" :close-on-click-modal="true">
      <div class="qr-body">
        <div class="qr-name">{{ qrRow?.name }}</div>
        <div class="qr-area">{{ qrRow?.area || '未分区' }} · {{ qrRow?.seats }} 座</div>
        <div class="qr-box">
          <img v-if="qrSrc" :src="qrSrc" alt="桌台二维码" class="qr-img" />
          <div v-else class="qr-loading"><el-icon class="is-loading"><Loading /></el-icon></div>
        </div>
        <div class="qr-tip">顾客扫码即可进入点餐页面</div>
      </div>
      <template #footer>
        <el-button @click="qrVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 新增/编辑菜单分类 -->
    <el-dialog v-model="catFormVisible" :title="catEditing ? '编辑分类' : '新增分类'" width="420px" :close-on-click-modal="false">
      <el-form :model="catForm" label-width="80px">
        <el-form-item label="分类名称" required>
          <el-input v-model="catForm.name" placeholder="如 招牌洗护、头皮护理" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="catForm.sortOrder" :min="0" :step="1" style="width: 180px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="catForm.status">
            <el-radio-button value="ENABLED">启用</el-radio-button>
            <el-radio-button value="DISABLED">禁用</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="catFormVisible = false">取消</el-button>
        <el-button type="primary" :loading="catSaving" @click="saveCategory" class="btn-scale">保存</el-button>
      </template>
    </el-dialog>

    <!-- 绑定商品弹窗 -->
    <el-dialog v-model="bindVisible" :title="`绑定商品到「${bindCategory?.name || ''}」`" width="560px">
      <div v-if="!products.length" class="empty">暂无可绑定的商品</div>
      <el-checkbox-group v-model="bindSelected" v-else>
        <div class="bind-list">
          <div v-for="p in products" :key="p.id" class="bind-item">
            <el-checkbox :value="p.id">{{ p.name }}（¥{{ (p.price / 100).toFixed(2) }}）</el-checkbox>
          </div>
        </div>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="bindVisible = false">取消</el-button>
        <el-button type="primary" :loading="bindSaving" @click="submitBind" class="btn-scale">保存绑定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Location, User, Rank, Loading } from '@element-plus/icons-vue'
import { diningApi, storesApi, productsApi } from '@/api'

const loading = ref(false)
const list = ref<any[]>([])
const stores = ref<any[]>([])
const storeId = ref<number | undefined>(undefined)
const activeTab = ref('tables')
const keyword = ref('')
const statusFilter = ref('')

const filteredList = computed(() => {
  let arr = list.value
  if (statusFilter.value) arr = arr.filter(t => t.status === statusFilter.value)
  if (keyword.value) {
    const k = keyword.value.toLowerCase()
    arr = arr.filter(t => (t.name || '').toLowerCase().includes(k) || (t.area || '').toLowerCase().includes(k))
  }
  return arr
})

async function loadStores() {
  try { stores.value = (await storesApi.list()) || [] } catch {}
}
async function loadList() {
  if (!storeId.value) { list.value = []; return }
  loading.value = true
  try {
    list.value = (await diningApi.tables(storeId.value)) || []
  } catch {
    list.value = []
  } finally {
    loading.value = false
  }
}

// 表单
const formVisible = ref(false)
const editing = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({ id: 0, storeId: 0, name: '', area: '', seats: 4, sortOrder: 0 })
const formRules: FormRules = {
  name: [{ required: true, message: '请输入桌台名称', trigger: 'blur' }],
  seats: [{ required: true, message: '请输入座位数', trigger: 'blur' }]
}
function openCreate() {
  editing.value = false
  Object.assign(form, { id: 0, storeId: storeId.value, name: '', area: '', seats: 4, sortOrder: 0 })
  formVisible.value = true
}
function openEdit(row: any) {
  editing.value = true
  Object.assign(form, {
    id: row.id,
    storeId: row.storeId || storeId.value,
    name: row.name || '',
    area: row.area || '',
    seats: row.seats ?? 4,
    sortOrder: row.sortOrder ?? 0
  })
  formVisible.value = true
}
async function submitForm() {
  await formRef.value?.validate()
  saving.value = true
  try {
    await diningApi.saveTable({ ...form })
    ElMessage.success(editing.value ? '已更新' : '已新增')
    formVisible.value = false
    loadList()
  } finally {
    saving.value = false
  }
}
async function onRemove(row: any) {
  await ElMessageBox.confirm(`确认删除桌台「${row.name}」？`, '提示', { type: 'warning' })
  await diningApi.removeTable(row.id)
  ElMessage.success('已删除')
  loadList()
}
async function onOccupy(row: any) {
  await diningApi.occupyTable(row.id)
  ElMessage.success('已标记占用')
  loadList()
}
async function onFree(row: any) {
  await diningApi.freeTable(row.id)
  ElMessage.success('已释放桌台')
  loadList()
}

// 二维码
const qrVisible = ref(false)
const qrRow = ref<any>(null)
const qrSrc = ref('')
async function onQrcode(row: any) {
  qrRow.value = row
  qrSrc.value = ''
  qrVisible.value = true
  try {
    const res: any = await diningApi.qrcode(row.id)
    // 兼容返回字符串或对象
    qrSrc.value = typeof res === 'string' ? res : (res?.qrcode || res?.url || res?.data || '')
  } catch {
    ElMessage.error('二维码生成失败')
  }
}

// ============ 菜单分类 ============
const categories = ref<any[]>([])
async function loadCategories() {
  if (!storeId.value) { categories.value = []; return }
  try {
    categories.value = (await diningApi.categories(storeId.value)) || []
  } catch {
    categories.value = []
  }
}

const catFormVisible = ref(false)
const catEditing = ref(false)
const catSaving = ref(false)
const catForm = reactive({ id: 0, storeId: 0, name: '', sortOrder: 0, status: 'ENABLED' })

function openCategoryCreate() {
  catEditing.value = false
  Object.assign(catForm, { id: 0, storeId: storeId.value, name: '', sortOrder: 0, status: 'ENABLED' })
  catFormVisible.value = true
}
function openCategoryEdit(row: any) {
  catEditing.value = true
  Object.assign(catForm, {
    id: row.id,
    storeId: row.storeId || storeId.value,
    name: row.name || '',
    sortOrder: row.sortOrder ?? 0,
    status: row.status || 'ENABLED'
  })
  catFormVisible.value = true
}
async function saveCategory() {
  if (!catForm.name?.trim()) { ElMessage.warning('请输入分类名称'); return }
  catSaving.value = true
  try {
    await diningApi.saveCategory({ ...catForm })
    ElMessage.success(catEditing.value ? '已更新' : '已新增')
    catFormVisible.value = false
    loadCategories()
  } finally {
    catSaving.value = false
  }
}
async function removeCategory(row: any) {
  await ElMessageBox.confirm(`确认删除分类「${row.name}」？已绑定商品将解除关联。`, '提示', { type: 'warning' })
  await diningApi.removeCategory(row.id)
  ElMessage.success('已删除')
  loadCategories()
}

// 绑定商品
const bindVisible = ref(false)
const bindCategory = ref<any>(null)
const products = ref<any[]>([])
const bindSelected = ref<number[]>([])
const bindSaving = ref(false)

async function openBindProducts(row: any) {
  bindCategory.value = row
  bindSelected.value = []
  bindVisible.value = true
  try {
    const data: any = await productsApi.list({ page: 1, size: 200, status: 'ACTIVE' })
    products.value = data?.records || data?.list || data?.content || []
    // 预选已绑定该分类的商品
    bindSelected.value = products.value
      .filter((p: any) => p.menuCategoryId === row.id)
      .map((p: any) => p.id)
  } catch {
    products.value = []
  }
}
async function submitBind() {
  if (!bindCategory.value) return
  bindSaving.value = true
  try {
    await diningApi.bindProducts(bindCategory.value.id, bindSelected.value)
    ElMessage.success('绑定已更新')
    bindVisible.value = false
  } finally {
    bindSaving.value = false
  }
}

// 门店切换：同时加载桌台和分类
function onStoreChange() {
  loadList()
  loadCategories()
}

onMounted(async () => {
  await loadStores()
  if (stores.value.length && !storeId.value) {
    storeId.value = stores.value[0].id
    loadList()
    loadCategories()
  }
})
</script>

<style scoped>
.header-actions { display: flex; gap: 10px; align-items: center; }

.table-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 14px;
}
.table-card { padding: 16px; }
.tc-head {
  display: flex; align-items: center; gap: 12px;
  margin-bottom: 12px;
}
.tc-mark {
  width: 38px; height: 38px;
  border-radius: 10px;
  background: var(--brand-soft);
  color: var(--brand-ink);
  display: flex; align-items: center; justify-content: center;
  font-size: 17px; font-weight: 600;
  font-family: var(--font-serif);
  flex-shrink: 0;
}
.tc-mark.is-busy { background: var(--warning-soft); color: var(--warning-deep); }
.tc-info { flex: 1; }
.tc-name {
  font-size: 15px; font-weight: 500; color: var(--ink);
  font-family: var(--font-serif); letter-spacing: 0.04em;
  margin-bottom: 5px;
}
.tc-meta { margin-bottom: 10px; }
.meta-row {
  display: flex; align-items: center; gap: 6px;
  font-size: 12.5px; color: var(--muted);
  margin: 5px 0;
}
.tc-actions {
  display: flex; justify-content: flex-end; gap: 2px;
  border-top: 1px solid var(--line);
  padding-top: 8px;
}
.empty { grid-column: 1 / -1; }

/* 二维码弹窗 */
.qr-body { text-align: center; padding: 4px 0; }
.qr-name {
  font-size: 15px; font-weight: 500; color: var(--ink);
  font-family: var(--font-serif); letter-spacing: 0.04em;
}
.qr-area { font-size: 12px; color: var(--muted); margin: 4px 0 14px; }
.qr-box {
  display: flex; justify-content: center; align-items: center;
  width: 200px; height: 200px; margin: 0 auto 12px;
  background: var(--surface-2); border: 1px solid var(--line);
  border-radius: var(--r-md);
}
.qr-img { width: 180px; height: 180px; object-fit: contain; }
.qr-loading { color: var(--muted); font-size: 28px; }
.qr-tip { font-size: 12px; color: var(--muted); letter-spacing: 0.04em; }
</style>
