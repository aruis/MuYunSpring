import type { ModuleContext } from '@muyun/web-core';
import type { UiIconName } from '@muyun/vue-ui-antdv';

export interface RecordActionItem {
  key?: string;
  actionCode?: string;
  title: string;
  visible?: boolean;
  disabled?: boolean;
  loading?: boolean;
  primary?: boolean;
  danger?: boolean;
  iconName?: UiIconName;
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
        iconName: action.iconName ?? defaultActionIcon(action),
        authorized,
        disabled: loading || action.disabled === true || !authorized,
        loading,
      };
    });
}

function defaultActionIcon(action: RecordActionItem): UiIconName | undefined {
  const code = action.actionCode ?? action.key;
  const operation = code?.split('_').at(-1);
  if (operation === 'create') {
    return 'plus';
  }
  if (operation === 'update' || action.key?.includes('edit')) {
    return 'edit';
  }
  if (operation === 'delete') {
    return 'delete';
  }
  if (operation === 'enable' || operation === 'disable' || action.key?.includes('toggle')) {
    return 'power';
  }
  if (action.key?.includes('save')) {
    return 'save';
  }
  if (action.key?.includes('cancel')) {
    return 'close';
  }
  return undefined;
}
