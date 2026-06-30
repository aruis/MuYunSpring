<script setup lang="ts">
import { computed } from 'vue';
import { UiInput, UiSelect } from '@muyun/vue-ui-antdv';
import RecordStatusSwitch from './RecordStatusSwitch.vue';
import RecordPicker from './RecordPicker.vue';
import {
  resolveRecordFormFieldNames,
  resolveRecordFormFieldState,
  type RecordFormFieldDescriptor,
  type RecordFormFieldFallback,
  type RecordFormFieldPickerConfig,
  type RecordFormFieldState,
  type RecordFormRecord,
} from './recordFormFieldModel';

defineOptions({ name: 'RecordFormFields' });

const props = withDefaults(
  defineProps<{
    record: RecordFormRecord;
    fieldNames?: string[];
    excludeFieldNames?: string[];
    fields?: Map<string, RecordFormFieldDescriptor>;
    fallback?: Record<string, RecordFormFieldFallback>;
    pickerConfigs?: Record<string, RecordFormFieldPickerConfig>;
    disabled?: boolean;
    disabledOf?: (fieldName: string, field: RecordFormFieldState) => boolean;
    placeholderOf?: (fieldName: string, field: RecordFormFieldState) => string | undefined;
  }>(),
  {
    fieldNames: undefined,
    fields: undefined,
    excludeFieldNames: () => [],
    fallback: () => ({}),
    pickerConfigs: () => ({}),
    disabled: false,
    disabledOf: undefined,
    placeholderOf: undefined,
  },
);

const emit = defineEmits<{
  'update:field': [fieldName: string, value: string | number | boolean | undefined];
}>();

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
    placeholderOf: props.placeholderOf,
  });
}

function fieldValue(fieldName: string) {
  const value = props.record[fieldName];
  return value === undefined || value === null ? undefined : String(value);
}

function booleanFieldValue(fieldName: string) {
  return props.record[fieldName] !== false;
}

function fieldDisabled(field: RecordFormFieldState) {
  return props.disabled || field.readOnly || props.disabledOf?.(field.fieldName, field) === true;
}

function updateField(fieldName: string, value: string | number | boolean | undefined) {
  emit('update:field', fieldName, value);
}
</script>

<template>
  <label v-for="field in fieldStates" :key="field.fieldName" class="record-form-field">
    <span class="record-form-field-label">
      {{ field.label }}
      <strong v-if="field.required" aria-hidden="true">*</strong>
    </span>
    <RecordStatusSwitch
      v-if="field.controlType === 'enabledStatus'"
      :enabled="booleanFieldValue(field.fieldName)"
      :disabled="fieldDisabled(field)"
      :show-label="false"
      @change="updateField(field.fieldName, $event)"
    />
    <RecordPicker
      v-else-if="field.controlType === 'recordPicker' && field.pickerConfig"
      :value="fieldValue(field.fieldName)"
      :context="field.pickerConfig.context"
      :reload-key="field.pickerConfig.reloadKey"
      :mode="field.pickerConfig.mode"
      :placeholder="field.placeholder"
      :disabled="fieldDisabled(field)"
      :allow-clear="field.pickerConfig.allowClear"
      :constraints="field.pickerConfig.constraints"
      :title-of="field.pickerConfig.titleOf"
      :description-of="field.pickerConfig.descriptionOf"
      :filter-option="field.pickerConfig.filterOption"
      @update:value="updateField(field.fieldName, $event)"
    />
    <UiSelect
      v-else-if="field.controlType === 'select' && field.options"
      :value="fieldValue(field.fieldName)"
      :options="field.options"
      :placeholder="field.placeholder"
      :disabled="fieldDisabled(field)"
      :allow-clear="!field.required"
      @update:value="updateField(field.fieldName, $event ?? undefined)"
    />
    <UiInput
      v-else
      :value="fieldValue(field.fieldName)"
      :disabled="fieldDisabled(field)"
      :placeholder="field.placeholder"
      @update:value="updateField(field.fieldName, $event)"
    />
  </label>
</template>

<style scoped>
.record-form-field {
  display: grid;
  gap: 6px;
  color: var(--muyun-text-muted);
  font-size: 13px;
}

.record-form-field-label {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.record-form-field-label strong {
  color: #d92d20;
  font-weight: 600;
}
</style>
