import { createHttpClient } from '@muyun/web-core';
import { effectiveAuthToken } from './authSession';

export function createBackendHttpClient(options: { withAuth?: boolean } = {}) {
  return createHttpClient({
    baseUrl: import.meta.env.VITE_MUYUN_API_BASE_URL,
    token: options.withAuth === false ? undefined : effectiveAuthToken(import.meta.env.VITE_MUYUN_AUTH_TOKEN),
    credentials: credentialsOf(import.meta.env.VITE_MUYUN_CREDENTIALS),
  });
}

function credentialsOf(value: string | undefined) {
  return value === 'include' || value === 'omit' || value === 'same-origin' ? value : undefined;
}
