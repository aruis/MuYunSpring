<script setup lang="ts">
import { Input as AInput } from 'ant-design-vue';
import type { Primitive } from '@muyun/web-contracts';

defineOptions({ name: 'UiInput' });

defineProps<{
  value?: Primitive;
  placeholder?: string;
  disabled?: boolean;
  autofocus?: boolean;
  allowClear?: boolean;
}>();

const emit = defineEmits<{
  'update:value': [value: string];
  blur: [event: FocusEvent];
  focus: [event: FocusEvent];
  keydown: [event: KeyboardEvent];
}>();
</script>

<template>
  <AInput
    :value="String(value ?? '')"
    :placeholder="placeholder"
    :disabled="disabled"
    :autofocus="autofocus"
    :allow-clear="allowClear"
    @blur="emit('blur', $event)"
    @focus="emit('focus', $event)"
    @keydown="emit('keydown', $event)"
    @update:value="emit('update:value', $event)"
  />
</template>
