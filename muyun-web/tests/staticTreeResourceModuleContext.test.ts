import test from 'node:test';
import assert from 'node:assert/strict';
import {
  createEmptyStaticTreeClient,
  createStaticTreeResourceModuleContext,
} from '../src/platform-components/staticTreeResourceModuleContext.ts';
import type { DictionaryCategory } from '../src/web-contracts/index.ts';
import type { ModuleContext, StaticModuleTreeClient } from '../src/web-core/index.ts';

test('static tree resource context exposes the selected resource client as crud and tree ability', async () => {
  const resourceClient = createTreeClient({ records: [{ id: 'category-status', title: '状态字典' }] });
  const context = createStaticTreeResourceModuleContext(createContext(), { client: resourceClient });

  assert.equal(context.crud, resourceClient);
  assert.equal(context.abilities.crud(), resourceClient);
  assert.equal(context.abilities.tree(), resourceClient);
  assert.equal(context.abilities.enable(), resourceClient);
  assert.deepEqual(await context.abilities.tree().treeFlat(), {
    records: [{ id: 'category-status', title: '状态字典' }],
  });
});

test('static tree resource context provides empty tree client before scope is selected', async () => {
  const context = createStaticTreeResourceModuleContext(createContext(), {
    emptyQueryScopeName: 'platform.dictionary_category',
  });

  assert.deepEqual(await context.abilities.tree().querySchema(), {
    scopeName: 'platform.dictionary_category',
    quickSearch: { enabled: false, fields: [], fieldSchemas: [] },
    fields: [],
    externalCriteria: [],
    defaultSorts: [],
  });
  assert.deepEqual(await context.abilities.tree().query(), {
    records: [],
    total: 0,
    pageNum: 1,
    pageSize: 20,
    pages: 0,
    totalKnown: true,
  });
  assert.deepEqual(await context.abilities.tree().tree(), { records: [] });
  assert.deepEqual(await context.abilities.tree().treeFlat(), { records: [] });
});

test('static tree resource context keeps runtime tree availability for optional consumers', () => {
  const context = createStaticTreeResourceModuleContext(createContext({ hasTree: false }), {
    client: createTreeClient(),
  });

  assert.equal(context.abilities.hasTree(), false);
  assert.equal(context.abilities.tryTree(), undefined);
});

test('empty static tree client is mutation-safe for unavailable scopes', async () => {
  const client = createEmptyStaticTreeClient<DictionaryCategory>('platform.dictionary_item');

  assert.deepEqual(await client.delete('missing'), { count: 0 });
  assert.deepEqual(await client.sort('missing', { beforeId: 'other' }), { count: 0 });
});

function createContext(options: { hasTree?: boolean } = {}): ModuleContext<DictionaryCategory> {
  const hasTree = options.hasTree ?? true;
  const crud = createTreeClient();
  return {
    moduleAlias: 'platform.dictionary_category',
    http: { request: async () => undefined as never },
    crud,
    runtime: {
      loading: { value: false },
      loaded: { value: true },
      error: { value: undefined },
      actions: { value: [] },
      permissions: { value: {} },
      ready: Promise.resolve(),
      reload: async () => undefined,
      action: () => undefined,
      can: () => true,
      hasAbility: () => hasTree,
    },
    abilities: {
      crud: () => crud,
      tree: () => crud,
      enable: () => crud,
      tryCrud: () => crud,
      tryTree: () => (hasTree ? crud : undefined),
      tryEnable: () => crud,
      has: () => hasTree,
      hasCrud: () => true,
      hasTree: () => hasTree,
      hasEnable: () => true,
    },
    action: () => undefined,
    can: () => true,
  };
}

function createTreeClient(
  treeFlatResponse: { records: DictionaryCategory[] } = { records: [] },
): StaticModuleTreeClient<DictionaryCategory> {
  return {
    querySchema: async () => ({
      scopeName: 'platform.dictionary_category',
      quickSearch: { enabled: false, fields: [], fieldSchemas: [] },
      fields: [],
      externalCriteria: [],
      defaultSorts: [],
    }),
    query: async () => ({
      records: treeFlatResponse.records,
      total: treeFlatResponse.records.length,
      pageNum: 1,
      pageSize: 20,
      pages: 1,
      totalKnown: true,
    }),
    view: async (id) => ({ id }),
    insert: async (record) => ({ record }),
    update: async (_id, record) => ({ record }),
    delete: async () => ({ count: 1 }),
    enable: async () => ({ count: 1 }),
    disable: async () => ({ count: 1 }),
    tree: async () => ({ records: [] }),
    treeFlat: async () => treeFlatResponse,
    subtree: async () => ({ records: [] }),
    sort: async () => ({ count: 1 }),
  };
}
