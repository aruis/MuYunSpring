<script setup lang="ts">
import { Alert as AAlert } from 'ant-design-vue';

defineOptions({ name: 'UiError', inheritAttrs: false });

defineProps<{
  title?: string;
  message: string;
}>();
</script>

<template>
  <AAlert
    type="error"
    show-icon
    :message="title ?? '加载失败'"
    :description="message"
    :class="$attrs.class"
    :style="$attrs.style"
  />
</template>
