import type { RoleDataGrantActionMatrix, RolePermissionAction } from '@muyun/web-contracts';
import {
  handOffWorkspaceViewSession,
  registerWorkspaceViewHandoffRecipient,
  takeWorkspaceViewSession,
} from '../app/workspaceViewSessions';
import {
  roleAuthorizationWorkspaceView,
  type RoleAuthorizationWorkspaceViewInput,
} from './roleAuthorizationWorkspaceView';

export interface RoleAuthorizationWorkspaceSession {
  selectedModuleAlias?: string;
  dataGrantMatrix?: RoleDataGrantActionMatrix;
  actionDrafts?: Array<{
    moduleAlias: string;
    actions: RolePermissionAction[];
  }>;
  actionSnapshots?: Array<{
    moduleAlias: string;
    snapshot: string;
  }>;
}

export function handOffRoleAuthorizationWorkspaceSession(
  input: RoleAuthorizationWorkspaceViewInput,
  session: RoleAuthorizationWorkspaceSession,
) {
  return handOffWorkspaceViewSession(roleAuthorizationWorkspaceView, input, session);
}

export function registerRoleAuthorizationWorkspaceHandoffRecipient(
  input: RoleAuthorizationWorkspaceViewInput,
  recipient: (session: RoleAuthorizationWorkspaceSession) => boolean,
) {
  return registerWorkspaceViewHandoffRecipient(roleAuthorizationWorkspaceView, input, recipient);
}

export function takeRoleAuthorizationWorkspaceSession(input: RoleAuthorizationWorkspaceViewInput) {
  return takeWorkspaceViewSession<RoleAuthorizationWorkspaceViewInput, RoleAuthorizationWorkspaceSession>(
    roleAuthorizationWorkspaceView,
    input,
  );
}
