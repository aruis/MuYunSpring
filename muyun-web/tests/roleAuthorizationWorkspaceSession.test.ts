import test from 'node:test';
import assert from 'node:assert/strict';
import {
  handOffRoleAuthorizationWorkspaceSession,
  takeRoleAuthorizationWorkspaceSession,
} from '../src/views/roleAuthorizationWorkspaceSession.ts';

test('role authorization workspace hand-off preserves an unsaved data-grant matrix once', () => {
  const input = { roleId: 'role-1' };
  handOffRoleAuthorizationWorkspaceSession(input, {
    selectedModuleAlias: 'iam.user',
    dataGrantMatrix: {
      roleId: 'role-1',
      actions: [{ actionCode: 'view', configured: true, dataScopePolicy: 'organizationAndChildren' }],
    },
    actionDrafts: [
      {
        moduleAlias: 'iam.user',
        actions: [{ moduleAlias: 'iam.user', actionCode: 'view', granted: true, dataAuth: false }],
      },
    ],
  });

  const restored = takeRoleAuthorizationWorkspaceSession(input);
  assert.equal(restored?.selectedModuleAlias, 'iam.user');
  assert.equal(restored?.dataGrantMatrix?.actions[0]?.configured, true);
  assert.equal(restored?.actionDrafts?.[0]?.actions[0]?.granted, true);
  assert.equal(takeRoleAuthorizationWorkspaceSession(input), undefined);
});
