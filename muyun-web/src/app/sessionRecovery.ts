import type { AppError } from '@muyun/web-core';

let authenticationRequiredHandler: ((error: AppError, token?: string) => boolean) | undefined;

export function configureAuthenticationRecovery(handler: (error: AppError, token?: string) => boolean) {
  authenticationRequiredHandler = handler;
}

export function recoverAuthentication(error: AppError, token?: string) {
  return authenticationRequiredHandler?.(error, token) ?? false;
}
