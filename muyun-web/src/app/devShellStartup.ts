import { createMockMenuClient, createMockSessionClient } from '@/web-core/mock';
import { loadShellStartupState } from './shellStartup';

export function loadDevShellStartupState() {
  return loadShellStartupState({
    sessionClient: createMockSessionClient(),
    menuClient: createMockMenuClient(),
  });
}
