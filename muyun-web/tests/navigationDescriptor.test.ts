import test from 'node:test';
import assert from 'node:assert/strict';
import {
  pageDescriptorFromUrl,
  pageDescriptorToUrl,
  resolvePageDescriptor,
  tabKeyOf,
  tryPageDescriptorFromUrl,
} from '../src/platform-shell/menuNavigation.ts';
import type { PageDescriptor } from '../src/web-contracts/index.ts';

test('resolvePageDescriptor resolves ROUTE targets as platform routes by default', () => {
  const descriptor = resolvePageDescriptor(
    {
      menuId: 'metadata',
      menuType: 'ROUTE',
      route: '/platform/metadata',
    },
    { title: 'Metadata' },
  );

  assert.equal(descriptor.pageType, 'platform-route');
  assert.equal(descriptor.openMode, 'shell-route');
  assert.equal(descriptor.hostType, 'platform-route-host');
  assert.equal(descriptor.title, 'Metadata');
  assert.equal(descriptor.target.route, '/platform/metadata');
  assert.equal(tabKeyOf(descriptor), 'menu:metadata');
  assert.equal(
    pageDescriptorToUrl(descriptor),
    '/platform/metadata?_muyunMenuId=metadata&_muyunTitle=Metadata',
  );
  const roundTrip = pageDescriptorFromUrl(pageDescriptorToUrl(descriptor));
  assert.equal(roundTrip.pageType, 'platform-route');
  assert.equal(roundTrip.menuId, 'metadata');
  assert.equal(roundTrip.title, 'Metadata');
  assert.equal(roundTrip.tabPolicy.identity, 'by-menu');
});

test('resolvePageDescriptor keeps path, routeName, and pageKey available for offline business routes', () => {
  const pathDescriptor = resolvePageDescriptor(
    {
      menuId: 'customer-list',
      menuType: 'ROUTE',
      route: '/crm/customer/list',
    },
    { businessRoutePrefixes: ['/crm'] },
  );
  const routeNameDescriptor = resolvePageDescriptor(
    {
      menuId: 'customer-name',
      menuType: 'ROUTE',
      route: 'crm.customer.list',
    },
    { businessRouteNames: ['crm.customer.list'] },
  );
  const pageKeyDescriptor = resolvePageDescriptor(
    {
      menuId: 'customer-page',
      menuType: 'ROUTE',
      route: 'customerList',
    },
    { businessPageKeys: ['customerList'] },
  );

  assert.equal(pathDescriptor.pageType, 'business-route');
  assert.equal(pathDescriptor.hostType, 'business-route-host');
  assert.equal(pathDescriptor.target.route, '/crm/customer/list');
  assert.equal(routeNameDescriptor.pageType, 'business-route');
  assert.equal(routeNameDescriptor.target.routeName, 'crm.customer.list');
  assert.equal(pageKeyDescriptor.pageType, 'business-route');
  assert.equal(pageKeyDescriptor.target.pageKey, 'customerList');
});

test('resolvePageDescriptor resolves MODULE targets as dynamic module descriptors', () => {
  const descriptor = resolvePageDescriptor({
    menuId: 'customer-module',
    menuType: 'MODULE',
    moduleAlias: 'crm.customer',
    pageMode: 'LIST',
    defaultUiConfigId: 'customer-list-v1',
    defaultQueryTemplateId: 'customer-query-v1',
    entryParamsJson: '{"source":"menu"}',
    query: { recordId: 'customer-1' },
  });

  assert.equal(descriptor.pageType, 'dynamic-module');
  assert.equal(descriptor.openMode, 'dynamic-runner');
  assert.equal(descriptor.hostType, 'dynamic-module-host');
  assert.equal(descriptor.target.moduleAlias, 'crm.customer');
  assert.equal(descriptor.target.pageMode, 'LIST');
  assert.equal(descriptor.entryParamsJson, '{"source":"menu"}');
  assert.equal(
    pageDescriptorToUrl(descriptor),
    '/platform/dynamic/crm.customer/list?_muyunEntryParams=%7B%22source%22%3A%22menu%22%7D&_muyunMenuId=customer-module&queryTemplateId=customer-query-v1&recordId=customer-1&uiConfigId=customer-list-v1',
  );
  const roundTrip = pageDescriptorFromUrl(pageDescriptorToUrl(descriptor));
  assert.equal(roundTrip.entryParamsJson, '{"source":"menu"}');
  assert.equal(roundTrip.params?.recordId, 'customer-1');
  assert.equal(roundTrip.menuId, 'customer-module');
  assert.equal(roundTrip.tabPolicy.identity, 'by-menu');
});

test('resolvePageDescriptor resolves LINK targets by open mode', () => {
  const iframeDescriptor = resolvePageDescriptor({
    menuId: 'crm-online',
    menuType: 'LINK',
    externalUrl: '/crm/customer/list',
  });
  const newWindowDescriptor = resolvePageDescriptor({
    menuId: 'external-bi',
    menuType: 'LINK',
    externalUrl: 'https://bi.example.com/report',
  });

  assert.equal(iframeDescriptor.pageType, 'remote-url');
  assert.equal(iframeDescriptor.openMode, 'iframe');
  assert.equal(iframeDescriptor.hostType, 'external-page-host');
  assert.equal(tabKeyOf(iframeDescriptor), 'menu:crm-online');
  assert.equal(
    pageDescriptorToUrl(iframeDescriptor),
    '/platform/external?_muyunMenuId=crm-online&mode=iframe&url=%2Fcrm%2Fcustomer%2Flist',
  );
  assert.equal(newWindowDescriptor.pageType, 'external-link');
  assert.equal(newWindowDescriptor.openMode, 'new-window');
  assert.equal(newWindowDescriptor.hostType, 'external-page-host');
  assert.equal(tabKeyOf(newWindowDescriptor), 'menu:external-bi');
  assert.equal(
    pageDescriptorToUrl(newWindowDescriptor),
    '/platform/external?_muyunMenuId=external-bi&mode=new-window&url=https%3A%2F%2Fbi.example.com%2Freport',
  );
});

test('pageDescriptorToUrl keeps new-window external links on shell-owned URLs', () => {
  const descriptor = resolvePageDescriptor({
    menuId: 'external-bi',
    menuType: 'LINK',
    externalUrl: 'https://bi.example.com/report',
  });

  assert.equal(
    pageDescriptorToUrl(descriptor),
    '/platform/external?_muyunMenuId=external-bi&mode=new-window&url=https%3A%2F%2Fbi.example.com%2Freport',
  );

  const restored = pageDescriptorFromUrl(pageDescriptorToUrl(descriptor));
  assert.equal(restored.pageType, 'external-link');
  assert.equal(restored.openMode, 'new-window');
  assert.equal(restored.target.url, 'https://bi.example.com/report');
  assert.equal(restored.menuId, 'external-bi');
  assert.equal(restored.tabPolicy.identity, 'by-menu');
});

test('resolvePageDescriptor does not treat protocol-relative LINK targets as same-origin paths', () => {
  const descriptor = resolvePageDescriptor({
    menuId: 'protocol-relative',
    menuType: 'LINK',
    externalUrl: '//bi.example.com/report',
  });

  assert.equal(descriptor.pageType, 'external-link');
  assert.equal(descriptor.openMode, 'new-window');
});

test('pageDescriptorFromUrl restores readable dynamic, external, and business URLs', () => {
  const dynamicDescriptor = pageDescriptorFromUrl(
    '/platform/dynamic/crm.customer/list?uiConfigId=customer-list-v1',
  );
  const externalDescriptor = pageDescriptorFromUrl(
    '/platform/external?url=%2Fcrm%2Fcustomer%2Flist&mode=iframe',
  );
  const businessDescriptor = pageDescriptorFromUrl('/crm/customer/list?status=active', {
    businessRoutePrefixes: ['/crm'],
  });

  assert.equal(dynamicDescriptor.pageType, 'dynamic-module');
  assert.equal(dynamicDescriptor.target.moduleAlias, 'crm.customer');
  assert.equal(dynamicDescriptor.target.pageMode, 'LIST');
  assert.equal(externalDescriptor.pageType, 'remote-url');
  assert.equal(externalDescriptor.target.url, '/crm/customer/list');
  assert.equal(businessDescriptor.pageType, 'business-route');
  assert.equal(businessDescriptor.target.route, '/crm/customer/list');
  assert.deepEqual(businessDescriptor.params, { status: 'active' });
});

test('pageDescriptorFromUrl restores dynamic module params and entry params', () => {
  const descriptor = pageDescriptorFromUrl(
    '/platform/dynamic/crm.customer/list?entryParamsJson=%7B%22source%22%3A%22menu%22%7D&recordId=customer-1&uiConfigId=customer-list-v1',
  );

  assert.equal(descriptor.pageType, 'dynamic-module');
  assert.equal(descriptor.entryParamsJson, '{"source":"menu"}');
  assert.deepEqual(descriptor.params, { recordId: 'customer-1' });
  assert.equal(descriptor.target.defaultUiConfigId, 'customer-list-v1');
});

test('tryPageDescriptorFromUrl rejects invalid shell-owned URLs', () => {
  assert.equal(tryPageDescriptorFromUrl('/platform/external'), undefined);
  assert.equal(tryPageDescriptorFromUrl('/platform/dynamic'), undefined);
  assert.equal(tryPageDescriptorFromUrl('/platform/dynamic//list'), undefined);
  assert.equal(tryPageDescriptorFromUrl('/platform/workspace'), undefined);
  assert.equal(tryPageDescriptorFromUrl('http://['), undefined);
});

test('pageDescriptorFromUrl keeps shell metadata separate from business route query', () => {
  const descriptor = pageDescriptorFromUrl(
    '/crm/customer/list?entryParamsJson=business-value&menuId=business-menu&_muyunEntryParams=%7B%22source%22%3A%22shell%22%7D&_muyunMenuId=customer-list&_muyunTitle=Customers&title=Business',
    { businessRoutePrefixes: ['/crm'] },
  );

  assert.equal(descriptor.pageType, 'business-route');
  assert.equal(descriptor.menuId, 'customer-list');
  assert.equal(descriptor.title, 'Customers');
  assert.equal(descriptor.entryParamsJson, '{"source":"shell"}');
  assert.deepEqual(descriptor.target.query, {
    entryParamsJson: 'business-value',
    menuId: 'business-menu',
    title: 'Business',
  });
});

test('pageDescriptorToUrl and pageDescriptorFromUrl preserve routeName and pageKey semantics', () => {
  const routeNameDescriptor = resolvePageDescriptor(
    {
      menuId: 'customer-name',
      menuType: 'ROUTE',
      route: 'crm.customer.list',
      query: { status: 'active' },
    },
    { businessRouteNames: ['crm.customer.list'], title: 'Customers' },
  );
  const pageKeyDescriptor = resolvePageDescriptor(
    {
      menuId: 'customer-page',
      menuType: 'ROUTE',
      route: 'customerList',
    },
    { businessPageKeys: ['customerList'] },
  );

  const routeNameRoundTrip = pageDescriptorFromUrl(pageDescriptorToUrl(routeNameDescriptor));
  const pageKeyRoundTrip = pageDescriptorFromUrl(pageDescriptorToUrl(pageKeyDescriptor));

  assert.equal(routeNameRoundTrip.pageType, 'business-route');
  assert.equal(routeNameRoundTrip.target.routeName, 'crm.customer.list');
  assert.equal(routeNameRoundTrip.target.query?.status, 'active');
  assert.equal(routeNameRoundTrip.menuId, 'customer-name');
  assert.equal(routeNameRoundTrip.tabPolicy.identity, 'by-menu');
  assert.equal(pageKeyRoundTrip.pageType, 'business-route');
  assert.equal(pageKeyRoundTrip.target.pageKey, 'customerList');
  assert.equal(pageKeyRoundTrip.menuId, 'customer-page');
  assert.equal(pageKeyRoundTrip.tabPolicy.identity, 'by-menu');
});

test('business route prefix matching uses path segment boundaries', () => {
  const businessDescriptor = pageDescriptorFromUrl('/crm/customer/list', {
    businessRoutePrefixes: ['/crm'],
  });
  const platformDescriptor = pageDescriptorFromUrl('/crm-old/customer/list', {
    businessRoutePrefixes: ['/crm'],
  });

  assert.equal(businessDescriptor.pageType, 'business-route');
  assert.equal(platformDescriptor.pageType, 'platform-route');
});

test('tabKeyOf uses menu identity as by-params base when available', () => {
  const descriptor: PageDescriptor = {
    pageType: 'business-route',
    openMode: 'shell-route',
    hostType: 'business-route-host',
    menuId: 'customer-list',
    target: { route: '/crm/customer/list' },
    params: { status: 'active', tags: ['vip', 'trial'] },
    tabPolicy: { identity: 'by-params' },
  };

  assert.equal(tabKeyOf(descriptor), 'menu:customer-list:status=active&tags=vip&tags=trial');
});
