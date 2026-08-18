<script setup lang="ts">
import { onMounted } from 'vue'

onMounted(() => {
  if (localStorage.getItem('theme') === 'dark') {
    document.documentElement.classList.add('dark')
    document.querySelector('meta[name="theme-color"]')?.setAttribute('content', '#16151a')
  }
})
</script>

<template>
  <router-view v-slot="{ Component }">
    <transition name="page-fade" mode="out-in">
      <keep-alive :include="['Home', 'Profile', 'MallOrders', 'Dining']">
        <component :is="Component" />
      </keep-alive>
    </transition>
  </router-view>
</template>

<style>
.page-fade-enter-active,
.page-fade-leave-active {
  transition: opacity var(--dur) var(--ease-out);
}
.page-fade-enter-from,
.page-fade-leave-to {
  opacity: 0;
}
</style>
