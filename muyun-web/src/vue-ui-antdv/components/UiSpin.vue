<script setup lang="ts">
import { Spin as ASpin } from 'ant-design-vue';

defineOptions({ name: 'UiSpin', inheritAttrs: false });

withDefaults(
  defineProps<{
    spinning?: boolean;
    tip?: string;
    size?: 'small' | 'default' | 'large';
  }>(),
  {
    spinning: true,
    size: 'default',
    tip: undefined,
  },
);
</script>

<template>
  <ASpin :spinning="spinning" :tip="tip" :size="size" :class="$attrs.class" :style="$attrs.style">
    <slot />
  </ASpin>
</template>
