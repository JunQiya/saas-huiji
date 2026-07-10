<template>
  <div class="page dashboard">
    <!-- 增强版 page-header -->
    <div class="page-header is-enhanced">
      <div class="header-left">
        <div class="header-icon"><el-icon><DataLine /></el-icon></div>
        <div class="header-text">
          <h2 class="page-title">经营仪表盘</h2>
          <div class="page-sub">{{ todaySlogan }}</div>
        </div>
      </div>
      <div class="header-actions">
        <el-button :icon="Refresh" @click="loadAll" :loading="loading" class="btn-scale">刷新</el-button>
      </div>
    </div>

    <!-- 经营摘要卡 -->
    <div class="summary-bar x-card">
      <div class="summary-item">
        <div class="sum-label">今日营业额</div>
        <div class="sum-value val">{{ formatMoney(summary?.todayRevenue) }}</div>
        <div class="sum-delta" :class="(summary?.todayDelta ?? 0) >= 0 ? 'pos' : 'neg'">
          {{ (summary?.todayDelta ?? 0) >= 0 ? '↑' : '↓' }}{{ Math.abs(summary?.todayDelta ?? 0) }}%
          <span class="sum-tip">较昨日</span>
        </div>
      </div>
      <div class="summary-divider"></div>
      <div class="summary-item">
        <div class="sum-label">本周营业额</div>
        <div class="sum-value val">{{ formatMoney(summary?.weekRevenue) }}</div>
        <div class="sum-delta" :class="(summary?.weekDelta ?? 0) >= 0 ? 'pos' : 'neg'">
          {{ (summary?.weekDelta ?? 0) >= 0 ? '↑' : '↓' }}{{ Math.abs(summary?.weekDelta ?? 0) }}%
          <span class="sum-tip">较上周</span>
        </div>
      </div>
      <div class="summary-divider"></div>
      <div class="summary-item">
        <div class="sum-label">本月营业额</div>
        <div class="sum-value val">{{ formatMoney(summary?.monthRevenue) }}</div>
        <div class="sum-delta" :class="(summary?.monthDelta ?? 0) >= 0 ? 'pos' : 'neg'">
          {{ (summary?.monthDelta ?? 0) >= 0 ? '↑' : '↓' }}{{ Math.abs(summary?.monthDelta ?? 0) }}%
          <span class="sum-tip">较上月</span>
        </div>
      </div>
      <div class="summary-divider"></div>
      <div class="summary-item">
        <div class="sum-label">今日新增会员</div>
        <div class="sum-value val">{{ summary?.newMembersToday ?? 0 }}</div>
        <div class="sum-tip">本月新增 {{ summary?.newMembersMonth ?? 0 }}</div>
      </div>
    </div>

    <!-- KPI 卡片 -->
    <div class="kpi-grid">
      <KpiCard
        label="营业额（近30天）"
        :value="overview?.revenue ?? 0"
        :icon="'Money'"
        :trend="overview?.revenueDelta ?? null"
        trend-label="较上30天"
        :precision="2"
      />
      <KpiCard
        label="会员总数"
        :value="overview?.memberCount ?? 0"
        :icon="'User'"
        :trend="overview?.memberDelta ?? null"
        trend-label="较上30天"
      />
      <KpiCard
        label="订单数"
        :value="overview?.orderCount ?? 0"
        :icon="'ShoppingCart'"
        :trend="overview?.orderDelta ?? null"
        trend-label="较上30天"
      />
      <KpiCard
        label="客单价"
        :value="overview?.avgPrice ?? 0"
        :icon="'Coin'"
        :trend="overview?.avgPriceDelta ?? null"
        trend-label="较上30天"
        :precision="2"
      />
    </div>

    <!-- 营业趋势 -->
    <ChartCard
      title="营业趋势"
      :subtitle="`当前指标：${metricLabel}`"
      :height="320"
      :loading="trendLoading"
      class="trend-card"
    >
      <template #extra>
        <el-radio-group v-model="range" size="small" @change="loadTrend">
          <el-radio-button value="7d">近7天</el-radio-button>
          <el-radio-button value="30d">近30天</el-radio-button>
          <el-radio-button value="90d">近90天</el-radio-button>
        </el-radio-group>
        <el-select v-model="metric" size="small" style="width: 110px; margin-left: 8px" @change="loadTrend">
          <el-option label="营业额" value="revenue" />
          <el-option label="订单数" value="orders" />
          <el-option label="新增会员" value="members" />
        </el-select>
      </template>
      <div ref="trendEl" class="chart-host"></div>
    </ChartCard>

    <!-- 第二排 -->
    <div class="grid-2">
      <ChartCard title="会员增长" subtitle="新增与活跃趋势" :height="300" :loading="growthLoading">
        <div ref="growthEl" class="chart-host"></div>
      </ChartCard>
      <ChartCard title="24小时下单分布" subtitle="订单时段热力" :height="300" :loading="hourLoading">
        <div ref="hourEl" class="chart-host"></div>
      </ChartCard>
    </div>

    <!-- 第三排 -->
    <div class="grid-2">
      <ChartCard title="热销服务 Top10" subtitle="按成交单数" :height="320" :loading="topLoading">
        <div ref="topEl" class="chart-host"></div>
      </ChartCard>
      <ChartCard title="会员 RFM 分层" subtitle="价值分层占比" :height="320" :loading="rfmLoading">
        <div ref="rfmEl" class="chart-host"></div>
      </ChartCard>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import { Refresh, DataLine } from '@element-plus/icons-vue'
import KpiCard from '@/components/KpiCard.vue'
import ChartCard from '@/components/ChartCard.vue'
import { statsApi } from '@/api'
import { formatMoney } from '@/utils/format'
import type { OverviewStats, SummaryStats, TrendPoint, MemberGrowthPoint, TopService, RfmStats, HourPoint } from '@/types'

const loading = ref(false)
const trendLoading = ref(true)
const growthLoading = ref(true)
const hourLoading = ref(true)
const topLoading = ref(true)
const rfmLoading = ref(true)

const overview = ref<OverviewStats>()
const summary = ref<SummaryStats>()

const range = ref<'7d' | '30d' | '90d'>('30d')
const metric = ref<'revenue' | 'orders' | 'members'>('revenue')
const metricLabel = computed(
  () => ({ revenue: '营业额', orders: '订单数', members: '新增会员' }[metric.value])
)

const todaySlogans = [
  '看看今天的客流、营业额与会员的近况',
  '把数字读成故事，把数据写成温度',
  '好的生意，是把每一次到店都妥善安放',
  '所有看得见的增长，背后是看不见的用心'
]
const todaySlogan = todaySlogans[new Date().getDate() % todaySlogans.length]

const trendEl = ref<HTMLDivElement>()
const growthEl = ref<HTMLDivElement>()
const hourEl = ref<HTMLDivElement>()
const topEl = ref<HTMLDivElement>()
const rfmEl = ref<HTMLDivElement>()

let trendChart: echarts.ECharts | null = null
let growthChart: echarts.ECharts | null = null
let hourChart: echarts.ECharts | null = null
let topChart: echarts.ECharts | null = null
let rfmChart: echarts.ECharts | null = null

const trendData = ref<TrendPoint[]>([])
const growthData = ref<MemberGrowthPoint[]>([])
const hourData = ref<HourPoint[]>([])
const topData = ref<TopService[]>([])
const rfmData = ref<RfmStats>()

const palette = ['#6f94b8', '#8a8278', '#a89a7e', '#7e9a8a', '#9b8aa6', '#b0a884']

async function loadAll() {
  loading.value = true
  try {
    await Promise.all([
      loadSummary(),
      loadOverview(),
      loadTrend(),
      loadGrowth(),
      loadHour(),
      loadTop(),
      loadRfm()
    ])
  } finally {
    loading.value = false
  }
}

async function loadSummary() {
  try { summary.value = await statsApi.summary() } catch { /* 容错 */ }
}
async function loadOverview() {
  try { overview.value = await statsApi.overview() } catch { /* 容错 */ }
}
async function loadTrend() {
  trendLoading.value = true
  try {
    trendData.value = await statsApi.trend({ range: range.value, metric: metric.value })
    await nextTick(); renderTrend()
  } finally { trendLoading.value = false }
}
async function loadGrowth() {
  growthLoading.value = true
  try {
    growthData.value = await statsApi.memberGrowth()
    await nextTick(); renderGrowth()
  } finally { growthLoading.value = false }
}
async function loadHour() {
  hourLoading.value = true
  try {
    hourData.value = await statsApi.hour()
    await nextTick(); renderHour()
  } finally { hourLoading.value = false }
}
async function loadTop() {
  topLoading.value = true
  try {
    topData.value = await statsApi.topServices()
    await nextTick(); renderTop()
  } finally { topLoading.value = false }
}
async function loadRfm() {
  rfmLoading.value = true
  try {
    rfmData.value = await statsApi.rfm()
    await nextTick(); renderRfm()
  } finally { rfmLoading.value = false }
}

function axisColor() { return { color: '#8a8e85' } }

function renderTrend() {
  if (!trendEl.value) return
  if (!trendChart) trendChart = echarts.init(trendEl.value)
  const dates = trendData.value.map((d) => d.date)
  const values = trendData.value.map((d) => d.value)
  const isMoney = metric.value === 'revenue'
  trendChart.setOption({
    grid: { left: 50, right: 20, top: 30, bottom: 30 },
    tooltip: {
      trigger: 'axis',
      valueFormatter: (v: any) => (isMoney ? `¥${formatMoney(v)}` : String(v))
    },
    xAxis: { type: 'category', data: dates, axisLine: { lineStyle: axisColor() }, axisLabel: axisColor() },
    yAxis: {
      type: 'value',
      axisLabel: { ...axisColor(), formatter: (v: number) => (isMoney ? `¥${(v / 100).toFixed(0)}` : String(v)) },
      splitLine: { lineStyle: { color: 'rgba(108,120,108,0.08)' } }
    },
    series: [{
      type: 'line', smooth: true, data: values, symbol: 'circle', symbolSize: 6,
      lineStyle: { color: palette[0], width: 2 }, itemStyle: { color: palette[0] },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(111,148,184,0.25)' },
          { offset: 1, color: 'rgba(111,148,184,0.02)' }
        ])
      }
    }]
  }, true)
}

function renderGrowth() {
  if (!growthEl.value) return
  if (!growthChart) growthChart = echarts.init(growthEl.value)
  const dates = growthData.value.map((d) => d.date)
  growthChart.setOption({
    grid: { left: 45, right: 20, top: 30, bottom: 30 },
    tooltip: { trigger: 'axis' },
    legend: { data: ['新增', '活跃'], right: 10, top: 0, textStyle: axisColor() },
    xAxis: { type: 'category', data: dates, axisLine: { lineStyle: axisColor() }, axisLabel: axisColor() },
    yAxis: { type: 'value', axisLabel: axisColor(), splitLine: { lineStyle: { color: 'rgba(108,120,108,0.08)' } } },
    series: [
      { name: '新增', type: 'line', smooth: true, data: growthData.value.map((d) => d.newCount), lineStyle: { color: palette[1] }, itemStyle: { color: palette[1] } },
      { name: '活跃', type: 'line', smooth: true, data: growthData.value.map((d) => d.activeCount), lineStyle: { color: palette[0] }, itemStyle: { color: palette[0] } }
    ]
  }, true)
}

function renderHour() {
  if (!hourEl.value) return
  if (!hourChart) hourChart = echarts.init(hourEl.value)
  const hours = Array.from({ length: 24 }, (_, i) => `${i}:00`)
  hourChart.setOption({
    grid: { left: 45, right: 20, top: 20, bottom: 30 },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: hours, axisLine: { lineStyle: axisColor() }, axisLabel: axisColor() },
    yAxis: { type: 'value', axisLabel: axisColor(), splitLine: { lineStyle: { color: 'rgba(108,120,108,0.08)' } } },
    series: [{
      type: 'bar', data: hourData.value.map((d) => d.count), barWidth: '55%',
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#6f94b8' }, { offset: 1, color: 'rgba(111,148,184,0.4)' }
        ]), borderRadius: [3, 3, 0, 0]
      }
    }]
  }, true)
}

function renderTop() {
  if (!topEl.value) return
  if (!topChart) topChart = echarts.init(topEl.value)
  const sorted = [...topData.value].slice(0, 10).reverse()
  topChart.setOption({
    grid: { left: 110, right: 30, top: 20, bottom: 30 },
    tooltip: {
      trigger: 'axis', axisPointer: { type: 'shadow' },
      formatter: (p: any) => {
        const item = p[0]
        const data = sorted[item.dataIndex]
        return `${data.name}<br/>单数：${data.count}<br/>金额：¥${formatMoney(data.amount)}`
      }
    },
    xAxis: { type: 'value', axisLabel: axisColor(), splitLine: { lineStyle: { color: 'rgba(108,120,108,0.08)' } } },
    yAxis: { type: 'category', data: sorted.map((d) => d.name), axisLine: { lineStyle: axisColor() }, axisLabel: axisColor() },
    series: [{
      type: 'bar', data: sorted.map((d) => d.count), barWidth: '60%',
      itemStyle: { color: '#8a8278', borderRadius: [0, 3, 3, 0] }
    }]
  }, true)
}

function renderRfm() {
  if (!rfmEl.value) return
  if (!rfmChart) rfmChart = echarts.init(rfmEl.value)
  const d = rfmData.value || { high: 0, mid: 0, low: 0, dormant: 0 }
  rfmChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0, textStyle: axisColor() },
    series: [{
      type: 'pie', radius: ['45%', '68%'], center: ['50%', '45%'],
      avoidLabelOverlap: true, itemStyle: { borderColor: '#fff', borderWidth: 2, borderRadius: 4 },
      label: { show: false },
      data: [
        { name: '高价值', value: d.high, itemStyle: { color: palette[0] } },
        { name: '中价值', value: d.mid, itemStyle: { color: palette[3] } },
        { name: '低价值', value: d.low, itemStyle: { color: palette[2] } },
        { name: '沉睡', value: d.dormant, itemStyle: { color: '#cfcabf' } }
      ]
    }]
  }, true)
}

function resizeAll() {
  trendChart?.resize(); growthChart?.resize(); hourChart?.resize(); topChart?.resize(); rfmChart?.resize()
}

onMounted(async () => {
  await loadAll()
  await nextTick()
  renderTrend(); renderGrowth(); renderHour(); renderTop(); renderRfm()
  window.addEventListener('resize', resizeAll)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeAll)
  trendChart?.dispose(); growthChart?.dispose(); hourChart?.dispose(); topChart?.dispose(); rfmChart?.dispose()
})

watch([range, metric], () => loadTrend())
</script>

<style scoped>
.dashboard { padding: 18px 22px 28px; }

/* 经营摘要条 */
.summary-bar {
  display: flex; align-items: stretch;
  padding: 4px 0;
  margin-bottom: 14px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-lg);
  position: relative;
  overflow: hidden;
}
.summary-item {
  flex: 1; padding: 18px 24px;
  position: relative;
  transition: background-color var(--dur) var(--ease);
}
.summary-item:hover { background: var(--surface-2); }
.summary-item::before {
  content: ''; position: absolute; top: 14px; left: 24px;
  width: 4px; height: 14px; border-radius: 2px;
  background: var(--brand);
}
.summary-item:nth-child(1)::before { background: var(--brand); }
.summary-item:nth-child(3)::before { background: var(--success); }
.summary-item:nth-child(5)::before { background: var(--warning); }
.summary-item:nth-child(7)::before { background: var(--accent-rose); }
.sum-label { font-size: 12px; color: var(--muted); margin-bottom: 6px; letter-spacing: 0.04em; }
.sum-value { font-size: 22px; font-weight: 600; color: var(--ink); line-height: 1.2; font-variant-numeric: tabular-nums; }
.sum-delta { font-size: 12px; margin-top: 6px; font-weight: 500; }
.sum-tip { color: var(--muted); font-weight: 400; margin-left: 4px; }
.summary-divider { width: 1px; align-self: center; height: 56px; background: var(--line); }

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 16px;
}
.grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  margin-top: 14px;
}
.trend-card { margin-bottom: 14px; }
.chart-host { width: 100%; height: 100%; }

@media (max-width: 1100px) {
  .kpi-grid { grid-template-columns: repeat(2, 1fr); }
  .grid-2 { grid-template-columns: 1fr; }
  .summary-bar { flex-wrap: wrap; }
  .summary-divider { display: none; }
  .summary-item { flex: 1 1 45%; margin-bottom: 8px; }
}
</style>
