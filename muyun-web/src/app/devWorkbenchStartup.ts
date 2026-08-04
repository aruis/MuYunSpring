import { createMockMenuClient, createMockSessionClient } from '@/web-core/mock';
import { businessModuleRoutes, businessRouteLayouts, businessRoutePrefixes } from './businessRoutes';
import { loadWorkbenchStartupState } from './workbenchStartup';

export function loadDevWorkbenchStartupState() {
  return loadWorkbenchStartupState(
    {
      sessionClient: createMockSessionClient(),
      menuClient: createMockMenuClient(),
    },
    { businessModuleRoutes, businessRouteLayouts, businessRoutePrefixes },
  );
}
