import type { ResetPasswordResponse, UserAccount } from '@muyun/web-contracts';
import { replaceWorkspaceViewSession, takeWorkspaceViewSession } from '../app/workspaceViewSessions';
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
  replaceWorkspaceViewSession(systemUserDetailWorkspaceView, input, {
    ...session,
    selectedUser: { ...session.selectedUser },
    draft: { ...session.draft },
  });
}

export function takeSystemUserDetailWorkspaceSession(input: SystemUserDetailWorkspaceViewInput) {
  return takeWorkspaceViewSession<SystemUserDetailWorkspaceViewInput, SystemUserDetailWorkspaceSession>(
    systemUserDetailWorkspaceView,
    input,
  );
}
