import test from 'node:test';
import assert from 'node:assert/strict';
import {
  activeTabUrlOf,
  closeMenuTab,
  initialOpenMenuKeys,
  loadShellStartupState,
  openMenuTab,
  restoreShellStartupStateFromUrl,
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
      menuType: 'MODULE',
      moduleAlias: 'platform.runtime',
      pageMode: 'LIST',
      defaultUiConfigId: 'runtime-list-v1',
    },
    children: [],
  },
  {
    record: {
      id: 'metadata-shortcut',
      schemeId: 'default',
      title: 'Metadata Shortcut',
      menuType: 'ROUTE',
      route: '/platform/metadata',
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

test('activeTabUrlOf returns the active tab descriptor URL', () => {
  const metadata = menus[0].children[0].children[0].record;
  const target = getMenuNavigationTarget(metadata);

  assert.ok(target);

  const tab = {
    key: 'menu:metadata',
    title: 'Metadata',
    target,
    pageDescriptor: {
      pageType: 'platform-route',
      openMode: 'shell-route',
      hostType: 'platform-route-host',
      menuId: 'metadata',
      target: { route: '/platform/metadata' },
      tabPolicy: { identity: 'by-menu' },
    },
  };

  assert.equal(
    activeTabUrlOf({
      session: { currentUser },
      menus,
      tabs: [tab],
      activeTabKey: tab.key,
    }),
    '/platform/metadata?_muyunMenuId=metadata',
  );
});

test('activeTabUrlOf keeps new-window external links on shell-owned URLs', () => {
  const tab = {
    key: 'menu:external-bi',
    title: 'BI',
    pageDescriptor: {
      pageType: 'external-link',
      openMode: 'new-window',
      hostType: 'external-page-host',
      menuId: 'external-bi',
      target: { url: 'https://bi.example.com/report' },
      tabPolicy: { identity: 'by-menu' },
    },
  };

  assert.equal(
    activeTabUrlOf({
      session: { currentUser },
      menus,
      tabs: [tab],
      activeTabKey: tab.key,
    }),
    '/platform/external?_muyunMenuId=external-bi&mode=new-window&url=https%3A%2F%2Fbi.example.com%2Freport',
  );
});

test('restoreShellStartupStateFromUrl activates the matching menu tab', () => {
  const state = {
    session: { currentUser },
    menus,
    tabs: [],
  };

  const restored = restoreShellStartupStateFromUrl(state, '/platform/metadata');

  assert.equal(restored.activeTabKey, 'menu:metadata');
  assert.deepEqual(
    restored.tabs?.map((tab) => tab.title),
    ['Metadata'],
  );
  assert.equal(restored.tabs?.[0]?.target?.menuId, 'metadata');
});

test('restoreShellStartupStateFromUrl preserves query when URL matches a menu tab', () => {
  const metadata = menus[0].children[0].children[0].record;
  const target = getMenuNavigationTarget(metadata);
  assert.ok(target);

  const defaultTab = openMenuTab([], metadata, target).tabs[0];
  const state = {
    session: { currentUser },
    menus,
    tabs: [defaultTab],
    activeTabKey: defaultTab.key,
  };

  const restored = restoreShellStartupStateFromUrl(state, '/platform/metadata?view=advanced');

  assert.equal(restored.activeTabKey, 'menu:metadata');
  assert.equal(restored.tabs?.length, 1);
  assert.equal(restored.tabs?.[0]?.pageDescriptor?.target.query?.view, 'advanced');
  assert.equal(
    activeTabUrlOf(restored),
    '/platform/metadata?_muyunMenuId=metadata&_muyunTitle=Metadata&view=advanced',
  );
});

test('restoreShellStartupStateFromUrl prefers explicit menu id when routes are duplicated', () => {
  const state = {
    session: { currentUser },
    menus,
    tabs: [],
  };

  const restored = restoreShellStartupStateFromUrl(
    state,
    '/platform/metadata?_muyunMenuId=metadata-shortcut',
  );

  assert.equal(restored.activeTabKey, 'menu:metadata-shortcut');
  assert.equal(restored.tabs?.[0]?.title, 'Metadata Shortcut');
  assert.equal(restored.tabs?.[0]?.target?.menuId, 'metadata-shortcut');
});

test('restoreShellStartupStateFromUrl ignores explicit menu id when target does not match', () => {
  const state = {
    session: { currentUser },
    menus,
    tabs: [],
  };

  const restored = restoreShellStartupStateFromUrl(state, '/platform/metadata?_muyunMenuId=runtime');

  assert.equal(restored.activeTabKey, 'menu:metadata');
  assert.equal(restored.tabs?.[0]?.title, 'Metadata');
  assert.equal(restored.tabs?.[0]?.target?.menuId, 'metadata');
});

test('activeTabUrlOf returns undefined when no active tab remains', () => {
  assert.equal(
    activeTabUrlOf({
      session: { currentUser },
      menus,
      tabs: [],
      activeTabKey: undefined,
    }),
    undefined,
  );
});

test('restoreShellStartupStateFromUrl creates direct tab when URL has no menu match', () => {
  const state = {
    session: { currentUser },
    menus,
    tabs: [],
  };

  const restored = restoreShellStartupStateFromUrl(state, '/crm/customer/list?status=active');

  assert.equal(restored.activeTabKey, 'platform-route:/crm/customer/list');
  assert.equal(restored.tabs?.[0]?.target, undefined);
  assert.equal(restored.tabs?.[0]?.pageDescriptor?.target.route, '/crm/customer/list');
});

test('restoreShellStartupStateFromUrl matches dynamic menu without title query', () => {
  const state = {
    session: { currentUser },
    menus,
    tabs: [],
  };

  const restored = restoreShellStartupStateFromUrl(
    state,
    '/platform/dynamic/platform.runtime/list?uiConfigId=runtime-list-v1',
  );

  assert.equal(restored.activeTabKey, 'menu:runtime');
  assert.equal(restored.tabs?.[0]?.title, 'Runtime');
  assert.equal(restored.tabs?.[0]?.target?.menuId, 'runtime');
});
