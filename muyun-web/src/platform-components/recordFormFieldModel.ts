import type { Option, ResolvedViewFieldDescriptor, ViewFieldDefinition } from '@muyun/web-contracts';
import type { ModuleContext } from '@muyun/web-core';
import type { PickerConstraint, RecordPickerRecord } from './recordPickerConstraints';

export type RecordFormFieldDescriptor = ViewFieldDefinition | ResolvedViewFieldDescriptor;
export type RecordFormRecord = Record<string, unknown>;
export type RecordFormFieldControlType = 'input' | 'select' | 'enabledStatus' | 'recordPicker';

export interface RecordFormFieldFallback {
  label: string;
  required?: boolean;
  readOnly?: boolean;
  visible?: boolean;
  controlType?: RecordFormFieldControlType;
  placeholder?: string;
  options?: Option[];
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

export interface RecordFormFieldState {
  fieldName: string;
  label: string;
  required: boolean;
  readOnly: boolean;
  visible: boolean;
  controlType: RecordFormFieldControlType;
  pickerConfig?: RecordFormFieldPickerConfig;
  placeholder?: string;
  options?: Option[];
}

export interface ResolveRecordFormFieldNamesOptions {
  explicitOrder?: string[];
  exclude?: Iterable<string>;
}

export function resolveRecordFormFieldNames(
  fields?: Map<string, RecordFormFieldDescriptor>,
  fallback: Record<string, RecordFormFieldFallback> = {},
  options: ResolveRecordFormFieldNamesOptions = {},
): string[] {
  const excluded = new Set(options.exclude ?? []);
  const names: string[] = [];
  const seen = new Set<string>();
  const append = (fieldName: string) => {
    if (!fieldName || excluded.has(fieldName) || seen.has(fieldName)) {
      return;
    }
    seen.add(fieldName);
    names.push(fieldName);
  };

  options.explicitOrder?.forEach(append);
  fields?.forEach((_, fieldName) => append(fieldName));
  Object.keys(fallback).forEach(append);
  return names;
}

export function resolveRecordFormFieldState(
  fieldName: string,
  options: {
    fields?: Map<string, RecordFormFieldDescriptor>;
    fallback?: Record<string, RecordFormFieldFallback>;
    pickerConfigs?: Record<string, RecordFormFieldPickerConfig>;
    placeholderOf?: (fieldName: string, field: RecordFormFieldState) => string | undefined;
  } = {},
): RecordFormFieldState {
  const field = options.fields?.get(fieldName);
  const fallback = options.fallback?.[fieldName];
  const label = field?.label ?? fallback?.label ?? fieldName;
  const required = field?.required?.constant ?? fallback?.required ?? false;
  const readOnly = field?.readOnly?.constant ?? fallback?.readOnly ?? false;
  const visible = field?.visible?.constant ?? fallback?.visible ?? true;
  const controlType = controlTypeOf(field, fallback);
  const pickerConfig = controlType === 'recordPicker' ? options.pickerConfigs?.[fieldName] : undefined;
  const baseState: RecordFormFieldState = {
    fieldName,
    label,
    required,
    readOnly,
    visible,
    controlType,
    pickerConfig,
  };
  return {
    ...baseState,
    ...(fallback?.options ? { options: fallback.options } : {}),
    placeholder:
      options.placeholderOf?.(fieldName, baseState) ?? fallback?.placeholder ?? pickerConfig?.placeholder,
  };
}

function controlTypeOf(
  field: RecordFormFieldDescriptor | undefined,
  fallback: RecordFormFieldFallback | undefined,
): RecordFormFieldControlType {
  if (field?.uiType === 'enabledStatus') {
    return 'enabledStatus';
  }
  if (field?.uiType === 'recordPicker') {
    return 'recordPicker';
  }
  if (field?.uiType === 'select') {
    return 'select';
  }
  return fallback?.controlType ?? 'input';
}
