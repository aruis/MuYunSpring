import { createHttpClient, createMenuClient, createSessionClient } from '@muyun/web-core';
import type { WorkbenchStartupState } from '@muyun/web-contracts';
import { effectiveAuthToken } from './authSession';
import { businessRoutePrefixes } from './businessRoutes';
import { loadWorkbenchStartupState } from './workbenchStartup';

export async function loadAppWorkbenchStartupState(): Promise<WorkbenchStartupState> {
  if (usesMockStartup()) {
    if (!import.meta.env.DEV) {
      throw new Error('Mock workbench startup is only available in dev mode.');
    }

    const { loadDevWorkbenchStartupState } = await import(
      /* @vite-ignore */ `/src/app/devWorkbenchStartup.ts?t=${Date.now()}`
    );
    return loadDevWorkbenchStartupState();
  }

  const httpClient = createHttpClient({
    baseUrl: import.meta.env.VITE_MUYUN_API_BASE_URL,
    token: effectiveAuthToken(import.meta.env.VITE_MUYUN_AUTH_TOKEN),
    credentials: credentialsOf(import.meta.env.VITE_MUYUN_CREDENTIALS),
  });
  return loadWorkbenchStartupState(
    {
      sessionClient: createSessionClient(httpClient),
      menuClient: createMenuClient(httpClient),
    },
    {
      businessRoutePrefixes,
    },
  );
}

function credentialsOf(value: string | undefined) {
  return value === 'include' || value === 'omit' || value === 'same-origin' ? value : undefined;
}

export function usesMockStartup() {
  if (import.meta.env.VITE_MUYUN_USE_MOCK === 'false') {
    return false;
  }

  return import.meta.env.DEV || import.meta.env.VITE_MUYUN_USE_MOCK === 'true';
}
