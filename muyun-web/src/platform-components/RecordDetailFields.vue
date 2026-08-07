<script setup lang="ts">
import { computed } from 'vue';
import { UiSwitch } from '@muyun/vue-ui-antdv';
import RecordStatusTag from './RecordStatusTag.vue';
import {
  resolveRecordFormFieldNames,
  resolveRecordFormFieldState,
  type RecordFormFieldDescriptor,
  type RecordFormFieldFallback,
  type RecordFormFieldPickerConfig,
  type RecordFormFieldState,
  type RecordFormRecord,
} from './recordFormFieldModel';
import { resolveRecordDetailDisplayValue, type RecordDetailDisplayResolver } from './recordDetailFieldModel';

defineOptions({ name: 'RecordDetailFields' });

const props = withDefaults(
  defineProps<{
    record: RecordFormRecord;
    fieldNames?: string[];
    excludeFieldNames?: string[];
    fields?: Map<string, RecordFormFieldDescriptor>;
    fallback?: Record<string, RecordFormFieldFallback>;
    pickerConfigs?: Record<string, RecordFormFieldPickerConfig>;
    displayOf?: RecordDetailDisplayResolver;
    emptyText?: string;
  }>(),
  {
    fieldNames: undefined,
    fields: undefined,
    excludeFieldNames: () => [],
    fallback: () => ({}),
    pickerConfigs: () => ({}),
    displayOf: undefined,
    emptyText: '-',
  },
);

const resolvedFieldNames = computed(
  () =>
    props.fieldNames ??
    resolveRecordFormFieldNames(props.fields, props.fallback, { exclude: props.excludeFieldNames }),
);

const fieldStates = computed<RecordFormFieldState[]>(() =>
  resolvedFieldNames.value.map(fieldState).filter((field) => field.visible),
);

function fieldState(fieldName: string): RecordFormFieldState {
  return resolveRecordFormFieldState(fieldName, {
    fields: props.fields,
    fallback: props.fallback,
    pickerConfigs: props.pickerConfigs,
  });
}

function booleanFieldValue(fieldName: string) {
  return props.record[fieldName] !== false;
}

function displayValue(field: RecordFormFieldState) {
  return resolveRecordDetailDisplayValue(field, props.record, {
    displayOf: props.displayOf,
    emptyText: props.emptyText,
  });
}
</script>

<template>
  <dl class="record-detail-fields">
    <div v-for="field in fieldStates" :key="field.fieldName" class="record-detail-field">
      <dt>{{ field.label }}</dt>
      <dd>
        <RecordStatusTag
          v-if="field.controlType === 'enabledStatus'"
          :enabled="booleanFieldValue(field.fieldName)"
        />
        <UiSwitch
          v-else-if="field.controlType === 'switch'"
          :checked="booleanFieldValue(field.fieldName)"
          disabled
        />
        <span v-else>{{ displayValue(field) }}</span>
      </dd>
    </div>
  </dl>
</template>

<style scoped>
.record-detail-fields {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 18px;
  margin: 0;
}

.record-detail-field {
  min-width: 0;
}

dt {
  color: #64748b;
  font-size: 12px;
}

dd {
  overflow-wrap: anywhere;
  margin: 4px 0 0;
  color: #243447;
  font-size: 13px;
  line-height: 20px;
}

@media (max-width: 900px) {
  .record-detail-fields {
    grid-template-columns: 1fr;
  }
}
</style>
