<template>
  <div class="page">
    <div class="page-header is-enhanced">
      <div class="header-left">
        <div class="header-icon"><el-icon><Calendar /></el-icon></div>
        <div class="header-text">
          <h2 class="page-title">营销日历</h2>
          <div class="page-sub">生日、节日、券到期、活动 — 一屏洞悉全月节奏</div>
        </div>
      </div>
      <div class="header-actions">
        <el-button :icon="DArrowLeft" @click="shift(-1)">上月</el-button>
        <span class="month-label">{{ year }} 年 {{ month }} 月</span>
        <el-button @click="shift(1)">下月<i class="el-icon--right"><DArrowRight /></i></el-button>
        <el-button type="primary" plain @click="goToday" class="btn-scale">回到今天</el-button>
      </div>
    </div>

    <div class="x-card calendar-card">
      <div class="week-row">
        <div v-for="w in weeks" :key="w" class="week-cell">{{ w }}</div>
      </div>
      <div class="day-grid">
        <div v-for="(d, idx) in days" :key="idx" class="day-cell x-card" :class="{ today: d.isToday, other: d.otherMonth, selected: d.isSelected }" @click="select(d)">
          <div class="d-head">
            <span class="d-num" :class="{ 'lunar': !d.isToday }">{{ d.day }}</span>
            <span v-if="d.festival" class="d-festival">{{ d.festival }}</span>
          </div>
          <div class="d-events">
            <div v-for="(e, i) in d.events.slice(0, 3)" :key="i" class="event-chip" :class="e.kind">
              <span class="dot" :class="dotClass(e.kind)"></span>{{ e.title }}
            </div>
            <div v-if="d.events.length > 3" class="more">+{{ d.events.length - 3 }} 更多</div>
          </div>
        </div>
      </div>
    </div>

    <el-drawer v-model="drawer" :title="selected ? `${selected.year}-${String(selected.month).padStart(2,'0')}-${String(selected.day).padStart(2,'0')}` : ''" size="380px">
      <div v-if="selected">
        <div v-if="selected.festival" class="kv"><span class="k">节日</span><span class="v">{{ selected.festival }}</span></div>
        <div class="section-title">当日事项</div>
        <div v-for="(e, i) in selected.events" :key="i" class="event-item x-card" @click="jump(e)">
          <div class="event-head">
            <span class="event-kind" :class="e.kind">{{ kindLabel(e.kind) }}</span>
            <span class="event-title">{{ e.title }}</span>
          </div>
          <div v-if="e.desc" class="event-desc">{{ e.desc }}</div>
        </div>
        <div v-if="selected.events.length === 0" class="empty-state">本日无事项</div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Calendar, DArrowLeft, DArrowRight } from '@element-plus/icons-vue'
import { membersApi, couponsApi, campaignsApi } from '@/api'

const router = useRouter()
const today = new Date()
const year = ref(today.getFullYear())
const month = ref(today.getMonth() + 1)
const weeks = ['日', '一', '二', '三', '四', '五', '六']

const members = ref<any[]>([])
const coupons = ref<any[]>([])
const campaigns = ref<any[]>([])

const drawer = ref(false)
const selected = ref<any>(null)

// 节日表(简化: 仅常用节日)
const festivals: Record<string, string> = {
  '01-01': '元旦',
  '02-14': '情人节',
  '03-08': '女神节',
  '03-12': '植树节',
  '04-01': '愚人节',
  '05-01': '劳动节',
  '05-04': '青年节',
  '05-20': '表白日',
  '06-01': '儿童节',
  '09-10': '教师节',
  '10-01': '国庆节',
  '11-11': '光棍节',
  '12-24': '平安夜',
  '12-25': '圣诞节'
}

const calSlogan = [
  '把一整月的营销，铺成一张看得见的地图',
  '好的节奏，是知道哪天该温柔地提醒一下'
][Math.floor(Math.random() * 2)]

async function loadAll() {
  try {
    const data: any = await membersApi.list({ page: 1, size: 200 })
    members.value = data?.records || data?.list || data?.content || []
  } catch { members.value = [] }
  try {
    coupons.value = await couponsApi.list({ page: 1, size: 200 }) || []
  } catch { coupons.value = [] }
  try {
    campaigns.value = await campaignsApi.list({ page: 1, size: 200 }) || []
  } catch { campaigns.value = [] }
}

const days = computed(() => {
  const result: any[] = []
  const firstDay = new Date(year.value, month.value - 1, 1)
  const startWeekday = firstDay.getDay()
  const daysInMonth = new Date(year.value, month.value, 0).getDate()
  const prevMonthDays = new Date(year.value, month.value - 1, 0).getDate()
  // 上月补位
  for (let i = startWeekday - 1; i >= 0; i--) {
    const d = prevMonthDays - i
    const m = month.value - 1 <= 0 ? 12 : month.value - 1
    const y = month.value - 1 <= 0 ? year.value - 1 : year.value
    result.push(buildDay(y, m, d, true))
  }
  for (let d = 1; d <= daysInMonth; d++) {
    result.push(buildDay(year.value, month.value, d, false))
  }
  // 下月补位(凑齐 6 行 = 42)
  while (result.length < 42) {
    const idx = result.length - (startWeekday + daysInMonth) + 1
    const m = month.value + 1 > 12 ? 1 : month.value + 1
    const y = month.value + 1 > 12 ? year.value + 1 : year.value
    result.push(buildDay(y, m, idx, true))
  }
  return result
})

function buildDay(y: number, m: number, d: number, other: boolean) {
  const key = `${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}`
  const events: any[] = []
  // 节日
  if (festivals[key]) {
    events.push({ kind: 'festival', title: festivals[key], desc: '节日' })
  }
  if (!other) {
    // 生日
    for (const mem of members.value) {
      if (mem.birthday && mem.birthday.endsWith(key)) {
        events.push({ kind: 'birthday', title: mem.name, desc: `会员生日 · ${mem.phone || ''}`, link: `/members`, id: mem.id })
      }
    }
    // 券到期: RANGE 类型用 validEnd; DAYS 类型按 createdAt + validDays 推算到期日
    for (const c of coupons.value) {
      let expireDate = ''
      if (c.validType === 'RANGE' && c.validEnd) {
        // validEnd 可能是 "2026-12-31" 或带时间的 ISO 串, 统一取前 10 位
        expireDate = String(c.validEnd).slice(0, 10)
      } else if (c.validType === 'DAYS' && c.validDays && c.createdAt) {
        const created = new Date(String(c.createdAt))
        if (!isNaN(created.getTime())) {
          const expire = new Date(created.getTime() + c.validDays * 86400000)
          expireDate = expire.toISOString().slice(0, 10)
        }
      }
      if (expireDate && expireDate === `${y}-${key}`) {
        events.push({ kind: 'coupon', title: c.name, desc: '券到期', link: `/coupons`, id: c.id })
      }
    }
    // 活动
    for (const c of campaigns.value) {
      if (c.startAt && c.startAt.startsWith(`${y}-${key}`)) {
        events.push({ kind: 'campaign', title: c.name, desc: c.description || '活动开始', link: `/campaigns`, id: c.id })
      }
    }
  }
  const dateStr = `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}`
  const isToday = dateStr === today.toISOString().slice(0, 10)
  return { year: y, month: m, day: d, otherMonth: other, isToday, festival: festivals[key], events }
}

function shift(delta: number) {
  let m = month.value + delta
  let y = year.value
  if (m > 12) { m = 1; y++ }
  if (m < 1) { m = 12; y-- }
  month.value = m
  year.value = y
}

function goToday() {
  year.value = today.getFullYear()
  month.value = today.getMonth() + 1
}

function select(d: any) {
  selected.value = { ...d, isSelected: true }
  drawer.value = true
}

function dotClass(kind: string) {
  return { birthday: 'info', festival: 'warning', coupon: 'success', campaign: 'primary' }[kind] || 'info'
}
function kindLabel(kind: string) {
  return { birthday: '生日', festival: '节日', coupon: '券到期', campaign: '活动' }[kind] || kind
}

function jump(e: any) {
  if (e.link) router.push(e.link)
}

onMounted(loadAll)
</script>

<style scoped>
.month-label { padding: 0 8px; color: var(--ink); font-weight: 500; }
.calendar-card { padding: 12px; }
.week-row { display: grid; grid-template-columns: repeat(7, 1fr); border-bottom: 1px solid var(--line); }
.week-cell { padding: 6px 8px; color: var(--muted); font-size: 12px; text-align: center; }
.day-grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 4px; padding-top: 4px; }
.day-cell { min-height: 96px; padding: 6px 8px; cursor: pointer; display: flex; flex-direction: column; }
.day-cell.other { background: #faf9f6; opacity: 0.5; }
.day-cell.today .d-num { background: var(--primary-action); color: #fff; }
.day-cell.selected { border-color: var(--primary-action); }
.d-head { display: flex; align-items: center; justify-content: space-between; }
.d-num { display: inline-flex; align-items: center; justify-content: center; width: 22px; height: 22px; border-radius: 50%; font-size: 12px; color: var(--ink); }
.d-festival { font-size: 11px; color: var(--warning); }
.d-events { display: flex; flex-direction: column; gap: 2px; margin-top: 4px; }
.event-chip { display: flex; align-items: center; gap: 4px; font-size: 11px; color: var(--ink-2); padding: 2px 4px; border-radius: 4px; background: rgba(108,120,108,0.06); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.event-chip.birthday { color: var(--primary-action); background: var(--primary-action-soft); }
.event-chip.festival { color: var(--warning); background: rgba(184, 161, 106, 0.12); }
.event-chip.coupon { color: var(--success); background: rgba(126, 154, 138, 0.12); }
.event-chip.campaign { color: var(--primary); background: rgba(138, 130, 120, 0.12); }
.more { font-size: 10px; color: var(--muted); }
.section-title { font-weight: 600; color: var(--ink); margin: 12px 0 6px; }
.event-item { padding: 10px 12px; margin-bottom: 8px; cursor: pointer; }
.event-head { display: flex; align-items: center; gap: 8px; }
.event-kind { font-size: 11px; padding: 1px 6px; border-radius: 4px; background: var(--primary-action-soft); color: var(--primary-action); }
.event-title { color: var(--ink); font-size: 13px; }
.event-desc { color: var(--muted); font-size: 12px; margin-top: 4px; }
.kv { display: flex; align-items: center; justify-content: space-between; padding: 4px 0; }
.kv .k { color: var(--muted); font-size: 13px; }
.kv .v { color: var(--ink); font-size: 13px; }
</style>
