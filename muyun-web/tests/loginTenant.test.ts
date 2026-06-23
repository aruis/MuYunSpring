import test from 'node:test';
import assert from 'node:assert/strict';
import { resolveLoginTenantDefaults } from '../src/app/loginTenant.ts';

test('login tenant from tenantId url parameter locks tenant input', () => {
  const defaults = resolveLoginTenantDefaults('env-tenant', '?tenantId=platform', '/');

  assert.deepEqual(defaults, {
    tenantId: 'platform',
    tenantLocked: true,
  });
});

test('login tenant accepts tenant url parameter as compatibility alias', () => {
  const defaults = resolveLoginTenantDefaults('env-tenant', '?tenant=tenant-a', '/');

  assert.deepEqual(defaults, {
    tenantId: 'tenant-a',
    tenantLocked: true,
  });
});

test('login tenant falls back to environment tenant when url tenant is blank', () => {
  const defaults = resolveLoginTenantDefaults(' env-tenant ', '?tenantId= ', '/');

  assert.deepEqual(defaults, {
    tenantId: 'env-tenant',
    tenantLocked: false,
  });
});

test('login tenant uses workbench-reserved tenant parameter on deep links', () => {
  const defaults = resolveLoginTenantDefaults(
    'env-tenant',
    '?_muyunTenantId=platform',
    '/platform/dynamic/crm.customer/list',
  );

  assert.deepEqual(defaults, {
    tenantId: 'platform',
    tenantLocked: true,
  });
});

test('login tenant does not lock from generic tenant parameter on deep links', () => {
  const defaults = resolveLoginTenantDefaults(
    'env-tenant',
    '?tenantId=business-tenant',
    '/platform/dynamic/crm.customer/list',
  );

  assert.deepEqual(defaults, {
    tenantId: 'env-tenant',
    tenantLocked: false,
  });
});
