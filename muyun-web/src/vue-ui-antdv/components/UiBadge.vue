<script setup lang="ts">
import { Badge as ABadge } from 'ant-design-vue';

defineOptions({ name: 'UiBadge', inheritAttrs: false });

withDefaults(
  defineProps<{
    count?: number;
    overflowCount?: number;
  }>(),
  {
    count: 0,
    overflowCount: 99,
  },
);
</script>

<template>
  <ABadge :count="count" :overflow-count="overflowCount" :class="$attrs.class" :style="$attrs.style">
    <slot />
  </ABadge>
</template>
