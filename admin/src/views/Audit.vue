<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2 class="page-title">审计日志</h2>
        <div class="page-sub">{{ auditSlogan }}</div>
      </div>
    </div>

    <div class="x-card table-wrap">
      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <!-- 操作日志 -->
        <el-tab-pane label="操作日志" name="ops">
          <div class="filter-bar">
            <el-input v-model="opsQuery.operator" placeholder="操作人" clearable @keyup.enter="loadOps" />
            <el-input v-model="opsQuery.action" placeholder="操作类型" clearable @keyup.enter="loadOps" />
            <el-date-picker v-model="opsRange" type="daterange" range-separator="—" start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DD" @change="loadOps" />
            <el-button type="primary" :icon="Search" @click="loadOps">查询</el-button>
            <el-button :icon="RefreshLeft" @click="resetOps">重置</el-button>
            <el-button :icon="Download" @click="exportOps" :disabled="!opsList.length">导出</el-button>
          </div>
          <el-table v-loading="opsLoading" :data="opsList" stripe size="small">
            <el-table-column label="时间" width="155">
              <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="操作人" prop="operatorName" width="110" />
            <el-table-column label="操作" prop="action" width="120" />
            <el-table-column label="目标" prop="target" min-width="120" show-overflow-tooltip />
            <el-table-column label="详情" prop="detail" min-width="200" show-overflow-tooltip />
            <el-table-column label="IP" prop="ip" width="130" />
          </el-table>
          <div class="pager">
            <el-pagination
              v-model:current-page="opsQuery.page"
              v-model:page-size="opsQuery.size"
              :total="opsTotal"
              :page-sizes="[20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
              @current-change="loadOps"
              @size-change="loadOps"
            />
          </div>
        </el-tab-pane>

        <!-- 登录日志 -->
        <el-tab-pane label="登录日志" name="login">
          <div class="filter-bar">
            <el-input v-model="loginQuery.username" placeholder="用户名" clearable @keyup.enter="loadLogins" />
            <el-date-picker v-model="loginRange" type="daterange" range-separator="—" start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DD" @change="loadLogins" />
            <el-button type="primary" :icon="Search" @click="loadLogins">查询</el-button>
            <el-button :icon="RefreshLeft" @click="resetLogin">重置</el-button>
            <el-button :icon="Download" @click="exportLogins" :disabled="!loginList.length">导出</el-button>
          </div>
          <el-table v-loading="loginLoading" :data="loginList" stripe size="small">
            <el-table-column label="时间" width="155">
              <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="用户名" prop="username" width="130" />
            <el-table-column label="IP" prop="ip" width="130" />
            <el-table-column label="归属地" prop="location" width="120">
              <template #default="{ row }">{{ row.location || '—' }}</template>
            </el-table-column>
            <el-table-column label="浏览器" prop="browser" min-width="120" show-overflow-tooltip />
            <el-table-column label="系统" prop="os" min-width="120" show-overflow-tooltip />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'" effect="light" size="small">
                  {{ row.status === 'SUCCESS' ? '成功' : '失败' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="消息" prop="message" min-width="160" show-overflow-tooltip />
          </el-table>
          <div class="pager">
            <el-pagination
              v-model:current-page="loginQuery.page"
              v-model:page-size="loginQuery.size"
              :total="loginTotal"
              :page-sizes="[20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
              @current-change="loadLogins"
              @size-change="loadLogins"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, RefreshLeft, Download } from '@element-plus/icons-vue'
import { auditApi } from '@/api'
import { formatDateTime } from '@/utils/format'
import { exportCsv } from '@/utils/csv'
import type { AuditLog, LoginLog } from '@/types'

const auditSlogan = [
  '每一行日志，都是一次不言之责',
  '守好每一次操作，便是对信任最大的回应',
  '不擅改过往，便是对未来最大的诚意'
][Math.floor(Math.random() * 3)]

const activeTab = ref('ops')

// 操作日志
const opsLoading = ref(false)
const opsList = ref<AuditLog[]>([])
const opsTotal = ref(0)
const opsRange = ref<[string, string] | null>(null)
const opsQuery = reactive({
  operator: '',
  action: '',
  start: '',
  end: '',
  page: 1,
  size: 20
})
async function loadOps() {
  opsLoading.value = true
  try {
    const params: any = { page: opsQuery.page, size: opsQuery.size }
    if (opsQuery.operator) params.operator = opsQuery.operator
    if (opsQuery.action) params.action = opsQuery.action
    if (opsRange.value && opsRange.value.length === 2) {
      params.start = opsRange.value[0]
      params.end = opsRange.value[1]
    }
    const res = await auditApi.logs(params)
    opsList.value = res.list || []
    opsTotal.value = res.total || 0
  } finally {
    opsLoading.value = false
  }
}
function resetOps() {
  opsQuery.operator = ''
  opsQuery.action = ''
  opsRange.value = null
  opsQuery.page = 1
  loadOps()
}

// 登录日志
const loginLoading = ref(false)
const loginList = ref<LoginLog[]>([])
const loginTotal = ref(0)
const loginRange = ref<[string, string] | null>(null)
const loginQuery = reactive({
  username: '',
  start: '',
  end: '',
  page: 1,
  size: 20
})
async function loadLogins() {
  loginLoading.value = true
  try {
    const params: any = { page: loginQuery.page, size: loginQuery.size }
    if (loginQuery.username) params.username = loginQuery.username
    if (loginRange.value && loginRange.value.length === 2) {
      params.start = loginRange.value[0]
      params.end = loginRange.value[1]
    }
    const res = await auditApi.logins(params)
    loginList.value = res.list || []
    loginTotal.value = res.total || 0
  } finally {
    loginLoading.value = false
  }
}
function resetLogin() {
  loginQuery.username = ''
  loginRange.value = null
  loginQuery.page = 1
  loadLogins()
}

function onTabChange(name: string | number) {
  if (name === 'login' && !loginList.value.length) loadLogins()
}

function exportOps() {
  if (!opsList.value.length) { ElMessage.info('暂无可导出的操作日志'); return }
  exportCsv(`操作日志-${new Date().toLocaleDateString('zh-CN')}`, opsList.value, [
    { key: 'createdAt', header: '时间', format: r => formatDateTime(r.createdAt) },
    { key: 'operatorName', header: '操作人' },
    { key: 'action', header: '操作' },
    { key: 'target', header: '目标' },
    { key: 'detail', header: '详情' },
    { key: 'ip', header: 'IP' }
  ])
  ElMessage.success(`已导出 ${opsList.value.length} 条`)
}

function exportLogins() {
  if (!loginList.value.length) { ElMessage.info('暂无可导出的登录日志'); return }
  exportCsv(`登录日志-${new Date().toLocaleDateString('zh-CN')}`, loginList.value, [
    { key: 'createdAt', header: '时间', format: r => formatDateTime(r.createdAt) },
    { key: 'username', header: '用户名' },
    { key: 'ip', header: 'IP' },
    { key: 'location', header: '归属地' },
    { key: 'browser', header: '浏览器' },
    { key: 'os', header: '系统' },
    { key: 'status', header: '状态', format: r => r.status === 'SUCCESS' ? '成功' : '失败' },
    { key: 'message', header: '消息' }
  ])
  ElMessage.success(`已导出 ${loginList.value.length} 条`)
}

onMounted(() => loadOps())
</script>

<style scoped>
.table-wrap {
  padding: 16px 18px;
}
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}
</style>
