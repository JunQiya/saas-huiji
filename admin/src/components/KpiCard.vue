<template>
  <div class="kpi-card x-card hoverable" :class="tone && `tone-${tone}`">
    <div class="kpi-mark" aria-hidden="true">
      <svg viewBox="0 0 24 24" width="18" height="18">
        <circle cx="12" cy="12" r="2" :fill="iconColor" />
        <g :stroke="iconColor" stroke-width="0.8" fill="none" opacity="0.55">
          <line x1="12" y1="3" x2="12" y2="6.5" />
          <line x1="12" y1="17.5" x2="12" y2="21" />
          <line x1="3" y1="12" x2="6.5" y2="12" />
          <line x1="17.5" y1="12" x2="21" y2="12" />
          <line x1="5.5" y1="5.5" x2="7.8" y2="7.8" />
          <line x1="16.2" y1="16.2" x2="18.5" y2="18.5" />
          <line x1="5.5" y1="18.5" x2="7.8" y2="16.2" />
          <line x1="16.2" y1="7.8" x2="18.5" y2="5.5" />
        </g>
      </svg>
    </div>
    <div class="kpi-body">
      <div class="kpi-label">{{ label }}</div>
      <div class="kpi-value">
        <span v-if="prefix" class="prefix">{{ prefix }}</span>
        <span class="num">{{ formattedValue }}</span>
        <span v-if="suffix" class="suffix">{{ suffix }}</span>
      </div>
      <div class="kpi-foot">
        <span
          v-if="trend !== null && trend !== undefined"
          :class="['trend', trend >= 0 ? 'pos' : 'neg']"
        >
          <span class="trend-arrow">{{ trend >= 0 ? '↑' : '↓' }}</span>
          {{ Math.abs(trend) }}%
        </span>
        <span v-if="trendLabel" class="trend-label">{{ trendLabel }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  label: string
  value: number | string
  suffix?: string
  prefix?: string
  trend?: number | null
  trendLabel?: string
  tone?: 'brand' | 'twilight' | 'mist' | 'clay' | 'sage' | 'rose'
  iconColor?: string
  precision?: number
}>()

const formattedValue = computed(() => {
  if (typeof props.value === 'string') return props.value
  if (props.precision !== undefined) return props.value.toFixed(props.precision)
  if (Number.isInteger(props.value)) return props.value.toLocaleString('zh-CN')
  return props.value.toLocaleString('zh-CN', { maximumFractionDigits: 2 })
})

const toneColor = computed(() => {
  const map: Record<string, string> = {
    brand: 'var(--brand-ink)',
    twilight: '#5e5278',
    mist: '#4d5e68',
    clay: '#8a5a3a',
    sage: '#4a6655',
    rose: '#8a5a52'
  }
  return props.iconColor || map[props.tone || 'brand'] || map.brand
})
</script>

<style scoped>
.kpi-card {
  padding: 16px 18px 14px;
  display: flex; align-items: flex-start; gap: 12px;
  position: relative;
  overflow: hidden;
  min-height: 96px;
}
.kpi-card::before {
  content: '';
  position: absolute;
  left: 0; top: 14px; bottom: 14px;
  width: 2px;
  background: var(--brand-soft);
  border-radius: 0 2px 2px 0;
  transition: background var(--dur) var(--ease-out), width var(--dur) var(--ease-out);
}
.kpi-card.tone-twilight::before { background: var(--accent-twilight-soft); }
.kpi-card.tone-mist::before     { background: var(--accent-mist-soft); }
.kpi-card.tone-clay::before     { background: var(--accent-clay-soft); }
.kpi-card.tone-sage::before     { background: var(--accent-sage-soft); }
.kpi-card.tone-rose::before     { background: var(--accent-rose-soft); }
.kpi-card:hover::before { width: 3px; }

.kpi-mark {
  width: 30px; height: 30px;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
  margin-top: 2px;
  opacity: 0.85;
}

.kpi-body { flex: 1; min-width: 0; }
.kpi-label {
  font-family: var(--font-serif);
  font-size: 12.5px; font-weight: 400;
  color: var(--muted);
  letter-spacing: 0.08em;
  margin-bottom: 6px;
}
.kpi-value {
  display: flex; align-items: baseline;
  color: var(--ink);
  line-height: 1.15;
  margin-bottom: 8px;
  font-family: var(--font-num);
}
.kpi-value .prefix, .kpi-value .suffix {
  font-family: var(--font-ui);
  font-size: 12px; color: var(--muted);
  font-weight: 400;
  letter-spacing: 0;
}
.kpi-value .prefix { margin-right: 2px; }
.kpi-value .suffix { margin-left: 4px; }
.kpi-value .num {
  font-size: 26px; font-weight: 500;
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.01em;
}

.kpi-foot {
  display: flex; align-items: center; gap: 8px;
  font-size: 11.5px;
}
.trend {
  display: inline-flex; align-items: center; gap: 2px;
  font-weight: 500;
  font-family: var(--font-num);
  font-size: 12px;
}
.trend .trend-arrow { font-size: 11px; line-height: 1; }
.trend.pos { color: var(--success); }
.trend.neg { color: var(--danger); }
.trend-label {
  color: var(--muted);
  font-family: var(--font-serif);
  letter-spacing: 0.04em;
}
</style>
