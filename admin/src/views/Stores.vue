<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2 class="page-title">门店管理</h2>
        <div class="page-sub">{{ storeSlogan }}</div>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate" class="btn-scale">新增门店</el-button>
    </div>

    <div class="store-grid" v-loading="loading">
      <div v-for="s in list" :key="s.id" class="store-card x-card btn-scale">
        <div class="store-head">
          <div class="store-mark">{{ s.name?.charAt(0) }}</div>
          <div class="store-info">
            <div class="store-name">{{ s.name }}</div>
            <el-tag :type="s.status === 'CLOSED' ? 'info' : 'success'" effect="light" size="small">
              {{ s.status === 'CLOSED' ? '已停业' : '营业中' }}
            </el-tag>
          </div>
        </div>
        <div class="store-meta">
          <div class="meta-row"><el-icon><Location /></el-icon><span>{{ s.address || '—' }}</span></div>
          <div class="meta-row"><el-icon><Phone /></el-icon><span>{{ s.phone || '—' }}</span></div>
          <div class="meta-row"><el-icon><Clock /></el-icon><span>{{ s.businessHours || '—' }}</span></div>
        </div>
        <div class="store-toggle">
          <span class="toggle-label">营业状态</span>
          <el-switch
            :model-value="s.status !== 'CLOSED'"
            active-text="营业中"
            inactive-text="已停业"
            inline-prompt
            @change="(v: any) => onToggleStatus(s, v)"
          />
        </div>
        <div class="store-actions">
          <el-button link type="primary" @click="openEdit(s)">编辑</el-button>
          <el-button link type="danger" @click="onRemove(s)">删除</el-button>
        </div>
      </div>
      <div v-if="!loading && !list.length" class="empty">
        <el-empty description="暂无门店" />
      </div>
    </div>

    <el-dialog v-model="formVisible" :title="editing ? '编辑门店' : '新增门店'" width="480px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="84px">
        <el-form-item label="门店名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="form.address" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="营业时间">
          <el-input v-model="form.businessHours" placeholder="如 09:00-22:00" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="OPEN">营业中</el-radio>
            <el-radio value="CLOSED">已停业</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Location, Phone, Clock } from '@element-plus/icons-vue'
import { storesApi } from '@/api'
import type { Store } from '@/types'

const storeSlogan = [
  '每一家门店，都是一个有温度的角落',
  '把门店写成名片，把地址写成安心',
  '灯火可亲，是一家店最朴素的骄傲'
][Math.floor(Math.random() * 3)]

const loading = ref(false)
const list = ref<Store[]>([])

async function loadList() {
  loading.value = true
  try {
    list.value = (await storesApi.list()) || []
  } finally {
    loading.value = false
  }
}

const formVisible = ref(false)
const editing = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  id: 0,
  name: '',
  address: '',
  phone: '',
  businessHours: '',
  status: 'OPEN'
})
const formRules: FormRules = {
  name: [{ required: true, message: '请输入门店名称', trigger: 'blur' }]
}
function openCreate() {
  editing.value = false
  Object.assign(form, { id: 0, name: '', address: '', phone: '', businessHours: '', status: 'OPEN' })
  formVisible.value = true
}
function openEdit(row: Store) {
  editing.value = true
  Object.assign(form, {
    id: row.id,
    name: row.name,
    address: row.address || '',
    phone: row.phone || '',
    businessHours: row.businessHours || '',
    status: row.status || 'OPEN'
  })
  formVisible.value = true
}
async function submitForm() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (editing.value) {
      await storesApi.update(form.id, { ...form })
      ElMessage.success('已更新')
    } else {
      await storesApi.create({ ...form })
      ElMessage.success('已新增')
    }
    formVisible.value = false
    loadList()
  } finally {
    saving.value = false
  }
}
async function onRemove(row: Store) {
  await ElMessageBox.confirm(`确认删除门店「${row.name}」？有会员/员工关联时将禁用`, '提示', { type: 'warning' })
  await storesApi.remove(row.id)
  ElMessage.success('已删除')
  loadList()
}

async function onToggleStatus(row: Store, open: boolean) {
  const newStatus = open ? 'OPEN' : 'CLOSED'
  try {
    await storesApi.update(row.id, { ...row, status: newStatus })
    row.status = newStatus
    ElMessage.success(`${row.name} 已${open ? '营业' : '停业'}`)
  } catch {
    ElMessage.error('操作失败')
  }
}

onMounted(() => loadList())
</script>

<style scoped>
.store-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 14px;
}
.store-card {
  padding: 18px;
}
.store-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}
.store-mark {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: var(--primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 600;
  flex-shrink: 0;
}
.store-info {
  flex: 1;
}
.store-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--ink);
  margin-bottom: 4px;
}
.store-meta {
  margin-bottom: 12px;
}
.meta-row {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--muted);
  margin: 6px 0;
}
.store-actions {
  display: flex;
  justify-content: flex-end;
  gap: 4px;
  border-top: 1px solid var(--line);
  padding-top: 10px;
}
.store-toggle {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
  border-top: 1px dashed var(--line);
  margin-bottom: 8px;
}
.toggle-label {
  font-size: 13px;
  color: var(--muted);
}
.empty {
  grid-column: 1 / -1;
}
</style>
