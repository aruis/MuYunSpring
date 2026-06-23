import test from 'node:test';
import assert from 'node:assert/strict';
import type { Organization } from '../src/web-contracts/index.ts';
import type { ModuleTreeContext } from '../src/web-core/index.ts';
import { createOrganizationManagementState } from '../src/views/organizationManagementState.ts';

test('organization management state selects first loaded organization and creates child records', async () => {
  const calls: unknown[] = [];
  const context = createContext({
    insert: async (record) => {
      calls.push(record);
      return { ...record, id: 'org-child' };
    },
  });
  const state = createOrganizationManagementState(context, async () => true);

  state.handleTreeLoaded([{ id: 'org-root', code: 'ROOT', title: '总部', enabled: true }]);
  state.startCreateChild();
  state.draft.value.title = '  华东区  ';
  state.draft.value.code = '  EAST  ';

  await state.save();

  assert.equal(state.selected.value?.id, 'org-child');
  assert.equal(state.mode.value, 'view');
  assert.equal(state.reloadKey.value, 1);
  assert.deepEqual(calls[0], {
    parentId: 'org-root',
    enabled: true,
    title: '华东区',
    code: 'EAST',
  });
});

test('organization management state updates existing records and refreshes enable state', async () => {
  const calls: string[] = [];
  const context = createContext({
    update: async (id, record) => {
      calls.push(`update:${id}:${record.title}`);
      return { ...record, title: '总部修订' };
    },
    disable: async (id) => {
      calls.push(`disable:${id}`);
      return { count: 1 };
    },
    view: async (id) => {
      calls.push(`view:${id}`);
      return { id, code: 'ROOT', title: '总部修订', enabled: false };
    },
  });
  const state = createOrganizationManagementState(context, async () => true);

  state.handleSelect({ id: 'org-root', code: 'ROOT', title: '总部', enabled: true });
  state.startEdit();
  state.draft.value.title = '总部修订';
  await state.save();
  await state.toggleEnabled();

  assert.deepEqual(calls, ['update:org-root:总部修订', 'disable:org-root', 'view:org-root']);
  assert.equal(state.selected.value?.enabled, false);
  assert.equal(state.actionMessage.value, '已停用');
  assert.equal(state.reloadKey.value, 2);
});

test('organization management state respects delete confirmation result', async () => {
  const calls: string[] = [];
  const context = createContext({
    delete: async (id) => {
      calls.push(`delete:${id}`);
      return { count: 1 };
    },
  });
  let confirmed = false;
  const state = createOrganizationManagementState(context, async () => confirmed);

  state.handleSelect({ id: 'org-root', code: 'ROOT', title: '总部', enabled: true });
  await state.removeSelected();

  assert.deepEqual(calls, []);
  assert.equal(state.selected.value?.id, 'org-root');

  confirmed = true;
  await state.removeSelected();

  assert.deepEqual(calls, ['delete:org-root']);
  assert.equal(state.selected.value, undefined);
  assert.equal(state.mode.value, 'create');
  assert.equal(state.actionMessage.value, '已删除');
});

function createContext(
  overrides: Partial<ModuleTreeContext<Organization>['crud']> = {},
): ModuleTreeContext<Organization> {
  const crud: ModuleTreeContext<Organization>['crud'] = {
    query: async () => ({
      records: [],
      total: 0,
      pageNum: 1,
      pageSize: 20,
      pages: 0,
      totalKnown: true,
    }),
    view: async (id) => ({ id, code: 'ROOT', title: '总部', enabled: true }),
    insert: async (record) => ({ ...record, id: 'org-new' }),
    update: async (_id, record) => record,
    delete: async () => ({ count: 1 }),
    enable: async () => ({ count: 1 }),
    disable: async () => ({ count: 1 }),
    ...overrides,
  };
  return {
    moduleAlias: 'iam.organization',
    crud,
    tree: {
      ...crud,
      tree: async () => ({ records: [] }),
      treeFlat: async () => ({ records: [] }),
      subtree: async () => ({ records: [] }),
      sort: async () => ({ count: 1 }),
    },
  };
}
