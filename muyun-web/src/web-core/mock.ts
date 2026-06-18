import type { CurrentUser, MenuRecord, WebTreeNode } from '@muyun/web-contracts';
import type { MenuClient, SessionClient } from './index';

export function createMockSessionClient(currentUser: CurrentUser = mockCurrentUser): SessionClient {
  return {
    current: async () => currentUser,
  };
}

export function createMockMenuClient(records: WebTreeNode<MenuRecord>[] = mockMenuTree): MenuClient {
  return {
    mine: async () => ({ records }),
  };
}

export const mockCurrentUser: CurrentUser = {
  userId: 'user-1',
  username: 'alice',
  tenantId: 'tenant-a',
  organizationId: 'org-1',
  system: false,
};

export const mockMenuTree: WebTreeNode<MenuRecord>[] = [
  {
    record: {
      id: 'menu-platform',
      schemeId: 'default',
      parentId: 'root',
      title: '平台配置',
      menuType: 'GROUP',
      enabled: true,
      sortOrder: 10,
    },
    children: [
      {
        record: {
          id: 'menu-platform-metadata',
          schemeId: 'default',
          parentId: 'menu-platform',
          title: '元数据',
          menuType: 'ROUTE',
          route: '/platform/metadata',
          enabled: true,
          sortOrder: 10,
        },
        children: [],
      },
    ],
  },
  {
    record: {
      id: 'menu-runtime',
      schemeId: 'default',
      parentId: 'root',
      title: '动态运行态',
      menuType: 'ROUTE',
      route: '/runtime',
      enabled: true,
      sortOrder: 20,
    },
    children: [],
  },
  {
    record: {
      id: 'menu-identity',
      schemeId: 'default',
      parentId: 'root',
      title: '身份权限',
      menuType: 'ROUTE',
      route: '/identity',
      enabled: true,
      sortOrder: 30,
    },
    children: [],
  },
];
