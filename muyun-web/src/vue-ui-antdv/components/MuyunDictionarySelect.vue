<script setup lang="ts">
import MuyunSelect from './MuyunSelect.vue';
import { resolveDictionaryOptions } from '../dictionaries';
import type { MuyunOptionValue } from '@muyun/web-contracts';

defineOptions({ name: 'MuyunDictionarySelect' });

defineProps<{
  value?: MuyunOptionValue | null;
  dictionaryAlias?: string;
  placeholder?: string;
  disabled?: boolean;
}>();

const emit = defineEmits<{
  'update:value': [value: MuyunOptionValue | null];
}>();
</script>

<template>
  <MuyunSelect
    :value="value"
    :options="resolveDictionaryOptions(dictionaryAlias ?? '')"
    :placeholder="placeholder"
    :disabled="disabled"
    @update:value="emit('update:value', $event)"
  />
</template>
