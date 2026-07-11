<template>
  <nav class="x-tabbar safe-bottom">
    <div
      v-for="item in items"
      :key="item.path"
      class="tab-item"
      :class="{ active: isActive(item.path) }"
      @click="go(item.path)"
    >
      <div class="tab-icon">
        <van-icon :name="isActive(item.path) ? item.activeIcon || item.icon : item.icon" size="22" />
      </div>
      <div class="tab-label">{{ item.label }}</div>
      <div v-if="isActive(item.path)" class="tab-dot" aria-hidden="true"></div>
    </div>
  </nav>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'

interface TabItem { path: string; label: string; icon: string; activeIcon?: string }
interface Props { items: TabItem[] }
defineProps<Props>()

const route = useRoute()
const router = useRouter()

function isActive(p: string) {
  return route.path === p || route.path.startsWith(p + '/')
}
function go(p: string) { router.push(p) }
</script>

<style scoped>
.x-tabbar {
  position: fixed; left: 0; right: 0; bottom: 0;
  max-width: 480px;
  margin: 0 auto;
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: saturate(160%) blur(12px);
  -webkit-backdrop-filter: saturate(160%) blur(12px);
  border-top: 1px dashed var(--line-2);
  display: flex;
  padding: 8px 0 6px;
  z-index: 100;
}
.tab-item {
  flex: 1;
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  gap: 3px;
  padding: 4px 0 2px;
  color: var(--muted);
  cursor: pointer;
  transition: color var(--dur) var(--ease-out);
  position: relative;
}
.tab-item.active { color: var(--brand-deep); }
.tab-icon { line-height: 1; display: flex; }
.tab-label {
  font-family: var(--font-serif);
  font-size: 10.5px; letter-spacing: 0.08em;
  font-weight: 500;
}
.tab-dot {
  position: absolute; top: 4px; left: 50%;
  width: 4px; height: 4px;
  border-radius: 50%;
  background: var(--brand);
  transform: translateX(calc(-50% + 16px));
  opacity: 0.7;
}
</style>
