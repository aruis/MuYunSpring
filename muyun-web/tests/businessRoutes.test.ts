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
  assert.deepEqual(businessRoutePrefixes, ['/config/applications', '/iam/tenants', '/iam/organizations']);
  assert.deepEqual(businessModuleRoutes, {
    'platform.application': '/config/applications',
    'iam.tenant': '/iam/tenants',
    'iam.organization': '/iam/organizations',
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
