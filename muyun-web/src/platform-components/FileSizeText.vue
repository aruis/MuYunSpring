<script setup lang="ts">
import { computed } from 'vue';
import { formatPlatformFileSize } from './platformFileSize';

defineOptions({ name: 'FileSizeText' });

const props = withDefaults(
  defineProps<{
    value?: number | string | bigint | null;
    emptyText?: string;
  }>(),
  {
    value: undefined,
    emptyText: '-',
  },
);

const display = computed(() => formatPlatformFileSize(props.value, { emptyText: props.emptyText }));
</script>

<template>
  <span :title="display.title">{{ display.text }}</span>
</template>
