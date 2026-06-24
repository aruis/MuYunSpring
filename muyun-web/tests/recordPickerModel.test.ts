import test from 'node:test';
import assert from 'node:assert/strict';
import { resolveRecordPickerMode } from '../src/platform-components/recordPickerModel.ts';

test('resolveRecordPickerMode falls back to list when tree ability is unavailable', () => {
  assert.equal(resolveRecordPickerMode('tree', true), 'tree');
  assert.equal(resolveRecordPickerMode('tree', false), 'list');
  assert.equal(resolveRecordPickerMode('list', true), 'list');
});
