<template>
  <div class="page">
    <div class="page-header is-enhanced">
      <div class="header-left">
        <div class="header-icon"><el-icon><ChatLineRound /></el-icon></div>
        <div class="header-text">
          <h2 class="page-title">消息中心</h2>
          <div class="page-sub">{{ msgSlogan }}</div>
        </div>
      </div>
      <div class="header-actions">
        <el-button :icon="RefreshRight" @click="loadAll">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate" class="btn-scale">新建发送</el-button>
      </div>
    </div>

    <!-- KPI -->
    <div class="kpi-row">
      <KpiCard label="今日发送" :value="stats.todaySent || 0" suffix="条" icon="Promotion" />
      <KpiCard label="今日费用" :value="formatMoney(stats.todayCost || 0)" prefix="¥" icon="Money" />
      <KpiCard label="本月发送" :value="stats.monthSent || 0" suffix="条" icon="DataLine" />
      <KpiCard label="本月费用" :value="formatMoney(stats.monthCost || 0)" prefix="¥" icon="Wallet" />
    </div>

    <!-- 趋势小图 -->
    <div class="x-card trend-card">
      <div class="trend-head">
        <div class="trend-title">最近 7 天发送趋势</div>
        <div class="trend-legend">
          <span class="dot dot-s"></span> 发送数
          <span class="dot dot-c"></span> 费用(分)
        </div>
      </div>
      <div class="trend-body">
        <div v-for="(p, i) in (stats.recent || [])" :key="i" class="trend-col">
          <div class="col-stack">
            <div class="bar bar-sent" :style="{ height: barHeight(p.sent, 'sent') + 'px' }"></div>
          </div>
          <div class="col-label">{{ formatTrendLabel(p.date) }}</div>
        </div>
      </div>
    </div>

    <!-- 筛选 -->
    <div class="x-card filter-wrap">
      <div class="filter-bar">
        <el-select v-model="query.status" placeholder="状态" clearable @change="onSearch" style="width: 140px">
          <el-option label="待发送" value="PENDING" />
          <el-option label="发送中" value="SENDING" />
          <el-option label="已完成" value="COMPLETED" />
          <el-option label="失败" value="FAILED" />
          <el-option label="已取消" value="CANCELED" />
        </el-select>
        <el-select v-model="query.channel" placeholder="渠道" clearable @change="onSearch" style="width: 140px">
          <el-option label="短信" value="SMS" />
          <el-option label="微信" value="WECHAT" />
          <el-option label="站内" value="IN_APP" />
        </el-select>
        <el-date-picker v-model="dateRange" type="daterange" range-separator="—" start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DD" @change="onSearch" />
        <el-button type="primary" :icon="Search" @click="onSearch">查询</el-button>
        <el-button :icon="RefreshLeft" @click="onReset">重置</el-button>
      </div>
    </div>

    <!-- 列表 -->
    <div class="x-card table-wrap">
      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column label="渠道" width="100">
          <template #default="{ row }">
            <el-tag :type="channelTagType(row.channel)" effect="light" size="small">{{ channelText(row.channel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="模板类型" width="120">
          <template #default="{ row }">
            <span>{{ templateText(row.templateType) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="主题/摘要" min-width="220">
          <template #default="{ row }">
            <div class="msg-title">{{ row.subject || '—' }}</div>
            <div class="msg-content">{{ row.content }}</div>
          </template>
        </el-table-column>
        <el-table-column label="目标" width="90" align="right">
          <template #default="{ row }">{{ row.totalCount }}</template>
        </el-table-column>
        <el-table-column label="已发" width="90" align="right">
          <template #default="{ row }">
            <span class="num-pos">{{ row.sentCount || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="失败" width="80" align="right">
          <template #default="{ row }">
            <span :class="(row.failedCount || 0) > 0 ? 'num-neg' : 'muted'">{{ row.failedCount || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="费用" width="100" align="right">
          <template #default="{ row }">¥{{ formatMoney(row.cost || 0) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="light" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="定时" width="160">
          <template #default="{ row }">{{ formatDateTime(row.scheduledAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button size="small" link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button v-if="canCancel(row)" size="small" link type="warning" @click="onCancel(row)">取消</el-button>
            <el-button v-if="canRetry(row)" size="small" link type="primary" @click="onRetry(row)">重试</el-button>
          </template>
        </el-table-column>
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

    <!-- 新建弹窗 -->
    <el-dialog v-model="formVisible" title="新建消息任务" width="640px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="92px">
        <el-form-item label="渠道" prop="channel">
          <el-radio-group v-model="form.channel">
            <el-radio value="SMS">短信 (0.05 元/条)</el-radio>
            <el-radio value="WECHAT">微信</el-radio>
            <el-radio value="IN_APP">站内</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="模板类型" prop="templateType">
          <el-select v-model="form.templateType" placeholder="请选择" style="width: 100%">
            <el-option label="生日提醒" value="BIRTHDAY" />
            <el-option label="券到期提醒" value="COUPON_EXPIRE" />
            <el-option label="活动推广" value="CAMPAIGN" />
            <el-option label="自定义" value="MANUAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="主题">
          <el-input v-model="form.subject" placeholder="可选, 便于检索" />
        </el-form-item>
        <el-form-item label="消息内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="4" placeholder="消息正文" />
        </el-form-item>
        <el-form-item label="目标会员" prop="memberIds">
          <el-input v-model="memberIdsText" type="textarea" :rows="2" placeholder="会员 id, 用英文逗号分隔" />
          <div class="form-hint">已填 {{ form.memberIds.length }} 位; 可调用会员接口选择后填入</div>
        </el-form-item>
        <el-form-item label="定时发送">
          <el-date-picker v-model="form.scheduledAt" type="datetime" placeholder="立即发送" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </el-form-item>
        <div class="cost-preview">
          预计费用: <b>¥{{ formatMoney(estimateCost) }}</b>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">立即发送</el-button>
      </template>
    </el-dialog>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" title="消息任务详情" size="520px">
      <div v-loading="detailLoading" class="detail-body">
        <div class="detail-row"><span class="dr-lbl">渠道</span><span>{{ channelText(detail.channel) }}</span></div>
        <div class="detail-row"><span class="dr-lbl">模板</span><span>{{ templateText(detail.templateType) }}</span></div>
        <div class="detail-row"><span class="dr-lbl">主题</span><span>{{ detail.subject || '—' }}</span></div>
        <div class="detail-row detail-content"><span class="dr-lbl">内容</span><span>{{ detail.content }}</span></div>
        <div class="detail-row"><span class="dr-lbl">目标</span><span>{{ detail.totalCount }} 人</span></div>
        <div class="detail-row"><span class="dr-lbl">已发/失败</span><span>{{ detail.sentCount }} / {{ detail.failedCount }}</span></div>
        <div class="detail-row"><span class="dr-lbl">费用</span><span>¥{{ formatMoney(detail.cost || 0) }}</span></div>
        <div class="detail-row"><span class="dr-lbl">状态</span><span>{{ statusText(detail.status) }}</span></div>
        <div class="detail-row"><span class="dr-lbl">创建时间</span><span>{{ formatDateTime(detail.createdAt) }}</span></div>
        <div class="detail-row"><span class="dr-lbl">完成时间</span><span>{{ formatDateTime(detail.completedAt) }}</span></div>
        <div class="detail-row detail-ids">
          <span class="dr-lbl">会员 ids</span>
          <span class="ids-text">{{ (detail.memberIds || []).join(', ') }}</span>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, RefreshRight, RefreshLeft, Search, ChatLineRound } from '@element-plus/icons-vue'
import { messagesApi } from '@/api'
import KpiCard from '@/components/KpiCard.vue'
import { formatDateTime, formatMoney } from '@/utils/format'

const msgSlogan = [
  '一句抵达，便是一次温暖的提醒',
  '把消息写成字句，把提醒写成牵挂',
  '通知之外，再多一点点在意'
][Math.floor(Math.random() * 3)]

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const query = reactive({ status: '' as string, channel: '' as string, page: 1, size: 20 })
const dateRange = ref<string[]>([])

const stats = reactive<any>({ todaySent: 0, todayCost: 0, monthSent: 0, monthCost: 0, recent: [] })

function loadAll() {
  loadList()
  loadStats()
}

async function loadList() {
  loading.value = true
  try {
    const params: any = {
      page: query.page,
      size: query.size,
      status: query.status || undefined,
      channel: query.channel || undefined,
      start: dateRange.value?.[0] || undefined,
      end: dateRange.value?.[1] || undefined
    }
    const res: any = await messagesApi.list(params)
    list.value = res?.list || []
    total.value = res?.total || 0
  } catch {/* 错误已被拦截器处理 */}
  finally { loading.value = false }
}

async function loadStats() {
  try {
    const s: any = await messagesApi.stats()
    Object.assign(stats, s || {})
  } catch {/* 容错 */}
}

function onSearch() { query.page = 1; loadList() }
function onReset() {
  query.status = ''
  query.channel = ''
  dateRange.value = []
  query.page = 1
  loadList()
}

function channelText(c?: string) {
  return ({ SMS: '短信', WECHAT: '微信', IN_APP: '站内' } as any)[c || ''] || c || '—'
}
function channelTagType(c?: string) {
  return ({ SMS: 'warning', WECHAT: 'success', IN_APP: 'info' } as any)[c || ''] || 'info'
}
function templateText(t?: string) {
  return ({ BIRTHDAY: '生日提醒', COUPON_EXPIRE: '券到期', CAMPAIGN: '活动推广', MANUAL: '自定义' } as any)[t || ''] || t || '—'
}
function statusText(s?: string) {
  return ({ PENDING: '待发送', SENDING: '发送中', COMPLETED: '已完成', FAILED: '失败', CANCELED: '已取消' } as any)[s || ''] || s || '—'
}
function statusTagType(s?: string) {
  return ({ PENDING: 'info', SENDING: 'warning', COMPLETED: 'success', FAILED: 'danger', CANCELED: '' } as any)[s || ''] || 'info'
}
function canCancel(row: any) { return row.status === 'PENDING' || row.status === 'SENDING' }
function canRetry(row: any) { return row.status === 'FAILED' }

function formatTrendLabel(d?: string) { return d ? d.slice(5) : '' }
function barHeight(v: number, key: 'sent') {
  const arr = (stats.recent || []) as any[]
  const max = Math.max(1, ...arr.map(p => p[key] || 0))
  return Math.max(4, Math.round((v / max) * 80))
}

// ============ 新建 ============
const formVisible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const memberIdsText = ref('')
const form = reactive({
  channel: 'IN_APP' as 'SMS' | 'WECHAT' | 'IN_APP',
  templateType: 'MANUAL' as 'BIRTHDAY' | 'COUPON_EXPIRE' | 'CAMPAIGN' | 'MANUAL',
  subject: '',
  content: '',
  memberIds: [] as number[],
  scheduledAt: '' as string
})
const formRules: FormRules = {
  channel: [{ required: true, message: '请选择渠道', trigger: 'change' }],
  templateType: [{ required: true, message: '请选择模板', trigger: 'change' }],
  content: [{ required: true, message: '请输入消息内容', trigger: 'blur' }],
  memberIds: [{
    validator: (_r, _v, cb) => {
      if (!form.memberIds || form.memberIds.length === 0) cb(new Error('请选择目标会员'))
      else cb()
    },
    trigger: 'change'
  }]
}

const estimateCost = computed(() => {
  const n = form.memberIds.length || 0
  if (form.channel === 'SMS') return Math.round(n * 0.05 * 100) / 100
  return 0
})

function openCreate() {
  form.channel = 'IN_APP'
  form.templateType = 'MANUAL'
  form.subject = ''
  form.content = ''
  form.memberIds = []
  form.scheduledAt = ''
  memberIdsText.value = ''
  formVisible.value = true
}

async function submitForm() {
  // 解析 ids
  const ids = memberIdsText.value
    .split(/[,，\s]+/)
    .map(s => parseInt(s.trim()))
    .filter(n => !isNaN(n) && n > 0)
  form.memberIds = ids
  await formRef.value?.validate()
  if (form.memberIds.length === 0) {
    ElMessage.error('请填写至少 1 位会员 id')
    return
  }
  saving.value = true
  try {
    await messagesApi.create({
      channel: form.channel,
      templateType: form.templateType,
      subject: form.subject,
      content: form.content,
      memberIds: form.memberIds,
      scheduledAt: form.scheduledAt || undefined
    })
    ElMessage.success('任务已创建, 正在后台发送')
    formVisible.value = false
    loadAll()
  } catch {/* 错误已被拦截 */}
  finally { saving.value = false }
}

async function onCancel(row: any) {
  try {
    await ElMessageBox.confirm(`确认取消任务 #${row.id}?`, '提示', { type: 'warning', closeOnPressEscape: true })
  } catch { return }
  await messagesApi.cancel(row.id)
  ElMessage.success('已取消')
  loadList()
}
async function onRetry(row: any) {
  await messagesApi.retry(row.id)
  ElMessage.success('已重新发送')
  loadList()
}

// ============ 详情 ============
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<any>({})
async function openDetail(row: any) {
  detailVisible.value = true
  detailLoading.value = true
  try {
    detail.value = await messagesApi.detail(row.id)
  } catch { ElMessage.error('加载详情失败') }
  finally { detailLoading.value = false }
}

let pollTimer: number | null = null
function startPolling() {
  stopPolling()
  pollTimer = window.setInterval(() => {
    loadList()
  }, 20000)
}
function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}
function onVisibilityChange() {
  if (document.hidden) {
    stopPolling()
  } else {
    loadList()
    startPolling()
  }
}

onMounted(() => {
  loadAll()
  document.addEventListener('visibilitychange', onVisibilityChange)
  startPolling()
})

onBeforeUnmount(() => {
  stopPolling()
  document.removeEventListener('visibilitychange', onVisibilityChange)
})
</script>

<style scoped>
.filter-wrap { padding: 14px 18px; margin-bottom: 14px; }
.kpi-row {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 14px;
}
@media (max-width: 1100px) { .kpi-row { grid-template-columns: repeat(2, 1fr); } }

.trend-card { padding: 16px 18px; margin-bottom: 14px; }
.trend-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.trend-title { font-size: 14px; font-weight: 500; color: var(--ink); }
.trend-legend { font-size: 12px; color: var(--muted); display: flex; align-items: center; gap: 12px; }
.trend-legend .dot {
  display: inline-block; width: 8px; height: 8px; border-radius: 50%;
  margin-right: 4px; vertical-align: middle;
}
.trend-legend .dot-s { background: #6f94b8; }
.trend-legend .dot-c { background: #b8a16a; }
.trend-body {
  display: grid; grid-template-columns: repeat(7, 1fr); gap: 12px;
  height: 110px; align-items: end;
}
.trend-col { display: flex; flex-direction: column; align-items: center; gap: 4px; }
.col-stack { width: 100%; height: 80px; display: flex; align-items: end; justify-content: center; }
.bar {
  width: 18px; border-radius: 4px 4px 0 0; transition: height 0.3s ease-out;
}
.bar-sent { background: linear-gradient(180deg, #6f94b8, #4a6a87); }
.col-label { font-size: 11px; color: var(--muted); }

.msg-title { font-size: 13px; color: var(--ink); font-weight: 500; }
.msg-content {
  font-size: 12px; color: var(--muted);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 360px;
}
.num-pos { color: var(--success); font-weight: 600; }
.num-neg { color: var(--danger); font-weight: 600; }
.muted { color: var(--muted); }

.form-hint { font-size: 11.5px; color: var(--muted); margin-top: 4px; }
.cost-preview {
  padding: 8px 12px; background: var(--primary-action-soft);
  border-radius: 6px; font-size: 12.5px; color: var(--ink-2);
}
.cost-preview b { color: var(--primary-action); font-size: 14px; margin-left: 4px; }

.detail-body { display: flex; flex-direction: column; gap: 10px; }
.detail-row { display: flex; font-size: 13px; line-height: 1.6; }
.dr-lbl { width: 80px; color: var(--muted); flex-shrink: 0; }
.detail-content { align-items: flex-start; }
.detail-ids .ids-text { color: var(--ink-2); word-break: break-all; }
</style>
