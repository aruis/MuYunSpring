import test from 'node:test';
import assert from 'node:assert/strict';
import type { Tenant } from '../src/web-contracts/index.ts';
import type { ModuleContext, ModuleRuntimeContextState } from '../src/web-core/index.ts';
import { createTenantManagementState } from '../src/views/tenantManagementState.ts';

test('tenant management state selects first loaded tenant and creates records with alias as id', async () => {
  const calls: unknown[] = [];
  const context = createContext({
    insert: async (record) => {
      calls.push(record);
      return { ...record, sortOrder: 10 };
    },
  });
  const state = createTenantManagementState(context, async () => true);

  state.handleListLoaded([{ id: 'platform', alias: 'platform', title: '平台', enabled: true }]);
  state.startCreate();
  state.draft.value.alias = '  tenant_a  ';
  state.draft.value.title = '  租户 A  ';

  await state.save();

  assert.equal(state.selected.value?.id, 'tenant_a');
  assert.equal(state.selected.value?.alias, 'tenant_a');
  assert.equal(state.mode.value, 'view');
  assert.equal(state.reloadKey.value, 1);
  assert.deepEqual(calls[0], {
    id: 'tenant_a',
    alias: 'tenant_a',
    enabled: true,
    title: '租户 A',
  });
});

test('tenant management state keeps existing alias stable while editing title', async () => {
  const calls: unknown[] = [];
  const context = createContext({
    update: async (id, record) => {
      calls.push({ id, record });
      return { ...record, title: '身份权限' };
    },
  });
  const state = createTenantManagementState(context, async () => true);

  state.handleSelect({ id: 'platform', alias: 'platform', title: '平台', enabled: true });
  state.startEdit();
  state.draft.value.alias = 'changed';
  state.draft.value.title = '身份权限';
  await state.save();

  assert.deepEqual(calls[0], {
    id: 'platform',
    record: {
      id: 'platform',
      alias: 'platform',
      title: '身份权限',
      enabled: true,
    },
  });
  assert.equal(state.selected.value?.alias, 'platform');
});

test('tenant management state toggles enable state and refreshes selected record', async () => {
  const calls: string[] = [];
  const context = createContext({
    disable: async (id) => {
      calls.push(`disable:${id}`);
      return { count: 1 };
    },
    view: async (id) => {
      calls.push(`view:${id}`);
      return { id, alias: id, title: '租户 A', enabled: false };
    },
  });
  const state = createTenantManagementState(context, async () => true);

  state.handleSelect({ id: 'tenant_a', alias: 'tenant_a', title: '租户 A', enabled: true });
  await state.toggleEnabled();

  assert.deepEqual(calls, ['disable:tenant_a', 'view:tenant_a']);
  assert.equal(state.selected.value?.enabled, false);
  assert.equal(state.reloadKey.value, 1);
});

test('tenant management state cancels creation back to selected tenant', () => {
  const context = createContext();
  const state = createTenantManagementState(context, async () => true);

  state.handleSelect({ id: 'tenant_a', alias: 'tenant_a', title: '租户 A', enabled: true });
  state.startCreate();
  state.cancelEdit();

  assert.equal(state.selected.value?.id, 'tenant_a');
  assert.equal(state.draft.value.id, 'tenant_a');
  assert.equal(state.mode.value, 'view');
});

test('tenant management state respects delete confirmation result', async () => {
  const calls: string[] = [];
  const context = createContext({
    delete: async (id) => {
      calls.push(`delete:${id}`);
      return { count: 1 };
    },
  });
  let confirmed = false;
  const state = createTenantManagementState(context, async () => confirmed);

  state.handleSelect({ id: 'tenant_a', alias: 'tenant_a', title: '租户 A', enabled: true });
  await state.removeSelected();

  assert.deepEqual(calls, []);
  assert.equal(state.selected.value?.id, 'tenant_a');

  confirmed = true;
  await state.removeSelected();

  assert.deepEqual(calls, ['delete:tenant_a']);
  assert.equal(state.selected.value, undefined);
  assert.equal(state.mode.value, 'create');
});

test('tenant management state protects platform tenant from disable and delete actions', async () => {
  const calls: string[] = [];
  const context = createContext({
    disable: async (id) => {
      calls.push(`disable:${id}`);
      return { count: 1 };
    },
    delete: async (id) => {
      calls.push(`delete:${id}`);
      return { count: 1 };
    },
  });
  const state = createTenantManagementState(context, async () => true);

  state.handleSelect({ id: 'platform', alias: 'platform', title: '平台租户', enabled: true });

  assert.equal(state.isPlatformTenant.value, true);
  assert.equal(state.canDelete.value, false);
  assert.equal(state.canEnable.value, false);

  await state.toggleEnabled();
  assert.equal(state.actionError.value, '当前用户无权变更租户启停状态');

  await state.removeSelected();
  assert.equal(state.actionError.value, '平台租户不能删除');
  assert.deepEqual(calls, []);
});

test('tenant management state rejects saving disabled platform tenant', async () => {
  const calls: unknown[] = [];
  const context = createContext({
    update: async (id, record) => {
      calls.push({ id, record });
      return record;
    },
  });
  const state = createTenantManagementState(context, async () => true);

  state.handleSelect({ id: 'platform', alias: 'platform', title: '平台租户', enabled: true });
  state.startEdit();
  state.draft.value.enabled = false;
  await state.save();

  assert.equal(state.actionError.value, '平台租户不能停用');
  assert.deepEqual(calls, []);
});

test('tenant management state does not enter create mode without create permission', async () => {
  const context = createContext({}, (actionCode) => actionCode !== 'create');
  const state = createTenantManagementState(context, async () => true);

  state.handleListLoaded([]);

  assert.equal(state.canCreate.value, false);
  assert.equal(state.mode.value, 'view');

  state.startCreate();

  assert.equal(state.mode.value, 'view');
  assert.equal(state.actionError.value, '当前用户无权新建租户');
});

test('tenant management state stays readonly after deleting last tenant without create permission', async () => {
  const calls: string[] = [];
  const context = createContext(
    {
      delete: async (id) => {
        calls.push(`delete:${id}`);
        return { count: 1 };
      },
    },
    (actionCode) => actionCode !== 'create',
  );
  const state = createTenantManagementState(context, async () => true);

  state.handleSelect({ id: 'tenant_a', alias: 'tenant_a', title: '租户 A', enabled: true });
  await state.removeSelected();

  assert.deepEqual(calls, ['delete:tenant_a']);
  assert.equal(state.selected.value, undefined);
  assert.equal(state.mode.value, 'view');
});

test('tenant management state exposes action authorization flags', () => {
  const context = createContext({}, (actionCode) => actionCode === 'view');
  const state = createTenantManagementState(context, async () => true);

  state.handleSelect({ id: 'platform', alias: 'platform', title: '平台', enabled: true });

  assert.equal(state.canCreate.value, false);
  assert.equal(state.canUpdate.value, false);
  assert.equal(state.canDelete.value, false);
  assert.equal(state.canEnable.value, false);
  assert.equal(state.canMutate.value, false);
});

function createContext(
  overrides: Partial<ModuleContext<Tenant>['crud']> = {},
  canAction: (actionCode: string) => boolean | undefined = () => true,
): ModuleContext<Tenant> {
  const crud: ModuleContext<Tenant>['crud'] = {
    query: async () => ({
      records: [],
      total: 0,
      pageNum: 1,
      pageSize: 20,
      pages: 0,
      totalKnown: true,
    }),
    view: async (id) => ({ id, alias: id, title: '平台', enabled: true }),
    insert: async (record) => record,
    update: async (_id, record) => record,
    delete: async () => ({ count: 1 }),
    enable: async () => ({ count: 1 }),
    disable: async () => ({ count: 1 }),
    ...overrides,
  };
  const enable = {
    enable: crud.enable,
    disable: crud.disable,
  };
  const tree = {
    ...crud,
    tree: async () => ({ records: [] }),
    treeFlat: async () => ({ records: [] }),
    subtree: async () => ({ records: [] }),
    sort: async () => ({ count: 1 }),
  };
  return {
    moduleAlias: 'iam.tenant',
    crud,
    runtime: fakeRuntimeState(),
    abilities: {
      crud: () => crud,
      tree: () => tree,
      enable: () => enable,
      tryCrud: () => crud,
      tryTree: () => undefined,
      tryEnable: () => enable,
      has: () => undefined,
      hasCrud: () => undefined,
      hasTree: () => undefined,
      hasEnable: () => undefined,
    },
    action: () => undefined,
    can: canAction,
  };
}

function fakeRuntimeState(): ModuleRuntimeContextState {
  return {
    ready: Promise.resolve({
      moduleAlias: 'iam.tenant',
      capabilities: ['CRUD', 'ENABLE', 'SORT'],
      abilities: ['crud', 'enable', 'sort'],
      actions: [],
    }),
    load: async () => ({
      moduleAlias: 'iam.tenant',
      capabilities: ['CRUD', 'ENABLE', 'SORT'],
      abilities: ['crud', 'enable', 'sort'],
      actions: [],
    }),
    snapshot: () => undefined,
    error: () => undefined,
    hasAbility: () => undefined,
    action: () => undefined,
    can: () => undefined,
  };
}
