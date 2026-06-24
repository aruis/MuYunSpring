import test from 'node:test';
import assert from 'node:assert/strict';
import { resolveRecordActions } from '../src/platform-components/recordActionBarModel.ts';

test('resolveRecordActions filters invisible actions and applies authorization', () => {
  const actions = resolveRecordActions({ can: (actionCode) => actionCode !== 'delete' }, [
    { key: 'edit', actionCode: 'update', title: '编辑' },
    { key: 'delete', actionCode: 'delete', title: '删除', danger: true },
    { key: 'hidden', title: '隐藏', visible: false },
  ]);

  assert.deepEqual(
    actions.map((action) => ({
      key: action.key,
      authorized: action.authorized,
      disabled: action.disabled,
      danger: action.danger,
    })),
    [
      { key: 'edit', authorized: true, disabled: false, danger: undefined },
      { key: 'delete', authorized: false, disabled: true, danger: true },
    ],
  );
});

test('resolveRecordActions applies default and per-action loading', () => {
  const actions = resolveRecordActions(
    { can: () => true },
    [
      { key: 'cancel', title: '取消', loading: false },
      { key: 'save', actionCode: 'update', title: '保存', primary: true },
    ],
    true,
  );

  assert.equal(actions[0].loading, false);
  assert.equal(actions[0].disabled, false);
  assert.equal(actions[1].loading, true);
  assert.equal(actions[1].disabled, true);
  assert.equal(actions[1].primary, true);
});
