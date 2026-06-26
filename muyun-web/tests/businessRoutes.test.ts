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
    '/iam/tenants',
    '/iam/organizations',
    '/iam/departments',
    '/iam/positions',
  ]);
  assert.deepEqual(businessModuleRoutes, {
    'platform.application': '/config/applications',
    'platform.dictionary_category': '/config/dictionaries',
    'iam.tenant': '/iam/tenants',
    'iam.organization': '/iam/organizations',
    'iam.department': '/iam/departments',
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
    target: { route: '/iam/users' },
    tabPolicy: { identity: 'by-target' },
  };

  assert.equal(resolveStaticBusinessRoute(descriptor), undefined);
  assert.equal(isStaticBusinessRoutePage(descriptor), false);
});

test('static business route registry does not classify unregistered sibling routes as business pages', () => {
  const descriptor = pageDescriptorFromUrl('/iam/users', { businessRoutePrefixes });

  assert.equal(descriptor.pageType, 'platform-route');
});
