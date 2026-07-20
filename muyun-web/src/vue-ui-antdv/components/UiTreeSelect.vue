<script setup lang="ts">
import { TreeSelect as ATreeSelect } from 'ant-design-vue';
import type { OptionValue, OptionValueList } from '@muyun/web-contracts';

defineOptions({ name: 'UiTreeSelect', inheritAttrs: false });

export interface UiTreeSelectNode {
  value: OptionValue;
  title: string;
  disabled?: boolean;
  children?: UiTreeSelectNode[];
}

withDefaults(
  defineProps<{
    value?: OptionValue | OptionValueList | null;
    treeData: UiTreeSelectNode[];
    mode?: 'multiple';
    placeholder?: string;
    disabled?: boolean;
    allowClear?: boolean;
    loading?: boolean;
  }>(),
  {
    value: undefined,
    mode: undefined,
    placeholder: undefined,
    disabled: false,
    allowClear: true,
    loading: false,
  },
);

const emit = defineEmits<{
  'update:value': [value: OptionValue | OptionValueList | null];
}>();

function normalize(value: unknown) {
  if (Array.isArray(value)) {
    emit(
      'update:value',
      value.filter((item): item is OptionValue => typeof item === 'string' || typeof item === 'number'),
    );
    return;
  }
  emit('update:value', typeof value === 'string' || typeof value === 'number' ? value : null);
}
</script>

<template>
  <ATreeSelect
    :value="value ?? undefined"
    :tree-data="treeData"
    :multiple="mode === 'multiple'"
    :allow-clear="allowClear"
    :placeholder="placeholder"
    :disabled="disabled"
    :loading="loading"
    tree-default-expand-all
    :class="$attrs.class"
    :style="$attrs.style"
    @update:value="normalize"
  />
</template>
