import {
  normalizeError,
  resolveGlobalErrorPresentation,
  type AppError,
  type ErrorUiContext,
  type GlobalErrorPresentation,
} from '@muyun/web-core';
import { showErrorMessage, showSuccessMessage } from '@muyun/vue-ui-antdv';

export interface PlatformErrorFeedbackContext {
  source?: string;
  phase?: 'load' | 'action' | 'validation' | 'authorization';
  tone?: 'error' | 'success';
}

export function presentPlatformError(cause: unknown, context: PlatformErrorFeedbackContext = {}) {
  const error = normalizeError(cause);
  const presentation = resolveGlobalErrorPresentation(error, toErrorUiContext(context));
  presentGlobalErrorPresentation(presentation);
  return error;
}

export function presentPlatformMessage(message: string, context: PlatformErrorFeedbackContext = {}) {
  if (context.tone === 'success') {
    presentPlatformSuccess(message);
    return;
  }
  showErrorMessage(message);
}

export function presentPlatformSuccess(message: string, _context: PlatformErrorFeedbackContext = {}) {
  showSuccessMessage(message);
}

export type PlatformActionErrorHandler<TContext> = {
  code?: string;
  marker?: string;
  handle: (error: AppError, context: TContext) => void;
};

export function matchesPlatformActionErrorHandler<TContext>(
  error: AppError,
  handler: PlatformActionErrorHandler<TContext>,
) {
  if (handler.code && error.code === handler.code) {
    return true;
  }
  if (!handler.marker) {
    return false;
  }
  return (
    error.details?.marker === handler.marker ||
    error.details?.reason === handler.marker ||
    error.details?.errorKey === handler.marker ||
    error.message.includes(handler.marker)
  );
}

function toErrorUiContext(context: PlatformErrorFeedbackContext): ErrorUiContext {
  return {
    phase: context.phase === 'load' ? 'page-load' : 'action',
    surface: context.source?.includes('dialog') ? 'dialog' : 'unknown',
  };
}

function presentGlobalErrorPresentation(presentation: GlobalErrorPresentation) {
  if (presentation.slot === 'silent' || presentation.slot === 'redirect-login') {
    return;
  }
  showErrorMessage(presentation.message);
}
