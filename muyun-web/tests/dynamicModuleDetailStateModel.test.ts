import test from 'node:test';
import assert from 'node:assert/strict';
import {
  canMutateDynamicModuleDetail,
  shouldCommitDynamicModuleDetailRequest,
} from '../src/dynamic-page-runtime/dynamicModuleDetailStateModel.ts';

test('dynamic module detail ignores stale and cancelled detail responses', () => {
  assert.equal(
    shouldCommitDynamicModuleDetailRequest({ activeRequestSequence: 3, requestSequence: 2 }),
    false,
  );
  assert.equal(
    shouldCommitDynamicModuleDetailRequest({ activeRequestSequence: 3, requestSequence: 3 }),
    true,
  );
});

test('dynamic module detail cannot mutate an incomplete, failed, or saving record', () => {
  assert.equal(
    canMutateDynamicModuleDetail({ hasRecord: true, saving: false, loading: true, loadFailed: false }),
    false,
  );
  assert.equal(
    canMutateDynamicModuleDetail({ hasRecord: true, saving: false, loading: false, loadFailed: true }),
    false,
  );
  assert.equal(
    canMutateDynamicModuleDetail({ hasRecord: true, saving: true, loading: false, loadFailed: false }),
    false,
  );
  assert.equal(
    canMutateDynamicModuleDetail({ hasRecord: true, saving: false, loading: false, loadFailed: false }),
    true,
  );
});
