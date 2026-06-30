import test from 'node:test';
import assert from 'node:assert/strict';
import {
  resolveRecordFormFieldNames,
  resolveRecordFormFieldState,
  type RecordFormFieldDescriptor,
  type RecordFormFieldFallback,
} from '../src/platform-components/recordFormFieldModel.ts';

test('record form field names prefer descriptor order and fill missing fallback fields', () => {
  const fields = new Map<string, RecordFormFieldDescriptor>([
    ['organizationId', field('所属机构')],
    ['title', field('名称')],
  ]);
  const fallback: Record<string, RecordFormFieldFallback> = {
    organizationId: { label: '所属机构' },
    title: { label: '名称' },
    enabled: { label: '启用状态', controlType: 'enabledStatus' },
  };

  assert.deepEqual(resolveRecordFormFieldNames(fields, fallback, { exclude: ['organizationId'] }), [
    'title',
    'enabled',
  ]);
});

test('record form field names use fallback order when descriptor is missing', () => {
  const fallback: Record<string, RecordFormFieldFallback> = {
    parentId: { label: '上级', controlType: 'recordPicker' },
    code: { label: '编码' },
    enabled: { label: '启用状态', controlType: 'enabledStatus' },
  };

  assert.deepEqual(resolveRecordFormFieldNames(undefined, fallback), ['parentId', 'code', 'enabled']);
});

test('record form field state resolves descriptor facts with fallback control metadata', () => {
  const fields = new Map<string, RecordFormFieldDescriptor>([
    ['title', field('显示名称', { required: true })],
  ]);
  const fallback: Record<string, RecordFormFieldFallback> = {
    title: { label: '名称', placeholder: '请输入名称' },
    parentId: { label: '上级', controlType: 'recordPicker', placeholder: '根节点留空' },
  };

  assert.deepEqual(resolveRecordFormFieldState('title', { fields, fallback }), {
    fieldName: 'title',
    label: '显示名称',
    required: true,
    readOnly: false,
    visible: true,
    controlType: 'input',
    pickerConfig: undefined,
    placeholder: '请输入名称',
  });
  assert.equal(resolveRecordFormFieldState('parentId', { fallback }).controlType, 'recordPicker');
});

test('record form field state resolves select options from fallback metadata', () => {
  const fields = new Map<string, RecordFormFieldDescriptor>([
    ['categoryKind', field('类目类型', { required: true, uiType: 'select' })],
  ]);
  const fallback: Record<string, RecordFormFieldFallback> = {
    categoryKind: {
      label: '类目类型',
      controlType: 'select',
      options: [
        { label: '字典', value: 'DICTIONARY' },
        { label: '目录', value: 'FOLDER' },
      ],
    },
  };

  assert.deepEqual(resolveRecordFormFieldState('categoryKind', { fields, fallback }), {
    fieldName: 'categoryKind',
    label: '类目类型',
    required: true,
    readOnly: false,
    visible: true,
    controlType: 'select',
    pickerConfig: undefined,
    placeholder: undefined,
    options: [
      { label: '字典', value: 'DICTIONARY' },
      { label: '目录', value: 'FOLDER' },
    ],
  });
});

function field(
  label: string,
  options: { required?: boolean; readOnly?: boolean; visible?: boolean; uiType?: string } = {},
): RecordFormFieldDescriptor {
  return {
    label,
    uiType: options.uiType,
    required: { constant: options.required ?? false },
    readOnly: { constant: options.readOnly ?? false },
    visible: { constant: options.visible ?? true },
  } as RecordFormFieldDescriptor;
}
