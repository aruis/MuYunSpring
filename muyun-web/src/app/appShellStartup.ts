import { createHttpClient, createMenuClient, createSessionClient } from '@muyun/web-core';
import type { ShellStartupState } from '@muyun/web-contracts';
import { effectiveAuthToken } from './authSession';
import { loadShellStartupState } from './shellStartup';

export async function loadAppShellStartupState(): Promise<ShellStartupState> {
  if (usesMockStartup()) {
    if (!import.meta.env.DEV) {
      throw new Error('Mock shell startup is only available in dev mode.');
    }

    const { loadDevShellStartupState } = await import(
      /* @vite-ignore */ `/src/app/devShellStartup.ts?t=${Date.now()}`
    );
    return loadDevShellStartupState();
  }

  const httpClient = createHttpClient({
    baseUrl: import.meta.env.VITE_MUYUN_API_BASE_URL,
    token: effectiveAuthToken(import.meta.env.VITE_MUYUN_AUTH_TOKEN),
    credentials: credentialsOf(import.meta.env.VITE_MUYUN_CREDENTIALS),
  });
  return loadShellStartupState({
    sessionClient: createSessionClient(httpClient),
    menuClient: createMenuClient(httpClient),
  });
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
