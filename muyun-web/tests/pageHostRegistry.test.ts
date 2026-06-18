import test from 'node:test';
import assert from 'node:assert/strict';
import { resolvePageHostComponentName } from '../src/platform-shell/pageHostRegistry.ts';

test('resolvePageHostComponentName maps platform route hosts to PlatformRouteHost', () => {
  assert.equal(resolvePageHostComponentName('platform-route-host'), 'PlatformRouteHost');
});

test('resolvePageHostComponentName maps business route hosts to BusinessRouteHost', () => {
  assert.equal(resolvePageHostComponentName('business-route-host'), 'BusinessRouteHost');
});

test('resolvePageHostComponentName maps dynamic and external hosts', () => {
  assert.equal(resolvePageHostComponentName('dynamic-module-host'), 'DynamicModuleHost');
  assert.equal(resolvePageHostComponentName('external-page-host'), 'ExternalPageHost');
});
