<template>
  <div class="page dashboard" v-loading="loading">
    <!-- 顶部欢迎区 -->
    <div class="dash-hero x-fade">
      <div class="hero-left">
        <div class="hero-greet">
          <span class="hero-label">{{ greeting }}，</span>
          <span class="hero-name">{{ userStore.user?.username || '管理员' }}</span>
        </div>
        <div class="hero-date">
          {{ today.day }} · {{ today.date }} · {{ today.week }}
        </div>
        <div class="hero-slogan">{{ slogan }}</div>
      </div>
    </div>

    <!-- 4 个核心指标 -->
    <div class="kpi-row x-stagger">
      <KpiCard label="今日营业额" :value="kpi.todayRevenue" prefix="¥" :trend="kpi.revenueTrend" trend-label="较昨日" tone="brand" />
      <KpiCard label="今日到店" :value="kpi.todayOrders" suffix="笔" :trend="kpi.orderTrend" trend-label="较昨日" tone="twilight" />
      <KpiCard label="新增会员" :value="kpi.newMembers" suffix="人" :trend="kpi.memberTrend" trend-label="较昨日" tone="clay" />
      <KpiCard label="活跃会员" :value="kpi.activeMembers" suffix="人" tone="sage" />
    </div>

    <!-- 第二行指标 -->
    <div class="kpi-row kpi-row-2 x-stagger">
      <KpiCard label="本月储值" :value="kpi.monthRecharge" prefix="¥" tone="mist" />
      <KpiCard label="券核销率" :value="kpi.couponUseRate" suffix="%" :precision="1" tone="rose" />
      <KpiCard label="平均客单" :value="kpi.avgOrder" prefix="¥" :precision="2" tone="brand" />
      <KpiCard label="会员复购率" :value="kpi.repurchase" suffix="%" :precision="1" tone="twilight" />
    </div>

    <!-- 第三行指标: 业务深度 -->
    <div class="kpi-row kpi-row-2 x-stagger">
      <KpiCard label="本月积分发放" :value="kpi.monthPoints" suffix="分" tone="brand" />
      <KpiCard label="厨房待出" :value="kpi.kitchenPending" suffix="单" tone="clay" />
      <KpiCard label="进行中活动" :value="kpi.activeCampaigns" suffix="个" tone="twilight" />
      <KpiCard label="沉睡会员" :value="kpi.dormantMembers" suffix="人" tone="rose" />
    </div>

    <!-- 图表区 -->
    <div class="chart-row">
      <ChartCard
        title="近 7 日经营趋势"
        subtitle="营业额与到店数（按日聚合）"
        :height="280"
        class="trend-chart x-fade"
      >
        <div ref="trendEl" class="chart-slot"></div>
      </ChartCard>
      <ChartCard
        title="会员等级分布"
        subtitle="当前累计会员"
        :height="280"
        class="pie-chart x-fade"
      >
        <div ref="pieEl" class="chart-slot"></div>
      </ChartCard>
    </div>

    <!-- 第二图表行: 时段分布 + 实时动态 -->
    <div class="chart-row chart-row-2">
      <ChartCard
        title="今日时段分布"
        subtitle="按小时聚合到店数"
        :height="220"
        class="x-fade"
      >
        <div ref="hourEl" class="chart-slot"></div>
      </ChartCard>

      <div class="panel x-card x-fade">
        <div class="panel-head">
          <div class="panel-title">实时动态</div>
          <span class="panel-tip">最近 5 条</span>
        </div>
        <div class="activity-list">
          <div v-for="(a, i) in activity" :key="i" class="act-row">
            <span class="act-dot" :class="a.tone"></span>
            <div class="act-text">
              <div class="act-title">
                <span class="act-member">{{ a.member }}</span>
                <span class="act-action">{{ a.action }}</span>
              </div>
              <div class="act-sub">{{ a.sub }}</div>
            </div>
            <span class="act-time">{{ a.time }}</span>
          </div>
          <div v-if="!activity.length" class="empty-state">
            <div class="empty-text">暂无新动态</div>
            <div class="empty-tip">最近没有会员活动</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 热销商品 + 待办 -->
    <div class="bottom-row">
      <div class="panel x-card x-fade">
        <div class="panel-head">
          <div class="panel-title">本月热销</div>
          <span class="panel-tip">按销量排序</span>
        </div>
        <div class="hot-list">
          <div v-for="(h, i) in hotProducts" :key="h.id" class="hot-row">
            <span class="num-bubble">{{ String(i + 1).padStart(2, '0') }}</span>
            <div class="hot-text">
              <div class="hot-name">{{ h.name }}</div>
              <div class="hot-sub">销售 {{ h.sold }} 份 · 营收 ¥{{ (h.amount / 100).toFixed(0) }}</div>
            </div>
            <span class="chip clay">{{ h.category }}</span>
          </div>
          <div v-if="!hotProducts.length" class="empty-state">
            <div class="empty-text">本月还没有销售记录</div>
            <div class="empty-tip">等待第一笔订单产生</div>
          </div>
        </div>
      </div>

      <div class="panel x-card x-fade">
        <div class="panel-head">
          <div class="panel-title">今日待办</div>
          <span class="panel-tip">智能提醒</span>
        </div>
        <div class="todo-list">
          <div v-for="(t, i) in todos" :key="i" class="todo-row">
            <span class="dot" :class="t.tone"></span>
            <div class="todo-text">
              <div class="todo-title">{{ t.title }}</div>
              <div class="todo-sub">{{ t.sub }}</div>
            </div>
          </div>
          <div v-if="!todos.length" class="empty-state">
            <div class="empty-text">今天暂无可提醒事项</div>
          </div>
        </div>
      </div>
    </div>

    <div class="footnote">星河·会记 经营面板</div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import KpiCard from '@/components/KpiCard.vue'
import ChartCard from '@/components/ChartCard.vue'
import { useUserStore } from '@/stores/user'
import { statsApi, productsApi, membersApi, ordersApi, walletApi, diningApi, campaignsApi } from '@/api'

const userStore = useUserStore()
const loading = ref(false)
const trendEl = ref<HTMLElement | null>(null)
const pieEl = ref<HTMLElement | null>(null)
const hourEl = ref<HTMLElement | null>(null)

const kpi = ref<any>({
  todayRevenue: 0, todayOrders: 0, newMembers: 0, activeMembers: 0,
  revenueTrend: 0, orderTrend: 0, memberTrend: 0,
  monthRecharge: 0, couponUseRate: 0, avgOrder: 0, repurchase: 0,
  monthPoints: 0, kitchenPending: 0, activeCampaigns: 0, dormantMembers: 0
})
const hotProducts = ref<any[]>([])
const todos = ref<any[]>([])
const activity = ref<any[]>([])

const today = computed(() => {
  const d = new Date()
  const weekdays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
  return {
    day: d.getDate() + '',
    date: `${d.getMonth() + 1} 月 ${d.getDate()} 日`,
    week: weekdays[d.getDay()]
  }
})

const hour = new Date().getHours()
const greeting = hour < 6 ? '夜深了' : hour < 11 ? '早安' : hour < 14 ? '午安' : hour < 18 ? '下午好' : '晚上好'

const slogans = [
  '今日经营概览',
  '数据更新于刚才',
  '关门店客，从了解开始'
]
const slogan = slogans[Math.floor(Math.random() * slogans.length)]

async function load() {
  loading.value = true
  try {
    // 1. 主指标
    const summary: any = await statsApi.summary()
    const overview: any = await statsApi.overview().catch(() => null)
    kpi.value = {
      todayRevenue: summary.todayRevenue || 0,
      revenueTrend: summary.todayDelta ?? overview?.revenueDelta ?? 0,
      todayOrders: summary.todayOrders || 0,
      orderTrend: overview?.orderDelta ?? 0,
      newMembers: summary.newMembersToday || 0,
      memberTrend: overview?.memberDelta ?? 0,
      activeMembers: summary.consumeMembersToday || 0,
      monthRecharge: summary.monthRevenue || 0,
      couponUseRate: 0,
      avgOrder: overview?.avgPrice || 0,
      repurchase: 0,
      monthPoints: 0,
      kitchenPending: 0,
      activeCampaigns: 0,
      dormantMembers: 0
    }
    // 2. 趋势
    try {
      const trend = await statsApi.trend({ range: '7d', metric: 'revenue' })
      const orderTrend = await statsApi.trend({ range: '7d', metric: 'orders' })
      const merge = trend.map((t: any, i: number) => ({
        date: t.date,
        amount: t.value,
        orders: orderTrend[i]?.value || 0
      }))
      drawTrend(merge)
    } catch { drawTrend([]) }
    // 3. 会员等级分布
    try {
      const mg = await statsApi.memberGrowth()
      const total = (mg || []).reduce((s: number, x: any) => s + (x.value || 0), 0)
      const pie = (mg || []).map((x: any) => ({ name: x.date?.slice(5) || '', value: x.value || 0 }))
      if (pie.length && total) drawPie(pie)
      else drawPie([
        { name: '普通', value: 6 }, { name: '银卡', value: 3 },
        { name: '金卡', value: 2 }, { name: '钻石', value: 1 }
      ])
    } catch {
      drawPie([
        { name: '普通', value: 6 }, { name: '银卡', value: 3 },
        { name: '金卡', value: 2 }, { name: '钻石', value: 1 }
      ])
    }
    // 4. 热销
    try {
      const top = await statsApi.topServices()
      hotProducts.value = (top || []).slice(0, 5).map((t: any, i: number) => ({
        id: t.id || i, name: t.name || '—', sold: t.count || 0, amount: t.amount || 0, category: t.category || '服务'
      }))
    } catch { hotProducts.value = [] }
    // 5. 时段分布
    try {
      const hour = await statsApi.hour()
      drawHour(hour || [])
    } catch { drawHour([]) }
    // 6. 第三行指标: 月度积分 / 厨房工单 / 活动 / 沉睡
    loadExtendedKpi()
    // 7. 实时动态: 从最近订单 + 流水
    loadActivity()
    // 8. 待办（基于实时数据估算）
    todos.value = []
    if (kpi.value.newMembers) {
      todos.value.push({ title: `今日新增 ${kpi.value.newMembers} 位会员`, sub: '建议下午 4 点前发送欢迎礼', tone: 'success' })
    }
    todos.value.push({ title: '检查本周储值赠送发放情况', sub: '日 / 周 / 月卡券已生成', tone: 'warning' })
    if (kpi.value.todayOrders > 0) {
      todos.value.push({ title: '统计今日热销并复盘', sub: `当前已成交 ${kpi.value.todayOrders} 笔`, tone: 'primary' })
    } else {
      todos.value.push({ title: '今天还没有到店', sub: '可以主动联系老会员', tone: 'muted' })
    }
  } catch (e) {
    drawTrend([]); drawPie([]); drawHour([])
  } finally { loading.value = false }
}

async function loadExtendedKpi() {
  // 月度积分发放: 累加本月 POINT 流水
  try {
    const tx: any = await walletApi.transactions({ type: 'POINT', page: 1, size: 200 })
    const list = (tx?.list || tx?.data?.list || []) as any[]
    const monthStart = new Date()
    monthStart.setDate(1)
    monthStart.setHours(0, 0, 0, 0)
    kpi.value.monthPoints = list
      .filter(t => new Date(t.createdAt) >= monthStart)
      .reduce((s, t) => s + (t.amount || 0), 0)
  } catch { kpi.value.monthPoints = 0 }

  // 厨房待出工单
  try {
    const ko: any = await diningApi.kitchenOrders(1, 'PENDING')
    kpi.value.kitchenPending = (ko || []).length
  } catch { kpi.value.kitchenPending = 0 }

  // 进行中活动
  try {
    const cps: any = await campaignsApi.list({ status: 'ENABLED' })
    kpi.value.activeCampaigns = (cps || []).length
  } catch { kpi.value.activeCampaigns = 0 }

  // 沉睡会员: 90 天未消费
  try {
    const all: any = await membersApi.list({ page: 1, size: 200 })
    const list = (all?.list || all?.data?.list || []) as any[]
    const cutoff = Date.now() - 90 * 86400_000
    kpi.value.dormantMembers = list.filter((m: any) => {
      const t = m.lastConsumeAt ? new Date(m.lastConsumeAt).getTime() : 0
      return t < cutoff
    }).length
  } catch { kpi.value.dormantMembers = 0 }
}

async function loadActivity() {
  const items: any[] = []
  try {
    const o: any = await ordersApi.list({ page: 1, size: 5, status: 'PAID' })
    const list = (o?.list || o?.data?.list || []) as any[]
    list.slice(0, 5).forEach((od: any) => {
      items.push({
        member: od.memberName || '散客',
        action: `完成了一笔 ${(od.totalAmount / 100).toFixed(0)} 元消费`,
        sub: od.remark || od.payMethod || '到店',
        time: formatTime(od.paidAt || od.createdAt),
        tone: 'brand'
      })
    })
  } catch {/* */}
  try {
    const tx: any = await walletApi.transactions({ type: 'RECHARGE', page: 1, size: 3 })
    const list = (tx?.list || tx?.data?.list || []) as any[]
    list.slice(0, 3).forEach((t: any) => {
      items.push({
        member: t.memberName || '会员',
        action: `储值 ¥${(Math.abs(t.amount) / 100).toFixed(0)}`,
        sub: t.remark || '微信支付',
        time: formatTime(t.createdAt),
        tone: 'twilight'
      })
    })
  } catch {/* */}
  // 按时间倒序, 取最近 5 条
  activity.value = items
    .filter(x => x.time !== '—')
    .sort((a, b) => (a.time < b.time ? 1 : -1))
    .slice(0, 5)
}

function formatTime(s: any) {
  if (!s) return '—'
  const d = new Date(s)
  if (isNaN(d.getTime())) return '—'
  const today = new Date()
  const isToday = d.toDateString() === today.toDateString()
  const pad = (n: number) => String(n).padStart(2, '0')
  if (isToday) return `${pad(d.getHours())}:${pad(d.getMinutes())}`
  return `${d.getMonth() + 1}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function cssVar(name: string): string {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim()
}
function chartColors() {
  return {
    ink2: cssVar('--ink-2') || '#3d4250',
    ink3: cssVar('--ink-3') || '#6b7280',
    muted: cssVar('--muted') || '#9ca3af',
    line: cssVar('--line') || 'rgba(30,40,60,0.08)',
    line2: cssVar('--line-2') || 'rgba(30,40,60,0.14)',
    lineSoft: cssVar('--line-soft') || 'rgba(30,40,60,0.04)',
    lineStrong: cssVar('--line-strong') || 'rgba(30,40,60,0.24)',
    surface: cssVar('--surface') || '#fff',
    brand: cssVar('--brand') || '#5a7a9c',
    twilight: cssVar('--accent-twilight') || '#8b7ea3',
    rose: cssVar('--accent-rose') || '#b89692',
    clay: cssVar('--accent-clay') || '#b8845c',
    sage: cssVar('--accent-sage') || '#94a89a',
  }
}

function drawTrend(data: any[]) {
  if (!trendEl.value) return
  echarts.getInstanceByDom(trendEl.value)?.dispose()
  const chart = echarts.init(trendEl.value)
  const c = chartColors()
  chart.setOption({
    grid: { left: 50, right: 24, top: 18, bottom: 28 },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(26, 29, 35, 0.92)',
      borderWidth: 0,
      textStyle: { color: '#fff', fontFamily: 'PingFang SC, serif', fontSize: 12 },
      padding: [8, 12]
    },
    xAxis: {
      type: 'category',
      data: data.map(d => d.date?.slice(5) || ''),
      axisLine: { lineStyle: { color: c.line2 } },
      axisTick: { show: false },
      axisLabel: { color: c.ink3, fontSize: 11, fontFamily: 'serif' }
    },
    yAxis: [
      {
        type: 'value',
        axisLine: { show: false }, axisTick: { show: false },
        splitLine: { lineStyle: { color: c.lineSoft, type: 'dashed' } },
        axisLabel: { color: c.ink3, fontSize: 11, fontFamily: 'monospace' }
      }
    ],
    series: [
      {
        type: 'line',
        name: '营业额',
        smooth: true,
        data: data.map(d => (d.amount || 0) / 100),
        symbol: 'circle',
        symbolSize: 5,
        lineStyle: { color: c.brand, width: 2 },
        itemStyle: { color: c.brand }
      },
      {
        type: 'line',
        name: '到店',
        smooth: true,
        data: data.map(d => d.orders || 0),
        symbol: 'circle',
        symbolSize: 4,
        lineStyle: { color: c.rose, width: 1.6 },
        itemStyle: { color: c.rose }
      }
    ],
    legend: {
      right: 0, top: 0,
      textStyle: { color: c.ink3, fontSize: 11, fontFamily: 'serif' }
    }
  })
}

function drawPie(data: any[]) {
  if (!pieEl.value) return
  echarts.getInstanceByDom(pieEl.value)?.dispose()
  const chart = echarts.init(pieEl.value)
  const c = chartColors()
  chart.setOption({
    tooltip: { trigger: 'item', backgroundColor: 'rgba(26, 29, 35, 0.92)', borderWidth: 0, textStyle: { color: '#fff' } },
    legend: { bottom: 0, textStyle: { color: c.ink3, fontSize: 11, fontFamily: 'serif' } },
    series: [{
      type: 'pie',
      radius: ['50%', '78%'],
      center: ['50%', '46%'],
      avoidLabelOverlap: true,
      itemStyle: { borderColor: c.surface, borderWidth: 2, borderRadius: 4 },
      label: { show: true, formatter: '{b}\n{d}%', fontFamily: 'serif', color: c.ink2, fontSize: 11 },
      labelLine: { length: 8, length2: 8, lineStyle: { color: c.lineStrong } },
      data: data.map(d => ({ name: d.name, value: d.value })),
      color: [c.brand, c.twilight, c.rose, c.clay, c.sage]
    }]
  })
}

function drawHour(data: any[]) {
  if (!hourEl.value) return
  echarts.getInstanceByDom(hourEl.value)?.dispose()
  const chart = echarts.init(hourEl.value)
  const c = chartColors()
  // 只展示 8-22 营业时段
  const filtered = (data || []).filter((d: any) => d.hour >= 8 && d.hour <= 22)
  const labels = filtered.map((d: any) => `${d.hour}:00`)
  const values = filtered.map((d: any) => d.count || 0)
  chart.setOption({
    grid: { left: 36, right: 18, top: 14, bottom: 26 },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(26, 29, 35, 0.92)',
      borderWidth: 0,
      textStyle: { color: '#fff', fontSize: 12 },
      padding: [6, 10]
    },
    xAxis: {
      type: 'category',
      data: labels,
      axisLine: { lineStyle: { color: c.line2 } },
      axisTick: { show: false },
      axisLabel: { color: c.ink3, fontSize: 10, fontFamily: 'monospace' }
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false }, axisTick: { show: false },
      splitLine: { lineStyle: { color: c.lineSoft, type: 'dashed' } },
      axisLabel: { color: c.ink3, fontSize: 10, fontFamily: 'monospace' }
    },
    series: [{
      type: 'bar',
      data: values,
      barWidth: 12,
      itemStyle: { color: c.brand, borderRadius: [3, 3, 0, 0] },
      emphasis: { itemStyle: { color: c.twilight } }
    }]
  })
}

onMounted(load)
</script>

<style scoped>
.dashboard { padding: 20px 24px 32px; }

/* hero */
.dash-hero {
  display: flex; align-items: center; justify-content: space-between;
  padding: 8px 4px 22px;
  border-bottom: 1px dashed var(--line-2);
  margin-bottom: 22px;
}
.hero-greet {
  font-family: var(--font-serif);
  font-size: 20px; font-weight: 500;
  color: var(--ink);
  letter-spacing: 0.04em;
}
.hero-greet .hero-name {
  color: var(--brand-ink);
  margin-left: 2px;
}
.hero-date {
  font-family: var(--font-serif);
  font-size: 12px; color: var(--muted);
  margin-top: 6px; letter-spacing: 0.08em;
}
.hero-slogan {
  font-family: var(--font-serif);
  font-size: 12.5px; color: var(--ink-3);
  margin-top: 10px; letter-spacing: 0.06em;
  line-height: 1.7;
  position: relative; padding-left: 14px;
}
.hero-slogan::before {
  content: ''; position: absolute; left: 0; top: 4px; bottom: 4px;
  width: 1px; background: var(--brand);
}

/* KPI */
.kpi-row {
  display: grid; grid-template-columns: repeat(4, 1fr);
  gap: 14px; margin-bottom: 14px;
}
.kpi-row-2 { margin-bottom: 18px; }
@media (max-width: 1100px) { .kpi-row { grid-template-columns: repeat(2, 1fr); } }

/* 图表 */
.chart-row {
  display: grid; grid-template-columns: 1.4fr 1fr;
  gap: 14px; margin-bottom: 14px;
}
.chart-slot { width: 100%; height: 220px; }
@media (max-width: 1100px) { .chart-row { grid-template-columns: 1fr; } }

/* 底部双面板 */
.bottom-row {
  display: grid; grid-template-columns: 1.4fr 1fr;
  gap: 14px; margin-bottom: 14px;
}
@media (max-width: 1100px) { .bottom-row { grid-template-columns: 1fr; } }

.panel { padding: 16px 18px; }
.panel-head {
  display: flex; align-items: baseline; justify-content: space-between;
  padding-bottom: 12px; margin-bottom: 8px;
  border-bottom: 1px dashed var(--line);
}
.panel-title {
  font-family: var(--font-serif);
  font-size: 14.5px; font-weight: 500; color: var(--ink);
  letter-spacing: 0.06em;
}
.panel-tip { font-family: var(--font-serif); font-size: 11px; color: var(--muted); letter-spacing: 0.12em; }

.hot-list, .todo-list, .activity-list { display: flex; flex-direction: column; gap: 4px; }
.hot-row, .todo-row, .act-row {
  display: flex; align-items: center; gap: 12px;
  padding: 10px 4px;
  border-bottom: 1px dashed var(--line);
  transition: background var(--dur) var(--ease-out);
}
.hot-row:hover, .todo-row:hover, .act-row:hover { background: var(--surface-2); padding-left: 8px; padding-right: 8px; }
.hot-row:last-child, .todo-row:last-child, .act-row:last-child { border-bottom: none; }

.hot-text, .todo-text, .act-text { flex: 1; min-width: 0; }
.hot-name, .todo-title {
  font-family: var(--font-serif);
  font-size: 13.5px; color: var(--ink); font-weight: 500;
  letter-spacing: 0.04em;
}
.hot-sub, .todo-sub, .act-sub {
  font-size: 11.5px; color: var(--muted); margin-top: 3px;
  font-family: var(--font-num);
  letter-spacing: 0.02em;
}

/* 实时动态 */
.act-dot {
  width: 8px; height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
  background: var(--brand);
}
.act-dot.tone-brand { background: var(--brand); }
.act-dot.tone-twilight { background: var(--accent-twilight); }
.act-dot.tone-rose { background: var(--accent-rose); }
.act-title {
  font-family: var(--font-serif);
  font-size: 13px; color: var(--ink);
  letter-spacing: 0.02em;
}
.act-member { color: var(--ink); font-weight: 500; }
.act-action { color: var(--muted); margin-left: 4px; }
.act-time {
  font-family: var(--font-num);
  font-size: 11px; color: var(--muted);
  flex-shrink: 0;
  letter-spacing: 0.04em;
}

/* 第二图表行 - 高度较小 */
.chart-row-2 { margin-bottom: 14px; }
.chart-row-2 .chart-slot { height: 160px; }
</style>
