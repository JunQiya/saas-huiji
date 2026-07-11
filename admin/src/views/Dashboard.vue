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
      <div class="hero-right">
        <div class="hero-constellation" aria-hidden="true">
          <svg viewBox="0 0 80 80" width="80" height="80">
            <circle cx="14" cy="20" r="0.8" fill="var(--brand-ink)" opacity="0.35" />
            <circle cx="48" cy="14" r="0.7" fill="var(--brand-ink)" opacity="0.30" />
            <circle cx="66" cy="36" r="0.9" fill="var(--brand-ink)" opacity="0.40" />
            <circle cx="56" cy="62" r="0.8" fill="var(--brand-ink)" opacity="0.35" />
            <circle cx="20" cy="56" r="0.7" fill="var(--brand-ink)" opacity="0.30" />
            <g stroke="var(--brand-ink)" stroke-width="0.5" fill="none" opacity="0.45">
              <line x1="14" y1="20" x2="34" y2="34" />
              <line x1="34" y1="34" x2="48" y2="14" />
              <line x1="34" y1="34" x2="66" y2="36" />
              <line x1="34" y1="34" x2="56" y2="62" />
              <line x1="34" y1="34" x2="20" y2="56" />
            </g>
            <circle cx="34" cy="34" r="2" fill="var(--brand-deep)" />
            <circle cx="14" cy="20" r="1.4" fill="var(--brand-deep)" />
            <circle cx="48" cy="14" r="1.4" fill="var(--brand-deep)" />
            <circle cx="66" cy="36" r="1.4" fill="var(--brand-deep)" />
            <circle cx="56" cy="62" r="1.2" fill="var(--brand-deep)" opacity="0.7" />
          </svg>
        </div>
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
            <div class="empty-tip">— 慢慢来，第一个订单尤其值得记住 —</div>
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

    <div class="footnote">星河滚烫 不如经营里的一次回访</div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import KpiCard from '@/components/KpiCard.vue'
import ChartCard from '@/components/ChartCard.vue'
import { useUserStore } from '@/stores/user'
import { statsApi, productsApi, membersApi } from '@/api'

const userStore = useUserStore()
const loading = ref(false)
const trendEl = ref<HTMLElement | null>(null)
const pieEl = ref<HTMLElement | null>(null)

const kpi = ref<any>({
  todayRevenue: 0, todayOrders: 0, newMembers: 0, activeMembers: 0,
  revenueTrend: 0, orderTrend: 0, memberTrend: 0,
  monthRecharge: 0, couponUseRate: 0, avgOrder: 0, repurchase: 0
})
const hotProducts = ref<any[]>([])
const todos = ref<any[]>([])

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
  '把今天的到店，妥帖安放',
  '把数字读成故事',
  '星河很远，到店很近',
  '用心服务，会被记得',
  '好生意，从一份会员名单开始',
  '慢慢来，是最好的节奏'
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
      repurchase: 0
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
    // 5. 待办（基于实时数据估算）
    todos.value = []
    if (kpi.value.newMembers) {
      todos.value.push({ title: `今日新增 ${kpi.value.newMembers} 位会员`, sub: '建议下午 4 点前发送欢迎礼', tone: 'success' })
    }
    todos.value.push({ title: '检查本周储值赠送发放情况', sub: '日 / 周 / 月卡券已生成', tone: 'warning' })
    if (kpi.value.todayOrders > 0) {
      todos.value.push({ title: '统计今日热销并复盘', sub: `当前已成交 ${kpi.value.todayOrders} 笔`, tone: 'primary' })
    } else {
      todos.value.push({ title: '今天还没有到店', sub: '一束好的开场，从问候开始', tone: 'muted' })
    }
  } catch (e) {
    drawTrend([]); drawPie([])
  } finally { loading.value = false }
}

function drawTrend(data: any[]) {
  if (!trendEl.value) return
  const chart = echarts.init(trendEl.value)
  chart.setOption({
    grid: { left: 50, right: 24, top: 18, bottom: 28 },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(31, 29, 24, 0.92)',
      borderWidth: 0,
      textStyle: { color: '#fff', fontFamily: 'PingFang SC, serif', fontSize: 12 },
      padding: [8, 12]
    },
    xAxis: {
      type: 'category',
      data: data.map(d => d.date?.slice(5) || ''),
      axisLine: { lineStyle: { color: 'rgba(70, 64, 56, 0.18)' } },
      axisTick: { show: false },
      axisLabel: { color: '#8a8578', fontSize: 11, fontFamily: 'serif' }
    },
    yAxis: [
      {
        type: 'value',
        axisLine: { show: false }, axisTick: { show: false },
        splitLine: { lineStyle: { color: 'rgba(70, 64, 56, 0.06)', type: 'dashed' } },
        axisLabel: { color: '#8a8578', fontSize: 11, fontFamily: 'monospace' }
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
        lineStyle: { color: '#5a7a9c', width: 2 },
        itemStyle: { color: '#5a7a9c' },
        areaStyle: {
          color: {
            type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(90, 122, 156, 0.16)' },
              { offset: 1, color: 'rgba(90, 122, 156, 0.02)' }
            ]
          }
        }
      },
      {
        type: 'line',
        name: '到店',
        smooth: true,
        data: data.map(d => d.orders || 0),
        symbol: 'circle',
        symbolSize: 4,
        lineStyle: { color: '#b89692', width: 1.6 },
        itemStyle: { color: '#b89692' }
      }
    ],
    legend: {
      right: 0, top: 0,
      textStyle: { color: '#6a655c', fontSize: 11, fontFamily: 'serif' }
    }
  })
}

function drawPie(data: any[]) {
  if (!pieEl.value) return
  const chart = echarts.init(pieEl.value)
  chart.setOption({
    tooltip: { trigger: 'item', backgroundColor: 'rgba(31, 29, 24, 0.92)', borderWidth: 0, textStyle: { color: '#fff' } },
    legend: { bottom: 0, textStyle: { color: '#6a655c', fontSize: 11, fontFamily: 'serif' } },
    series: [{
      type: 'pie',
      radius: ['50%', '78%'],
      center: ['50%', '46%'],
      avoidLabelOverlap: true,
      itemStyle: { borderColor: '#fff', borderWidth: 2, borderRadius: 4 },
      label: { show: true, formatter: '{b}\n{d}%', fontFamily: 'serif', color: '#43403a', fontSize: 11 },
      labelLine: { length: 8, length2: 8, lineStyle: { color: 'rgba(70, 64, 56, 0.30)' } },
      data: data.map(d => ({ name: d.name, value: d.value })),
      color: ['#5a7a9c', '#8b7ea3', '#b89692', '#b8845c', '#94a89a']
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
.hero-constellation { animation: x-fade-in 0.6s var(--ease-out) 0.2s both; }

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

.hot-list, .todo-list { display: flex; flex-direction: column; gap: 4px; }
.hot-row, .todo-row {
  display: flex; align-items: center; gap: 12px;
  padding: 10px 4px;
  border-bottom: 1px dashed var(--line);
  transition: background var(--dur) var(--ease-out);
}
.hot-row:hover, .todo-row:hover { background: var(--surface-2); padding-left: 8px; padding-right: 8px; }
.hot-row:last-child, .todo-row:last-child { border-bottom: none; }

.hot-text, .todo-text { flex: 1; min-width: 0; }
.hot-name, .todo-title {
  font-family: var(--font-serif);
  font-size: 13.5px; color: var(--ink); font-weight: 500;
  letter-spacing: 0.04em;
}
.hot-sub, .todo-sub {
  font-size: 11.5px; color: var(--muted); margin-top: 3px;
  font-family: var(--font-num);
  letter-spacing: 0.02em;
}
</style>
