<template>
  <div class="kpi-card x-card hoverable">
    <div class="kpi-highlight"></div>
    <div class="kpi-top">
      <div class="kpi-icon" :style="{ background: iconBg, color: iconColor }">
        <el-icon><component :is="iconComp" /></el-icon>
      </div>
      <div class="kpi-label">{{ label }}</div>
    </div>
    <div class="kpi-value val">
      <span class="num">{{ formattedValue }}</span>
      <span v-if="suffix" class="suffix">{{ suffix }}</span>
    </div>
    <div class="kpi-foot">
      <span v-if="trend !== null && trend !== undefined" :class="['trend', trend >= 0 ? 'pos' : 'neg']">
        <el-icon><CaretTop v-if="trend >= 0" /><CaretBottom v-else /></el-icon>
        {{ Math.abs(trend) }}%
      </span>
      <span v-if="trendLabel" class="trend-label">{{ trendLabel }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, markRaw } from 'vue'
import * as ElIcons from '@element-plus/icons-vue'
import { CaretBottom, CaretTop } from '@element-plus/icons-vue'

const props = defineProps<{
  label: string
  value: number | string
  suffix?: string
  prefix?: string
  trend?: number | null       // 环比 %
  trendLabel?: string
  icon?: string
  iconBg?: string
  iconColor?: string
  precision?: number
}>()

const iconComp = computed(() => {
  const name = props.icon || 'DataLine'
  // 从 Element Plus icons 中按名查找
  const lib: any = (ElIcons as any)
  return markRaw(lib[name] || lib.DataLine)
})

const formattedValue = computed(() => {
  if (typeof props.value === 'string') return props.value
  if (props.precision !== undefined) {
    return props.value.toFixed(props.precision)
  }
  // 整数
  if (Number.isInteger(props.value)) return props.value.toLocaleString('zh-CN')
  return props.value.toLocaleString('zh-CN', { maximumFractionDigits: 2 })
})

const iconBg = computed(() => props.iconBg || 'var(--primary-action-soft)')
const iconColor = computed(() => props.iconColor || 'var(--primary-action)')
</script>

<style scoped>
.kpi-card {
  padding: 18px 20px 16px;
  position: relative;
  overflow: hidden;
}
.kpi-highlight {
  position: absolute;
  left: 0; right: 0; top: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent 0%, rgba(111,148,184,0.45) 50%, transparent 100%);
  pointer-events: none;
}
.kpi-top {
  display: flex; align-items: center; gap: 8px;
  margin-bottom: 12px;
}
.kpi-icon {
  width: 32px; height: 32px;
  border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  font-size: 16px;
}
.kpi-label {
  font-size: 13px;
  color: var(--muted);
  letter-spacing: 0.3px;
}
.kpi-value {
  font-size: 26px;
  font-weight: 600;
  color: var(--ink);
  line-height: 1.2;
  margin-bottom: 8px;
}
.kpi-value .num { font-variant-numeric: tabular-nums; letter-spacing: -0.3px; }
.kpi-value .suffix {
  font-size: 12px;
  color: var(--muted);
  margin-left: 4px;
  font-weight: 400;
}
.kpi-foot {
  display: flex; align-items: center; gap: 8px;
  font-size: 12px;
}
.trend {
  display: inline-flex; align-items: center; gap: 2px;
  font-weight: 500;
}
.trend .el-icon { font-size: 12px; }
.trend-label { color: var(--muted); }
</style>
