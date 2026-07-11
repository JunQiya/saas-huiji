<template>
  <div class="chart-card x-card" v-loading="loading" :style="{ minHeight: height + 'px' }">
    <div class="chart-head">
      <div class="chart-text">
        <div class="chart-title">{{ title }}</div>
        <div v-if="subtitle" class="chart-sub">{{ subtitle }}</div>
      </div>
      <div v-if="$slots.extra" class="chart-extra">
        <slot name="extra" />
      </div>
    </div>
    <div class="chart-divider"></div>
    <div class="chart-body" :style="{ height: bodyHeight + 'px' }">
      <slot />
      <div v-if="loading" class="skeleton">
        <div class="sk-bar" v-for="n in 4" :key="n" :style="{ height: 8 + (n * 5) + 'px' }"></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  title: string
  subtitle?: string
  height?: number
  loading?: boolean
}>()

const height = computed(() => props.height || 300)
const bodyHeight = computed(() => height.value - 64)
</script>

<style scoped>
.chart-card {
  padding: 16px 18px 14px;
  display: flex; flex-direction: column;
}
.chart-head {
  display: flex; align-items: flex-start; justify-content: space-between; gap: 10px;
  margin-bottom: 10px;
}
.chart-text { min-width: 0; }
.chart-title {
  font-family: var(--font-serif);
  font-size: 14.5px; font-weight: 500; color: var(--ink);
  letter-spacing: 0.06em;
}
.chart-sub {
  font-size: 12px; color: var(--muted);
  margin-top: 3px; letter-spacing: 0.02em;
  font-family: var(--font-serif);
}
.chart-extra { display: flex; align-items: center; gap: 6px; flex-shrink: 0; }
.chart-divider {
  height: 1px; border-top: 1px dashed var(--line-2);
  margin-bottom: 6px;
}
.chart-body { position: relative; }
.skeleton {
  position: absolute; inset: 0;
  display: flex; flex-direction: column; justify-content: flex-end; gap: 8px;
  padding: 12px 0;
}
.sk-bar {
  background: var(--surface-3);
  border-radius: 3px;
  height: 8px;
}
</style>
