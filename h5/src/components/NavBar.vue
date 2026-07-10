<template>
  <div class="nav-bar" :class="{ 'is-fixed': fixed, 'is-transparent': transparent }">
    <div v-if="!transparent" class="nav-row">
      <div class="nav-left">
        <div v-if="back" class="nav-icon" @click="onBack">
          <van-icon name="arrow-left" size="20" />
        </div>
        <slot name="left" />
      </div>
      <div class="nav-title">
        <div class="title-main">{{ title }}</div>
        <div v-if="subtitle" class="title-sub">{{ subtitle }}</div>
      </div>
      <div class="nav-right">
        <slot name="right" />
      </div>
    </div>
    <slot />
  </div>
</template>

<script setup lang="ts">
interface Props { title?: string; subtitle?: string; back?: boolean; fixed?: boolean; transparent?: boolean }
withDefaults(defineProps<Props>(), { title: '', subtitle: '', back: false, fixed: false, transparent: false })

const router = useRouter()
function onBack() { if (window.history.length > 1) router.back(); else router.push('/') }
import { useRouter } from 'vue-router'
</script>

<style scoped>
.nav-bar {
  background: var(--surface);
  position: relative;
  z-index: 10;
}
.nav-bar.is-fixed {
  position: sticky; top: 0; left: 0; right: 0;
  max-width: 480px; margin: 0 auto;
}
.nav-bar.is-transparent { background: transparent; }
.nav-row {
  display: flex; align-items: center;
  height: 44px; padding: 0 6px;
  border-bottom: 1px solid var(--line);
}
.nav-left, .nav-right {
  display: flex; align-items: center;
  min-width: 56px;
}
.nav-right { justify-content: flex-end; }
.nav-icon {
  width: 36px; height: 36px;
  display: flex; align-items: center; justify-content: center;
  color: var(--ink);
  border-radius: 8px;
  transition: background-color var(--dur) var(--ease);
  cursor: pointer;
}
.nav-icon:hover { background: var(--surface-2); }
.nav-icon:active { background: var(--surface-3); }
.nav-title {
  flex: 1; text-align: center;
  display: flex; flex-direction: column; gap: 0;
  line-height: 1.2;
}
.title-main {
  font-size: 15px; font-weight: 600; color: var(--ink);
  letter-spacing: 0.04em;
}
.title-sub {
  font-size: 10.5px; color: var(--muted);
  letter-spacing: 0.06em; margin-top: 1px;
}
</style>
