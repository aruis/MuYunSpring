import {
  resolveWebActionResult,
  resolveWebActionResultMessage,
  type ResolvedWebActionResult,
} from '@muyun/web-core';

export const platformActionResultReactionTypes = {
  refreshList: 'refresh-list',
  refreshDetail: 'refresh-detail',
  closeEditor: 'close-editor',
  clearSelection: 'clear-selection',
  selectRecord: 'select-record',
} as const;

export type PlatformActionResultReactionType =
  (typeof platformActionResultReactionTypes)[keyof typeof platformActionResultReactionTypes];

export interface PlatformActionResultReactionPayload {
  moduleAlias?: string;
  resourceKey?: string;
  scope?: string;
  recordId?: string;
  [key: string]: unknown;
}

export interface PlatformActionResultReaction {
  type: string;
  payload?: PlatformActionResultReactionPayload;
}

export const platformActionResultReactions = {
  refreshList(payload?: PlatformActionResultReactionPayload): PlatformActionResultReaction {
    return reactionOf(platformActionResultReactionTypes.refreshList, payload);
  },
  refreshDetail(payload?: PlatformActionResultReactionPayload): PlatformActionResultReaction {
    return reactionOf(platformActionResultReactionTypes.refreshDetail, payload);
  },
  closeEditor(payload?: PlatformActionResultReactionPayload): PlatformActionResultReaction {
    return reactionOf(platformActionResultReactionTypes.closeEditor, payload);
  },
  clearSelection(payload?: PlatformActionResultReactionPayload): PlatformActionResultReaction {
    return reactionOf(platformActionResultReactionTypes.clearSelection, payload);
  },
  selectRecord(payload?: PlatformActionResultReactionPayload): PlatformActionResultReaction {
    return reactionOf(platformActionResultReactionTypes.selectRecord, payload);
  },
};

export const resolvePlatformActionResultMessage = resolveWebActionResultMessage;

export interface PlatformActionResult extends ResolvedWebActionResult {
  reactions: PlatformActionResultReaction[];
}

export type PlatformActionResultReactionHandler = (
  reaction: PlatformActionResultReaction,
  result: PlatformActionResult,
) => void | Promise<void>;

export interface PlatformActionResultStandardReactionHandlers {
  refreshList?: PlatformActionResultReactionHandler;
  refreshDetail?: PlatformActionResultReactionHandler;
  closeEditor?: PlatformActionResultReactionHandler;
  clearSelection?: PlatformActionResultReactionHandler;
  selectRecord?: PlatformActionResultReactionHandler;
}

export function createPlatformActionResultReactionHandlers(
  handlers: PlatformActionResultStandardReactionHandlers,
) {
  return {
    [platformActionResultReactionTypes.refreshList]: handlers.refreshList,
    [platformActionResultReactionTypes.refreshDetail]: handlers.refreshDetail,
    [platformActionResultReactionTypes.closeEditor]: handlers.closeEditor,
    [platformActionResultReactionTypes.clearSelection]: handlers.clearSelection,
    [platformActionResultReactionTypes.selectRecord]: handlers.selectRecord,
  };
}

export function mergePlatformActionResultReactionHandlers(
  defaultHandlers: Record<string, PlatformActionResultReactionHandler | undefined>,
  customHandlers: Record<string, PlatformActionResultReactionHandler | undefined> | undefined,
) {
  const reactionTypes = new Set([...Object.keys(defaultHandlers), ...Object.keys(customHandlers ?? {})]);
  const handlers: Record<string, PlatformActionResultReactionHandler | undefined> = {};
  for (const reactionType of reactionTypes) {
    const defaultHandler = defaultHandlers[reactionType];
    const customHandler = customHandlers?.[reactionType];
    handlers[reactionType] = async (reaction, result) => {
      await defaultHandler?.(reaction, result);
      await customHandler?.(reaction, result);
    };
  }
  return handlers;
}

export function withPlatformActionResultReactions<T>(
  result: T,
  reactions: PlatformActionResultReaction[],
): T & { reactions: PlatformActionResultReaction[] } {
  const existingReactions = actionResultReactions(result);
  const existingKeys = new Set(existingReactions.map(platformActionResultReactionKey));
  const missingReactions = reactions.filter(
    (reaction) => !existingKeys.has(platformActionResultReactionKey(reaction)),
  );
  if (result && typeof result === 'object') {
    return {
      ...result,
      reactions: [...existingReactions, ...missingReactions],
    };
  }
  return {
    message: undefined,
    raw: result,
    reactions: missingReactions,
  } as unknown as T & { reactions: PlatformActionResultReaction[] };
}

export function resolvePlatformActionResult(
  result: unknown,
  options: { fallbackMessage?: string } = {},
): PlatformActionResult {
  return {
    ...resolveWebActionResult(result, options),
    reactions: actionResultReactions(result),
  };
}

function actionResultReactions(result: unknown): PlatformActionResultReaction[] {
  if (!result || typeof result !== 'object' || !('reactions' in result)) {
    return [];
  }
  const reactions = (result as { reactions?: unknown }).reactions;
  if (!Array.isArray(reactions)) {
    return [];
  }
  return reactions.filter(isPlatformActionResultReaction);
}

function isPlatformActionResultReaction(reaction: unknown): reaction is PlatformActionResultReaction {
  if (!reaction || typeof reaction !== 'object' || !('type' in reaction)) {
    return false;
  }
  const type = (reaction as { type?: unknown }).type;
  if (typeof type !== 'string' || !type.trim()) {
    return false;
  }
  const payload = (reaction as { payload?: unknown }).payload;
  return (
    payload === undefined || (typeof payload === 'object' && payload !== null && !Array.isArray(payload))
  );
}

function platformActionResultReactionKey(reaction: PlatformActionResultReaction) {
  const moduleAlias = payloadString(reaction.payload, 'moduleAlias');
  const recordId = payloadString(reaction.payload, 'recordId');
  const resourceKey = payloadString(reaction.payload, 'resourceKey');
  const scope = payloadString(reaction.payload, 'scope');
  if (!moduleAlias && !recordId && !resourceKey && !scope) {
    return reaction.type;
  }
  return [
    reaction.type,
    `moduleAlias:${moduleAlias ?? ''}`,
    `recordId:${recordId ?? ''}`,
    `resourceKey:${resourceKey ?? ''}`,
    `scope:${scope ?? ''}`,
  ].join('|');
}

function payloadString(
  payload: PlatformActionResultReactionPayload | undefined,
  key: 'moduleAlias' | 'recordId' | 'resourceKey' | 'scope',
) {
  const value = payload?.[key];
  return typeof value === 'string' && value.trim() ? value : undefined;
}

function reactionOf(type: PlatformActionResultReactionType, payload?: PlatformActionResultReactionPayload) {
  return payload ? { type, payload } : { type };
}
