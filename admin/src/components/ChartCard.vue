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
    <div class="chart-body" :style="{ height: bodyHeight + 'px' }">
      <slot />
      <div v-if="loading" class="skeleton">
        <div class="sk-bar" v-for="n in 4" :key="n" :style="{ height: 12 + (n * 6) + 'px' }"></div>
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
const bodyHeight = computed(() => height.value - 56)
</script>

<style scoped>
.chart-card {
  padding: 18px 20px;
  display: flex; flex-direction: column;
}
.chart-head {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 12px;
}
.chart-title {
  font-size: 14.5px; font-weight: 600; color: var(--ink);
}
.chart-sub {
  font-size: 12.5px; color: var(--muted); margin-top: 2px;
}
.chart-extra { display: flex; align-items: center; gap: 8px; }
.chart-body { position: relative; }
.skeleton {
  position: absolute; inset: 0;
  display: flex; flex-direction: column; justify-content: flex-end; gap: 8px;
  padding: 12px 0;
}
.sk-bar {
  background: linear-gradient(90deg, #f1efe9 0%, #e8e6e0 50%, #f1efe9 100%);
  background-size: 200% 100%;
  border-radius: 4px;
  animation: shimmer 1.4s linear infinite;
}
@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>
