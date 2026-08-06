import type { ResetPasswordResponse, Tenant, UserAccount } from '@muyun/web-contracts';
import {
  handOffWorkspaceViewSession,
  registerWorkspaceViewHandoffRecipient,
  takeWorkspaceViewSession,
} from '../platform-admin-runtime/workspaceViewSessions';
import { userDetailWorkspaceView, type UserDetailWorkspaceViewInput } from './userDetailWorkspaceView';
import type { UserDetailMode } from './userDetailStateModel';

export interface UserDetailWorkspaceSession {
  selectedUser: UserAccount;
  draft: Partial<UserAccount>;
  tenant?: Tenant;
  mode: Extract<UserDetailMode, 'view' | 'edit'>;
  password: string;
  resetPasswordResult?: ResetPasswordResponse;
}

export function handOffUserDetailWorkspaceSession(
  input: UserDetailWorkspaceViewInput,
  session: UserDetailWorkspaceSession,
) {
  return handOffWorkspaceViewSession(userDetailWorkspaceView, input, {
    ...session,
    selectedUser: { ...session.selectedUser },
    draft: { ...session.draft },
    tenant: session.tenant ? { ...session.tenant } : undefined,
  });
}

export function registerUserDetailWorkspaceHandoffRecipient(
  input: UserDetailWorkspaceViewInput,
  recipient: (session: UserDetailWorkspaceSession) => boolean,
) {
  return registerWorkspaceViewHandoffRecipient(userDetailWorkspaceView, input, recipient);
}

export function takeUserDetailWorkspaceSession(input: UserDetailWorkspaceViewInput) {
  return takeWorkspaceViewSession<UserDetailWorkspaceViewInput, UserDetailWorkspaceSession>(
    userDetailWorkspaceView,
    input,
  );
}
