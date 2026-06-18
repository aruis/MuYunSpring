<script setup lang="ts">
import { Select as ASelect } from 'ant-design-vue';
import type { MuyunOption, MuyunOptionValue } from '@muyun/web-contracts';

defineOptions({ name: 'MuyunSelect' });

defineProps<{
  value?: MuyunOptionValue | null;
  options: MuyunOption[];
  placeholder?: string;
  disabled?: boolean;
}>();

const emit = defineEmits<{
  'update:value': [value: MuyunOptionValue | null];
}>();

function normalize(value: unknown) {
  emit('update:value', typeof value === 'string' || typeof value === 'number' ? value : null);
}
</script>

<template>
  <ASelect
    allow-clear
    :value="value ?? undefined"
    :options="options"
    :placeholder="placeholder"
    :disabled="disabled"
    @update:value="normalize"
  />
</template>
