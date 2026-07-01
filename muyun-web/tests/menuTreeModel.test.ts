import test from 'node:test';
import assert from 'node:assert/strict';
import {
  buildWorkbenchMegaMenuModel,
  createWorkbenchMenuNodes,
  filterWorkbenchMenuNodes,
  findWorkbenchMenuPath,
  firstDeepRootIdOf,
} from '../src/platform-workbench/menuTreeModel.ts';
import type { MenuTreeNode } from '../src/web-contracts/index.ts';

const menus: MenuTreeNode[] = [
  {
    record: {
      id: 'platform',
      schemeId: 'default',
      title: '平台管理',
      nodeType: 'group',
    },
    children: [
      {
        record: {
          id: 'config',
          schemeId: 'default',
          title: '平台配置',
          nodeType: 'group',
        },
        children: [
          {
            record: {
              id: 'dictionary',
              schemeId: 'default',
              title: '字典管理',
              nodeType: 'entry',
              openMode: 'tab',
              moduleAlias: 'platform.dictionary_category',
            },
            children: [
              {
                record: {
                  id: 'dictionary-items',
                  schemeId: 'default',
                  title: '字典项',
                  nodeType: 'entry',
                  openMode: 'tab',
                  moduleAlias: 'platform.dictionary_item',
                  enabled: false,
                },
                children: [],
              },
            ],
          },
        ],
      },
    ],
  },
];

test('createWorkbenchMenuNodes annotates navigable state without changing tree shape', () => {
  const [root] = createWorkbenchMenuNodes(menus);
  const dictionary = root.children[0].children[0];
  const dictionaryItems = dictionary.children[0];

  assert.equal(root.navigable, false);
  assert.equal(root.hasChildren, true);
  assert.equal(dictionary.navigable, true);
  assert.equal(dictionary.target?.menuType, 'module');
  assert.equal(dictionaryItems.navigable, false);
  assert.equal(dictionaryItems.target, undefined);
});

test('filterWorkbenchMenuNodes keeps matching descendants and their ancestors', () => {
  const filtered = filterWorkbenchMenuNodes(createWorkbenchMenuNodes(menus), 'dictionary');

  assert.deepEqual(
    findWorkbenchMenuPath(filtered, 'dictionary').map((node) => node.record.id),
    ['platform', 'config', 'dictionary'],
  );
});

test('buildWorkbenchMegaMenuModel exposes groups and active deep tree root', () => {
  const [root] = createWorkbenchMenuNodes(menus);
  const activeDeepRootId = firstDeepRootIdOf(root);
  const model = buildWorkbenchMegaMenuModel(root, activeDeepRootId);

  assert.equal(activeDeepRootId, 'dictionary');
  assert.deepEqual(
    model.groups.map((node) => node.record.id),
    ['config'],
  );
  assert.equal(model.activeDeepRoot?.record.id, 'dictionary');
});
