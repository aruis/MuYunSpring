import type { ActionContract } from '@muyun/web-contracts';

export type DynamicLegacyActionRefresh = 'none' | 'record' | 'list' | 'all';

export interface DynamicActionReaction {
  type: 'refresh-list' | 'refresh-detail';
  payload?: {
    moduleAlias?: string;
    recordId?: string;
  };
}

export interface DynamicLegacyRefreshActionContract extends ActionContract {
  refresh?: DynamicLegacyActionRefresh;
}

export function resolveDynamicActionReactions(
  action: ActionContract,
  context: { moduleAlias: string; recordId?: string },
): DynamicActionReaction[] {
  const refresh = legacyRefreshOf(action);
  if (refresh === 'none') {
    return [];
  }
  if (refresh === 'record') {
    return [refreshDetailReaction(context)];
  }
  if (refresh === 'list') {
    return [refreshListReaction(context)];
  }
  return [refreshListReaction(context), refreshDetailReaction(context)];
}

function legacyRefreshOf(action: ActionContract): DynamicLegacyActionRefresh {
  const refresh = (action as DynamicLegacyRefreshActionContract).refresh;
  if (refresh === 'record' || refresh === 'list' || refresh === 'all') {
    return refresh;
  }
  return 'none';
}

function refreshListReaction(context: { moduleAlias: string }): DynamicActionReaction {
  return {
    type: 'refresh-list',
    payload: { moduleAlias: context.moduleAlias },
  };
}

function refreshDetailReaction(context: { moduleAlias: string; recordId?: string }): DynamicActionReaction {
  return {
    type: 'refresh-detail',
    payload: {
      moduleAlias: context.moduleAlias,
      recordId: context.recordId,
    },
  };
}
