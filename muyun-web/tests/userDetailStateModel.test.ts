import test from 'node:test';
import assert from 'node:assert/strict';
import { hasUserDetailUnsavedChanges } from '../src/views/userDetailStateModel.ts';

test('user detail promotion dirty state ignores password-only client fields', () => {
  assert.equal(
    hasUserDetailUnsavedChanges(
      { id: 'user-1', username: 'alice', password: undefined },
      { id: 'user-1', username: 'alice' },
    ),
    false,
  );
  assert.equal(
    hasUserDetailUnsavedChanges({ id: 'user-1', username: 'alice-2' }, { id: 'user-1', username: 'alice' }),
    true,
  );
});
