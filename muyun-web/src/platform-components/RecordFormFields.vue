<script setup lang="ts">
import { computed } from 'vue';
import { UiInput } from '@muyun/vue-ui-antdv';
import type { ResolvedViewFieldDescriptor, ViewFieldDefinition } from '@muyun/web-contracts';
import type { ModuleContext } from '@muyun/web-core';
import RecordStatusSwitch from './RecordStatusSwitch.vue';
import RecordPicker from './RecordPicker.vue';
import type { PickerConstraint, RecordPickerRecord } from './recordPickerConstraints';

defineOptions({ name: 'RecordFormFields' });

export type RecordFormFieldDescriptor = ViewFieldDefinition | ResolvedViewFieldDescriptor;
export type RecordFormRecord = Record<string, unknown>;

export interface RecordFormFieldFallback {
  label: string;
  required?: boolean;
  readOnly?: boolean;
  visible?: boolean;
  placeholder?: string;
}

export interface RecordFormFieldPickerConfig {
  context: ModuleContext<RecordPickerRecord>;
  reloadKey?: number;
  mode?: 'list' | 'tree';
  placeholder?: string;
  allowClear?: boolean;
  constraints?: PickerConstraint<RecordPickerRecord>[];
  titleOf?: (record: RecordPickerRecord) => string;
  descriptionOf?: (record: RecordPickerRecord) => string | undefined;
  filterOption?: (record: RecordPickerRecord, keyword: string) => boolean;
}

const props = withDefaults(
  defineProps<{
    record: RecordFormRecord;
    fieldNames: string[];
    fields?: Map<string, RecordFormFieldDescriptor>;
    fallback?: Record<string, RecordFormFieldFallback>;
    pickerConfigs?: Record<string, RecordFormFieldPickerConfig>;
    disabled?: boolean;
    placeholderOf?: (fieldName: string, field: RecordFormFieldState) => string | undefined;
  }>(),
  {
    fields: undefined,
    fallback: () => ({}),
    pickerConfigs: () => ({}),
    disabled: false,
    placeholderOf: undefined,
  },
);

const emit = defineEmits<{
  'update:field': [fieldName: string, value: string | boolean | undefined];
}>();

export interface RecordFormFieldState {
  fieldName: string;
  label: string;
  required: boolean;
  readOnly: boolean;
  visible: boolean;
  controlType: 'input' | 'enabledStatus' | 'recordPicker';
  pickerConfig?: RecordFormFieldPickerConfig;
  placeholder?: string;
}

const fieldStates = computed<RecordFormFieldState[]>(() =>
  props.fieldNames.map(fieldState).filter((field) => field.visible),
);

function fieldState(fieldName: string): RecordFormFieldState {
  const field = props.fields?.get(fieldName);
  const fallback = props.fallback?.[fieldName];
  const label = field?.label ?? fallback?.label ?? fieldName;
  const required = field?.required?.constant ?? fallback?.required ?? false;
  const readOnly = field?.readOnly?.constant ?? fallback?.readOnly ?? false;
  const visible = field?.visible?.constant ?? fallback?.visible ?? true;
  const controlType = controlTypeOf(field);
  const pickerConfig = controlType === 'recordPicker' ? props.pickerConfigs?.[fieldName] : undefined;
  return {
    fieldName,
    label,
    required,
    readOnly,
    visible,
    controlType,
    pickerConfig,
    placeholder: props.placeholderOf?.(fieldName, { fieldName, label, required, readOnly, visible, controlType })
      ?? fallback?.placeholder
      ?? pickerConfig?.placeholder,
  };
}

function controlTypeOf(field: RecordFormFieldDescriptor | undefined): RecordFormFieldState['controlType'] {
  if (field?.uiType === 'enabledStatus') {
    return 'enabledStatus';
  }
  if (field?.uiType === 'recordPicker') {
    return 'recordPicker';
  }
  return 'input';
}

function fieldValue(fieldName: string) {
  const value = props.record[fieldName];
  return value === undefined || value === null ? undefined : String(value);
}

function booleanFieldValue(fieldName: string) {
  return props.record[fieldName] !== false;
}

function updateField(fieldName: string, value: string | boolean | undefined) {
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
      :disabled="disabled || field.readOnly"
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
      :disabled="disabled || field.readOnly"
      :allow-clear="field.pickerConfig.allowClear"
      :constraints="field.pickerConfig.constraints"
      :title-of="field.pickerConfig.titleOf"
      :description-of="field.pickerConfig.descriptionOf"
      :filter-option="field.pickerConfig.filterOption"
      @update:value="updateField(field.fieldName, $event)"
    />
    <UiInput
      v-else
      :value="fieldValue(field.fieldName)"
      :disabled="disabled || field.readOnly"
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
