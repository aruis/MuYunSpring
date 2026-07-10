import {
  presentPlatformSuccess,
  type PlatformErrorFeedbackContext,
} from './platformErrorFeedback';

export interface PlatformActionResultEffect {
  type: string;
  payload?: Record<string, unknown>;
}

export interface PlatformActionResult {
  message: string;
  resultType?: string;
  effects: PlatformActionResultEffect[];
  raw: unknown;
}

export type PlatformActionResultEffectHandler = (
  effect: PlatformActionResultEffect,
  result: PlatformActionResult,
) => void | Promise<void>;

export interface PlatformActionResultFeedbackContext extends PlatformErrorFeedbackContext {
  fallbackMessage?: string;
}

export interface PlatformActionResultHandlingContext extends PlatformActionResultFeedbackContext {
  effectHandlers?: Record<string, PlatformActionResultEffectHandler | undefined>;
}

export function resolvePlatformActionResultMessage(result: unknown, fallbackMessage = '操作成功') {
  return resolvePlatformActionResult(result, { fallbackMessage }).message;
}

export function resolvePlatformActionResult(
  result: unknown,
  options: { fallbackMessage?: string } = {},
): PlatformActionResult {
  return {
    message: actionResultMessage(result) ?? options.fallbackMessage ?? '操作成功',
    resultType: actionResultType(result),
    effects: actionResultEffects(result),
    raw: result,
  };
}

export async function handlePlatformActionSuccess(
  result: unknown,
  context: PlatformActionResultHandlingContext = {},
) {
  const actionResult = resolvePlatformActionResult(result, {
    fallbackMessage: context.fallbackMessage,
  });
  presentPlatformSuccess(actionResult.message, context);
  for (const effect of actionResult.effects) {
    await context.effectHandlers?.[effect.type]?.(effect, actionResult);
  }
  return actionResult;
}

export function presentPlatformActionSuccess(
  result: unknown,
  context: PlatformActionResultFeedbackContext = {},
) {
  presentPlatformSuccess(resolvePlatformActionResult(result, context).message, context);
}

function actionResultMessage(result: unknown): string | undefined {
  if (!result || typeof result !== 'object' || !('message' in result)) {
    return undefined;
  }
  const message = (result as { message?: unknown }).message;
  return typeof message === 'string' && message.trim() ? message : undefined;
}

function actionResultType(result: unknown): string | undefined {
  if (!result || typeof result !== 'object' || !('resultType' in result)) {
    return undefined;
  }
  const resultType = (result as { resultType?: unknown }).resultType;
  return typeof resultType === 'string' && resultType.trim() ? resultType : undefined;
}

function actionResultEffects(result: unknown): PlatformActionResultEffect[] {
  if (!result || typeof result !== 'object' || !('effects' in result)) {
    return [];
  }
  const effects = (result as { effects?: unknown }).effects;
  if (!Array.isArray(effects)) {
    return [];
  }
  return effects.filter(isPlatformActionResultEffect);
}

function isPlatformActionResultEffect(effect: unknown): effect is PlatformActionResultEffect {
  if (!effect || typeof effect !== 'object' || !('type' in effect)) {
    return false;
  }
  const type = (effect as { type?: unknown }).type;
  if (typeof type !== 'string' || !type.trim()) {
    return false;
  }
  const payload = (effect as { payload?: unknown }).payload;
  return payload === undefined || (typeof payload === 'object' && payload !== null && !Array.isArray(payload));
}
