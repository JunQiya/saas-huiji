<template>
  <div class="x-empty">
    <div class="empty-art" :class="`art-${art}`">
      <svg v-if="art === 'leaf'" viewBox="0 0 80 80" width="64" height="64">
        <path d="M40 12 C24 18 18 36 22 52 C26 64 36 68 40 68 C44 68 54 64 58 52 C62 36 56 18 40 12 Z" fill="none" stroke="currentColor" stroke-width="1.2" />
        <path d="M40 18 L40 62" stroke="currentColor" stroke-width="0.8" stroke-dasharray="2,2" />
        <path d="M40 30 L32 36" stroke="currentColor" stroke-width="0.8" />
        <path d="M40 30 L48 36" stroke="currentColor" stroke-width="0.8" />
        <path d="M40 42 L30 50" stroke="currentColor" stroke-width="0.8" />
        <path d="M40 42 L50 50" stroke="currentColor" stroke-width="0.8" />
      </svg>
      <svg v-else-if="art === 'star'" viewBox="0 0 80 80" width="64" height="64">
        <circle cx="40" cy="40" r="2" fill="currentColor" />
        <circle cx="22" cy="20" r="1.2" fill="currentColor" opacity="0.6" />
        <circle cx="58" cy="24" r="1" fill="currentColor" opacity="0.5" />
        <circle cx="62" cy="58" r="1.4" fill="currentColor" opacity="0.7" />
        <circle cx="20" cy="56" r="1" fill="currentColor" opacity="0.5" />
        <path d="M40 28 L42 38 L52 40 L42 42 L40 52 L38 42 L28 40 L38 38 Z" fill="currentColor" opacity="0.85" />
      </svg>
      <svg v-else viewBox="0 0 80 80" width="64" height="64">
        <rect x="20" y="22" width="40" height="36" rx="2" fill="none" stroke="currentColor" stroke-width="1.2" />
        <line x1="20" y1="32" x2="60" y2="32" stroke="currentColor" stroke-width="1" />
        <line x1="34" y1="22" x2="34" y2="58" stroke="currentColor" stroke-width="0.8" />
        <circle cx="46" cy="44" r="1.2" fill="currentColor" opacity="0.5" />
        <circle cx="52" cy="50" r="1.2" fill="currentColor" opacity="0.5" />
      </svg>
    </div>
    <div class="empty-title">{{ title }}</div>
    <div v-if="sub" class="empty-sub">{{ sub }}</div>
    <slot />
  </div>
</template>

<script setup lang="ts">
interface Props {
  title?: string
  sub?: string
  art?: 'leaf' | 'star' | 'box'
}
withDefaults(defineProps<Props>(), { title: '空空如也', sub: '', art: 'leaf' })
</script>

<style scoped>
.x-empty {
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  padding: 56px 24px;
  color: var(--muted);
  gap: 4px;
}
.empty-art { color: var(--muted-2); margin-bottom: 14px; opacity: 0.85; }
.empty-title { font-size: 13.5px; color: var(--ink-2); letter-spacing: 0.06em; }
.empty-sub { font-size: 11.5px; color: var(--muted); margin-top: 4px; line-height: 1.6; text-align: center; }
</style>
