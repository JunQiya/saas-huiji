<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2 class="page-title">员工管理</h2>
        <div class="page-sub">{{ empSlogan }}</div>
      </div>
      <div class="header-actions">
        <el-button :icon="RefreshRight" @click="loadList">刷新</el-button>
        <el-button :icon="Upload" @click="openImport" plain>导入</el-button>
        <el-button :icon="Download" @click="onExport" plain>导出</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate" class="btn-scale">新增员工</el-button>
      </div>
    </div>

    <div class="x-card table-wrap">
      <div class="filter-bar">
        <el-select v-model="query.storeId" placeholder="门店" clearable @change="loadList">
          <el-option v-for="s in stores" :key="s.id" :label="s.name" :value="s.id" />
        </el-select>
        <el-select v-model="query.role" placeholder="角色" clearable @change="loadList" style="width: 160px">
          <el-option label="租户管理员" value="TENANT_ADMIN" />
          <el-option label="店长" value="STORE_MANAGER" />
          <el-option label="员工" value="STAFF" />
          <el-option label="收银" value="CASHIER" />
        </el-select>
      </div>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column label="员工" min-width="150">
          <template #default="{ row }">
            <div class="cell-member">
              <el-avatar :size="32" class="m-avatar">{{ row.name?.charAt(0) }}</el-avatar>
              <div>
                <div class="m-name">{{ row.name }}</div>
                <div class="m-phone">{{ row.username }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="手机号" width="130" prop="phone">
          <template #default="{ row }">{{ row.phone || '—' }}</template>
        </el-table-column>
        <el-table-column label="角色" width="110">
          <template #default="{ row }">
            <el-tag :type="roleType(row.role)" effect="light" size="small">{{ roleText(row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="所属门店" min-width="160">
          <template #default="{ row }">
            <span v-if="!row.storeIds?.length" class="muted">—</span>
            <el-tag v-for="sid in row.storeIds" :key="sid" size="small" class="m-tag" effect="plain">{{ storeName(sid) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'DISABLED' ? 'info' : 'success'" effect="light" size="small">{{ row.status === 'DISABLED' ? '已禁用' : '正常' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right" class-name="row-actions">
          <template #default="{ row }">
            <el-button link type="primary" @click="openPerf(row)">业绩</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="warning" @click="onResetPwd(row)">重置密码</el-button>
            <el-button link type="danger" @click="onDisable(row)">禁用</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 新增/编辑 -->
    <el-dialog v-model="formVisible" :title="editing ? '编辑员工' : '新增员工'" width="500px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="84px">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="登录账号" prop="username">
          <el-input v-model="form.username" :disabled="editing" />
        </el-form-item>
        <el-form-item v-if="!editing" label="初始密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="至少 6 位" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" style="width: 100%">
            <el-option label="租户管理员" value="TENANT_ADMIN" />
            <el-option label="店长" value="STORE_MANAGER" />
            <el-option label="员工" value="STAFF" />
            <el-option label="收银" value="CASHIER" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属门店">
          <el-select v-model="form.storeIds" multiple collapse-tags placeholder="可多选" style="width: 100%">
            <el-option v-for="s in stores" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <!-- 业绩弹窗 -->
    <el-dialog v-model="perfVisible" title="员工业绩" width="560px">
      <div v-loading="perfLoading">
        <div class="perf-head">
          <el-avatar :size="40" class="m-avatar">{{ perfEmployee?.name?.charAt(0) }}</el-avatar>
          <div>
            <div class="m-name">{{ perfEmployee?.name }}</div>
            <div class="m-phone">{{ roleText(perfEmployee?.role) }}</div>
          </div>
        </div>
        <el-table :data="perfList" size="small" style="margin-top: 14px">
          <el-table-column label="月份" prop="month" width="120" />
          <el-table-column label="业绩金额" align="right">
            <template #default="{ row }">¥{{ formatMoney(row.amount) }}</template>
          </el-table-column>
          <el-table-column label="订单数" prop="count" align="right" width="100" />
        </el-table>
        <div v-if="!perfList.length && !perfLoading" class="muted center">暂无业绩数据</div>
      </div>
    </el-dialog>

    <!-- 隐藏文件输入（必须在 template 内） -->
    <input ref="fileInputRef" type="file" accept=".csv" style="display: none" @change="onImportFile" />

    <!-- 导入预览弹窗 -->
    <el-dialog v-model="importVisible" title="导入员工" width="560px">
      <div v-if="importPreview.length">
        <div class="import-tip">共解析 {{ importPreview.length }} 条，确认后批量创建：</div>
        <el-table :data="importPreview.slice(0, 50)" size="small" max-height="300">
          <el-table-column label="姓名" prop="name" width="100" />
          <el-table-column label="账号" prop="username" width="120" />
          <el-table-column label="手机号" prop="phone" width="130" />
          <el-table-column label="角色" prop="role" width="100" />
        </el-table>
        <div v-if="importPreview.length > 50" class="import-tip" style="margin-top: 8px">仅显示前 50 条，共 {{ importPreview.length }} 条</div>
      </div>
      <div v-else class="muted center">无有效数据</div>
      <template #footer>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button type="primary" :loading="importing" :disabled="!importPreview.length" @click="onImportSubmit">开始导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">

import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, ElMessageBoxOptions, type FormInstance, type FormRules } from 'element-plus'
import { Plus, RefreshRight , Upload, Download } from '@element-plus/icons-vue'
import { employeesApi, storesApi } from '@/api'
import { formatMoney } from '@/utils/format'
import { exportCsv } from '@/utils/csv'
import type { Employee, Store, Performance, Role } from '@/types'

const empSlogan = [
  '每一位伙伴，都值得被善待',
  '好的团队，是彼此记得名字的人',
  '把岗位当作托付，把同事当作同行者'
][Math.floor(Math.random() * 3)]

const fileInputRef = ref<HTMLInputElement>()
const importVisible = ref(false)
const importPreview = ref<any[]>([])
const importing = ref(false)

function openImport() { fileInputRef.value?.click() }

function onExport() {
  if (!list.value.length) { ElMessage.warning('暂无可导出的员工'); return }
  exportCsv(`员工列表-${new Date().toLocaleDateString('zh-CN')}`, list.value, [
    { key: 'name', header: '姓名' },
    { key: 'username', header: '登录账号' },
    { key: 'phone', header: '手机号' },
    { key: 'role', header: '角色', format: r => roleText(r.role) },
    { key: 'storeIds', header: '所属门店', format: r => (r.storeIds || []).map((id: number) => storeName(id)).join('; ') },
    { key: 'status', header: '状态', format: r => r.status === 'DISABLED' ? '已禁用' : '正常' }
  ])
  ElMessage.success(`已导出 ${list.value.length} 条员工`)
}

function onImportFile(e: Event) {
  const f = (e.target as HTMLInputElement).files?.[0]
  if (!f) return
  const reader = new FileReader()
  reader.onload = () => {
    const text = String(reader.result || '')
    const lines = text.split('\n').filter(l => l.trim())
    if (lines.length < 2) { ElMessage.warning('CSV 文件无有效数据'); return }
    const parsed: any[] = []
    for (let i = 1; i < lines.length; i++) {
      const cells = parseCsvLine(lines[i])
      if (cells.length < 2) continue
      const roleMap: Record<string, string> = { '租户管理员': 'TENANT_ADMIN', '店长': 'STORE_MANAGER', '员工': 'STAFF', '收银': 'CASHIER' }
      parsed.push({
        name: cells[0]?.trim() || '',
        username: cells[1]?.trim() || '',
        phone: cells[2]?.trim() || '',
        role: roleMap[cells[3]?.trim()] || cells[3]?.trim() || 'STAFF',
        password: '123456',
        storeIds: []
      })
    }
    importPreview.value = parsed
    importVisible.value = true
  }
  reader.readAsText(f, 'UTF-8')
  ;(e.target as HTMLInputElement).value = ''
}

function parseCsvLine(line: string): string[] {
  const result: string[] = []
  let cur = ''
  let inQuote = false
  for (let i = 0; i < line.length; i++) {
    const ch = line[i]
    if (ch === '"') { inQuote = !inQuote; continue }
    if (ch === ',' && !inQuote) { result.push(cur); cur = ''; continue }
    cur += ch
  }
  result.push(cur)
  return result
}

async function onImportSubmit() {
  importing.value = true
  let ok = 0, fail = 0
  for (const item of importPreview.value) {
    try {
      await employeesApi.create(item)
      ok++
    } catch { fail++ }
  }
  importing.value = false
  importVisible.value = false
  ElMessage.success(`导入完成：成功 ${ok} 条，失败 ${fail} 条`)
  loadList()
}



const loading = ref(false)
const list = ref<Employee[]>([])
const stores = ref<Store[]>([])
const query = reactive({ storeId: undefined as number | undefined, role: '' })

async function loadList() {
  loading.value = true
  try {
    const params: any = {}
    if (query.storeId) params.storeId = query.storeId
    if (query.role) params.role = query.role
    list.value = (await employeesApi.list(params)) || []
  } finally {
    loading.value = false
  }
}
function storeName(id: number) {
  return stores.value.find((s) => s.id === id)?.name || `#${id}`
}
function roleText(r?: Role) {
  return ({ TENANT_ADMIN: '租户管理员', STORE_MANAGER: '店长', STAFF: '员工', CASHIER: '收银' } as any)[r || 'STAFF'] || '员工'
}
function roleType(r?: Role) {
  return ({ TENANT_ADMIN: 'danger', STORE_MANAGER: 'warning', STAFF: 'primary', CASHIER: 'info' } as any)[r || 'STAFF'] || 'info'
}

// 新增/编辑
const formVisible = ref(false)
const editing = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  id: 0,
  name: '',
  username: '',
  password: '',
  phone: '',
  role: 'STAFF' as Role,
  storeIds: [] as number[]
})
const formRules: FormRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, message: '至少 6 位', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}
function openCreate() {
  editing.value = false
  Object.assign(form, { id: 0, name: '', username: '', password: '', phone: '', role: 'STAFF', storeIds: [] })
  formVisible.value = true
}
function openEdit(row: Employee) {
  editing.value = true
  Object.assign(form, {
    id: row.id,
    name: row.name,
    username: row.username,
    password: '',
    phone: row.phone || '',
    role: row.role,
    storeIds: row.storeIds ? [...row.storeIds] : []
  })
  formVisible.value = true
}
async function submitForm() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (editing.value) {
      const { password, ...rest } = form
      await employeesApi.update(form.id, rest)
      ElMessage.success('已更新')
    } else {
      await employeesApi.create({ ...form })
      ElMessage.success('已新增')
    }
    formVisible.value = false
    loadList()
  } finally {
    saving.value = false
  }
}
async function onResetPwd(row: Employee) {
  const { value } = await ElMessageBox.prompt(`为「${row.name}」重置密码`, '重置密码', {
    inputPattern: /^.{6,}$/,
    inputErrorMessage: '至少 6 位',
    inputPlaceholder: '输入新密码'
  })
  await employeesApi.resetPassword(row.id, value)
  ElMessage.success('密码已重置')
}
async function onDisable(row: Employee) {
  await ElMessageBox.confirm(`确认禁用员工「${row.name}」？`, '提示', { type: 'warning' })
  await employeesApi.remove(row.id)
  ElMessage.success('已禁用')
  loadList()
}

// 业绩
const perfVisible = ref(false)
const perfLoading = ref(false)
const perfEmployee = ref<Employee>()
const perfList = ref<Performance[]>([])
async function openPerf(row: Employee) {
  perfEmployee.value = row
  perfVisible.value = true
  perfLoading.value = true
  perfList.value = []
  try {
    perfList.value = (await employeesApi.performance(row.id)) || []
  } finally {
    perfLoading.value = false
  }
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
.header-actions {
  display: flex;
  gap: 8px;
}
.cell-member {
  display: flex;
  align-items: center;
  gap: 10px;
}
.m-avatar {
  background: var(--accent);
  color: #fff;
  font-size: 13px;
  flex-shrink: 0;
}
.m-name {
  font-size: 13px;
  color: var(--ink);
  font-weight: 500;
}
.m-phone {
  font-size: 12px;
  color: var(--muted);
  margin-top: 2px;
}
.m-tag {
  margin-right: 4px;
}
.muted {
  color: var(--muted);
}
.center {
  text-align: center;
  padding: 20px;
}
.perf-head {
  display: flex;
  align-items: center;
  gap: 10px;
}
.import-tip {
  font-size: 12.5px;
  color: var(--muted);
  margin-bottom: 10px;
  font-family: var(--font-serif);
  letter-spacing: 0.04em;
}
</style>
