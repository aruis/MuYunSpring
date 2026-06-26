<script setup lang="ts">
import { Input as AInput } from 'ant-design-vue';
import type { Primitive } from '@muyun/web-contracts';

defineOptions({ name: 'UiInput' });

defineProps<{
  value?: Primitive;
  placeholder?: string;
  disabled?: boolean;
  autofocus?: boolean;
}>();

const emit = defineEmits<{
  'update:value': [value: string];
  blur: [event: FocusEvent];
  focus: [event: FocusEvent];
}>();
</script>

<template>
  <AInput
    :value="String(value ?? '')"
    :placeholder="placeholder"
    :disabled="disabled"
    :autofocus="autofocus"
    @blur="emit('blur', $event)"
    @focus="emit('focus', $event)"
    @update:value="emit('update:value', $event)"
  />
</template>
