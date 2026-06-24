import type { ModuleContext } from '@muyun/web-core';

export interface RecordActionItem {
  key?: string;
  actionCode?: string;
  title: string;
  visible?: boolean;
  disabled?: boolean;
  loading?: boolean;
  primary?: boolean;
  danger?: boolean;
}

export interface ResolvedRecordActionItem extends RecordActionItem {
  key: string;
  actionCode?: string;
  authorized: boolean;
  disabled: boolean;
  loading: boolean;
}

export function resolveRecordActions(
  context: Pick<ModuleContext<unknown>, 'can'>,
  actions: RecordActionItem[],
  defaultLoading = false,
): ResolvedRecordActionItem[] {
  return actions
    .filter((action) => action.visible !== false)
    .map((action, index) => {
      const authorized = action.actionCode ? context.can(action.actionCode) === true : true;
      const loading = action.loading ?? defaultLoading;
      return {
        ...action,
        key: action.key ?? action.actionCode ?? `action-${index}`,
        authorized,
        disabled: loading || action.disabled === true || !authorized,
        loading,
      };
    });
}
