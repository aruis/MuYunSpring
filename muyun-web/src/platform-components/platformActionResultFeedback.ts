import {
  presentPlatformSuccess,
  type PlatformErrorFeedbackContext,
} from './platformErrorFeedback';

export interface PlatformActionResultEffect {
  type: string;
  payload?: Record<string, unknown>;
}

export const platformActionResultEffectTypes = {
  refreshList: 'refresh-list',
  refreshDetail: 'refresh-detail',
  closeEditor: 'close-editor',
  clearSelection: 'clear-selection',
  selectRecord: 'select-record',
} as const;

export type PlatformActionResultEffectType =
  (typeof platformActionResultEffectTypes)[keyof typeof platformActionResultEffectTypes];

export const platformActionResultEffects = {
  refreshList(payload?: Record<string, unknown>): PlatformActionResultEffect {
    return effectOf(platformActionResultEffectTypes.refreshList, payload);
  },
  refreshDetail(payload?: Record<string, unknown>): PlatformActionResultEffect {
    return effectOf(platformActionResultEffectTypes.refreshDetail, payload);
  },
  closeEditor(payload?: Record<string, unknown>): PlatformActionResultEffect {
    return effectOf(platformActionResultEffectTypes.closeEditor, payload);
  },
  clearSelection(payload?: Record<string, unknown>): PlatformActionResultEffect {
    return effectOf(platformActionResultEffectTypes.clearSelection, payload);
  },
  selectRecord(payload?: Record<string, unknown>): PlatformActionResultEffect {
    return effectOf(platformActionResultEffectTypes.selectRecord, payload);
  },
};

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

export interface PlatformActionResultStandardEffectHandlers {
  refreshList?: PlatformActionResultEffectHandler;
  refreshDetail?: PlatformActionResultEffectHandler;
  closeEditor?: PlatformActionResultEffectHandler;
  clearSelection?: PlatformActionResultEffectHandler;
  selectRecord?: PlatformActionResultEffectHandler;
}

export function createPlatformActionResultEffectHandlers(
  handlers: PlatformActionResultStandardEffectHandlers,
) {
  return {
    [platformActionResultEffectTypes.refreshList]: handlers.refreshList,
    [platformActionResultEffectTypes.refreshDetail]: handlers.refreshDetail,
    [platformActionResultEffectTypes.closeEditor]: handlers.closeEditor,
    [platformActionResultEffectTypes.clearSelection]: handlers.clearSelection,
    [platformActionResultEffectTypes.selectRecord]: handlers.selectRecord,
  };
}

export function withPlatformActionResultEffects<T>(
  result: T,
  effects: PlatformActionResultEffect[],
): T & { effects: PlatformActionResultEffect[] } {
  const existingEffects = actionResultEffects(result);
  const existingTypes = new Set(existingEffects.map((effect) => effect.type));
  const missingEffects = effects.filter((effect) => !existingTypes.has(effect.type));
  if (result && typeof result === 'object') {
    return {
      ...result,
      effects: [...existingEffects, ...missingEffects],
    };
  }
  return ({
    message: undefined,
    raw: result,
    effects: missingEffects,
  } as unknown) as T & { effects: PlatformActionResultEffect[] };
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

function effectOf(type: PlatformActionResultEffectType, payload?: Record<string, unknown>) {
  return payload ? { type, payload } : { type };
}
