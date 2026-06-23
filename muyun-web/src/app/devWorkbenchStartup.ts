import { createMockMenuClient, createMockSessionClient } from '@/web-core/mock';
import { loadWorkbenchStartupState } from './workbenchStartup';

export function loadDevWorkbenchStartupState() {
  return loadWorkbenchStartupState({
    sessionClient: createMockSessionClient(),
    menuClient: createMockMenuClient(),
  });
}
