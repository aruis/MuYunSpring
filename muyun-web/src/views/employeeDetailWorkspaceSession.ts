import type { Department, Employee, EmployeeAccount, Organization, UserAccount } from '@muyun/web-contracts';
import { replaceWorkspaceViewSession, takeWorkspaceViewSession } from '../app/workspaceViewSessions';
import type { EmployeeDetailWorkspaceViewInput } from './employeeDetailWorkspaceView';
import { employeeDetailWorkspaceView } from './employeeDetailWorkspaceView';

export interface EmployeeDetailWorkspaceSession {
  selectedEmployee: Employee;
  draft: Partial<Employee>;
  organization?: Organization;
  department?: Department;
  account?: EmployeeAccount;
  accountUser?: UserAccount;
  showAccountProvisionForm: boolean;
  accountProvisionDraft: Partial<UserAccount>;
  mode: 'view' | 'edit';
}

export function handOffEmployeeDetailWorkspaceSession(
  input: EmployeeDetailWorkspaceViewInput,
  session: EmployeeDetailWorkspaceSession,
) {
  replaceWorkspaceViewSession(employeeDetailWorkspaceView, input, {
    ...session,
    selectedEmployee: { ...session.selectedEmployee },
    draft: { ...session.draft },
    organization: session.organization ? { ...session.organization } : undefined,
    department: session.department ? { ...session.department } : undefined,
    account: session.account ? { ...session.account } : undefined,
    accountUser: session.accountUser ? { ...session.accountUser } : undefined,
    accountProvisionDraft: { ...session.accountProvisionDraft },
  });
}

export function takeEmployeeDetailWorkspaceSession(input: EmployeeDetailWorkspaceViewInput) {
  return takeWorkspaceViewSession<EmployeeDetailWorkspaceViewInput, EmployeeDetailWorkspaceSession>(
    employeeDetailWorkspaceView,
    input,
  );
}
