import type { BusinessRoutePageDescriptor, RouteQueryValue } from '@muyun/web-contracts';
import { workspaceViewContributions } from './workspaceViewContributions';
import type {
  WorkspaceViewDefinition,
  WorkspaceViewInput,
  WorkspaceViewPresentation,
} from './workspaceViewContract';

export {
  defineWorkspaceView,
  type WorkspaceViewDefinition,
  type WorkspaceViewInput,
  type WorkspaceViewPresentation,
} from './workspaceViewContract';

export interface ResolvedWorkspaceView {
  view: WorkspaceViewDefinition<WorkspaceViewInput>;
  input: WorkspaceViewInput;
  presentation: WorkspaceViewPresentation;
}

export interface WorkspaceViewRegistry {
  definitions: readonly WorkspaceViewDefinition<WorkspaceViewInput>[];
  resolve(descriptor: BusinessRoutePageDescriptor): ResolvedWorkspaceView | undefined;
}

export function createWorkspaceViewRegistry(
  definitions: readonly WorkspaceViewDefinition<WorkspaceViewInput>[],
): WorkspaceViewRegistry {
  const definitionsByType = new Map<string, WorkspaceViewDefinition<WorkspaceViewInput>>();
  for (const definition of definitions) {
    if (definitionsByType.has(definition.type)) {
      throw new Error(`重复的工作视图类型：${definition.type}`);
    }
    definitionsByType.set(definition.type, definition);
  }

  return {
    definitions: [...definitionsByType.values()],
    resolve(descriptor) {
      const query = descriptor.target.query ?? {};
      const type = String(query.workspaceView ?? '');
      const presentation = query.workspacePresentation;
      if (presentation !== 'drawer' && presentation !== 'tab') return undefined;
      const view = definitionsByType.get(type);
      const input = view?.parse(query);
      return view && input && view.presentations.includes(presentation)
        ? { view, input, presentation }
        : undefined;
    },
  };
}

export const workspaceViewRegistry = createWorkspaceViewRegistry(workspaceViewContributions);

export function createWorkspaceViewDescriptor<TInput extends WorkspaceViewInput>(
  view: WorkspaceViewDefinition<TInput>,
  input: TInput,
  presentation: WorkspaceViewPresentation = 'tab',
  title = view.titleOf(input),
): BusinessRoutePageDescriptor {
  if (!view.presentations.includes(presentation)) {
    throw new Error(`工作视图 ${view.type} 不支持 ${presentation} 承载`);
  }
  const params = {
    workspaceView: view.type,
    workspacePresentation: presentation,
    ...(input as Record<string, RouteQueryValue>),
  };
  return {
    pageType: 'business-route',
    openMode: 'workbench-route',
    hostType: 'business-route-host',
    title,
    layout: view.layout,
    target: { route: view.route, moduleAlias: view.moduleAlias, query: params },
    params,
    tabPolicy: { identity: 'by-params' },
  };
}

/** Returns the ordinary route descriptor after a URL-restored drawer is dismissed. */
export function dismissWorkspaceViewDescriptor(
  descriptor: BusinessRoutePageDescriptor,
  view: WorkspaceViewDefinition<WorkspaceViewInput>,
): BusinessRoutePageDescriptor {
  const input = view.parse(descriptor.target.query ?? {});
  const transientKeys = new Set(['workspaceView', 'workspacePresentation', ...Object.keys(input ?? {})]);
  const parentRouteQuery = input ? (view.parentRouteQueryOf?.(input) ?? {}) : {};
  const routeQuery = {
    ...withoutWorkspaceViewParams(descriptor.target.query ?? {}, transientKeys),
    ...parentRouteQuery,
  };
  const routeParams = {
    ...withoutWorkspaceViewParams(descriptor.params ?? {}, transientKeys),
    ...parentRouteQuery,
  };
  return {
    ...descriptor,
    title: view.routeTitle ?? descriptor.title,
    target: { ...descriptor.target, query: routeQuery },
    params: routeParams,
    tabPolicy: { identity: 'by-params' },
  };
}

export function resolveWorkspaceView(descriptor: BusinessRoutePageDescriptor) {
  return workspaceViewRegistry.resolve(descriptor);
}

function withoutWorkspaceViewParams(
  params: Record<string, RouteQueryValue>,
  transientKeys: ReadonlySet<string>,
) {
  return Object.fromEntries(Object.entries(params).filter(([key]) => !transientKeys.has(key)));
}
