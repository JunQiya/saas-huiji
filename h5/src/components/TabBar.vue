<template>
  <nav class="x-tabbar safe-bottom">
    <div
      v-for="(item, i) in items"
      :key="item.path"
      class="tab-item"
      :class="{ active: isActive(item.path) }"
      @click="go(item.path)"
    >
      <div class="tab-icon">
        <van-icon :name="isActive(item.path) ? item.activeIcon || item.icon : item.icon" size="22" />
      </div>
      <div class="tab-label">{{ item.label }}</div>
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
  border-top: 1px solid var(--line);
  display: flex;
  padding: 6px 0 8px;
  z-index: 100;
}
.tab-item {
  flex: 1;
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  gap: 2px;
  padding: 4px 0;
  color: var(--muted);
  cursor: pointer;
  transition: color var(--dur) var(--ease);
  position: relative;
}
.tab-item.active { color: var(--brand-deep); }
.tab-item.active::before {
  content: '';
  position: absolute; top: 0; left: 50%;
  width: 18px; height: 2px;
  background: var(--brand);
  border-radius: 1px;
  transform: translateX(-50%);
}
.tab-icon { line-height: 1; display: flex; }
.tab-label { font-size: 10.5px; letter-spacing: 0.04em; }
</style>
