import test from 'node:test';
import assert from 'node:assert/strict';
import {
  handOffUserDetailWorkspaceSession,
  takeUserDetailWorkspaceSession,
} from '../src/views/userDetailWorkspaceSession.ts';

test('user detail workspace hand-off preserves an edit draft and consumes it once', () => {
  const input = { recordId: 'user-1' } as const;
  handOffUserDetailWorkspaceSession(input, {
    selectedUser: { id: 'user-1', username: 'alice', enabled: true },
    draft: { id: 'user-1', username: 'alice-renamed', enabled: true },
    mode: 'edit',
    password: '',
  });

  const restored = takeUserDetailWorkspaceSession(input);
  assert.equal(restored?.draft.username, 'alice-renamed');
  assert.equal(restored?.mode, 'edit');
  assert.equal(takeUserDetailWorkspaceSession(input), undefined);
});
