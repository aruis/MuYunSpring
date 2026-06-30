import test from 'node:test';
import assert from 'node:assert/strict';
import {
  isEmployeeFormDisabled,
  shouldCommitEmployeeDetailRequest,
  shouldShowEmployeeDetailContent,
} from '../src/views/employeeDetailStateModel.ts';

test('employee detail form stays disabled until edit detail record is loaded', () => {
  assert.equal(
    isEmployeeFormDisabled({
      mode: 'edit',
      loadingDetail: false,
      saving: false,
      selectedEmployeeId: undefined,
    }),
    true,
  );
  assert.equal(
    isEmployeeFormDisabled({
      mode: 'edit',
      loadingDetail: false,
      saving: false,
      selectedEmployeeId: 'emp-1',
    }),
    false,
  );
});

test('employee detail request commits only for latest selected record', () => {
  assert.equal(
    shouldCommitEmployeeDetailRequest({
      activeRequestSeq: 2,
      requestSeq: 1,
      selectedEmployeeKey: 'emp-1',
      recordId: 'emp-1',
    }),
    false,
  );
  assert.equal(
    shouldCommitEmployeeDetailRequest({
      activeRequestSeq: 2,
      requestSeq: 2,
      selectedEmployeeKey: 'emp-2',
      recordId: 'emp-1',
    }),
    false,
  );
  assert.equal(
    shouldCommitEmployeeDetailRequest({
      activeRequestSeq: 2,
      requestSeq: 2,
      selectedEmployeeKey: 'emp-2',
      recordId: 'emp-2',
    }),
    true,
  );
});

test('employee detail content hides temporary records while loading or failed', () => {
  assert.equal(
    shouldShowEmployeeDetailContent({ mode: 'create', loadingDetail: false, loadFailed: true }),
    true,
  );
  assert.equal(
    shouldShowEmployeeDetailContent({
      mode: 'view',
      loadingDetail: true,
      loadFailed: false,
      selectedEmployeeId: 'emp-1',
    }),
    false,
  );
  assert.equal(
    shouldShowEmployeeDetailContent({
      mode: 'edit',
      loadingDetail: false,
      loadFailed: true,
      selectedEmployeeId: 'emp-1',
    }),
    false,
  );
  assert.equal(
    shouldShowEmployeeDetailContent({ mode: 'view', loadingDetail: false, loadFailed: false }),
    false,
  );
  assert.equal(
    shouldShowEmployeeDetailContent({
      mode: 'view',
      loadingDetail: false,
      loadFailed: false,
      selectedEmployeeId: 'emp-1',
    }),
    true,
  );
});
