import type { Organization, Role, RoleOwnerScopeType, Tenant } from '@muyun/web-contracts';
import { replaceWorkspaceViewSession, takeWorkspaceViewSession } from '../app/workspaceViewSessions';
import { roleDetailWorkspaceView, type RoleDetailWorkspaceViewInput } from './roleDetailWorkspaceView';

export interface RoleDetailWorkspaceScope {
  kind: RoleOwnerScopeType;
  id?: string;
  key: string;
  title: string;
  tenant?: Tenant;
  organization?: Organization;
}

export interface RoleDetailWorkspaceSession {
  selectedRole: Role;
  draft: Partial<Role>;
  scope: RoleDetailWorkspaceScope;
  mode: 'view' | 'edit';
}

export function handOffRoleDetailWorkspaceSession(
  input: RoleDetailWorkspaceViewInput,
  session: RoleDetailWorkspaceSession,
) {
  replaceWorkspaceViewSession(roleDetailWorkspaceView, input, {
    ...session,
    selectedRole: { ...session.selectedRole },
    draft: { ...session.draft },
    scope: {
      ...session.scope,
      tenant: session.scope.tenant ? { ...session.scope.tenant } : undefined,
      organization: session.scope.organization ? { ...session.scope.organization } : undefined,
    },
  });
}

export function takeRoleDetailWorkspaceSession(input: RoleDetailWorkspaceViewInput) {
  return takeWorkspaceViewSession<RoleDetailWorkspaceViewInput, RoleDetailWorkspaceSession>(
    roleDetailWorkspaceView,
    input,
  );
}
