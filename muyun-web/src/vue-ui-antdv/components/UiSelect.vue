<script setup lang="ts">
import { Select as ASelect } from 'ant-design-vue';
import type { Option, OptionValue } from '@muyun/web-contracts';

defineOptions({ name: 'UiSelect' });

withDefaults(
  defineProps<{
    value?: OptionValue | null;
    options: Option[];
    placeholder?: string;
    disabled?: boolean;
    allowClear?: boolean;
  }>(),
  {
    value: undefined,
    placeholder: undefined,
    disabled: false,
    allowClear: true,
  },
);

const emit = defineEmits<{
  'update:value': [value: OptionValue | null];
}>();

function normalize(value: unknown) {
  emit('update:value', typeof value === 'string' || typeof value === 'number' ? value : null);
}
</script>

<template>
  <ASelect
    :allow-clear="allowClear"
    :value="value ?? undefined"
    :options="options"
    :placeholder="placeholder"
    :disabled="disabled"
    @update:value="normalize"
  />
</template>
