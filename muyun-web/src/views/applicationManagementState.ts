import { computed } from 'vue';
import type { Application } from '@muyun/web-contracts';
import type { ModuleContext } from '@muyun/web-core';
import type { UiConfirmOptions } from '@muyun/vue-ui-antdv';
import {
  type StaticCrudActionErrorHandler,
  useFlatCrudManagementState,
} from '../platform-components/staticCrudManagementState';

type CardMode = 'view' | 'edit' | 'create';
type ConfirmAction = (options: UiConfirmOptions) => Promise<boolean>;

export interface ApplicationManagementStateOptions {
  actionErrorHandlers?: StaticCrudActionErrorHandler<Application>[];
}

export function createApplicationManagementState(
  applicationContext: ModuleContext<Application>,
  confirmAction: ConfirmAction,
  options: ApplicationManagementStateOptions = {},
) {
  const state = useFlatCrudManagementState<Application>({
    context: applicationContext,
    confirmAction,
    emptyDraft,
    normalizeDraft: normalizedDraft,
    copyRecord,
    titleOf: applicationTitleOf,
    fallbackTitle: '应用详情',
    createTitle: '新建应用',
    requiredMessage: '应用 alias 和应用名称不能为空',
    isValid: (record) => Boolean(record.id && record.title),
    recordName: '应用',
    deleteTitle: '删除应用',
    saveDeniedMessage: '当前用户无权保存应用',
    createDeniedMessage: '当前用户无权新建应用',
    enableDeniedMessage: '当前用户无权变更应用启停状态',
    deleteDeniedMessage: () => '当前用户无权删除应用',
    actionErrorHandlers: options.actionErrorHandlers,
  });
  const aliasReadonly = computed(() => state.mode.value !== 'create');
  return {
    ...state,
    aliasReadonly,
  };
}

function applicationTitleOf(record: Application) {
  return record.title ?? record.alias ?? record.id ?? '应用详情';
}

function copyRecord(record: Application): Application {
  const alias = applicationAliasOf(record);
  return { ...record, alias };
}

function emptyDraft(): Application {
  return {
    alias: '',
    title: '',
    enabled: true,
  };
}

function normalizedDraft(
  record: Application,
  selected: Application | undefined,
  mode: CardMode,
): Application {
  const alias = mode === 'create' ? record.alias?.trim() : applicationAliasOf(selected ?? record);
  return {
    ...record,
    id: alias,
    alias,
    title: record.title?.trim(),
  };
}

function applicationAliasOf(record: Application) {
  return record.alias?.trim() || record.id?.trim();
}
