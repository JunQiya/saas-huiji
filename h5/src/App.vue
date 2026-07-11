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
    <keep-alive :include="['Home', 'Profile', 'MallOrders', 'Dining']">
      <component :is="Component" />
    </keep-alive>
  </router-view>
</template>
