import test from 'node:test';
import assert from 'node:assert/strict';
import {
  businessRoutePrefixes,
  isStaticBusinessRoutePage,
  resolveStaticBusinessRoute,
} from '../src/app/businessRoutes.ts';
import { pageDescriptorFromUrl } from '../src/platform-workbench/menuNavigation.ts';
import type { BusinessRoutePageDescriptor } from '../src/web-contracts/index.ts';

test('static business route registry exposes route prefixes for navigation resolution', () => {
  assert.deepEqual(businessRoutePrefixes, ['/iam/organizations']);
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
