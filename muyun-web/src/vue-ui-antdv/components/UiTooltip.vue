<script setup lang="ts">
import { Tooltip as ATooltip } from 'ant-design-vue';

defineOptions({ name: 'UiTooltip', inheritAttrs: false });

withDefaults(
  defineProps<{
    title: string;
    placement?:
      | 'top'
      | 'topLeft'
      | 'topRight'
      | 'right'
      | 'rightTop'
      | 'rightBottom'
      | 'bottom'
      | 'bottomLeft'
      | 'bottomRight'
      | 'left'
      | 'leftTop'
      | 'leftBottom';
  }>(),
  { placement: 'top' },
);
</script>

<template>
  <ATooltip :title="title" :placement="placement" :class="$attrs.class" :style="$attrs.style">
    <slot />
  </ATooltip>
</template>
