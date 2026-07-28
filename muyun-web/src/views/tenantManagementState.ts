import { computed } from 'vue';
import type { Tenant } from '@muyun/web-contracts';
import type { ModuleContext } from '@muyun/web-core';
import type { UiConfirmOptions } from '@muyun/vue-ui-antdv';
import {
  type StaticCrudActionErrorHandler,
  useFlatCrudManagementState,
} from '../platform-components/staticCrudManagementState';
import type { PlatformActionResultReactionHandler } from '../platform-components/platformActionResultFeedback';

type CardMode = 'view' | 'edit' | 'create';
type ConfirmAction = (options: UiConfirmOptions) => Promise<boolean>;

export interface TenantManagementStateOptions {
  actionErrorHandlers?: StaticCrudActionErrorHandler<Tenant>[];
  actionResultReactionHandlers?: Record<string, PlatformActionResultReactionHandler | undefined>;
}

export function createTenantManagementState(
  tenantContext: ModuleContext<Tenant>,
  confirmAction: ConfirmAction,
  options: TenantManagementStateOptions = {},
) {
  const state = useFlatCrudManagementState<Tenant>({
    context: tenantContext,
    confirmAction,
    emptyDraft,
    normalizeDraft: normalizedDraft,
    copyRecord,
    titleOf: tenantTitleOf,
    fallbackTitle: '租户详情',
    createTitle: '新建租户',
    requiredMessage: '租户 alias 和租户名称不能为空',
    isValid: (record) => Boolean(record.id && record.title),
    recordName: '租户',
    deleteTitle: '删除租户',
    saveDeniedMessage: '当前用户无权保存租户',
    createDeniedMessage: '当前用户无权新建租户',
    enableDeniedMessage: '当前用户无权变更租户启停状态',
    deleteDeniedMessage: () => '当前用户无权删除租户',
    actionErrorHandlers: options.actionErrorHandlers,
    actionResultReactionHandlers: options.actionResultReactionHandlers,
  });
  const aliasReadonly = computed(() => state.mode.value !== 'create');
  return {
    ...state,
    aliasReadonly,
  };
}

function tenantTitleOf(record: Tenant) {
  return record.title ?? record.alias ?? record.id ?? '租户详情';
}

function copyRecord(record: Tenant): Tenant {
  const alias = tenantAliasOf(record);
  return { ...record, alias };
}

function emptyDraft(): Tenant {
  return {
    alias: '',
    title: '',
    enabled: true,
  };
}

function normalizedDraft(record: Tenant, selected: Tenant | undefined, mode: CardMode): Tenant {
  const alias = mode === 'create' ? record.alias?.trim() : tenantAliasOf(selected ?? record);
  return {
    ...record,
    id: alias,
    alias,
    title: record.title?.trim(),
  };
}

function tenantAliasOf(record: Tenant | undefined) {
  return record?.alias?.trim() || record?.id?.trim();
}
