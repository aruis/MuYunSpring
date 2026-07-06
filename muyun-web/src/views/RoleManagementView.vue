<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import {
  CrudRecordListExplorer,
  RecordActionBar,
  RecordDetailDrawer,
  RecordDetailFields,
  RecordExplorerPanel,
  RecordFormFields,
  RecordMetaSection,
  RecordQueryListPanel,
  RecordStatusSwitch,
  TreeRecordExplorer,
  createScopedTreeModuleContext,
  executeStaticFormSave,
  executeStaticRecordAction,
  presentPlatformError,
  presentPlatformMessage,
  resolveRecordFormFieldState,
  resolveRecordFormFields,
  type CrudRecordListBase,
  type QueryListRecord,
  type RecordActionItem,
  type RecordExplorerItemDescriptor,
  type RecordFormFieldFallback,
  type RecordFormRecord,
  type ResolvedRecordActionItem,
  type TreeRecordBase,
} from '@muyun/platform-components';
import {
  UiButton,
  UiEmpty,
  UiError,
  UiInput,
  UiRecordExplorerItem,
  UiSpin,
  confirmAction,
} from '@muyun/vue-ui-antdv';
import type {
  Organization,
  Role,
  RoleAssignmentType,
  RoleKind,
  RoleOwnerScopeType,
  RoleSharePolicy,
  Tenant,
  WebQueryRequest,
} from '@muyun/web-contracts';
import { useModuleContext, type ModuleContext } from '@muyun/web-core';
import { useCurrentUserContext } from '../app/currentUserContext';

defineOptions({ name: 'RoleManagementView' });

type RoleDetailMode = 'view' | 'create' | 'edit';
type RoleScopeKind = RoleOwnerScopeType;
type RoleFormFieldName =
  | 'title'
  | 'assignmentType'
  | 'roleKind'
  | 'memberRoleIds'
  | 'ownerScopeType'
  | 'ownerScopeId'
  | 'sharePolicy'
  | 'description'
  | 'enabled'
  | 'sortOrder';

interface RoleScope {
  kind: RoleScopeKind;
  id?: string;
  key: string;
  title: string;
  tenant?: Tenant;
  organization?: Organization;
}

const tenantContext = useModuleContext<Tenant>({ moduleAlias: 'iam.tenant' });
const organizationContext = useModuleContext<Organization>({ moduleAlias: 'iam.organization' });
const roleContext = useModuleContext<Role>({ moduleAlias: 'iam.role' });
const currentUser = useCurrentUserContext();
const tenantSearchKeyword = ref('');
const organizationSearchKeyword = ref('');
const tenantReloadKey = ref(0);
const organizationReloadKey = ref(0);
const roleReloadKey = ref(0);
const selectedTenant = ref<Tenant>();
const selectedScope = ref<RoleScope>();
const selectedRoleKey = ref<string>();
const selectedRole = ref<Role>();
const roleDetailOpen = ref(false);
const roleDetailMode = ref<RoleDetailMode>('view');
const loadingRoleDetail = ref(false);
const roleDetailLoadFailed = ref(false);
const savingRole = ref(false);
const roleDetailRequestSeq = ref(0);
const roleDraft = ref<Partial<Role>>(createRoleDraft(undefined));
const roleFormFieldDefinitions = ref(resolveRecordFormFields(undefined));

const tenantListContext = computed(() => tenantContext as unknown as ModuleContext<CrudRecordListBase>);
const selectedTenantId = computed(() => selectedTenant.value?.id);
const canSelectPlatformScope = computed(() => currentUser?.value?.system === true);
const organizationTreeContext = computed(() =>
  createScopedTreeModuleContext(organizationContext, {
    scopeFieldName: 'tenantId',
    scopeValue: selectedTenantId.value,
    treePath: '/iam.organization/tree',
  }),
);
const roleListContext = computed(
  () => createScopedRoleModuleContext(roleContext, selectedScope.value) as ModuleContext<QueryListRecord>,
);
const roleListReady = computed(() => Boolean(selectedScope.value));
const roleDetailTitle = computed(() => {
  if (roleDetailMode.value === 'create') {
    return '新建角色';
  }
  return roleTitle(selectedRole.value ?? roleDraft.value);
});
const roleFormDisabled = computed(() => savingRole.value || loadingRoleDetail.value);
const canSaveRole = computed(() => {
  if (loadingRoleDetail.value || !selectedScope.value) {
    return false;
  }
  if (roleDetailMode.value === 'create') {
    return roleContext.can('create') === true;
  }
  return (
    Boolean(selectedRole.value?.id) &&
    roleContext.can('update') === true &&
    !selectedRole.value?.systemManaged
  );
});
const canToggleRole = computed(() => {
  if (!selectedRole.value?.id || loadingRoleDetail.value || selectedRole.value.systemManaged) {
    return false;
  }
  return roleContext.can(roleToggleActionCode(selectedRole.value)) === true;
});
const roleDetailActions = computed<RecordActionItem[]>(() => {
  if (roleDetailMode.value === 'view') {
    if (!selectedRole.value?.id) {
      return [];
    }
    return [
      {
        key: 'edit',
        actionCode: 'update',
        title: '编辑',
        iconName: 'edit',
        disabled: savingRole.value || selectedRole.value.systemManaged === true,
      },
      {
        key: 'delete',
        actionCode: 'delete',
        title: '删除',
        iconName: 'delete',
        danger: true,
        disabled: savingRole.value || selectedRole.value.systemManaged === true,
      },
    ];
  }
  return [
    { key: 'cancel', title: '取消', iconName: 'close', disabled: savingRole.value },
    {
      key: 'save',
      actionCode: roleDetailMode.value === 'create' ? 'create' : 'update',
      title: '保存',
      iconName: 'save',
      primary: true,
      loading: savingRole.value,
      disabled: !canSaveRole.value,
    },
  ];
});
const roleFormFieldFallback = computed<Record<RoleFormFieldName, RecordFormFieldFallback>>(() => ({
  title: { label: '角色名称', required: true, visible: true, placeholder: '请输入角色名称' },
  assignmentType: {
    label: '授权层级',
    required: true,
    visible: true,
    controlType: 'select',
    options: [
      { label: '账号角色', value: 'account' },
      { label: '任职角色', value: 'employment' },
    ],
  },
  roleKind: {
    label: '角色类型',
    required: true,
    visible: true,
    controlType: 'select',
    options: [
      { label: '标准角色', value: 'standard' },
      { label: '角色组', value: 'group' },
      { label: '数据授权角色', value: 'dataGrant' },
      { label: '系统角色', value: 'system' },
    ],
  },
  memberRoleIds: {
    label: '成员角色',
    visible: roleDraft.value.roleKind === 'group',
    placeholder: '多个角色 ID 用逗号分隔',
  },
  ownerScopeType: {
    label: '归属范围',
    required: true,
    visible: true,
    readOnly: true,
    controlType: 'select',
    options: [
      { label: '平台', value: 'platform' },
      { label: '租户', value: 'tenant' },
      { label: '机构', value: 'organization' },
    ],
  },
  ownerScopeId: { label: '归属对象', visible: true, readOnly: true },
  sharePolicy: {
    label: '公开策略',
    required: true,
    visible: true,
    controlType: 'select',
    options: sharePolicyOptions(selectedScope.value?.kind ?? roleDraft.value.ownerScopeType),
  },
  description: { label: '说明', visible: true, placeholder: '请输入角色说明' },
  enabled: { label: '启用状态', visible: true, controlType: 'enabledStatus' },
  sortOrder: { label: '排序号', visible: true, placeholder: '请输入排序号' },
}));
const roleFormFieldNames = computed<RoleFormFieldName[]>(() => [
  'title',
  'assignmentType',
  'roleKind',
  'memberRoleIds',
  'ownerScopeType',
  'ownerScopeId',
  'sharePolicy',
  'description',
  'enabled',
  'sortOrder',
]);

onMounted(loadRoleFormDefinition);

watch(selectedScope, () => {
  selectedRoleKey.value = undefined;
  selectedRole.value = undefined;
  roleDraft.value = createRoleDraft(selectedScope.value);
  closeRoleDetail();
  roleReloadKey.value += 1;
});

async function loadRoleFormDefinition() {
  try {
    const runtimeContext = await roleContext.runtime.ready;
    roleFormFieldDefinitions.value = resolveRecordFormFields(runtimeContext.uiDescriptor);
  } catch (cause) {
    presentPlatformError(cause, { source: 'role-management', phase: 'load' });
  }
}

function createScopedRoleModuleContext(
  context: ModuleContext<Role>,
  scope: RoleScope | undefined,
): ModuleContext<Role> {
  return {
    ...context,
    crud: {
      ...context.crud,
      query: (request) => {
        if (!scope) {
          return context.crud.query(request);
        }
        return context.crud.query(scopedRoleQuery(request, scope));
      },
    },
  };
}

function scopedRoleQuery(request: WebQueryRequest | undefined, scope: RoleScope): WebQueryRequest {
  const conditions = [
    ...(request?.conditions ?? []),
    { fieldName: 'ownerScopeType', operator: 'EQ', values: [scope.kind] },
  ];
  if (scope.kind !== 'platform' && scope.id) {
    conditions.push({ fieldName: 'ownerScopeId', operator: 'EQ', values: [scope.id] });
  }
  return { ...request, conditions };
}

function roleFormFieldDisabled(fieldName: string) {
  if (fieldName === 'ownerScopeType' || fieldName === 'ownerScopeId') {
    return true;
  }
  if (roleDetailMode.value === 'edit' && ['assignmentType', 'roleKind'].includes(fieldName)) {
    return true;
  }
  return selectedRole.value?.systemManaged === true;
}

function updateRoleDraftField(fieldName: string, value: string | number | boolean | undefined) {
  const next = {
    ...roleDraft.value,
    [fieldName]: value,
  };
  if (fieldName === 'roleKind' && (value === 'group' || value === 'dataGrant')) {
    next.assignmentType = 'employment';
  }
  if (
    fieldName === 'assignmentType' &&
    next.roleKind &&
    next.roleKind !== 'standard' &&
    value !== 'employment'
  ) {
    next.assignmentType = 'employment';
  }
  roleDraft.value = next;
}

function handleTenantsLoaded(records: CrudRecordListBase[]) {
  if (!selectedScope.value && canSelectPlatformScope.value) {
    selectPlatformScope();
    return;
  }
  if (!selectedTenant.value && records.length > 0) {
    selectTenant(records[0] as Tenant);
  }
}

function selectPlatformScope() {
  if (!canLeaveRoleDetailContext() || !canSelectPlatformScope.value) {
    return;
  }
  selectedTenant.value = undefined;
  selectedScope.value = {
    kind: 'platform',
    key: 'platform',
    title: '平台角色',
  };
}

function selectTenant(record: Tenant) {
  if (!canLeaveRoleDetailContext()) {
    return;
  }
  selectedTenant.value = record;
  selectTenantRootScope(record);
  organizationReloadKey.value += 1;
}

function selectTenantRootScope(record = selectedTenant.value) {
  if (!record?.id || !canLeaveRoleDetailContext()) {
    return;
  }
  selectedScope.value = {
    kind: 'tenant',
    id: record.id,
    key: `tenant:${record.id}`,
    title: `${tenantTitle(record)} / 租户本级`,
    tenant: record,
  };
}

function selectOrganizationScope(record: Organization) {
  if (!record.id || !canLeaveRoleDetailContext()) {
    return;
  }
  selectedScope.value = {
    kind: 'organization',
    id: record.id,
    key: `organization:${record.id}`,
    title: organizationTitle(record),
    tenant: selectedTenant.value,
    organization: record,
  };
}

function handleRoleListAction(action: RecordActionItem) {
  if (action.key === 'create') {
    startCreateRole();
  }
}

function handleRoleRowAction(action: ResolvedRecordActionItem, record: QueryListRecord) {
  if (!canLeaveRoleDetailContext()) {
    return;
  }
  if (action.key === 'view') {
    void openRoleDetail(record, 'view');
    return;
  }
  if (action.key === 'edit') {
    void openRoleDetail(record, 'edit');
    return;
  }
  if (action.key === 'delete') {
    void removeRole(record);
  }
}

function handleRoleRowDblclick(record: QueryListRecord) {
  if (!canLeaveRoleDetailContext()) {
    return;
  }
  void openRoleDetail(record, 'view');
}

function startCreateRole() {
  if (!canLeaveRoleDetailContext()) {
    return;
  }
  if (!selectedScope.value) {
    presentPlatformMessage('请先选择角色归属范围', { phase: 'validation' });
    return;
  }
  selectedRole.value = undefined;
  selectedRoleKey.value = undefined;
  roleDraft.value = createRoleDraft(selectedScope.value);
  roleDetailMode.value = 'create';
  loadingRoleDetail.value = false;
  roleDetailLoadFailed.value = false;
  roleDetailRequestSeq.value += 1;
  roleDetailOpen.value = true;
}

async function openRoleDetail(record: QueryListRecord, mode: RoleDetailMode) {
  if (!canLeaveRoleDetailContext()) {
    return;
  }
  const id = String(record.id ?? '');
  if (!id) {
    return;
  }
  selectedRoleKey.value = id;
  roleDetailOpen.value = true;
  roleDetailMode.value = mode;
  selectedRole.value = undefined;
  roleDraft.value = copyRole(record as Role);
  loadingRoleDetail.value = true;
  roleDetailLoadFailed.value = false;
  const requestSeq = roleDetailRequestSeq.value + 1;
  roleDetailRequestSeq.value = requestSeq;
  try {
    const fullRecord = await roleContext.crud.view(id);
    if (!canCommitRoleDetailRequest(id, requestSeq)) {
      return;
    }
    commitRoleDetailRecord(fullRecord);
  } catch (cause) {
    if (canCommitRoleDetailRequest(id, requestSeq)) {
      roleDetailLoadFailed.value = true;
      presentPlatformError(cause, { source: 'role-management', phase: 'load' });
    }
  } finally {
    if (canCommitRoleDetailRequest(id, requestSeq)) {
      loadingRoleDetail.value = false;
    }
  }
}

function closeRoleDetail() {
  if (savingRole.value) {
    return;
  }
  roleDetailRequestSeq.value += 1;
  loadingRoleDetail.value = false;
  roleDetailLoadFailed.value = false;
  roleDetailOpen.value = false;
  roleDetailMode.value = 'view';
  roleDraft.value = selectedRole.value ? copyRole(selectedRole.value) : createRoleDraft(selectedScope.value);
}

function cancelRoleDetail() {
  if (savingRole.value) {
    return;
  }
  if (!selectedRole.value?.id || roleDetailMode.value === 'create') {
    closeRoleDetail();
    return;
  }
  roleDraft.value = copyRole(selectedRole.value);
  roleDetailMode.value = 'view';
  loadingRoleDetail.value = false;
  roleDetailLoadFailed.value = false;
}

function handleRoleDetailAction(action: RecordActionItem) {
  if (action.key === 'cancel') {
    cancelRoleDetail();
    return;
  }
  if (action.key === 'save') {
    void saveRole();
    return;
  }
  if (!canLeaveRoleDetailContext()) {
    return;
  }
  if (action.key === 'edit' && selectedRole.value && !selectedRole.value.systemManaged) {
    roleDraft.value = copyRole(selectedRole.value);
    roleDetailMode.value = 'edit';
    return;
  }
  if (action.key === 'delete') {
    void removeRole(selectedRole.value);
  }
}

function retryRoleDetail() {
  const id = String(roleDraft.value.id ?? selectedRoleKey.value ?? '');
  if (!id) {
    return;
  }
  const mode = roleDetailMode.value === 'create' ? 'view' : roleDetailMode.value;
  void openRoleDetail({ ...roleDraft.value, id } as QueryListRecord, mode);
}

async function saveRole() {
  await executeStaticFormSave<Role>({
    loading: savingRole,
    mode: roleDetailMode.value === 'edit' ? 'edit' : 'create',
    source: 'role-management',
    validateContext: () => (selectedScope.value ? undefined : '请先选择角色归属范围'),
    canSave: () => canSaveRole.value,
    deniedMessage: '当前用户无权保存角色',
    createRecord: () => normalizedRoleDraft(roleDraft.value, selectedScope.value!),
    validateRecord: validateRoleDraft,
    save: (draft, mode) =>
      mode === 'edit' && selectedRole.value?.id
        ? roleContext.crud.update(selectedRole.value.id, draft)
        : roleContext.crud.insert(draft),
    onSaved: ({ record }) => {
      commitRoleDetailRecord(record);
      roleReloadKey.value += 1;
    },
  });
}

async function toggleRoleEnabled() {
  await executeStaticRecordAction({
    loading: savingRole,
    source: 'role-management',
    record: () => (selectedRole.value?.id ? selectedRole.value : undefined),
    canExecute: () => canToggleRole.value,
    deniedMessage: '当前用户无权变更角色启停状态',
    execute: (role) =>
      role.enabled === false ? roleContext.crud.enable(role.id!) : roleContext.crud.disable(role.id!),
    onExecuted: async (_, role) => {
      const refreshed = await roleContext.crud.view(role.id!);
      commitRoleDetailRecord(refreshed);
      roleReloadKey.value += 1;
    },
  });
}

async function removeRole(record: Partial<Role> | QueryListRecord | undefined) {
  await executeStaticRecordAction({
    loading: savingRole,
    source: 'role-management',
    record: () => (record?.id ? record : undefined),
    canExecute: (target) => roleContext.can('delete') === true && (target as Role).systemManaged !== true,
    deniedMessage: '当前用户无权删除角色',
    confirm: (target) =>
      confirmAction({
        title: '删除角色',
        content: `确认删除角色「${roleTitle(target)}」？`,
        okText: '删除',
        danger: true,
      }),
    execute: (target) => roleContext.crud.delete(String(target.id)),
    onExecuted: (_, target) => {
      if (selectedRoleKey.value === String(target.id)) {
        selectedRoleKey.value = undefined;
        selectedRole.value = undefined;
        roleDraft.value = createRoleDraft(selectedScope.value);
        roleDetailOpen.value = false;
        roleDetailMode.value = 'view';
        loadingRoleDetail.value = false;
        roleDetailLoadFailed.value = false;
        roleDetailRequestSeq.value += 1;
      }
      roleReloadKey.value += 1;
    },
  });
}

function canLeaveRoleDetailContext() {
  return !savingRole.value;
}

function canCommitRoleDetailRequest(recordId: string, requestSeq: number) {
  return roleDetailRequestSeq.value === requestSeq && selectedRoleKey.value === recordId;
}

function commitRoleDetailRecord(record: Role) {
  selectedRole.value = record;
  selectedRoleKey.value = record.id;
  roleDraft.value = copyRole(record);
  roleDetailMode.value = 'view';
  roleDetailOpen.value = true;
  loadingRoleDetail.value = false;
  roleDetailLoadFailed.value = false;
  roleDetailRequestSeq.value += 1;
}

function createRoleDraft(scope: RoleScope | undefined): Partial<Role> {
  return {
    tenantId: scopeTenantId(scope),
    ownerScopeType: scope?.kind,
    ownerScopeId: scope?.kind === 'platform' ? undefined : scope?.id,
    assignmentType: 'employment',
    roleKind: 'standard',
    sharePolicy: defaultSharePolicy(),
    enabled: true,
    sortOrder: 100,
  };
}

function copyRole(record: Partial<Role>): Partial<Role> {
  return { ...record };
}

function normalizedRoleDraft(draft: Partial<Role>, scope: RoleScope): Role {
  const roleKind = normalizedRoleKind(draft.roleKind);
  const sharePolicy = normalizedSharePolicy(draft.sharePolicy, scope.kind);
  return {
    ...draft,
    title: draft.title?.trim(),
    assignmentType: normalizedAssignmentType(draft.assignmentType, roleKind),
    roleKind,
    memberRoleIds: draft.memberRoleIds?.trim() || undefined,
    tenantId: scopeTenantId(scope),
    ownerScopeType: scope.kind,
    ownerScopeId: scope.kind === 'platform' ? undefined : scope.id,
    ownerScopeKey: undefined,
    sharePolicy,
    description: draft.description?.trim() || undefined,
    enabled: draft.enabled !== false,
    sortOrder: normalizeSortOrder(draft.sortOrder),
  } as Role;
}

function scopeTenantId(scope: RoleScope | undefined) {
  return scope?.kind === 'platform' ? undefined : scope?.tenant?.id ?? scope?.id;
}

function validateRoleDraft(draft: Role) {
  const requiredFields: RoleFormFieldName[] = [
    'title',
    'assignmentType',
    'roleKind',
    'ownerScopeType',
    'sharePolicy',
  ];
  for (const fieldName of requiredFields) {
    const field = resolveRecordFormFieldState(fieldName, {
      fields: roleFormFieldDefinitions.value,
      fallback: roleFormFieldFallback.value,
    });
    if (field.visible && field.required && !draft[fieldName]) {
      return `请填写${field.label}`;
    }
  }
  if (draft.ownerScopeType !== 'platform' && !draft.ownerScopeId) {
    return '请选择归属对象';
  }
  if (!sharePolicyOptions(draft.ownerScopeType).some((option) => option.value === draft.sharePolicy)) {
    return '请选择当前归属范围可用的公开策略';
  }
  return undefined;
}

function normalizedAssignmentType(
  value: RoleAssignmentType | undefined,
  roleKind: RoleKind,
): RoleAssignmentType {
  if (roleKind === 'group' || roleKind === 'dataGrant') {
    return 'employment';
  }
  return value === 'account' ? 'account' : 'employment';
}

function normalizedRoleKind(value: RoleKind | undefined): RoleKind {
  return value === 'group' || value === 'dataGrant' || value === 'system' ? value : 'standard';
}

function normalizedSharePolicy(
  value: RoleSharePolicy | undefined,
  scopeType: RoleOwnerScopeType | undefined,
): RoleSharePolicy {
  const allowed = sharePolicyOptions(scopeType).map((option) => option.value);
  return allowed.includes(value ?? '') ? (value as RoleSharePolicy) : defaultSharePolicy();
}

function defaultSharePolicy(): RoleSharePolicy {
  return 'private';
}

function normalizeSortOrder(value: unknown) {
  if (typeof value === 'number') {
    return Number.isFinite(value) ? value : 100;
  }
  const parsed = Number(String(value ?? '').trim());
  return Number.isFinite(parsed) ? parsed : 100;
}

function sharePolicyOptions(scopeType: RoleOwnerScopeType | undefined) {
  if (scopeType === 'platform') {
    return [
      { label: '私有', value: 'private' },
      { label: '全局公开', value: 'platform' },
    ];
  }
  if (scopeType === 'organization') {
    return [
      { label: '私有', value: 'private' },
      { label: '本级及下级', value: 'ownerAndChildren' },
    ];
  }
  return [
    { label: '私有', value: 'private' },
    { label: '租户公开', value: 'tenant' },
  ];
}

function roleToggleActionCode(record: Partial<Role>) {
  return record.enabled === false ? 'enable' : 'disable';
}

function roleTitle(record: Partial<Role> | QueryListRecord | undefined) {
  return String(record?.title ?? record?.id ?? '角色');
}

function tenantTitle(record: Tenant | CrudRecordListBase | undefined) {
  return String(record?.title ?? record?.alias ?? record?.id ?? '未命名租户');
}

function organizationTitle(record: Organization | undefined) {
  return String(record?.title ?? record?.code ?? record?.id ?? '未命名机构');
}

function scopeDisplayValue(fieldName: string, value: unknown) {
  if (fieldName === 'ownerScopeType') {
    return ownerScopeTypeTitle(roleDraft.value.ownerScopeType);
  }
  if (fieldName === 'ownerScopeId') {
    return selectedScope.value?.title ?? String(value ?? '');
  }
  return undefined;
}

function ownerScopeTypeTitle(value: RoleOwnerScopeType | undefined) {
  if (value === 'platform') {
    return '平台';
  }
  if (value === 'organization') {
    return '机构';
  }
  return '租户';
}

function tenantItemOf(record: CrudRecordListBase): RecordExplorerItemDescriptor {
  return {
    title: tenantTitle(record),
    secondary: record.alias ?? record.id,
    muted: record.enabled === false,
  };
}

function organizationItemOf(record: TreeRecordBase): RecordExplorerItemDescriptor {
  return {
    title: record.title ?? record.code ?? record.id ?? '未命名机构',
    secondary: record.code ?? record.id,
    muted: record.enabled === false,
  };
}
</script>

<template>
  <section class="role-management-page">
    <RecordExplorerPanel
      class="role-scope-panel"
      title="租户"
      refresh-title="刷新租户列表"
      :search-keyword="tenantSearchKeyword"
      search-placeholder="搜索租户名称、alias 或 ID"
      @refresh="tenantReloadKey += 1"
      @update:search-keyword="tenantSearchKeyword = $event"
    >
      <template #actions>
        <UiButton
          v-if="canSelectPlatformScope"
          :type="selectedScope?.kind === 'platform' ? 'primary' : 'text'"
          icon-name="app"
          title="平台角色"
          @click="selectPlatformScope"
        />
      </template>
      <CrudRecordListExplorer
        :context="tenantListContext"
        :selected-id="selectedTenant?.id"
        :reload-key="tenantReloadKey"
        :keyword="tenantSearchKeyword"
        empty-description="暂无租户"
        loading-tip="加载租户列表"
        fallback-title="未命名租户"
        :item-of="tenantItemOf"
        @loaded="handleTenantsLoaded"
        @select="selectTenant($event as Tenant)"
      />
    </RecordExplorerPanel>

    <RecordExplorerPanel
      class="role-scope-panel"
      title="归属范围"
      refresh-title="刷新机构树"
      :search-keyword="organizationSearchKeyword"
      search-placeholder="搜索机构名称、编码或 ID"
      :searchable="Boolean(selectedTenant)"
      @refresh="organizationReloadKey += 1"
      @update:search-keyword="organizationSearchKeyword = $event"
    >
      <UiEmpty v-if="selectedScope?.kind === 'platform'" description="平台角色不需要选择租户内范围" />
      <UiEmpty v-else-if="!selectedTenant" description="请选择左侧租户" />
      <template v-else>
        <button
          class="role-tenant-root-scope"
          type="button"
          @click="selectTenantRootScope()"
        >
          <UiRecordExplorerItem
            :title="tenantTitle(selectedTenant)"
            secondary="租户本级角色"
            clickable
            :selected="selectedScope?.kind === 'tenant' && selectedScope.id === selectedTenant.id"
          />
        </button>
        <TreeRecordExplorer
          :context="organizationTreeContext"
          :selected-id="selectedScope?.kind === 'organization' ? selectedScope.id : undefined"
          :reload-key="organizationReloadKey"
          :keyword="organizationSearchKeyword"
          search-mode="none"
          search-trigger="external"
          empty-description="当前租户暂无机构"
          loading-tip="加载机构树"
          fallback-title="未命名机构"
          :item-of="organizationItemOf"
          @select="selectOrganizationScope($event as Organization)"
        />
      </template>
    </RecordExplorerPanel>

    <RecordQueryListPanel
      class="role-list-panel"
      :context="roleListContext"
      :title="selectedScope ? `角色列表 - ${selectedScope.title}` : '角色列表'"
      standard-crud-actions
      standard-crud-row-actions
      create-title="新建角色"
      :selected-key="selectedRoleKey"
      :reload-key="roleReloadKey"
      :ready="roleListReady"
      quick-search-placeholder="搜索角色名称或说明"
      empty-description="当前范围暂无角色"
      waiting-description="请选择角色归属范围"
      @action="handleRoleListAction"
      @row-action="handleRoleRowAction"
      @row-dblclick="handleRoleRowDblclick"
      @select="selectedRoleKey = String($event.id ?? '')"
    />

    <RecordDetailDrawer
      :open="roleDetailOpen"
      :title="roleDetailTitle"
      :close-on-outside="roleDetailMode === 'view'"
      @close="closeRoleDetail"
    >
      <template #status>
        <RecordStatusSwitch
          v-if="roleDetailMode === 'view' && selectedRole"
          :enabled="selectedRole.enabled !== false"
          :disabled="savingRole || !canToggleRole"
          :loading="savingRole"
          :show-label="false"
          @change="toggleRoleEnabled"
        />
      </template>
      <template #actions>
        <RecordActionBar
          :context="roleListContext"
          :actions="roleDetailActions"
          @action="handleRoleDetailAction"
        />
      </template>

      <UiSpin v-if="loadingRoleDetail" class="role-detail-state" tip="加载角色详情" />
      <div v-else-if="roleDetailLoadFailed" class="role-detail-state">
        <UiError title="详情加载失败" message="无法加载角色详情，请重试" />
        <UiButton type="primary" icon-name="reload" @click="retryRoleDetail">重试</UiButton>
      </div>

      <template v-else-if="roleDetailMode === 'view' || roleDetailMode === 'create' || selectedRole">
        <RecordDetailFields
          v-if="roleDetailMode === 'view'"
          :record="roleDraft as RecordFormRecord"
          :fields="roleFormFieldDefinitions"
          :fallback="roleFormFieldFallback"
          :display-of="scopeDisplayValue"
        />

        <form v-else class="role-form" @submit.prevent="saveRole">
          <label>
            <span class="role-form-label">当前范围</span>
            <UiInput :value="selectedScope?.title ?? '-'" disabled />
          </label>
          <RecordFormFields
            :record="roleDraft as RecordFormRecord"
            :field-names="roleFormFieldNames"
            :fields="roleFormFieldDefinitions"
            :fallback="roleFormFieldFallback"
            :disabled="roleFormDisabled"
            :disabled-of="roleFormFieldDisabled"
            @update:field="updateRoleDraftField"
          />
        </form>
        <RecordMetaSection v-if="roleDetailMode !== 'create'" :record="roleDraft" show-sort-order />
      </template>
    </RecordDetailDrawer>
  </section>
</template>

<style scoped>
.role-management-page {
  position: relative;
  display: grid;
  grid-template-columns: minmax(220px, 280px) minmax(260px, 340px) minmax(0, 1fr);
  gap: 12px;
  height: calc(100vh - 116px);
  overflow: hidden;
}

.role-scope-panel,
.role-list-panel {
  min-width: 0;
  min-height: 0;
}

  .role-tenant-root-scope {
    display: block;
  width: 100%;
  margin: 0 0 8px;
  padding: 0 0 8px;
  border: 0;
  border-bottom: 1px solid var(--muyun-border);
  background: transparent;
  color: inherit;
  text-align: left;
  cursor: pointer;
}

.role-form {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
}

.role-form > label {
  display: grid;
  gap: 6px;
  color: var(--muyun-text-muted);
  font-size: 13px;
}

.role-form-label {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.role-detail-state {
  display: grid;
  justify-items: center;
  gap: 12px;
  padding: 32px 0;
}

@media (max-width: 1180px) {
  .role-management-page {
    grid-template-columns: minmax(220px, 280px) minmax(0, 1fr);
    grid-template-rows: minmax(0, 0.95fr) minmax(0, 1.3fr);
  }

  .role-list-panel {
    grid-column: 1 / -1;
  }
}

@media (max-width: 760px) {
  .role-management-page {
    grid-template-columns: 1fr;
    grid-template-rows: minmax(180px, 0.65fr) minmax(220px, 0.8fr) minmax(360px, 1fr);
    height: auto;
    min-height: calc(100vh - 116px);
    overflow: visible;
  }

  .role-list-panel {
    grid-column: auto;
  }
}
</style>
