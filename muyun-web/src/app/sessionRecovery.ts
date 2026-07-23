import type { AppError } from '@muyun/web-core';

let authenticationRequiredHandler: ((error: AppError) => void) | undefined;

export function configureAuthenticationRecovery(handler: (error: AppError) => void) {
  authenticationRequiredHandler = handler;
}

export function recoverAuthentication(error: AppError) {
  authenticationRequiredHandler?.(error);
}
