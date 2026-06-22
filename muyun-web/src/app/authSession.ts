import { AppError } from '@muyun/web-core';

const AUTH_TOKEN_STORAGE_KEY = 'muyun.auth.token';

export function effectiveAuthToken(envToken?: string) {
  return storedAuthToken() ?? normalizeToken(envToken);
}

export function storedAuthToken() {
  if (typeof window === 'undefined') {
    return undefined;
  }
  return normalizeToken(window.localStorage.getItem(AUTH_TOKEN_STORAGE_KEY));
}

export function saveAuthToken(token: string) {
  if (typeof window === 'undefined') {
    return;
  }
  window.localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, token);
}

export function clearAuthToken() {
  if (typeof window === 'undefined') {
    return;
  }
  window.localStorage.removeItem(AUTH_TOKEN_STORAGE_KEY);
}

export function isAuthenticationRequiredError(cause: unknown) {
  if (!(cause instanceof AppError)) {
    return false;
  }
  return (
    cause.code === 'AUTH_REQUIRED' ||
    cause.code === 'AUTH_EXPIRED' ||
    (cause.status === 401 && cause.code === 'HTTP_ERROR')
  );
}

function normalizeToken(token?: string | null) {
  const value = token?.trim();
  return value ? value : undefined;
}
