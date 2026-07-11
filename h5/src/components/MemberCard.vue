<template>
  <div class="member-card" :class="`level-${level}`">
    <div class="card-content">
      <div class="row-top">
        <div class="brand-mark" aria-hidden="true">
          <svg viewBox="0 0 24 24" width="20" height="20">
            <circle cx="8" cy="9" r="1.2" fill="#fff" />
            <circle cx="16" cy="9" r="1.2" fill="#fff" />
            <circle cx="12" cy="13" r="1.6" fill="#fff" />
            <circle cx="12" cy="17" r="0.8" fill="#fff" opacity="0.7" />
            <g stroke="#fff" stroke-width="0.5" opacity="0.5" fill="none">
              <line x1="8" y1="9" x2="12" y2="13" />
              <line x1="16" y1="9" x2="12" y2="13" />
              <line x1="12" y1="13" x2="12" y2="17" />
            </g>
          </svg>
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
          <div class="stat-val">¥<span class="num">{{ formatMoney(balance) }}</span></div>
          <div class="stat-lbl">储值余额</div>
        </div>
        <div class="stat-sep"></div>
        <div class="stat">
          <div class="stat-val num">{{ points ?? 0 }}</div>
          <div class="stat-lbl">积分</div>
        </div>
        <div class="stat-sep"></div>
        <div class="stat">
          <div class="stat-val num">{{ consumeCount ?? 0 }}</div>
          <div class="stat-lbl">到店</div>
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
withDefaults(defineProps<Props>(), { level: 1, levelName: '普通会员', points: 0, consumeCount: 0 })

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
  border-radius: var(--r-lg);
  padding: 16px 18px 18px;
  color: #fff;
  overflow: hidden;
  box-shadow: 0 6px 24px rgba(31, 29, 24, 0.18);
  border: 1px solid transparent;
}
/* 等级配色：深浅有别 */
.member-card.level-1 { background: #8a8578; }
.member-card.level-2 { background: #6b7e8e; }
.member-card.level-3 { background: #4a6583; }
.member-card.level-4 { background: #2e4863; }

.card-content { position: relative; z-index: 1; }
.row-top { display: flex; align-items: center; gap: 8px; margin-bottom: 22px; }
.brand-mark {
  width: 22px; height: 22px;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.brand-text {
  font-family: var(--font-serif);
  font-size: 11.5px; letter-spacing: 0.18em;
  opacity: 0.85;
  font-weight: 400;
}
.card-level {
  margin-left: auto;
  font-family: var(--font-serif);
  font-size: 10.5px;
  padding: 2px 9px;
  background: rgba(255, 255, 255, 0.12);
  border-radius: 2px;
  letter-spacing: 0.18em;
  font-weight: 400;
}
.row-name { margin-bottom: 4px; }
.hi {
  font-family: var(--font-serif);
  font-size: 18px; font-weight: 400;
  letter-spacing: 0.04em;
}
.row-phone {
  font-family: var(--font-num);
  font-size: 11.5px;
  opacity: 0.65;
  letter-spacing: 0.16em;
  margin-bottom: 22px;
}
.row-stats {
  display: flex; align-items: center;
  gap: 10px;
  padding-top: 14px;
  border-top: 1px dashed rgba(255, 255, 255, 0.20);
}
.stat { flex: 1; }
.stat-val {
  font-family: var(--font-num);
  font-size: 18px; font-weight: 500;
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.01em;
  line-height: 1.2;
}
.stat-val .num { margin-left: 1px; }
.stat-lbl {
  font-family: var(--font-serif);
  font-size: 10.5px;
  opacity: 0.65;
  letter-spacing: 0.16em;
  margin-top: 4px;
}
.stat-sep {
  width: 1px; height: 24px;
  background: rgba(255, 255, 255, 0.18);
}
</style>
