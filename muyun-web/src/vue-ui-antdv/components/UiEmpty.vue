<script setup lang="ts">
import { Empty as AEmpty } from 'ant-design-vue';

defineOptions({ name: 'UiEmpty', inheritAttrs: false });

defineProps<{
  description?: string;
}>();
</script>

<template>
  <AEmpty :description="description" :class="$attrs.class" :style="$attrs.style" />
</template>
