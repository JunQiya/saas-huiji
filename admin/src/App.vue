<template>
  <el-config-provider size="default" :z-index="3000" :locale="zhCn">
    <router-view />
  </el-config-provider>
</template>

<script setup lang="ts">
// 应用根组件：统一 Element Plus 全局配置（按需引入由 vite unplugin 处理）
import zhCn from 'element-plus/es/locale/lang/zh-cn'
</script>
