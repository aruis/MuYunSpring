import { presentPlatformSuccess, type PlatformErrorFeedbackContext } from './platformErrorFeedback';
import {
  resolvePlatformActionResult,
  type PlatformActionResultReactionHandler,
} from './platformActionResultReactions';

export {
  createPlatformActionResultReactionHandlers,
  mergePlatformActionResultReactionHandlers,
  platformActionResultReactions,
  platformActionResultReactionTypes,
  resolvePlatformActionResult,
  resolvePlatformActionResultMessage,
  withPlatformActionResultReactions,
} from './platformActionResultReactions';
export type {
  PlatformActionResult,
  PlatformActionResultReaction,
  PlatformActionResultReactionHandler,
  PlatformActionResultReactionPayload,
  PlatformActionResultReactionType,
  PlatformActionResultStandardReactionHandlers,
} from './platformActionResultReactions';

export interface PlatformActionResultFeedbackContext extends PlatformErrorFeedbackContext {
  fallbackMessage?: string;
}

export interface PlatformActionResultHandlingContext extends PlatformActionResultFeedbackContext {
  reactionHandlers?: Record<string, PlatformActionResultReactionHandler | undefined>;
}

export async function handlePlatformActionSuccess(
  result: unknown,
  context: PlatformActionResultHandlingContext = {},
) {
  const actionResult = resolvePlatformActionResult(result, {
    fallbackMessage: context.fallbackMessage,
  });
  presentPlatformSuccess(actionResult.message, context);
  for (const reaction of actionResult.reactions) {
    await context.reactionHandlers?.[reaction.type]?.(reaction, actionResult);
  }
  return actionResult;
}

export function presentPlatformActionSuccess(
  result: unknown,
  context: PlatformActionResultFeedbackContext = {},
) {
  presentPlatformSuccess(resolvePlatformActionResult(result, context).message, context);
}
