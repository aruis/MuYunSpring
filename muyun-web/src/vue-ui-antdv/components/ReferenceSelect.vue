<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { Select as ASelect } from 'ant-design-vue';
import { searchReferenceRecords } from '../references';
import type { OptionValue, Primitive, ReferenceContract } from '@muyun/web-contracts';

defineOptions({ name: 'ReferenceSelect' });

const props = defineProps<{
  value?: OptionValue | null;
  reference?: ReferenceContract;
  placeholder?: string;
  disabled?: boolean;
}>();

const emit = defineEmits<{
  'update:value': [value: OptionValue | null];
  'fill-back': [patch: Record<string, Primitive>];
}>();

const loading = ref(false);
const records = ref<Awaited<ReturnType<typeof searchReferenceRecords>>>([]);

const options = computed(() =>
  records.value.map((record) => ({
    label: `${record.title}${record.subtitle ? ` · ${record.subtitle}` : ''}`,
    value: record.key,
  })),
);

async function refresh(keyword = '') {
  if (!props.reference) {
    records.value = [];
    return;
  }
  loading.value = true;
  try {
    records.value = await searchReferenceRecords(props.reference.targetModuleAlias, keyword);
  } finally {
    loading.value = false;
  }
}

function handleUpdate(value: unknown) {
  if (typeof value !== 'string' && typeof value !== 'number') {
    emit('update:value', null);
    return;
  }

  emit('update:value', value);
  const selected = records.value.find((record) => record.key === value);
  if (!selected) {
    return;
  }

  const patch: Record<string, Primitive> = {};
  Object.entries(props.reference?.fillBack ?? {}).forEach(([targetField, sourceField]) => {
    patch[targetField] = selected.fields[sourceField];
  });
  emit('fill-back', patch);
}

watch(
  () => props.reference?.targetModuleAlias,
  () => void refresh(),
  { immediate: true },
);
</script>

<template>
  <ASelect
    show-search
    allow-clear
    :filter-option="false"
    :value="value ?? undefined"
    :options="options"
    :loading="loading"
    :placeholder="placeholder"
    :disabled="disabled"
    @search="refresh"
    @update:value="handleUpdate"
  />
</template>
