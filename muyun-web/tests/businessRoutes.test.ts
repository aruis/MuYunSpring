import test from 'node:test';
import assert from 'node:assert/strict';
import {
  businessModuleRoutes,
  businessRoutePrefixes,
  isStaticBusinessRoutePage,
  resolveStaticBusinessRoute,
} from '../src/app/businessRoutes.ts';
import { pageDescriptorFromUrl } from '../src/platform-workbench/menuNavigation.ts';
import type { BusinessRoutePageDescriptor } from '../src/web-contracts/index.ts';

test('static business route registry exposes route prefixes for navigation resolution', () => {
  assert.deepEqual(businessRoutePrefixes, [
    '/config/applications',
    '/config/dictionaries',
    '/config/menus',
    '/platform/security/passwords',
    '/iam/tenants',
    '/iam/organizations',
    '/iam/departments',
    '/iam/employees',
    '/iam/users',
    '/iam/system-users',
    '/iam/roles',
    '/iam/positions',
  ]);
  assert.deepEqual(businessModuleRoutes, {
    'platform.application': '/config/applications',
    'platform.dictionary_category': '/config/dictionaries',
    'platform.menu_scheme': '/config/menus',
    'iam.password_policy_rule': '/platform/security/passwords',
    'iam.tenant': '/iam/tenants',
    'iam.organization': '/iam/organizations',
    'iam.department': '/iam/departments',
    'iam.employee': '/iam/employees',
    'iam.user': '/iam/users',
    'iam.system_user': '/iam/system-users',
    'iam.role': '/iam/roles',
    'iam.position_category': '/iam/positions',
  });
});

test('static business route registry resolves module alias by route', () => {
  const descriptor: BusinessRoutePageDescriptor = {
    pageType: 'business-route',
    openMode: 'workbench-route',
    hostType: 'business-route-host',
    target: { route: '/iam/organizations' },
    tabPolicy: { identity: 'by-target' },
  };

  const route = resolveStaticBusinessRoute(descriptor);

  assert.equal(route?.moduleAlias, 'iam.organization');
  assert.equal(isStaticBusinessRoutePage(descriptor), true);
});

test('static business route registry resolves by module alias for module menus', () => {
  const descriptor: BusinessRoutePageDescriptor = {
    pageType: 'business-route',
    openMode: 'workbench-route',
    hostType: 'business-route-host',
    target: { route: '/config/applications', moduleAlias: 'platform.application' },
    tabPolicy: { identity: 'by-menu' },
  };

  const route = resolveStaticBusinessRoute(descriptor);

  assert.equal(route?.route, '/config/applications');
  assert.equal(route?.moduleAlias, 'platform.application');
  assert.equal(isStaticBusinessRoutePage(descriptor), true);
});

test('static business route registry resolves password management module route', () => {
  const descriptor: BusinessRoutePageDescriptor = {
    pageType: 'business-route',
    openMode: 'workbench-route',
    hostType: 'business-route-host',
    target: { route: '/platform/security/passwords', moduleAlias: 'iam.password_policy_rule' },
    tabPolicy: { identity: 'by-menu' },
  };

  const route = resolveStaticBusinessRoute(descriptor);

  assert.equal(route?.route, '/platform/security/passwords');
  assert.equal(route?.moduleAlias, 'iam.password_policy_rule');
  assert.equal(isStaticBusinessRoutePage(descriptor), true);
});

test('static business route registry resolves password management URL under platform namespace', () => {
  const descriptor = pageDescriptorFromUrl('/platform/security/passwords', { businessRoutePrefixes });

  assert.equal(descriptor.pageType, 'business-route');
  assert.equal(descriptor.hostType, 'business-route-host');
  assert.deepEqual(descriptor.target, {
    route: '/platform/security/passwords',
    query: undefined,
  });
  assert.equal(isStaticBusinessRoutePage(descriptor), true);
});

test('static business route registry resolves tenant management module route', () => {
  const descriptor: BusinessRoutePageDescriptor = {
    pageType: 'business-route',
    openMode: 'workbench-route',
    hostType: 'business-route-host',
    target: { route: '/iam/tenants', moduleAlias: 'iam.tenant' },
    tabPolicy: { identity: 'by-menu' },
  };

  const route = resolveStaticBusinessRoute(descriptor);

  assert.equal(route?.route, '/iam/tenants');
  assert.equal(route?.moduleAlias, 'iam.tenant');
});

test('static business route registry resolves position category as position management entry', () => {
  const descriptor: BusinessRoutePageDescriptor = {
    pageType: 'business-route',
    openMode: 'workbench-route',
    hostType: 'business-route-host',
    target: { route: '/iam/positions', moduleAlias: 'iam.position_category' },
    tabPolicy: { identity: 'by-menu' },
  };

  const route = resolveStaticBusinessRoute(descriptor);

  assert.equal(route?.route, '/iam/positions');
  assert.equal(route?.moduleAlias, 'iam.position_category');
  assert.equal(isStaticBusinessRoutePage(descriptor), true);
});

test('static business route registry resolves department management module route', () => {
  const descriptor: BusinessRoutePageDescriptor = {
    pageType: 'business-route',
    openMode: 'workbench-route',
    hostType: 'business-route-host',
    target: { route: '/iam/departments', moduleAlias: 'iam.department' },
    tabPolicy: { identity: 'by-menu' },
  };

  const route = resolveStaticBusinessRoute(descriptor);

  assert.equal(route?.route, '/iam/departments');
  assert.equal(route?.moduleAlias, 'iam.department');
  assert.equal(isStaticBusinessRoutePage(descriptor), true);
});

test('static business route registry resolves employee management module route', () => {
  const descriptor: BusinessRoutePageDescriptor = {
    pageType: 'business-route',
    openMode: 'workbench-route',
    hostType: 'business-route-host',
    target: { route: '/iam/employees', moduleAlias: 'iam.employee' },
    tabPolicy: { identity: 'by-menu' },
  };

  const route = resolveStaticBusinessRoute(descriptor);

  assert.equal(route?.route, '/iam/employees');
  assert.equal(route?.moduleAlias, 'iam.employee');
  assert.equal(isStaticBusinessRoutePage(descriptor), true);
});

test('static business route registry resolves role management module route', () => {
  const descriptor: BusinessRoutePageDescriptor = {
    pageType: 'business-route',
    openMode: 'workbench-route',
    hostType: 'business-route-host',
    target: { route: '/iam/roles', moduleAlias: 'iam.role' },
    tabPolicy: { identity: 'by-menu' },
  };

  const route = resolveStaticBusinessRoute(descriptor);

  assert.equal(route?.route, '/iam/roles');
  assert.equal(route?.moduleAlias, 'iam.role');
  assert.equal(isStaticBusinessRoutePage(descriptor), true);
});

test('static business route registry resolves user management module route', () => {
  const descriptor: BusinessRoutePageDescriptor = {
    pageType: 'business-route',
    openMode: 'workbench-route',
    hostType: 'business-route-host',
    target: { route: '/iam/users', moduleAlias: 'iam.user' },
    tabPolicy: { identity: 'by-menu' },
  };

  const route = resolveStaticBusinessRoute(descriptor);

  assert.equal(route?.route, '/iam/users');
  assert.equal(route?.moduleAlias, 'iam.user');
  assert.equal(isStaticBusinessRoutePage(descriptor), true);
});

test('static business route registry resolves system user management module route', () => {
  const descriptor: BusinessRoutePageDescriptor = {
    pageType: 'business-route',
    openMode: 'workbench-route',
    hostType: 'business-route-host',
    target: { route: '/iam/system-users', moduleAlias: 'iam.system_user' },
    tabPolicy: { identity: 'by-menu' },
  };

  const route = resolveStaticBusinessRoute(descriptor);

  assert.equal(route?.route, '/iam/system-users');
  assert.equal(route?.moduleAlias, 'iam.system_user');
  assert.equal(isStaticBusinessRoutePage(descriptor), true);
});

test('static business route registry resolves dictionary category as dictionary management entry', () => {
  const descriptor: BusinessRoutePageDescriptor = {
    pageType: 'business-route',
    openMode: 'workbench-route',
    hostType: 'business-route-host',
    target: { route: '/config/dictionaries', moduleAlias: 'platform.dictionary_category' },
    tabPolicy: { identity: 'by-menu' },
  };

  const route = resolveStaticBusinessRoute(descriptor);

  assert.equal(route?.route, '/config/dictionaries');
  assert.equal(route?.moduleAlias, 'platform.dictionary_category');
  assert.equal(isStaticBusinessRoutePage(descriptor), true);
});

test('static business route registry resolves menu scheme as menu management entry', () => {
  const descriptor: BusinessRoutePageDescriptor = {
    pageType: 'business-route',
    openMode: 'workbench-route',
    hostType: 'business-route-host',
    target: { route: '/config/menus', moduleAlias: 'platform.menu_scheme' },
    tabPolicy: { identity: 'by-menu' },
  };

  const route = resolveStaticBusinessRoute(descriptor);

  assert.equal(route?.route, '/config/menus');
  assert.equal(route?.moduleAlias, 'platform.menu_scheme');
  assert.equal(isStaticBusinessRoutePage(descriptor), true);
});

test('static business route registry prefers explicit route over module alias fallback', () => {
  const descriptor: BusinessRoutePageDescriptor = {
    pageType: 'business-route',
    openMode: 'workbench-route',
    hostType: 'business-route-host',
    target: { route: '/config/unknown', moduleAlias: 'platform.application' },
    tabPolicy: { identity: 'by-target' },
  };

  assert.equal(resolveStaticBusinessRoute(descriptor), undefined);
  assert.equal(isStaticBusinessRoutePage(descriptor), false);
});

test('static business route registry rejects unregistered business routes', () => {
  const descriptor: BusinessRoutePageDescriptor = {
    pageType: 'business-route',
    openMode: 'workbench-route',
    hostType: 'business-route-host',
    target: { route: '/iam/accounts' },
    tabPolicy: { identity: 'by-target' },
  };

  assert.equal(resolveStaticBusinessRoute(descriptor), undefined);
  assert.equal(isStaticBusinessRoutePage(descriptor), false);
});

test('static business route registry does not classify unregistered sibling routes as business pages', () => {
  const descriptor = pageDescriptorFromUrl('/iam/users-extra', { businessRoutePrefixes });

  assert.equal(descriptor.pageType, 'platform-route');
});
