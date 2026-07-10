<template>
  <component :is="tag" class="chip" :class="cls">
    <slot />
  </component>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props { type?: 'default' | 'brand' | 'rose' | 'success' | 'warning' | 'danger' | 'mist'; tag?: string }
const props = withDefaults(defineProps<Props>(), { type: 'default', tag: 'span' })
const cls = computed(() => props.type !== 'default' ? props.type : '')
</script>
