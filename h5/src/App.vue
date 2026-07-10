<script setup lang="ts">
// 根组件：仅承载路由出口
</script>

<template>
  <router-view v-slot="{ Component }">
    <keep-alive :include="['Home', 'Profile']">
      <component :is="Component" />
    </keep-alive>
  </router-view>
</template>
