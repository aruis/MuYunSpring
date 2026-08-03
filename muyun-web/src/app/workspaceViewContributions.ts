import type { WorkspaceViewDefinition, WorkspaceViewInput } from './workspaceViewContract';
import { userDetailWorkspaceView } from '../views/userDetailWorkspaceView';
import { employeeDetailWorkspaceView } from '../views/employeeDetailWorkspaceView';
import { roleDetailWorkspaceView } from '../views/roleDetailWorkspaceView';
import { systemUserDetailWorkspaceView } from '../views/systemUserDetailWorkspaceView';
import { roleAuthorizationWorkspaceView } from '../views/roleAuthorizationWorkspaceView';
import { moduleActionManagementWorkspaceView } from '../views/moduleActionManagementWorkspaceView';

/** Application assembly for restorable workspace views. */
export const workspaceViewContributions: readonly WorkspaceViewDefinition<WorkspaceViewInput>[] = [
  userDetailWorkspaceView,
  employeeDetailWorkspaceView,
  roleDetailWorkspaceView,
  systemUserDetailWorkspaceView,
  roleAuthorizationWorkspaceView,
  moduleActionManagementWorkspaceView,
];
