<template>
  <div class="page">
    <div class="page-header is-enhanced">
      <div class="header-left">
        <div class="header-icon"><el-icon><DataAnalysis /></el-icon></div>
        <div class="header-text">
          <h2 class="page-title">报表中心</h2>
          <div class="page-sub">{{ reportSlogan }}</div>
        </div>
      </div>
      <div class="header-actions">
        <el-button :icon="RefreshRight" @click="loadAll">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate" class="btn-scale">新建报表</el-button>
      </div>
    </div>

    <!-- KPI -->
    <div class="kpi-row">
      <KpiCard label="今日生成" :value="stats.todayRuns || 0" suffix="次" icon="Document" />
      <KpiCard label="今日文件" :value="stats.todayFiles || 0" suffix="份" icon="Folder" />
      <KpiCard label="订阅总数" :value="stats.tasksTotal || 0" suffix="个" icon="List" />
      <KpiCard label="启用中" :value="stats.tasksEnabled || 0" suffix="个" icon="Select" />
    </div>

    <!-- 实时数据图表 -->
    <div class="chart-row x-stagger">
      <ChartCard title="近 7 日营业额" subtitle="按日聚合（元）" :height="240" class="x-fade">
        <div ref="trendEl" class="chart-slot"></div>
      </ChartCard>
      <ChartCard title="热销 TOP 5" subtitle="本月销量" :height="240" class="x-fade">
        <div ref="topEl" class="chart-slot"></div>
      </ChartCard>
      <ChartCard title="会员 RFM 分布" subtitle="当前累计会员" :height="240" class="x-fade">
        <div ref="rfmEl" class="chart-slot"></div>
      </ChartCard>
    </div>

    <!-- 列表 -->
    <div class="x-card table-wrap">
      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column label="报表名称" min-width="180">
          <template #default="{ row }">
            <div class="rp-name">{{ row.name }}</div>
            <div class="rp-sub">{{ typeText(row.type) }} · {{ scheduleText(row.schedule) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="接收人" min-width="200">
          <template #default="{ row }">
            <el-tag v-for="(r, i) in (row.recipients || []).slice(0, 3)" :key="i" size="small" effect="plain" class="rcpt-tag">{{ r }}</el-tag>
            <span v-if="(row.recipients || []).length > 3" class="muted">+{{ (row.recipients || []).length - 3 }}</span>
            <span v-if="!(row.recipients || []).length" class="muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="上次运行" width="160">
          <template #default="{ row }">{{ formatDateTime(row.lastRunAt) }}</template>
        </el-table-column>
        <el-table-column label="下次运行" width="160">
          <template #default="{ row }">{{ formatDateTime(row.nextRunAt) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-switch :model-value="row.enabled" @change="(v: any) => onToggle(row, !!v)" inline-prompt active-text="开" inactive-text="关" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button size="small" link type="primary" @click="onRun(row)">立即运行</el-button>
            <el-button size="small" link type="primary" @click="onEdit(row)">编辑</el-button>
            <el-dropdown @command="(c: string) => onDownload(row, c)">
              <el-button size="small" link type="primary">下载最新 <el-icon><ArrowDown /></el-icon></el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="pdf">PDF</el-dropdown-item>
                  <el-dropdown-item command="xlsx">Excel</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-button size="small" link type="danger" @click="onRemove(row)">删除</el-button>
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

    <!-- 新建/编辑 -->
    <el-dialog v-model="formVisible" :title="editing ? '编辑报表' : '新建报表'" width="560px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="92px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="如：每日营业额简报" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择" style="width: 100%">
            <el-option label="经营看板" value="DASHBOARD" />
            <el-option label="营业额" value="REVENUE" />
            <el-option label="会员" value="MEMBER" />
            <el-option label="优惠券" value="COUPON" />
            <el-option label="订单" value="ORDER" />
          </el-select>
        </el-form-item>
        <el-form-item label="调度" prop="schedule">
          <el-radio-group v-model="form.schedule">
            <el-radio value="DAILY">每日</el-radio>
            <el-radio value="WEEKLY">每周</el-radio>
            <el-radio value="MONTHLY">每月</el-radio>
            <el-radio value="ONCE">仅一次</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="接收邮箱" prop="recipients">
          <el-input v-model="recipientsText" placeholder="多个邮箱用英文逗号分隔" />
          <div class="form-hint">已填 {{ form.recipients.length }} 位</div>
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" />
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
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import * as echarts from '@/utils/echarts'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, RefreshRight, ArrowDown, DataAnalysis } from '@element-plus/icons-vue'
import { reportsApi, statsApi } from '@/api'
import request from '@/api/request'
import KpiCard from '@/components/KpiCard.vue'
import ChartCard from '@/components/ChartCard.vue'
import { formatDateTime } from '@/utils/format'

const trendEl = ref<HTMLElement | null>(null)
const topEl = ref<HTMLElement | null>(null)
const rfmEl = ref<HTMLElement | null>(null)

const reportSlogan = [
  '把每个月的经营，写成一段可被回望的故事',
  '报表是给生意的一份温热小结',
  '数字之外，还有用心在生长'
][Math.floor(Math.random() * 3)]

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const query = reactive({ page: 1, size: 20 })
const stats = reactive<any>({ todayRuns: 0, todayFiles: 0, tasksTotal: 0, tasksEnabled: 0 })

function loadAll() { loadList(); loadStats(); drawCharts() }

function drawCharts() {
  // 复用或重建 echarts 实例，避免内存泄漏和图表叠加
  const getChart = (el: HTMLElement) => {
    const exist = echarts.getInstanceByDom(el)
    if (exist) exist.dispose()
    return echarts.init(el)
  }
  // 趋势（柱状图）
  if (trendEl.value) {
    const c = getChart(trendEl.value)
    statsApi.trend({ range: '7d', metric: 'revenue' }).then((d: any) => {
      c.setOption({
        grid: { left: 50, right: 12, top: 16, bottom: 24 },
        tooltip: { trigger: 'axis', backgroundColor: 'rgba(31,29,24,0.92)', borderWidth: 0, textStyle: { color: '#fff' } },
        xAxis: { type: 'category', data: (d || []).map((x: any) => x.date?.slice(5) || ''), axisLine: { lineStyle: { color: 'rgba(70,64,56,0.18)' } }, axisTick: { show: false }, axisLabel: { color: '#8a8578', fontSize: 10 } },
        yAxis: { type: 'value', axisLine: { show: false }, splitLine: { lineStyle: { color: 'rgba(70,64,56,0.06)', type: 'dashed' } }, axisLabel: { color: '#8a8578', fontSize: 10 } },
        series: [{
          type: 'bar', barWidth: 14,
          itemStyle: { color: '#5a7a9c', borderRadius: [3, 3, 0, 0] },
          data: (d || []).map((x: any) => (x.value || 0) / 100)
        }]
      })
    }).catch(() => {
      c.setOption({ title: { text: '暂无数据', left: 'center', top: 'middle', textStyle: { color: '#b3ad9f', fontSize: 12, fontFamily: 'serif' } }, grid: { show: false }, xAxis: { show: false }, yAxis: { show: false }, series: [] })
    })
  }
  // 热销 TOP 5
  if (topEl.value) {
    const c = getChart(topEl.value)
    statsApi.topServices().then((d: any) => {
      const list = (d || []).slice(0, 5)
      c.setOption({
        grid: { left: 80, right: 16, top: 8, bottom: 20 },
        tooltip: { trigger: 'axis', backgroundColor: 'rgba(31,29,24,0.92)', borderWidth: 0, textStyle: { color: '#fff' } },
        xAxis: { type: 'value', axisLine: { show: false }, splitLine: { lineStyle: { color: 'rgba(70,64,56,0.06)', type: 'dashed' } }, axisLabel: { color: '#8a8578', fontSize: 10 } },
        yAxis: { type: 'category', data: list.map((x: any) => x.name).reverse(), axisLine: { show: false }, axisTick: { show: false }, axisLabel: { color: '#43403a', fontSize: 11, fontFamily: 'serif' } },
        series: [{
          type: 'bar', barWidth: 8,
          itemStyle: { color: '#b89692', borderRadius: [0, 4, 4, 0] },
          data: list.map((x: any) => x.count || 0).reverse()
        }]
      })
    }).catch(() => {
      c.setOption({ title: { text: '暂无数据', left: 'center', top: 'middle', textStyle: { color: '#b3ad9f', fontSize: 12, fontFamily: 'serif' } }, grid: { show: false }, xAxis: { show: false }, yAxis: { show: false }, series: [] })
    })
  }
  // 会员 RFM 分布
  if (rfmEl.value) {
    const c = getChart(rfmEl.value)
    const emptyOption = { title: { text: '暂无数据', left: 'center', top: 'middle', textStyle: { color: '#b3ad9f', fontSize: 12, fontFamily: 'serif' } }, grid: { show: false }, xAxis: { show: false }, yAxis: { show: false }, series: [] }
    statsApi.rfm().then((d: any) => {
      // 用 RfmStats 的 high/mid/low/dormant 字段构造饼图数据
      const hasData = d && (d.high !== undefined || d.mid !== undefined || d.low !== undefined || d.dormant !== undefined)
      if (!hasData) {
        c.setOption(emptyOption)
        return
      }
      const data = [
        { name: '高价值', value: d.high || 0 },
        { name: '活跃', value: d.mid || 0 },
        { name: '沉睡', value: d.low || 0 },
        { name: '流失', value: d.dormant || 0 }
      ]
      c.setOption({
        tooltip: { trigger: 'item', backgroundColor: 'rgba(31,29,24,0.92)', borderWidth: 0, textStyle: { color: '#fff' } },
        legend: { bottom: 0, textStyle: { color: '#6a655c', fontSize: 10, fontFamily: 'serif' } },
        series: [{
          type: 'pie', radius: ['46%', '72%'], center: ['50%', '46%'],
          avoidLabelOverlap: true,
          itemStyle: { borderColor: '#fff', borderWidth: 2, borderRadius: 4 },
          label: { show: true, formatter: '{b}\n{d}%', fontFamily: 'serif', color: '#43403a', fontSize: 10 },
          labelLine: { length: 6, length2: 6, lineStyle: { color: 'rgba(70,64,56,0.30)' } },
          data: data,
          color: ['#5a7a9c', '#8b7ea3', '#b89692', '#b8845c']
        }]
      })
    }).catch(() => {
      c.setOption(emptyOption)
    })
  }
}

onMounted(loadAll)

onBeforeUnmount(() => {
  [trendEl.value, topEl.value, rfmEl.value].forEach(el => {
    if (el) {
      const inst = echarts.getInstanceByDom(el)
      if (inst) inst.dispose()
    }
  })
})

async function loadList() {
  loading.value = true
  try {
    const res: any = await reportsApi.list({ page: query.page, size: query.size })
    list.value = res?.list || []
    total.value = res?.total || 0
  } catch {/* */}
  finally { loading.value = false }
}

async function loadStats() {
  try {
    const s: any = await reportsApi.stats()
    Object.assign(stats, s || {})
  } catch {/* */}
}

function typeText(t: string) {
  return ({ DASHBOARD: '经营看板', REVENUE: '营业额', MEMBER: '会员', COUPON: '优惠券', ORDER: '订单' } as any)[t] || t
}
function scheduleText(s: string) {
  return ({ DAILY: '每日', WEEKLY: '每周', MONTHLY: '每月', ONCE: '仅一次' } as any)[s] || s
}

// ============ 表单 ============
const formVisible = ref(false)
const editing = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const recipientsText = ref('')
const form = reactive({
  id: 0,
  name: '',
  type: 'DASHBOARD' as 'DASHBOARD' | 'REVENUE' | 'MEMBER' | 'COUPON' | 'ORDER',
  schedule: 'DAILY' as 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'ONCE',
  recipients: [] as string[],
  enabled: true
})
const formRules: FormRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  schedule: [{ required: true, message: '请选择调度', trigger: 'change' }],
  recipients: [{
    validator: (_r, _v, cb) => {
      if (!form.recipients || form.recipients.length === 0) cb(new Error('请填写至少 1 个邮箱'))
      else cb()
    },
    trigger: 'change'
  }]
}

function openCreate() {
  editing.value = false
  Object.assign(form, { id: 0, name: '', type: 'DASHBOARD', schedule: 'DAILY', recipients: [], enabled: true })
  recipientsText.value = ''
  formVisible.value = true
}
function onEdit(row: any) {
  editing.value = true
  Object.assign(form, {
    id: row.id, name: row.name, type: row.type, schedule: row.schedule,
    recipients: [...(row.recipients || [])], enabled: row.enabled
  })
  recipientsText.value = (form.recipients || []).join(',')
  formVisible.value = true
}
async function submitForm() {
  form.recipients = recipientsText.value
    .split(/[,，\s]+/)
    .map(s => s.trim())
    .filter(s => s && /.+@.+\..+/.test(s))
  await formRef.value?.validate()
  if (form.recipients.length === 0) {
    ElMessage.error('请填写至少 1 个合法邮箱')
    return
  }
  saving.value = true
  try {
    const payload: any = { name: form.name, type: form.type, schedule: form.schedule, recipients: form.recipients }
    if (editing.value) {
      await reportsApi.update(form.id, payload)
      ElMessage.success('已更新')
    } else {
      await reportsApi.create({ ...payload, enabled: form.enabled })
      ElMessage.success('已创建')
    }
    formVisible.value = false
    loadAll()
  } catch {/* */}
  finally { saving.value = false }
}

async function onRun(row: any) {
  const loading = ElMessage({ message: '正在生成...', duration: 0 })
  try {
    const r: any = await reportsApi.run(row.id)
    loading.close()
    ElMessage.success('已生成: ' + (r?.lastRunAt ? formatDateTime(r.lastRunAt) : '完成'))
    loadList()
    loadStats()
  } catch { loading.close() }
}
async function onToggle(row: any, enabled: boolean) {
  await reportsApi.toggle(row.id, enabled)
  row.enabled = enabled
  ElMessage.success(enabled ? '已启用' : '已停用')
  loadList()
  loadStats()
}
async function onRemove(row: any) {
  try {
    await ElMessageBox.confirm(`确认删除报表「${row.name}」?`, '提示', { type: 'warning', closeOnPressEscape: true })
  } catch { return }
  await reportsApi.remove(row.id)
  ElMessage.success('已删除')
  loadList()
  loadStats()
}
async function onDownload(row: any, type: string) {
  const url = reportsApi.downloadUrl(row.id, type as 'pdf' | 'xlsx')
  const filename = `${row.name || 'report'}_${type === 'pdf' ? 'PDF' : 'Excel'}.${type === 'pdf' ? 'pdf' : 'xlsx'}`
  try {
    const res: any = await request.get(url, { responseType: 'blob' })
    const blob = new Blob([res instanceof Blob ? res : res?.data])
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = filename
    link.click()
    URL.revokeObjectURL(link.href)
  } catch {
    ElMessage.error('下载失败')
  }
}
</script>

<style scoped>
.kpi-row {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 14px;
}
@media (max-width: 1100px) { .kpi-row { grid-template-columns: repeat(2, 1fr); } }
.chart-row {
  display: grid; grid-template-columns: repeat(3, 1fr);
  gap: 14px; margin-bottom: 18px;
}
@media (max-width: 1100px) { .chart-row { grid-template-columns: 1fr; } }
.chart-slot { width: 100%; height: 190px; }
.rp-name { font-size: 13.5px; color: var(--ink); font-weight: 500; }
.rp-sub { font-size: 11.5px; color: var(--muted); margin-top: 2px; }
.rcpt-tag { margin-right: 4px; margin-bottom: 2px; }
.muted { color: var(--muted); font-size: 12px; }
.form-hint { font-size: 11.5px; color: var(--muted); margin-top: 4px; }
</style>
