<script setup lang="ts">
import { Empty as AEmpty } from 'ant-design-vue';

defineOptions({ name: 'UiEmpty' });

defineProps<{
  description?: string;
}>();
</script>

<template>
  <AEmpty :description="description" />
</template>
