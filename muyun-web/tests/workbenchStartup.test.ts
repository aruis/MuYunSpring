import test from 'node:test';
import assert from 'node:assert/strict';
import {
  activeTabUrlOf,
  closeMenuTab,
  loadWorkbenchStartupState,
  openMenuTab,
  restoreWorkbenchStartupStateFromUrl,
} from '../src/app/workbenchStartup.ts';
import { getMenuNavigationTarget } from '../src/platform-workbench/menuNavigation.ts';

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

const platformAdminMenus = [
  {
    record: {
      id: 'platform.menu.group.platform',
      schemeId: 'platform.menu_scheme.admin',
      parentId: 'ROOT',
      title: '平台管理',
      menuType: 'GROUP',
      enabled: true,
      sortOrder: 10,
    },
    children: [
      {
        record: {
          id: 'platform.menu.group.config',
          schemeId: 'platform.menu_scheme.admin',
          parentId: 'platform.menu.group.platform',
          title: '平台配置与低代码运维',
          menuType: 'GROUP',
          enabled: true,
          sortOrder: 10,
        },
        children: [
          {
            record: {
              id: 'platform.menu.module.platform.application',
              schemeId: 'platform.menu_scheme.admin',
              parentId: 'platform.menu.group.config',
              title: '应用管理',
              menuType: 'MODULE',
              moduleAlias: 'platform.application',
              pageMode: 'LIST',
              enabled: true,
              sortOrder: 10,
            },
            children: [],
          },
          {
            record: {
              id: 'platform.menu.module.platform.module',
              schemeId: 'platform.menu_scheme.admin',
              parentId: 'platform.menu.group.config',
              title: '模块管理',
              menuType: 'MODULE',
              moduleAlias: 'platform.module',
              pageMode: 'LIST',
              enabled: true,
              sortOrder: 20,
            },
            children: [],
          },
          {
            record: {
              id: 'platform.menu.module.platform.dictionary_category',
              schemeId: 'platform.menu_scheme.admin',
              parentId: 'platform.menu.group.config',
              title: '字典管理',
              menuType: 'MODULE',
              moduleAlias: 'platform.dictionary_category',
              pageMode: 'LIST',
              enabled: true,
              sortOrder: 50,
            },
            children: [],
          },
        ],
      },
      {
        record: {
          id: 'platform.menu.group.identity',
          schemeId: 'platform.menu_scheme.admin',
          parentId: 'platform.menu.group.platform',
          title: '组织与权限',
          menuType: 'GROUP',
          enabled: true,
          sortOrder: 20,
        },
        children: [
          {
            record: {
              id: 'platform.menu.module.iam.employee',
              schemeId: 'platform.menu_scheme.admin',
              parentId: 'platform.menu.group.identity',
              title: '职员管理',
              menuType: 'MODULE',
              moduleAlias: 'iam.employee',
              pageMode: 'LIST',
              enabled: true,
              sortOrder: 50,
            },
            children: [],
          },
          {
            record: {
              id: 'platform.menu.module.iam.role',
              schemeId: 'platform.menu_scheme.admin',
              parentId: 'platform.menu.group.identity',
              title: '角色管理',
              menuType: 'MODULE',
              moduleAlias: 'iam.role',
              pageMode: 'LIST',
              enabled: true,
              sortOrder: 70,
            },
            children: [],
          },
        ],
      },
      {
        record: {
          id: 'platform.menu.group.ops',
          schemeId: 'platform.menu_scheme.admin',
          parentId: 'platform.menu.group.platform',
          title: '平台运行运维',
          menuType: 'GROUP',
          enabled: true,
          sortOrder: 30,
        },
        children: [],
      },
    ],
  },
];

test('loadWorkbenchStartupState creates the first available navigation tab', async () => {
  const state = await loadWorkbenchStartupState({
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

test('loadWorkbenchStartupState skips disabled navigation menus', async () => {
  const state = await loadWorkbenchStartupState({
    sessionClient: {
      current: async () => currentUser,
    },
    menuClient: {
      mine: async () => ({
        records: [
          {
            record: {
              id: 'disabled-runtime',
              schemeId: 'default',
              title: 'Disabled Runtime',
              menuType: 'MODULE',
              moduleAlias: 'platform.runtime',
              enabled: false,
            },
            children: [],
          },
          ...menus,
        ],
      }),
    },
  });

  assert.equal(state.activeTabKey, 'menu:metadata');
  assert.deepEqual(
    state.tabs?.map((tab) => tab.key),
    ['menu:metadata'],
  );
});

test('loadWorkbenchStartupState accepts backend initialized platform admin menus', async () => {
  const state = await loadWorkbenchStartupState({
    sessionClient: {
      current: async () => ({
        userId: 'platform.user.super_admin',
        username: 'admin',
        system: true,
      }),
    },
    menuClient: {
      mine: async () => ({ records: platformAdminMenus }),
    },
  });

  assert.equal(state.activeTabKey, 'menu:platform.menu.module.platform.application');
  assert.equal(state.tabs?.[0]?.title, '应用管理');
  assert.deepEqual(state.tabs?.[0]?.target, {
    menuId: 'platform.menu.module.platform.application',
    menuType: 'MODULE',
    moduleAlias: 'platform.application',
    pageMode: 'LIST',
    defaultUiConfigId: undefined,
    defaultQueryTemplateId: undefined,
    entryParamsJson: undefined,
  });
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
      openMode: 'workbench-route',
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

test('activeTabUrlOf keeps new-window external links on workbench-owned URLs', () => {
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

test('restoreWorkbenchStartupStateFromUrl activates the matching menu tab', () => {
  const state = {
    session: { currentUser },
    menus,
    tabs: [],
  };

  const restored = restoreWorkbenchStartupStateFromUrl(state, '/platform/metadata');

  assert.equal(restored.activeTabKey, 'menu:metadata');
  assert.deepEqual(
    restored.tabs?.map((tab) => tab.title),
    ['Metadata'],
  );
  assert.equal(restored.tabs?.[0]?.target?.menuId, 'metadata');
});

test('restoreWorkbenchStartupStateFromUrl preserves query when URL matches a menu tab', () => {
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

  const restored = restoreWorkbenchStartupStateFromUrl(state, '/platform/metadata?view=advanced');

  assert.equal(restored.activeTabKey, 'menu:metadata');
  assert.equal(restored.tabs?.length, 1);
  assert.equal(restored.tabs?.[0]?.pageDescriptor?.target.query?.view, 'advanced');
  assert.equal(
    activeTabUrlOf(restored),
    '/platform/metadata?_muyunMenuId=metadata&_muyunTitle=Metadata&view=advanced',
  );
});

test('restoreWorkbenchStartupStateFromUrl prefers explicit menu id when routes are duplicated', () => {
  const state = {
    session: { currentUser },
    menus,
    tabs: [],
  };

  const restored = restoreWorkbenchStartupStateFromUrl(
    state,
    '/platform/metadata?_muyunMenuId=metadata-shortcut',
  );

  assert.equal(restored.activeTabKey, 'menu:metadata-shortcut');
  assert.equal(restored.tabs?.[0]?.title, 'Metadata Shortcut');
  assert.equal(restored.tabs?.[0]?.target?.menuId, 'metadata-shortcut');
});

test('restoreWorkbenchStartupStateFromUrl ignores explicit menu id when target does not match', () => {
  const state = {
    session: { currentUser },
    menus,
    tabs: [],
  };

  const restored = restoreWorkbenchStartupStateFromUrl(state, '/platform/metadata?_muyunMenuId=runtime');

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

test('restoreWorkbenchStartupStateFromUrl creates direct tab when URL has no menu match', () => {
  const state = {
    session: { currentUser },
    menus,
    tabs: [],
  };

  const restored = restoreWorkbenchStartupStateFromUrl(state, '/crm/customer/list?status=active');

  assert.equal(restored.activeTabKey, 'platform-route:/crm/customer/list');
  assert.equal(restored.tabs?.[0]?.target, undefined);
  assert.equal(restored.tabs?.[0]?.pageDescriptor?.target.route, '/crm/customer/list');
});

test('restoreWorkbenchStartupStateFromUrl keeps current state for invalid workbench-owned URLs', () => {
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

  for (const url of ['/platform/external', '/platform/workspace']) {
    const restored = restoreWorkbenchStartupStateFromUrl(state, url);

    assert.equal(restored.activeTabKey, 'menu:metadata');
    assert.equal(restored.tabs?.length, 1);
    assert.equal(restored.tabs?.[0]?.key, 'menu:metadata');
  }
});

test('restoreWorkbenchStartupStateFromUrl keeps empty workspace for invalid workbench-owned URLs', () => {
  const state = {
    session: { currentUser },
    menus,
    tabs: [],
    activeTabKey: undefined,
  };

  for (const url of ['/platform/dynamic', '/platform/dynamic//list']) {
    const restored = restoreWorkbenchStartupStateFromUrl(state, url);

    assert.equal(restored.activeTabKey, undefined);
    assert.deepEqual(restored.tabs, []);
  }
});

test('restoreWorkbenchStartupStateFromUrl matches dynamic menu without title query', () => {
  const state = {
    session: { currentUser },
    menus,
    tabs: [],
  };

  const restored = restoreWorkbenchStartupStateFromUrl(
    state,
    '/platform/dynamic/platform.runtime/list?uiConfigId=runtime-list-v1',
  );

  assert.equal(restored.activeTabKey, 'menu:runtime');
  assert.equal(restored.tabs?.[0]?.title, 'Runtime');
  assert.equal(restored.tabs?.[0]?.target?.menuId, 'runtime');
});
