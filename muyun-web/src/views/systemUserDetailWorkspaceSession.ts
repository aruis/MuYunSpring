import type { ResetPasswordResponse, UserAccount } from '@muyun/web-contracts';
import {
  handOffWorkspaceViewSession,
  registerWorkspaceViewHandoffRecipient,
  takeWorkspaceViewSession,
} from '../platform-admin-runtime/workspaceViewSessions';
import {
  systemUserDetailWorkspaceView,
  type SystemUserDetailWorkspaceViewInput,
} from './systemUserDetailWorkspaceView';

export interface SystemUserDetailWorkspaceSession {
  selectedUser: UserAccount;
  draft: Partial<UserAccount>;
  mode: 'view' | 'edit';
  password: string;
  resetPasswordResult?: ResetPasswordResponse;
}

export function handOffSystemUserDetailWorkspaceSession(
  input: SystemUserDetailWorkspaceViewInput,
  session: SystemUserDetailWorkspaceSession,
) {
  return handOffWorkspaceViewSession(systemUserDetailWorkspaceView, input, {
    ...session,
    selectedUser: { ...session.selectedUser },
    draft: { ...session.draft },
  });
}

export function registerSystemUserDetailWorkspaceHandoffRecipient(
  input: SystemUserDetailWorkspaceViewInput,
  recipient: (session: SystemUserDetailWorkspaceSession) => boolean,
) {
  return registerWorkspaceViewHandoffRecipient(systemUserDetailWorkspaceView, input, recipient);
}

export function takeSystemUserDetailWorkspaceSession(input: SystemUserDetailWorkspaceViewInput) {
  return takeWorkspaceViewSession<SystemUserDetailWorkspaceViewInput, SystemUserDetailWorkspaceSession>(
    systemUserDetailWorkspaceView,
    input,
  );
}
