import type {
  Option,
  OptionValueList,
  ResolvedReferenceFieldDescriptor,
  BooleanStatusPresentation,
  ResolvedOptionFieldDescriptor,
  ResolvedModuleUiDescriptor,
  ResolvedViewFieldDescriptor,
  ViewFieldDefinition,
} from '@muyun/web-contracts';
import type { ModuleContext } from '@muyun/web-core';
import type { PickerConstraint, RecordPickerRecord } from './recordPickerConstraints';

export type RecordFormFieldDescriptor = (ViewFieldDefinition | ResolvedViewFieldDescriptor) & {
  option?: ResolvedOptionFieldDescriptor;
  reference?: ResolvedReferenceFieldDescriptor;
};
export type RecordFormRecord = Record<string, unknown>;
export type RecordFormFieldValue = string | number | boolean | OptionValueList | string[] | undefined;
/** A business boolean does not inherit the lifecycle field's implicit enabled default. */
export type RecordBooleanStatusValue = boolean | undefined;
export type RecordFormFieldControlType =
  | 'input'
  | 'textarea'
  | 'colorPicker'
  | 'select'
  | 'enabledStatus'
  | 'booleanStatus'
  | 'switch'
  | 'recordPicker'
  | 'recordMultiPicker';

export interface RecordFormFieldFallback {
  label: string;
  required?: boolean;
  readOnly?: boolean;
  visible?: boolean;
  controlType?: RecordFormFieldControlType;
  placeholder?: string;
  options?: Option[];
  optionSelectionMode?: 'SINGLE' | 'MULTIPLE';
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
  columnSpan: number;
  hasOption: boolean;
  optionSelectionMode?: 'SINGLE' | 'MULTIPLE';
  optionTitleField?: string;
  pickerConfig?: RecordFormFieldPickerConfig;
  booleanStatus?: BooleanStatusPresentation;
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

export function resolveRecordFormFields(
  uiDescriptor: ResolvedModuleUiDescriptor | undefined,
  viewCode = 'default_form',
): Map<string, RecordFormFieldDescriptor> {
  const formView = uiDescriptor?.views?.find(
    (view) => view.viewKind === 'FORM' && view.viewCode === viewCode,
  );
  return new Map(formView?.fields.map((field) => [field.fieldRef.fieldName, field]) ?? []);
}

export function childResourceDefaultFormViewCode(resource: string): string {
  if (!/^[a-z][a-z0-9_]{0,62}$/.test(resource)) {
    throw new Error(`invalid child resource code: ${resource}`);
  }
  return `${resource}_default_form`;
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
  const booleanStatus = controlType === 'booleanStatus' ? field?.booleanStatus : undefined;
  const hasOption = field?.option != null;
  const pickerConfig =
    controlType === 'recordPicker' || controlType === 'recordMultiPicker'
      ? options.pickerConfigs?.[fieldName]
      : undefined;
  const baseState: RecordFormFieldState = {
    fieldName,
    label,
    required,
    readOnly,
    visible,
    controlType,
    columnSpan: field?.columnSpan === 2 ? 2 : 1,
    hasOption,
    pickerConfig,
    ...(booleanStatus ? { booleanStatus } : {}),
  };
  return {
    ...baseState,
    ...(field?.option
      ? {
          optionSelectionMode: field.option.selectionMode,
          ...(field.option.titleField ? { optionTitleField: field.option.titleField } : {}),
        }
      : {}),
    ...(fallback?.options ? { options: fallback.options } : {}),
    placeholder:
      options.placeholderOf?.(fieldName, baseState) ?? fallback?.placeholder ?? pickerConfig?.placeholder,
  };
}

/**
 * Preserves an absent business value as unknown instead of treating it as true.
 * `enabledStatus` deliberately retains its separate lifecycle default semantics.
 */
export function resolveRecordBooleanStatusValue(value: unknown): RecordBooleanStatusValue {
  return typeof value === 'boolean' ? value : undefined;
}

function controlTypeOf(
  field: RecordFormFieldDescriptor | undefined,
  fallback: RecordFormFieldFallback | undefined,
): RecordFormFieldControlType {
  const referenceControlType = referenceControlTypeOf(field?.reference, field?.uiType);
  if (referenceControlType) {
    return referenceControlType;
  }
  if (field?.uiType === 'enabledStatus') {
    return 'enabledStatus';
  }
  if (field?.uiType === 'booleanStatus' && field.booleanStatus) {
    return 'booleanStatus';
  }
  if (field?.uiType === 'switch') {
    return 'switch';
  }
  if (field?.uiType === 'textarea') {
    return 'textarea';
  }
  if (field?.uiType === 'colorPicker') {
    return 'colorPicker';
  }
  if (field?.uiType === 'recordPicker') {
    return 'recordPicker';
  }
  if (field?.uiType === 'recordMultiPicker') {
    return 'recordMultiPicker';
  }
  if (field?.uiType === 'select') {
    return 'select';
  }
  if (field?.option) {
    return 'select';
  }
  return fallback?.controlType ?? 'input';
}

/** References are semantic fields: their cardinality determines the default editor when metadata has no explicit picker. */
function referenceControlTypeOf(
  reference: ResolvedReferenceFieldDescriptor | undefined,
  uiType: string | undefined,
): Extract<RecordFormFieldControlType, 'recordPicker' | 'recordMultiPicker'> | undefined {
  if (!reference || (uiType != null && uiType !== 'text')) {
    return undefined;
  }
  return reference.cardinality === 'MANY' ? 'recordMultiPicker' : 'recordPicker';
}
