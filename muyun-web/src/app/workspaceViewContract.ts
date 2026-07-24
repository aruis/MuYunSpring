import type { Component } from 'vue';
import type { RouteQueryValue } from '@muyun/web-contracts';

export type WorkspaceViewInput = object;
export type WorkspaceViewPresentation = 'drawer' | 'tab';
/**
 * Semantic drawer capacities owned by the workbench, rather than by a
 * particular business page.  A work view declares the amount of workspace it
 * needs; the host decides the actual pixel value and narrow-screen fallback.
 */
export type WorkspaceDrawerProfile = 'detail' | 'wide-work';

/** Stable declaration shared by all workbench view hosts. */
export interface WorkspaceViewDefinition<TInput extends WorkspaceViewInput> {
  type: string;
  route: string;
  moduleAlias: string;
  component: Component;
  /** Drawer capacity used when this view is opened as a drawer. */
  drawerProfile?: WorkspaceDrawerProfile;
  /** Title restored when a URL-addressable drawer returns to its ordinary route. */
  routeTitle?: string;
  titleOf(input: TInput): string;
  /**
   * Route context that remains after a drawer is closed.
   *
   * A work view may need parent-page context (for example a role ownership
   * scope) both to identify the view and to return the user to the same
   * ordinary route.  This keeps that context distinct from the transient
   * work-view presentation and record identity.
   */
  parentRouteQueryOf?(input: TInput): Record<string, RouteQueryValue>;
  parse(query: Record<string, RouteQueryValue>): TInput | undefined;
  presentations: readonly WorkspaceViewPresentation[];
}

export function defineWorkspaceView<TInput extends WorkspaceViewInput>(
  definition: WorkspaceViewDefinition<TInput>,
) {
  return definition;
}
