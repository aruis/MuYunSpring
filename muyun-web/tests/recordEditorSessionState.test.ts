import test from 'node:test';
import assert from 'node:assert/strict';
import { createRecordEditorSessionState } from '../src/platform-components/recordEditorSessionState.ts';

interface DemoRecord {
  id?: string;
  title: string;
}

test('record editor session closes creation when canceling without selected record', () => {
  const session = createRecordEditorSessionState<DemoRecord, 'view' | 'edit' | 'create'>({
    viewMode: 'view',
    createMode: 'create',
    editMode: 'edit',
    emptyDraft: () => ({ title: '' }),
  });

  session.startCreate();
  session.draft.value.title = '临时记录';
  session.cancel();

  assert.equal(session.mode.value, 'view');
  assert.equal(session.readonly.value, true);
  assert.equal(session.selected.value, undefined);
  assert.deepEqual(session.draft.value, { title: '' });
});

test('record editor session restores selected draft when canceling edit', () => {
  const session = createRecordEditorSessionState<DemoRecord, 'view' | 'edit' | 'create'>({
    viewMode: 'view',
    createMode: 'create',
    editMode: 'edit',
    emptyDraft: () => ({ title: '' }),
  });

  session.select({ id: 'record-1', title: '正式记录' });
  assert.equal(session.startEdit(), true);
  session.draft.value.title = '已修改';
  session.cancel();

  assert.equal(session.mode.value, 'view');
  assert.deepEqual(session.selected.value, { id: 'record-1', title: '正式记录' });
  assert.deepEqual(session.draft.value, { id: 'record-1', title: '正式记录' });
});

test('record editor session can create while preserving selected context', () => {
  const session = createRecordEditorSessionState<DemoRecord, 'view' | 'edit' | 'create'>({
    viewMode: 'view',
    createMode: 'create',
    editMode: 'edit',
    emptyDraft: () => ({ title: '' }),
  });

  session.select({ id: 'record-1', title: '正式记录' });
  session.startCreate({
    preserveSelection: true,
    draft: () => ({ title: '临时记录' }),
  });
  session.cancel();

  assert.equal(session.mode.value, 'view');
  assert.deepEqual(session.selected.value, { id: 'record-1', title: '正式记录' });
  assert.deepEqual(session.draft.value, { id: 'record-1', title: '正式记录' });
});

test('record editor session can create with selected context and custom mode', () => {
  const session = createRecordEditorSessionState<DemoRecord, 'view' | 'edit' | 'create' | 'create-child'>({
    viewMode: 'view',
    createMode: 'create',
    editMode: 'edit',
    emptyDraft: () => ({ title: '' }),
  });

  session.startCreate({
    mode: 'create-child',
    selectedRecord: { id: 'parent-1', title: '父记录' },
    draft: { title: '子记录草稿' },
  });

  assert.equal(session.mode.value, 'create-child');
  assert.deepEqual(session.selected.value, { id: 'parent-1', title: '父记录' });
  assert.deepEqual(session.draft.value, { title: '子记录草稿' });
});

test('record editor session can refresh selected without touching draft or mode', () => {
  const session = createRecordEditorSessionState<DemoRecord, 'view' | 'edit' | 'create'>({
    viewMode: 'view',
    createMode: 'create',
    editMode: 'edit',
    emptyDraft: () => ({ title: '' }),
  });

  session.select({ id: 'record-1', title: '正式记录' });
  session.startEdit();
  session.draft.value.title = '未保存草稿';
  session.replaceSelected({ id: 'record-1', title: '刷新后的记录' });

  assert.equal(session.mode.value, 'edit');
  assert.deepEqual(session.selected.value, { id: 'record-1', title: '刷新后的记录' });
  assert.deepEqual(session.draft.value, { id: 'record-1', title: '未保存草稿' });
});

test('record editor session rejects view or edit mode for create sessions', () => {
  const session = createRecordEditorSessionState<DemoRecord, 'view' | 'edit' | 'create'>({
    viewMode: 'view',
    createMode: 'create',
    editMode: 'edit',
    emptyDraft: () => ({ title: '' }),
  });

  assert.throws(
    () => session.startCreate({ mode: 'edit' }),
    /Record editor create mode cannot be view or edit mode/,
  );
  assert.throws(
    () => session.startCreate({ mode: 'view' }),
    /Record editor create mode cannot be view or edit mode/,
  );
});
