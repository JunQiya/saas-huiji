<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2 class="page-title">桌台管理</h2>
        <div class="page-sub">把每一张桌台都安排妥当，是店里最朴素的秩序</div>
      </div>
      <div class="header-actions">
        <el-select v-model="storeId" placeholder="选择门店" style="width: 180px" @change="loadList">
          <el-option v-for="s in stores" :key="s.id" :label="s.name" :value="s.id" />
        </el-select>
        <el-button type="primary" :icon="Plus" :disabled="!storeId" @click="openCreate" class="btn-scale">新增桌台</el-button>
      </div>
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
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Location, User, Rank, Loading } from '@element-plus/icons-vue'
import { diningApi, storesApi } from '@/api'

const loading = ref(false)
const list = ref<any[]>([])
const stores = ref<any[]>([])
const storeId = ref<number | undefined>(undefined)
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

onMounted(async () => {
  await loadStores()
  if (stores.value.length && !storeId.value) {
    storeId.value = stores.value[0].id
    loadList()
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
