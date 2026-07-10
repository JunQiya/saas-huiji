<template>
  <header class="blog-header">
    <div v-if="slogan" class="header-slogan">{{ slogan }}</div>
    <div class="header-main">
      <div class="header-left">
        <div class="brand-mark">
          <span class="star"></span>
        </div>
        <div class="brand-text">
          <div class="brand-name">星河·会记</div>
          <div class="brand-tag">HUIJI</div>
        </div>
      </div>
      <div class="header-right">
        <slot name="right">
          <div class="header-time">
            <div class="time-day">{{ today.day }}</div>
            <div class="time-detail">{{ today.detail }}</div>
          </div>
        </slot>
      </div>
    </div>
    <div class="header-bottom"></div>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  slogan?: string
}
withDefaults(defineProps<Props>(), { slogan: '' })

const today = computed(() => {
  const d = new Date()
  const weekdays = ['日', '一', '二', '三', '四', '五', '六']
  return {
    day: `${d.getDate()}`,
    detail: `${d.getMonth() + 1}月·周${weekdays[d.getDay()]}`
  }
})
</script>

<style scoped>
.blog-header {
  padding: 14px 16px 12px;
  background: var(--surface);
  position: relative;
  animation: fade-in 0.5s var(--ease);
}
.header-slogan {
  font-size: 11.5px;
  color: var(--muted);
  letter-spacing: 0.12em;
  margin-bottom: 10px;
  padding-left: 2px;
  font-weight: 400;
  animation: fade-in 0.6s var(--ease) 0.1s both;
}
.header-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 11px;
  animation: fade-in 0.6s var(--ease) 0.2s both;
}
.brand-mark {
  width: 34px; height: 34px;
  display: flex; align-items: center; justify-content: center;
  background: var(--brand-soft);
  border-radius: 10px;
  position: relative;
}
.brand-mark .star {
  width: 16px; height: 16px;
  background: var(--brand-deep);
  clip-path: polygon(50% 0%, 61% 35%, 98% 35%, 68% 57%, 79% 91%, 50% 70%, 21% 91%, 32% 57%, 2% 35%, 39% 35%);
}
.brand-text {
  display: flex; flex-direction: column;
  gap: 1px;
}
.brand-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--ink);
  letter-spacing: 0.04em;
  font-family: 'Songti SC', 'STSong', 'SimSun', serif;
  line-height: 1.2;
}
.brand-tag {
  font-size: 10px;
  color: var(--muted);
  letter-spacing: 0.32em;
  font-weight: 500;
  margin-top: 1px;
}
.header-right {
  display: flex; align-items: center; gap: 10px;
  animation: fade-in 0.6s var(--ease) 0.3s both;
}
.header-time {
  text-align: right;
  display: flex;
  flex-direction: column;
  gap: 1px;
  line-height: 1.1;
}
.time-day {
  font-size: 20px;
  font-weight: 500;
  color: var(--ink);
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.01em;
}
.time-detail {
  font-size: 10.5px;
  color: var(--muted);
  letter-spacing: 0.08em;
}
.header-bottom {
  margin: 12px -16px 0;
  height: 1px;
  border-top: 1px dashed var(--line-2);
  position: relative;
}
.header-bottom::after {
  content: '';
  position: absolute;
  top: -1px;
  left: 16px;
  width: 40px;
  height: 1px;
  background: var(--brand);
  border-radius: 1px;
}
@keyframes fade-in {
  from { opacity: 0; transform: translateY(-4px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
