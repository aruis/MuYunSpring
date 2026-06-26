import { computed } from 'vue';
import type { Tenant } from '@muyun/web-contracts';
import type { ModuleContext } from '@muyun/web-core';
import type { UiConfirmOptions } from '@muyun/vue-ui-antdv';
import {
  type StaticCrudActionErrorHandler,
  useFlatCrudManagementState,
} from '../platform-components/staticCrudManagementState';

type CardMode = 'view' | 'edit' | 'create';
type ConfirmAction = (options: UiConfirmOptions) => Promise<boolean>;
const PLATFORM_TENANT_ID = 'platform';

export interface TenantManagementStateOptions {
  actionErrorHandlers?: StaticCrudActionErrorHandler<Tenant>[];
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
    deleteDeniedMessage: (record) =>
      isPlatformTenantRecord(record) ? '平台租户不能删除' : '当前用户无权删除租户',
    canDeleteRecord: (record) => !isPlatformTenantRecord(record),
    canEnableRecord: (record, actionCode) => !(isPlatformTenantRecord(record) && actionCode === 'disable'),
    validateBeforeSave: (record) =>
      record.id === PLATFORM_TENANT_ID && record.enabled === false ? '平台租户不能停用' : undefined,
    actionErrorHandlers: options.actionErrorHandlers,
  });
  const aliasReadonly = computed(() => state.mode.value !== 'create');
  const isPlatformTenant = computed(() => isPlatformTenantRecord(state.selected.value));
  return {
    ...state,
    aliasReadonly,
    isPlatformTenant,
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

function isPlatformTenantRecord(record: Tenant | undefined) {
  return tenantAliasOf(record) === PLATFORM_TENANT_ID;
}
