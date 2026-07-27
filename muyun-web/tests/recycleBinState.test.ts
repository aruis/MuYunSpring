import test from 'node:test';
import assert from 'node:assert/strict';
import type { RecycleBinItem, RestoreReport, PurgeReport } from '../src/web-contracts/index.ts';
import type { ModuleContext, ModuleRuntimeContextState } from '../src/web-core/index.ts';
import { useRecycleBinState } from '../src/platform-components/recycleBinState.ts';

interface Tenant {
  id?: string;
  alias?: string;
  title?: string;
  enabled?: boolean;
}

test('recycle bin state loads items from backend', async () => {
  const items: RecycleBinItem<Tenant>[] = [
    {
      record: { id: 'tenant_a', alias: 'tenant_a', title: '租户 A' },
      sourceDeleteOperationId: 'op-1',
      deletedAt: '2024-01-15T10:30:00Z',
      restorable: true,
    },
    {
      record: { id: 'tenant_b', alias: 'tenant_b', title: '租户 B' },
      sourceDeleteOperationId: 'op-2',
      deletedAt: '2024-01-16T14:00:00Z',
      restorable: false,
      unavailableReason: '生命周期已变化',
    },
  ];
  const context = createContext({
    request: async (options) => {
      assert.equal(options.path, '/iam.tenant/recycle-bin/query');
      return { records: items };
    },
  });
  const state = useRecycleBinState({ context });

  await state.load();

  assert.equal(state.items.value.length, 2);
  assert.equal(state.items.value[0].sourceDeleteOperationId, 'op-1');
  assert.equal(state.items.value[1].restorable, false);
  assert.equal(state.isEmpty.value, false);
});

test('recycle bin state returns empty when no deleted items', async () => {
  const context = createContext({
    request: async () => ({ records: [] }),
  });
  const state = useRecycleBinState({ context });

  await state.load();

  assert.equal(state.items.value.length, 0);
  assert.equal(state.isEmpty.value, true);
});

test('recycle bin state restores item and reloads list', async () => {
  const calls: string[] = [];
  const restoreReport: RestoreReport = {
    sourceOperationId: 'op-1',
    restoreOperationId: 'restore-op-1',
    entries: [
      { sourceEntryId: 'e1', moduleAlias: 'iam.tenant', recordId: 'tenant_a', status: 'RESTORED' },
      { sourceEntryId: 'e2', moduleAlias: 'iam.tenant_application', recordId: 'app-1', status: 'RESTORED' },
    ],
  };
  const context = createContext({
    request: async (options) => {
      calls.push(options.path);
      if (options.path.includes('/restore')) {
        return restoreReport;
      }
      return { records: [] };
    },
  });
  const state = useRecycleBinState({ context });

  const item: RecycleBinItem<Tenant> = {
    record: { id: 'tenant_a', title: '租户 A' },
    sourceDeleteOperationId: 'op-1',
    deletedAt: '2024-01-15T10:30:00Z',
    restorable: true,
  };
  const report = await state.restore(item);

  assert.ok(report);
  assert.equal(report.restoreOperationId, 'restore-op-1');
  assert.equal(report.entries.length, 2);
  assert.deepEqual(calls, ['/iam.tenant/recycle-bin/op-1/restore', '/iam.tenant/recycle-bin/query']);
});

test('recycle bin state purges item and reloads list', async () => {
  const calls: string[] = [];
  const purgeReport: PurgeReport = {
    sourceOperationId: 'op-1',
    purgeOperationId: 'purge-op-1',
    entries: [{ sourceEntryId: 'e1', moduleAlias: 'iam.tenant', recordId: 'tenant_a', status: 'PURGED' }],
  };
  const context = createContext({
    request: async (options) => {
      calls.push(options.path);
      if (options.path.includes('/purge')) {
        return purgeReport;
      }
      return { records: [] };
    },
  });
  const state = useRecycleBinState({ context });

  const item: RecycleBinItem<Tenant> = {
    record: { id: 'tenant_a', title: '租户 A' },
    sourceDeleteOperationId: 'op-1',
    deletedAt: '2024-01-15T10:30:00Z',
    restorable: true,
  };
  const report = await state.purge(item);

  assert.ok(report);
  assert.equal(report.purgeOperationId, 'purge-op-1');
  assert.deepEqual(calls, ['/iam.tenant/recycle-bin/op-1/purge', '/iam.tenant/recycle-bin/query']);
});

test('recycle bin state prevents concurrent actions', async () => {
  let requestCount = 0;
  const context = createContext({
    request: async (options) => {
      requestCount++;
      if (options.path.includes('/restore')) {
        await new Promise((resolve) => setTimeout(resolve, 10));
        return { sourceOperationId: 'op-1', restoreOperationId: 'r1', entries: [] };
      }
      return { records: [] };
    },
  });
  const state = useRecycleBinState({ context });

  const item: RecycleBinItem<Tenant> = {
    record: { id: 'tenant_a', title: '租户 A' },
    sourceDeleteOperationId: 'op-1',
    deletedAt: '2024-01-15T10:30:00Z',
    restorable: true,
  };

  const [result1, result2] = await Promise.all([state.restore(item), state.restore(item)]);

  assert.ok(result1);
  assert.equal(result2, undefined);
  assert.equal(requestCount, 2);
});

test('recycle bin state uses custom record title resolver', () => {
  const context = createContext({ request: async () => ({ records: [] }) });
  const state = useRecycleBinState({
    context,
    recordTitle: (record) => `自定义: ${(record as Tenant).alias}`,
  });

  const item: RecycleBinItem<Tenant> = {
    record: { id: 'tenant_a', alias: 'tenant_a', title: '租户 A' },
    sourceDeleteOperationId: 'op-1',
    deletedAt: '2024-01-15T10:30:00Z',
    restorable: true,
  };

  assert.equal(state.recordTitleOf(item), '自定义: tenant_a');
});

test('recycle bin state falls back to default title resolution', () => {
  const context = createContext({ request: async () => ({ records: [] }) });
  const state = useRecycleBinState({ context });

  const itemWithTitle: RecycleBinItem<Tenant> = {
    record: { id: '1', title: '有标题' },
    sourceDeleteOperationId: 'op-1',
    deletedAt: '2024-01-15T10:30:00Z',
    restorable: true,
  };
  const itemWithAlias: RecycleBinItem<Tenant> = {
    record: { id: '2', alias: 'alias_only' },
    sourceDeleteOperationId: 'op-2',
    deletedAt: '2024-01-15T10:30:00Z',
    restorable: true,
  };
  const itemWithId: RecycleBinItem<Tenant> = {
    record: { id: 'id_only' },
    sourceDeleteOperationId: 'op-3',
    deletedAt: '2024-01-15T10:30:00Z',
    restorable: true,
  };

  assert.equal(state.recordTitleOf(itemWithTitle), '有标题');
  assert.equal(state.recordTitleOf(itemWithAlias), 'alias_only');
  assert.equal(state.recordTitleOf(itemWithId), 'id_only');
});

// --- helpers ---

function createContext(overrides: { request: (options: { path: string }) => Promise<unknown> }) {
  return {
    moduleAlias: 'iam.tenant',
    http: { request: overrides.request },
    crud: {},
    runtime: fakeRuntimeState(),
    abilities: {},
    action: () => undefined,
    can: () => undefined,
  } as unknown as ModuleContext<Tenant>;
}

function fakeRuntimeState(): ModuleRuntimeContextState {
  return {
    ready: Promise.resolve({
      moduleAlias: 'iam.tenant',
      capabilities: ['CRUD'],
      abilities: ['crud'],
      actions: [],
    }),
    load: async () => ({
      moduleAlias: 'iam.tenant',
      capabilities: ['CRUD'],
      abilities: ['crud'],
      actions: [],
    }),
    snapshot: () => undefined,
    error: () => undefined,
    hasAbility: () => undefined,
    action: () => undefined,
    can: () => undefined,
  };
}
