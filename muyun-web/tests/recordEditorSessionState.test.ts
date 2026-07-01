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
