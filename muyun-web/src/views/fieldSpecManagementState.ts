import { computed } from 'vue';
import type { FieldSpec } from '@muyun/web-contracts';
import type { ModuleContext } from '@muyun/web-core';
import type { UiConfirmOptions } from '@muyun/vue-ui-antdv';
import {
  type StaticCrudActionErrorHandler,
  useFlatCrudManagementState,
} from '../platform-components/staticCrudManagementState';
import type { PlatformActionResultReactionHandler } from '../platform-components/platformActionResultFeedback';

type CardMode = 'view' | 'edit' | 'create';
type ConfirmAction = (options: UiConfirmOptions) => Promise<boolean>;

export interface FieldSpecManagementStateOptions {
  actionErrorHandlers?: StaticCrudActionErrorHandler<FieldSpec>[];
  actionResultReactionHandlers?: Record<string, PlatformActionResultReactionHandler | undefined>;
}

export function createFieldSpecManagementState(
  fieldSpecContext: ModuleContext<FieldSpec>,
  confirmAction: ConfirmAction,
  options: FieldSpecManagementStateOptions = {},
) {
  const state = useFlatCrudManagementState<FieldSpec>({
    context: fieldSpecContext,
    confirmAction,
    emptyDraft,
    normalizeDraft: normalizedDraft,
    copyRecord,
    titleOf: applicationTitleOf,
    fallbackTitle: '字段规格详情',
    createTitle: '新建字段规格',
    requiredMessage: '规格 alias 和规格名称不能为空',
    isValid: (record) => Boolean(record.id && record.title),
    recordName: '字段规格',
    deleteTitle: '删除字段规格',
    saveDeniedMessage: '当前用户无权保存字段规格',
    createDeniedMessage: '当前用户无权新建字段规格',
    enableDeniedMessage: '当前用户无权变更字段规格启停状态',
    deleteDeniedMessage: () => '当前用户无权删除字段规格',
    actionErrorHandlers: options.actionErrorHandlers,
    actionResultReactionHandlers: options.actionResultReactionHandlers,
  });
  const specAliasReadonly = computed(() => state.mode.value !== 'create');
  return {
    ...state,
    specAliasReadonly,
  };
}

function applicationTitleOf(record: FieldSpec) {
  return record.title ?? record.alias ?? record.id ?? '字段规格详情';
}

function copyRecord(record: FieldSpec): FieldSpec {
  const alias = applicationAliasOf(record);
  return { ...record, alias };
}

function emptyDraft(): FieldSpec {
  return {
    alias: '',
    title: '',
    enabled: true,
  };
}

function normalizedDraft(record: FieldSpec, selected: FieldSpec | undefined, mode: CardMode): FieldSpec {
  const alias = mode === 'create' ? record.alias?.trim() : applicationAliasOf(selected ?? record);
  return {
    ...record,
    id: alias,
    alias,
    title: record.title?.trim(),
  };
}

function applicationAliasOf(record: FieldSpec) {
  return record.alias?.trim() || record.id?.trim();
}
