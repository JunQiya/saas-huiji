<template>
  <header class="blog-header">
    <div v-if="slogan" class="header-slogan">{{ slogan }}</div>
    <div class="header-main">
      <div class="header-left">
        <div class="brand-mark" aria-hidden="true">
          <svg viewBox="0 0 36 36" width="34" height="34">
            <!-- 背景小圆点（远星） -->
            <circle cx="9" cy="9" r="0.8" fill="var(--brand-ink)" opacity="0.35" />
            <circle cx="27" cy="6" r="0.7" fill="var(--brand-ink)" opacity="0.28" />
            <circle cx="30" cy="22" r="0.8" fill="var(--brand-ink)" opacity="0.32" />
            <circle cx="7" cy="28" r="0.7" fill="var(--brand-ink)" opacity="0.25" />
            <!-- 星座连线 -->
            <g stroke="var(--brand-ink)" stroke-width="0.6" opacity="0.45" fill="none">
              <line x1="13" y1="14" x2="18" y2="20" />
              <line x1="18" y1="20" x2="23" y2="14" />
              <line x1="18" y1="20" x2="18" y2="26" />
            </g>
            <!-- 主星 -->
            <circle cx="13" cy="14" r="1.6" fill="var(--brand-deep)" />
            <circle cx="23" cy="14" r="1.6" fill="var(--brand-deep)" />
            <circle cx="18" cy="20" r="2.2" fill="var(--brand-deep)" />
            <circle cx="18" cy="26" r="1.2" fill="var(--brand-deep)" opacity="0.7" />
          </svg>
        </div>
        <div class="brand-text">
          <div class="brand-name">星河·会记</div>
          <div class="brand-tag">HUIJI · 夜读手记</div>
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
  padding: 16px 16px 0;
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
  width: 34px; height: 34px;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
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
