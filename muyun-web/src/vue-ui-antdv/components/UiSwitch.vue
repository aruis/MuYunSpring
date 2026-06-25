<script setup lang="ts">
import { Switch as ASwitch } from 'ant-design-vue';

defineOptions({ name: 'UiSwitch' });

withDefaults(
  defineProps<{
    checked?: boolean;
    disabled?: boolean;
    loading?: boolean;
    checkedText?: string;
    uncheckedText?: string;
  }>(),
  {
    checked: false,
    disabled: false,
    loading: false,
    checkedText: undefined,
    uncheckedText: undefined,
  },
);

const emit = defineEmits<{
  'update:checked': [checked: boolean];
  change: [checked: boolean];
}>();

function handleChange(checked: unknown) {
  const normalized = checked === true;
  emit('update:checked', normalized);
  emit('change', normalized);
}
</script>

<template>
  <ASwitch :checked="checked" :disabled="disabled" :loading="loading" @change="handleChange">
    <template v-if="checkedText" #checkedChildren>{{ checkedText }}</template>
    <template v-if="uncheckedText" #unCheckedChildren>{{ uncheckedText }}</template>
  </ASwitch>
</template>
