import test from 'node:test';
import assert from 'node:assert/strict';
import type { Component } from 'vue';
import {
  discardWorkspaceViewSession,
  getOrCreateWorkspaceViewSession,
  workspaceViewInstanceKey,
} from '../src/app/workspaceViewSessions.ts';
import { defineWorkspaceView } from '../src/app/workspaceViews.ts';

const view = defineWorkspaceView({
  type: 'iam.user.detail.session-test',
  route: '/iam/users',
  moduleAlias: 'iam.user',
  component: {} as Component,
  presentations: ['drawer', 'tab'],
  titleOf: () => '用户详情',
  parse: () => undefined,
});

test('workspace view session identity is stable across input key order', () => {
  const left = workspaceViewInstanceKey(view, { recordId: 'user-1', mode: 'edit' });
  const right = workspaceViewInstanceKey(view, { mode: 'edit', recordId: 'user-1' });
  assert.equal(left, right);
});

test('workspace view session is shared by matching view identity and can be discarded', () => {
  const input = { recordId: 'user-2', mode: 'view' };
  const session = getOrCreateWorkspaceViewSession(view, input, () => ({ draft: 'alice' }));
  assert.equal(
    getOrCreateWorkspaceViewSession(view, input, () => ({ draft: 'bob' })),
    session,
  );
  discardWorkspaceViewSession(view, input);
  assert.deepEqual(
    getOrCreateWorkspaceViewSession(view, input, () => ({ draft: 'bob' })),
    { draft: 'bob' },
  );
});
