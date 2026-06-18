<script setup lang="ts">
import { Input as AInput } from 'ant-design-vue';
import type { MuyunPrimitive } from '@muyun/web-contracts';

defineOptions({ name: 'MuyunInput' });

defineProps<{
  value?: MuyunPrimitive;
  placeholder?: string;
  disabled?: boolean;
}>();

const emit = defineEmits<{
  'update:value': [value: string];
}>();
</script>

<template>
  <AInput
    :value="String(value ?? '')"
    :placeholder="placeholder"
    :disabled="disabled"
    @update:value="emit('update:value', $event)"
  />
</template>
