<script setup lang="ts">
import UiSelect from './UiSelect.vue';
import { resolveDictionaryOptions } from '../dictionaries';
import type { OptionValue, OptionValueList } from '@muyun/web-contracts';

defineOptions({ name: 'DictionarySelect' });

defineProps<{
  value?: OptionValue | null;
  dictionaryAlias?: string;
  placeholder?: string;
  disabled?: boolean;
}>();

const emit = defineEmits<{
  'update:value': [value: OptionValue | null];
}>();

function handleUpdate(value: OptionValue | OptionValueList | null) {
  emit('update:value', Array.isArray(value) ? null : value);
}
</script>

<template>
  <UiSelect
    :value="value"
    :options="resolveDictionaryOptions(dictionaryAlias ?? '')"
    :placeholder="placeholder"
    :disabled="disabled"
    @update:value="handleUpdate"
  />
</template>
