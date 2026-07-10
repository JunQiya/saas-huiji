<template>
  <div class="member-card" :class="`level-${level}`">
    <div class="card-decor decor-1"></div>
    <div class="card-decor decor-2"></div>
    <div class="card-content">
      <div class="row-top">
        <div class="brand-mark">
          <span class="star"></span>
        </div>
        <div class="brand-text">星河·会记</div>
        <div class="card-level">{{ levelName }}</div>
      </div>
      <div class="row-name">
        <div class="hi">{{ greeting }}，{{ name }}</div>
      </div>
      <div class="row-phone">{{ formatPhone(phone) }}</div>
      <div class="row-stats">
        <div class="stat">
          <div class="stat-val">¥{{ formatMoney(balance) }}</div>
          <div class="stat-lbl">储值余额</div>
        </div>
        <div class="stat-sep"></div>
        <div class="stat">
          <div class="stat-val">{{ points ?? 0 }}</div>
          <div class="stat-lbl">积分</div>
        </div>
        <div class="stat-sep"></div>
        <div class="stat">
          <div class="stat-val">{{ consumeCount ?? 0 }}</div>
          <div class="stat-lbl">到店次数</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
interface Props {
  name: string
  phone: string
  level?: number
  levelName?: string
  balance: number | string
  points?: number
  consumeCount?: number
}
const props = withDefaults(defineProps<Props>(), { level: 1, levelName: '普通会员', points: 0, consumeCount: 0 })

const hour = new Date().getHours()
const greeting = hour < 6 ? '夜深了' : hour < 11 ? '早安' : hour < 14 ? '午安' : hour < 18 ? '下午好' : '晚上好'

function formatMoney(n: any) {
  return (Number(n || 0) / 100).toFixed(2)
}
function formatPhone(p: string) {
  if (!p || p.length !== 11) return p || ''
  return p.slice(0, 3) + ' ' + p.slice(3, 7) + ' ' + p.slice(7)
}
</script>

<style scoped>
.member-card {
  position: relative;
  background: linear-gradient(135deg, #4a6a87 0%, #5a7d9f 55%, #6f94b8 100%);
  border-radius: var(--r-lg);
  padding: 16px 18px 18px;
  color: #fff;
  overflow: hidden;
  box-shadow: 0 6px 24px rgba(74, 106, 135, 0.22);
}
.member-card.level-1 { background: linear-gradient(135deg, #6c7066 0%, #8a8e85 100%); }
.member-card.level-2 { background: linear-gradient(135deg, #5a7d9f 0%, #6f94b8 100%); }
.member-card.level-3 { background: linear-gradient(135deg, #4a6a87 0%, #5a7d9f 100%); }
.member-card.level-4 {
  background:
    radial-gradient(circle at 80% 20%, rgba(255, 255, 255, 0.18), transparent 40%),
    linear-gradient(135deg, #3a5a76 0%, #4a6a87 100%);
}
.card-decor {
  position: absolute; border-radius: 50%;
  pointer-events: none;
}
.decor-1 {
  width: 180px; height: 180px;
  top: -60px; right: -40px;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.10), transparent 70%);
}
.decor-2 {
  width: 100px; height: 100px;
  bottom: -30px; left: -20px;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.06), transparent 70%);
}
.card-content { position: relative; z-index: 1; }
.row-top { display: flex; align-items: center; gap: 8px; margin-bottom: 18px; }
.brand-mark {
  width: 22px; height: 22px;
  display: flex; align-items: center; justify-content: center;
  background: rgba(255, 255, 255, 0.18);
  border-radius: 6px;
}
.brand-mark .star {
  width: 11px; height: 11px;
  background: #fff;
  clip-path: polygon(50% 0%, 61% 35%, 98% 35%, 68% 57%, 79% 91%, 50% 70%, 21% 91%, 32% 57%, 2% 35%, 39% 35%);
}
.brand-text {
  font-size: 11px; letter-spacing: 0.16em;
  opacity: 0.85;
  font-weight: 500;
}
.card-level {
  margin-left: auto;
  font-size: 10.5px;
  padding: 2px 8px;
  background: rgba(255, 255, 255, 0.18);
  border-radius: 999px;
  letter-spacing: 0.08em;
  font-weight: 500;
}
.row-name { margin-bottom: 4px; }
.hi {
  font-size: 17px; font-weight: 500;
  letter-spacing: 0.02em;
}
.row-phone {
  font-size: 11.5px;
  opacity: 0.72;
  font-family: 'SF Mono', monospace;
  letter-spacing: 0.12em;
  margin-bottom: 18px;
}
.row-stats {
  display: flex; align-items: center;
  gap: 12px;
  padding-top: 14px;
  border-top: 1px dashed rgba(255, 255, 255, 0.18);
}
.stat { flex: 1; }
.stat-val {
  font-size: 18px; font-weight: 600;
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.01em;
  line-height: 1.2;
}
.stat-lbl {
  font-size: 10.5px;
  opacity: 0.7;
  letter-spacing: 0.08em;
  margin-top: 2px;
}
.stat-sep {
  width: 1px; height: 26px;
  background: rgba(255, 255, 255, 0.18);
}
</style>
