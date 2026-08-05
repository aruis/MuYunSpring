import { computed } from 'vue';
import type { FieldUiControl } from '@muyun/web-contracts';
import type { ModuleContext } from '@muyun/web-core';
import type { UiConfirmOptions } from '@muyun/vue-ui-antdv';
import {
  type StaticCrudActionErrorHandler,
  useFlatCrudManagementState,
} from '../platform-components/staticCrudManagementState';
import type { PlatformActionResultReactionHandler } from '../platform-components/platformActionResultFeedback';

type CardMode = 'view' | 'edit' | 'create';
type ConfirmAction = (options: UiConfirmOptions) => Promise<boolean>;

export interface FieldUiControlManagementStateOptions {
  actionErrorHandlers?: StaticCrudActionErrorHandler<FieldUiControl>[];
  actionResultReactionHandlers?: Record<string, PlatformActionResultReactionHandler | undefined>;
}

export function createFieldUiControlManagementState(
  fieldUiControlContext: ModuleContext<FieldUiControl>,
  confirmAction: ConfirmAction,
  options: FieldUiControlManagementStateOptions = {},
) {
  const state = useFlatCrudManagementState<FieldUiControl>({
    context: fieldUiControlContext,
    confirmAction,
    emptyDraft,
    normalizeDraft: normalizedDraft,
    copyRecord,
    titleOf: fieldUiControlTitleOf,
    fallbackTitle: '字段 UI 控件详情',
    createTitle: '新建字段 UI 控件',
    requiredMessage: '控件 alias 和控件名称不能为空',
    isValid: (record) => Boolean(record.id && record.title),
    recordName: '字段 UI 控件',
    deleteTitle: '删除字段 UI 控件',
    saveDeniedMessage: '当前用户无权保存字段 UI 控件',
    createDeniedMessage: '当前用户无权新建字段 UI 控件',
    enableDeniedMessage: '当前用户无权变更字段 UI 控件启停状态',
    deleteDeniedMessage: () => '当前用户无权删除字段 UI 控件',
    actionErrorHandlers: options.actionErrorHandlers,
    actionResultReactionHandlers: options.actionResultReactionHandlers,
  });
  const controlAliasReadonly = computed(() => state.mode.value !== 'create');
  return {
    ...state,
    controlAliasReadonly,
  };
}

function fieldUiControlTitleOf(record: FieldUiControl) {
  return record.title ?? record.alias ?? record.id ?? '字段 UI 控件详情';
}

function copyRecord(record: FieldUiControl): FieldUiControl {
  const alias = fieldUiControlAliasOf(record);
  return { ...record, alias };
}

function emptyDraft(): FieldUiControl {
  return {
    alias: '',
    title: '',
    enabled: true,
  };
}

function normalizedDraft(
  record: FieldUiControl,
  selected: FieldUiControl | undefined,
  mode: CardMode,
): FieldUiControl {
  const alias = mode === 'create' ? record.alias?.trim() : fieldUiControlAliasOf(selected ?? record);
  return {
    ...record,
    id: alias,
    alias,
    title: record.title?.trim(),
  };
}

function fieldUiControlAliasOf(record: FieldUiControl) {
  return record.alias?.trim() || record.id?.trim();
}
