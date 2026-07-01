import type { RecordPickerRecord } from './recordPickerConstraints';
import type { RecordFormFieldState, RecordFormRecord } from './recordFormFieldModel';

export type RecordDetailDisplayValue = string | number | boolean | undefined | null;

export type RecordDetailDisplayResolver = (
  fieldName: string,
  value: unknown,
  record: RecordFormRecord,
  field: RecordFormFieldState,
) => RecordDetailDisplayValue;

export function resolveRecordDetailDisplayValue(
  field: RecordFormFieldState,
  record: RecordFormRecord,
  options: {
    displayOf?: RecordDetailDisplayResolver;
    emptyText?: string;
  } = {},
) {
  const emptyText = options.emptyText ?? '-';
  const value = record[field.fieldName];
  const customValue = options.displayOf?.(field.fieldName, value, record, field);
  if (isPresent(customValue)) {
    return String(customValue);
  }
  if (field.controlType === 'select' && field.options) {
    const option = field.options.find((item) => item.value === value);
    if (option?.label) {
      return option.label;
    }
  }
  if (field.controlType === 'recordPicker' && isRecordPickerRecord(value)) {
    return field.pickerConfig?.titleOf?.(value) ?? value.title ?? value.code ?? value.id ?? emptyText;
  }
  if (!isPresent(value)) {
    return emptyText;
  }
  return String(value);
}

function isPresent(value: unknown): value is string | number | boolean {
  return value !== undefined && value !== null && value !== '';
}

function isRecordPickerRecord(value: unknown): value is RecordPickerRecord {
  return typeof value === 'object' && value !== null;
}
