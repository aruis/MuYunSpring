import test from 'node:test';
import assert from 'node:assert/strict';
import type { ActionContract } from '../src/web-contracts/index.ts';
import {
  resolveDynamicActionReactions,
  type DynamicLegacyRefreshActionContract,
} from '../src/dynamic-page-runtime/actionReactions.ts';

test('dynamic action legacy refresh none resolves no local reaction', () => {
  const action: ActionContract = { actionCode: 'export', title: '导出' };

  assert.deepEqual(resolveDynamicActionReactions(action, { moduleAlias: 'crm.customer' }), []);
});

test('dynamic action legacy refresh record resolves local detail reaction', () => {
  const action: DynamicLegacyRefreshActionContract = {
    actionCode: 'save',
    title: '保存',
    refresh: 'record',
  };

  assert.deepEqual(
    resolveDynamicActionReactions(action, { moduleAlias: 'crm.customer', recordId: 'cust-1' }),
    [{ type: 'refresh-detail', payload: { moduleAlias: 'crm.customer', recordId: 'cust-1' } }],
  );
});

test('dynamic action legacy refresh all resolves list and detail reactions', () => {
  const action: DynamicLegacyRefreshActionContract = {
    actionCode: 'submit',
    title: '提交',
    refresh: 'all',
  };

  assert.deepEqual(
    resolveDynamicActionReactions(action, { moduleAlias: 'crm.customer', recordId: 'cust-1' }),
    [
      { type: 'refresh-list', payload: { moduleAlias: 'crm.customer' } },
      { type: 'refresh-detail', payload: { moduleAlias: 'crm.customer', recordId: 'cust-1' } },
    ],
  );
});
