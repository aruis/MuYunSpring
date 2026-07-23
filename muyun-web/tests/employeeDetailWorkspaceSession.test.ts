import test from 'node:test';
import assert from 'node:assert/strict';
import {
  handOffEmployeeDetailWorkspaceSession,
  takeEmployeeDetailWorkspaceSession,
} from '../src/views/employeeDetailWorkspaceSession.ts';

test('employee detail workspace hand-off preserves the draft and account binding state once', async () => {
  const input = { recordId: 'employee-1' } as const;
  await handOffEmployeeDetailWorkspaceSession(input, {
    selectedEmployee: { id: 'employee-1', title: 'Alice', enabled: true },
    draft: { id: 'employee-1', title: 'Alice Renamed', enabled: true },
    department: { id: 'department-1', title: '技术部', enabled: true },
    account: { id: 'binding-1', userId: 'user-1' },
    accountUser: { id: 'user-1', username: 'alice', enabled: true },
    showAccountProvisionForm: false,
    accountProvisionDraft: { username: 'alice', password: '' },
    mode: 'edit',
  });

  const restored = takeEmployeeDetailWorkspaceSession(input);
  assert.equal(restored?.draft.title, 'Alice Renamed');
  assert.equal(restored?.accountUser?.username, 'alice');
  assert.equal(restored?.department?.title, '技术部');
  assert.equal(takeEmployeeDetailWorkspaceSession(input), undefined);
});
