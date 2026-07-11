<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2 class="page-title">代理商管理</h2>
        <div class="page-sub">{{ agentSlogan }}</div>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate" class="btn-scale">新增代理商</el-button>
    </div>

    <div class="x-card list-card" v-loading="loading">
      <el-table :data="list" size="small">
        <el-table-column label="名称" prop="name" min-width="120" />
        <el-table-column label="联系人" prop="contactName" width="100" />
        <el-table-column label="电话" prop="phone" width="130" />
        <el-table-column label="AppId" prop="appId" min-width="140">
          <template #default="{ row }">
            <span class="val muted">{{ row.appId || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="商户号" prop="mchId" width="120">
          <template #default="{ row }">
            <span class="val">{{ row.mchId || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="抽佣比例" width="100" align="right">
          <template #default="{ row }">
            <span class="val t-brand">{{ formatRate(row.commissionRate) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="挂靠商家" width="90" align="right">
          <template #default="{ row }">
            <span class="val">{{ row.storeCount ?? 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'" effect="light" size="small">
              {{ row.status === 'ENABLED' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" class-name="row-actions">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="primary" @click="openStats(row)">业绩</el-button>
            <el-button link type="danger" @click="onRemove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="!loading && !list.length" class="empty-state">
        <div class="empty-text">暂无代理商</div>
        <div class="empty-tip">点击右上角「新增代理商」开始拓展渠道</div>
      </div>
    </div>

    <!-- 编辑/新增弹窗 -->
    <el-dialog v-model="formVisible" :title="editing ? '编辑代理商' : '新增代理商'" width="520px" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="联系人" prop="contactName">
          <el-input v-model="form.contactName" />
        </el-form-item>
        <el-form-item label="电话" prop="phone">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="AppId">
          <el-input v-model="form.appId" placeholder="公众号 AppId（可空）" />
        </el-form-item>
        <el-form-item label="商户号">
          <el-input v-model="form.mchId" placeholder="微信支付商户号（可空）" />
        </el-form-item>
        <el-form-item label="抽佣比例">
          <el-input-number v-model="form.commissionRate" :min="0" :max="100" :precision="2" controls-position="right" style="width: 160px" />
          <span class="muted" style="margin-left: 8px">%</span>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="ENABLED">启用</el-radio>
            <el-radio value="DISABLED">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm" class="btn-scale">保存</el-button>
      </template>
    </el-dialog>

    <!-- 业绩弹窗 -->
    <el-dialog v-model="statsVisible" :title="`${statsTarget?.name} · 业绩统计`" width="460px" destroy-on-close>
      <div v-loading="statsLoading" class="stats-grid">
        <div class="x-card stat-tile">
          <div class="stat-label">挂靠商家数</div>
          <div class="stat-value val">{{ stats.storeCount ?? 0 }}</div>
        </div>
        <div class="x-card stat-tile">
          <div class="stat-label">总交易额</div>
          <div class="stat-value val">¥ {{ formatMoney(stats.totalAmount) }}</div>
        </div>
        <div class="x-card stat-tile">
          <div class="stat-label">抽佣金额</div>
          <div class="stat-value val t-brand">¥ {{ formatMoney(stats.commissionAmount) }}</div>
        </div>
      </div>
      <div v-if="stats.updatedAt" class="stats-time">统计时间: {{ formatDateTime(stats.updatedAt) }}</div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { agentsApi } from '@/api'
import { formatMoney, formatDateTime } from '@/utils/format'

const agentSlogan = [
  '让代理帮你拓展，让商家安心经营',
  '把渠道理成一张清晰的网',
  '一个人走得快，一群代理走得远'
][Math.floor(Math.random() * 3)]

const loading = ref(false)
const list = ref<any[]>([])

// 抽佣比例展示：后端若用整数百分比或小数需统一展示
function formatRate(rate: number | undefined | null): string {
  if (rate === null || rate === undefined || isNaN(rate as number)) return '0%'
  return `${Number(rate).toFixed(2)}%`
}

async function loadList() {
  loading.value = true
  try {
    list.value = (await agentsApi.list()) || []
  } finally {
    loading.value = false
  }
}

// ============ 编辑/新增 ============
const formVisible = ref(false)
const editing = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<any>({
  id: 0,
  name: '',
  contactName: '',
  phone: '',
  appId: '',
  mchId: '',
  commissionRate: 0,
  status: 'ENABLED',
  remark: ''
})
const formRules: FormRules = {
  name: [{ required: true, message: '请输入代理商名称', trigger: 'blur' }],
  contactName: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }]
}
function openCreate() {
  editing.value = false
  Object.assign(form, {
    id: 0, name: '', contactName: '', phone: '',
    appId: '', mchId: '', commissionRate: 0, status: 'ENABLED', remark: ''
  })
  formVisible.value = true
}
function openEdit(row: any) {
  editing.value = true
  Object.assign(form, {
    id: row.id,
    name: row.name || '',
    contactName: row.contactName || '',
    phone: row.phone || '',
    appId: row.appId || '',
    mchId: row.mchId || '',
    commissionRate: row.commissionRate ?? 0,
    status: row.status || 'ENABLED',
    remark: row.remark || ''
  })
  formVisible.value = true
}
async function submitForm() {
  await formRef.value?.validate()
  saving.value = true
  try {
    if (editing.value) {
      await agentsApi.update(form.id, { ...form })
      ElMessage.success('已更新')
    } else {
      await agentsApi.create({ ...form })
      ElMessage.success('已新增')
    }
    formVisible.value = false
    loadList()
  } finally {
    saving.value = false
  }
}
async function onRemove(row: any) {
  await ElMessageBox.confirm(`确认删除代理商「${row.name}」？关联商家将解除挂靠`, '提示', { type: 'warning' })
  await agentsApi.remove(row.id)
  ElMessage.success('已删除')
  loadList()
}

// ============ 业绩统计 ============
const statsVisible = ref(false)
const statsLoading = ref(false)
const statsTarget = ref<any>(null)
const stats = reactive<any>({
  storeCount: 0,
  totalAmount: 0,
  commissionAmount: 0,
  updatedAt: ''
})
async function openStats(row: any) {
  statsTarget.value = row
  statsVisible.value = true
  Object.assign(stats, { storeCount: 0, totalAmount: 0, commissionAmount: 0, updatedAt: '' })
  statsLoading.value = true
  try {
    const data: any = await agentsApi.stats(row.id)
    if (data) {
      Object.assign(stats, {
        storeCount: data.storeCount ?? 0,
        totalAmount: data.totalAmount ?? 0,
        commissionAmount: data.commissionAmount ?? 0,
        updatedAt: data.updatedAt || ''
      })
    }
  } finally {
    statsLoading.value = false
  }
}

onMounted(() => loadList())
</script>

<style scoped>
.list-card {
  padding: 6px 10px;
}
.empty-state {
  padding: 48px 16px;
}

/* 业绩统计 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}
.stat-tile {
  padding: 16px 14px;
  text-align: center;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.stat-label {
  font-size: 12px;
  color: var(--muted);
  letter-spacing: 0.06em;
}
.stat-value {
  font-size: 20px;
  font-weight: 500;
  color: var(--ink);
  line-height: 1.2;
}
.stats-time {
  font-size: 12px;
  color: var(--muted);
  text-align: right;
  margin-top: 12px;
}
</style>
