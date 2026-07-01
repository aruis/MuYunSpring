import test from 'node:test';
import assert from 'node:assert/strict';
import { normalizeMenuDraft, normalizeSchemeDraft, validateMenu } from '../src/views/menuManagementState.ts';
import type { MenuRecord } from '../src/web-contracts/index.ts';

test('menu management normalizes scheme identity only on create', () => {
  const created = normalizeSchemeDraft(
    { alias: ' admin ', title: ' 管理菜单 ', scopeType: 'system', scopeId: '  ' },
    undefined,
    'create',
  );
  const updated = normalizeSchemeDraft(
    { id: 'scheme-1', alias: ' changed ', title: ' 管理菜单 ', scopeType: 'tenant' },
    { id: 'scheme-1', alias: 'admin', title: '管理菜单', scopeType: 'tenant' },
    'edit',
  );

  assert.deepEqual(created, {
    id: 'admin',
    alias: 'admin',
    title: '管理菜单',
    scopeType: 'system',
    scopeId: undefined,
  });
  assert.equal(updated.id, 'scheme-1');
  assert.equal(updated.alias, 'admin');
});

test('menu management keeps only fields that match the selected menu type', () => {
  const moduleMenu = normalizeMenuDraft(
    baseMenu({ menuType: 'module', moduleAlias: ' platform.menu ' }),
    'scheme-1',
  );
  const routeMenu = normalizeMenuDraft(baseMenu({ menuType: 'route', route: ' /config/menus ' }), 'scheme-1');
  const linkMenu = normalizeMenuDraft(
    baseMenu({ menuType: 'link', externalUrl: ' https://example.com ' }),
    'scheme-1',
  );
  const groupMenu = normalizeMenuDraft(baseMenu({ menuType: 'group' }), 'scheme-1');

  assert.equal(moduleMenu.moduleAlias, 'platform.menu');
  assert.equal(moduleMenu.route, undefined);
  assert.equal(moduleMenu.externalUrl, undefined);
  assert.equal(moduleMenu.openMode, 'tab');
  assert.equal(routeMenu.route, '/config/menus');
  assert.equal(routeMenu.moduleAlias, undefined);
  assert.equal(linkMenu.externalUrl, 'https://example.com');
  assert.equal(linkMenu.openMode, 'tab');
  assert.equal(groupMenu.openMode, undefined);
  assert.equal(groupMenu.moduleAlias, undefined);
});

test('menu management validates required target by menu type', () => {
  assert.equal(validateMenu(baseMenu({ menuType: 'group' })), undefined);
  assert.equal(
    validateMenu(baseMenu({ menuType: 'module', moduleAlias: undefined })),
    '模块菜单必须选择模块',
  );
  assert.equal(validateMenu(baseMenu({ menuType: 'route', route: undefined })), '路由菜单必须填写路由');
  assert.equal(validateMenu(baseMenu({ menuType: 'link', externalUrl: undefined })), '外链菜单必须填写 URL');
  assert.equal(validateMenu(baseMenu({ menuType: 'module', moduleAlias: 'platform.menu' })), undefined);
});

function baseMenu(overrides: Partial<MenuRecord>): MenuRecord {
  return {
    id: 'menu-1',
    schemeId: 'scheme-1',
    title: '菜单',
    menuType: 'group',
    openMode: 'tab',
    moduleAlias: 'dirty.module',
    route: '/dirty',
    externalUrl: 'https://dirty.example.com',
    pageMode: 'LIST',
    defaultUiConfigId: 'ui-1',
    defaultQueryTemplateId: 'query-1',
    entryParamsJson: '{}',
    ...overrides,
  };
}
