import test from 'node:test';
import assert from 'node:assert/strict';
import {
  closeMenuTab,
  initialOpenMenuKeys,
  loadShellStartupState,
  openMenuTab,
} from '../src/app/shellStartup.ts';
import { getMenuNavigationTarget } from '../src/platform-shell/menuNavigation.ts';

const currentUser = {
  userId: 'user-1',
  system: false,
};

const menus = [
  {
    record: {
      id: 'root',
      schemeId: 'default',
      title: 'Root',
      menuType: 'GROUP',
    },
    children: [
      {
        record: {
          id: 'nested',
          schemeId: 'default',
          parentId: 'root',
          title: 'Nested',
          menuType: 'GROUP',
        },
        children: [
          {
            record: {
              id: 'metadata',
              schemeId: 'default',
              parentId: 'nested',
              title: 'Metadata',
              menuType: 'ROUTE',
              route: '/platform/metadata',
            },
            children: [],
          },
        ],
      },
    ],
  },
  {
    record: {
      id: 'runtime',
      schemeId: 'default',
      title: 'Runtime',
      menuType: 'ROUTE',
      route: '/runtime',
    },
    children: [],
  },
];

test('loadShellStartupState creates the first available navigation tab', async () => {
  const state = await loadShellStartupState({
    sessionClient: {
      current: async () => currentUser,
    },
    menuClient: {
      mine: async () => ({ records: menus }),
    },
  });

  assert.equal(state.session.currentUser.userId, 'user-1');
  assert.equal(state.activeTabKey, 'menu:metadata');
  assert.deepEqual(
    state.tabs?.map((tab) => tab.key),
    ['menu:metadata'],
  );
});

test('openMenuTab reuses an existing tab instead of duplicating it', () => {
  const metadata = menus[0].children[0].children[0].record;
  const runtime = menus[1].record;
  const metadataTarget = getMenuNavigationTarget(metadata);
  const runtimeTarget = getMenuNavigationTarget(runtime);

  assert.ok(metadataTarget);
  assert.ok(runtimeTarget);

  const first = openMenuTab([], metadata, metadataTarget);
  const duplicate = openMenuTab(first.tabs, metadata, metadataTarget);
  const second = openMenuTab(duplicate.tabs, runtime, runtimeTarget);

  assert.equal(duplicate.tabs.length, 1);
  assert.equal(duplicate.activeTabKey, 'menu:metadata');
  assert.deepEqual(
    second.tabs.map((tab) => tab.key),
    ['menu:metadata', 'menu:runtime'],
  );
});

test('closeMenuTab keeps active tab when closing an inactive tab', () => {
  const tabs = [
    {
      key: 'ROUTE:metadata',
      title: 'Metadata',
      target: getMenuNavigationTarget(menus[0].children[0].children[0].record),
    },
    { key: 'ROUTE:runtime', title: 'Runtime', target: getMenuNavigationTarget(menus[1].record) },
  ];

  const result = closeMenuTab(tabs, 'ROUTE:runtime', 'ROUTE:metadata');

  assert.equal(result.activeTabKey, 'ROUTE:runtime');
  assert.deepEqual(
    result.tabs.map((tab) => tab.key),
    ['ROUTE:runtime'],
  );
});

test('closeMenuTab activates the neighboring tab when closing the active tab', () => {
  const tabs = [
    { key: 'A', title: 'A', target: { menuId: 'a', menuType: 'ROUTE', route: '/a' } },
    { key: 'B', title: 'B', target: { menuId: 'b', menuType: 'ROUTE', route: '/b' } },
    { key: 'C', title: 'C', target: { menuId: 'c', menuType: 'ROUTE', route: '/c' } },
  ];

  const middle = closeMenuTab(tabs, 'B', 'B');
  const last = closeMenuTab(tabs, 'C', 'C');

  assert.equal(middle.activeTabKey, 'C');
  assert.equal(last.activeTabKey, 'B');
});

test('initialOpenMenuKeys expands ancestors of the active menu', () => {
  const state = {
    session: { currentUser },
    menus,
    tabs: [
      {
        key: 'menu:metadata',
        title: 'Metadata',
        target: {
          menuId: 'metadata',
          menuType: 'ROUTE',
          route: '/platform/metadata',
        },
      },
    ],
    activeTabKey: 'menu:metadata',
  };

  assert.deepEqual(initialOpenMenuKeys(state), ['root', 'nested']);
});
