<template>
  <header class="blog-header">
    <div v-if="slogan" class="header-slogan">{{ slogan }}</div>
    <div class="header-main">
      <div class="header-left">
        <div class="brand-mark" aria-hidden="true">星</div>
        <div class="brand-text">
          <div class="brand-name">星河·会记</div>
          <div class="brand-tag">HUIJI · 会员经营</div>
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
    <div class="header-divider" aria-hidden="true"></div>
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
    detail: `${d.getMonth() + 1}月 · 周${weekdays[d.getDay()]}`
  }
})
</script>

<style scoped>
.blog-header {
  padding: calc(env(safe-area-inset-top, 0px) + 16px) 16px 0;
  background: var(--surface);
  position: relative;
}
.header-slogan {
  font-family: var(--font-serif);
  font-size: 11.5px;
  color: var(--ink-3);
  letter-spacing: 0.18em;
  margin-bottom: 12px;
  padding-left: 2px;
  animation: x-fade-in 0.5s var(--ease-out) both;
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
  animation: x-fade-in 0.5s var(--ease-out) 0.08s both;
}
.brand-mark {
  width: 36px; height: 36px;
  background: var(--brand);
  color: #fff;
  font-family: var(--font-serif);
  font-size: 17px;
  border-radius: var(--r-md);
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
  letter-spacing: 0.04em;
}
.brand-text {
  display: flex; flex-direction: column;
  gap: 2px;
}
.brand-name {
  font-family: var(--font-serif);
  font-size: 17px;
  font-weight: 500;
  color: var(--ink);
  letter-spacing: 0.08em;
  line-height: 1.2;
}
.brand-tag {
  font-family: var(--font-num);
  font-size: 9.5px;
  color: var(--muted);
  letter-spacing: 0.28em;
  font-weight: 400;
}
.header-right {
  display: flex; align-items: center; gap: 10px;
  animation: x-fade-in 0.5s var(--ease-out) 0.16s both;
}
.header-time {
  text-align: right;
  display: flex;
  flex-direction: column;
  gap: 1px;
  line-height: 1.1;
}
.time-day {
  font-family: var(--font-num);
  font-size: 22px;
  font-weight: 500;
  color: var(--ink);
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.01em;
  line-height: 1;
}
.time-detail {
  font-family: var(--font-serif);
  font-size: 10.5px;
  color: var(--muted);
  letter-spacing: 0.08em;
  margin-top: 4px;
}
.header-divider {
  margin: 12px -16px 0;
  height: 1px;
  border-top: 1px dashed var(--line-2);
  position: relative;
}
.header-divider::after {
  content: '';
  position: absolute;
  top: -1px;
  left: 16px;
  width: 36px;
  height: 1px;
  background: var(--brand);
  border-radius: 1px;
  opacity: 0.8;
}
</style>
