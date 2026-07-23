import { inject, provide, type InjectionKey } from 'vue';
import type { WorkspaceViewPresentation } from './workspaceViewContract';

/** Runtime host facts shared with a view without exposing workbench navigation to business code. */
export interface WorkspaceViewHost {
  presentation: WorkspaceViewPresentation;
  /** Updates the host tab label without leaking workbench navigation into business views. */
  setTitle(title: string): void;
  dismiss(): void;
}

const workspaceViewHostKey: InjectionKey<WorkspaceViewHost> = Symbol('workspace-view-host');

export function provideWorkspaceViewHost(host: WorkspaceViewHost) {
  provide(workspaceViewHostKey, host);
}

export function useWorkspaceViewHost() {
  return inject(workspaceViewHostKey, undefined);
}
