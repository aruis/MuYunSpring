<script setup lang="ts">
import UiSelect from './UiSelect.vue';
import { resolveDictionaryOptions } from '../dictionaries';
import type { OptionValue } from '@muyun/web-contracts';

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
</script>

<template>
  <UiSelect
    :value="value"
    :options="resolveDictionaryOptions(dictionaryAlias ?? '')"
    :placeholder="placeholder"
    :disabled="disabled"
    @update:value="emit('update:value', $event)"
  />
</template>
