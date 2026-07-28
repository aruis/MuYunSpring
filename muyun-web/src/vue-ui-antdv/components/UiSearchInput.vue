<script setup lang="ts">
import { InputSearch as AInputSearch } from 'ant-design-vue';

defineOptions({ name: 'UiSearchInput', inheritAttrs: false });

withDefaults(
  defineProps<{
    value?: string;
    placeholder?: string;
    disabled?: boolean;
    loading?: boolean;
    searchText?: string;
  }>(),
  {
    value: '',
    placeholder: undefined,
    disabled: false,
    loading: false,
    searchText: undefined,
  },
);

const emit = defineEmits<{
  'update:value': [value: string];
  search: [value: string];
}>();

function handleKeydown(event: KeyboardEvent) {
  if (event.key !== 'Escape') return;
  event.preventDefault();
  emit('update:value', '');
  emit('search', '');
}
</script>

<template>
  <AInputSearch
    :value="value"
    :placeholder="placeholder"
    :disabled="disabled"
    :loading="loading"
    :enter-button="searchText ?? false"
    allow-clear
    :class="$attrs.class"
    :style="$attrs.style"
    @update:value="emit('update:value', $event)"
    @search="emit('search', $event)"
    @keydown="handleKeydown"
  />
</template>
