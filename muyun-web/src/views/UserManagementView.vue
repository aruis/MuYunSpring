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
  type RecordQueryListColumn,
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
  ResetPasswordResponse,
  Tenant,
  UserAccount,
  WebCountResponse,
  WebQueryRequest,
} from '@muyun/web-contracts';
import { useModuleContext, type ModuleContext } from '@muyun/web-core';
import { useCurrentUserContext } from '../app/currentUserContext';

defineOptions({ name: 'UserManagementView' });

type UserDetailMode = 'view' | 'create' | 'edit' | 'resetPassword';
type UserFormFieldName =
  | 'username'
  | 'title'
  | 'mobile'
  | 'email'
  | 'organizationId'
  | 'enabled'
  | 'sortOrder';

const tenantContext = useModuleContext<Tenant>({ moduleAlias: 'iam.tenant' });
const organizationContext = useModuleContext<Organization>({ moduleAlias: 'iam.organization' });
const userContext = useModuleContext<UserAccount>({ moduleAlias: 'iam.user' });
const currentUser = useCurrentUserContext();
const tenantSearchKeyword = ref('');
const organizationSearchKeyword = ref('');
const tenantReloadKey = ref(0);
const organizationReloadKey = ref(0);
const userReloadKey = ref(0);
const selectedTenant = ref<Tenant>();
const selectedOrganization = ref<Organization>();
const selectedUserKey = ref<string>();
const selectedUser = ref<UserAccount>();
const userDetailOpen = ref(false);
const userDetailMode = ref<UserDetailMode>('view');
const loadingUserDetail = ref(false);
const userDetailLoadFailed = ref(false);
const savingUser = ref(false);
const userDetailRequestSeq = ref(0);
const userDraft = ref<Partial<UserAccount>>(createUserDraft(undefined, undefined));
const passwordDraft = ref('');
const resetPasswordResult = ref<ResetPasswordResponse>();
const userFormFieldDefinitions = ref(resolveRecordFormFields(undefined));

const tenantListContext = computed(() => tenantContext as unknown as ModuleContext<CrudRecordListBase>);
const selectedTenantId = computed(() => selectedTenant.value?.id);
const canBrowseTenants = computed(() => currentUser?.value?.system === true);
const currentUserTenant = computed<Tenant | undefined>(() => {
  const tenantId = currentUser?.value?.tenantId;
  if (currentUser?.value?.system === true || !tenantId) {
    return undefined;
  }
  return {
    id: tenantId,
    title: tenantId,
    alias: tenantId,
    enabled: true,
  } as Tenant;
});
const organizationTreeContext = computed(() =>
  createScopedTreeModuleContext(organizationContext, {
    scopeFieldName: 'tenantId',
    scopeValue: selectedTenantId.value,
    treePath: '/iam.organization/tree',
  }),
);
const userListContext = computed(
  () =>
    createScopedUserModuleContext(
      userContext,
      selectedTenant.value,
      selectedOrganization.value,
    ) as ModuleContext<QueryListRecord>,
);
const userListReady = computed(() => Boolean(selectedTenant.value?.id));
const organizationPanelVisible = computed(() => Boolean(selectedTenant.value));
const userListColumns = computed<RecordQueryListColumn[]>(() => [
  { key: 'username', title: '账号', width: '18%' },
  { key: 'title', title: '姓名', width: '18%' },
  {
    key: 'organizationId',
    title: '所属机构',
    width: '18%',
    render: (record) => organizationDisplayValue(record.organizationId),
  },
  { key: 'mobile', title: '手机号', width: '16%' },
  { key: 'email', title: '邮箱', width: '18%' },
  { key: 'enabled', title: '状态', type: 'enabledStatus', width: '10%' },
]);
const userListTitle = computed(() => {
  if (!selectedTenant.value) {
    return '用户列表';
  }
  return `用户列表 - ${selectedOrganization.value ? organizationTitle(selectedOrganization.value) : tenantTitle(selectedTenant.value)}`;
});
const userDetailTitle = computed(() => {
  if (userDetailMode.value === 'create') {
    return '新建用户';
  }
  if (userDetailMode.value === 'resetPassword') {
    return `修改密码 - ${userTitle(selectedUser.value ?? userDraft.value)}`;
  }
  return userTitle(selectedUser.value ?? userDraft.value);
});
const userFormDisabled = computed(() => savingUser.value || loadingUserDetail.value);
const canSaveUser = computed(() => {
  if (loadingUserDetail.value) {
    return false;
  }
  if (userDetailMode.value === 'create') {
    return Boolean(selectedTenant.value?.id) && userContext.can('create') === true;
  }
  if (userDetailMode.value === 'edit') {
    return Boolean(selectedUser.value?.id) && userContext.can('update') === true;
  }
  if (userDetailMode.value === 'resetPassword') {
    return Boolean(selectedUser.value?.id) && userContext.can('changePassword') === true;
  }
  return false;
});
const canToggleUser = computed(() => {
  if (!selectedUser.value?.id || loadingUserDetail.value) {
    return false;
  }
  return userContext.can(userToggleActionCode(selectedUser.value)) === true;
});
const userDetailActions = computed<RecordActionItem[]>(() => {
  if (userDetailMode.value === 'view') {
    if (!selectedUser.value?.id) {
      return [];
    }
    return [
      { key: 'edit', actionCode: 'update', title: '编辑', iconName: 'edit', disabled: savingUser.value },
      {
        key: 'resetPassword',
        actionCode: 'changePassword',
        title: '修改密码',
        iconName: 'lock',
        disabled: savingUser.value,
      },
      {
        key: 'resetGeneratedPassword',
        actionCode: 'resetPassword',
        title: '重置密码',
        iconName: 'reload',
        disabled: savingUser.value,
      },
      {
        key: 'delete',
        actionCode: 'delete',
        title: '删除',
        iconName: 'delete',
        danger: true,
        disabled: savingUser.value,
      },
    ];
  }
  return [
    { key: 'cancel', title: '取消', iconName: 'close', disabled: savingUser.value },
    {
      key: 'save',
      actionCode:
        userDetailMode.value === 'create'
          ? 'create'
          : userDetailMode.value === 'resetPassword'
            ? 'changePassword'
            : 'update',
      title: '保存',
      iconName: 'save',
      primary: true,
      loading: savingUser.value,
      disabled: !canSaveUser.value,
    },
  ];
});
const userFormFieldFallback = computed<Record<UserFormFieldName, RecordFormFieldFallback>>(() => ({
  username: { label: '账号', required: true, visible: true, placeholder: '请输入登录账号' },
  title: { label: '姓名', visible: true, placeholder: '请输入姓名或显示名' },
  mobile: { label: '手机号', visible: true, placeholder: '请输入手机号' },
  email: { label: '邮箱', visible: true, placeholder: '请输入邮箱' },
  organizationId: { label: '所属机构', visible: true, readOnly: true },
  enabled: { label: '启用状态', visible: true, controlType: 'enabledStatus' },
  sortOrder: { label: '排序号', visible: true, placeholder: '请输入排序号' },
}));
const userFormFieldNames = computed<UserFormFieldName[]>(() => [
  'username',
  'title',
  'mobile',
  'email',
  'organizationId',
  'enabled',
  'sortOrder',
]);

onMounted(loadUserFormDefinition);

watch(currentUserTenant, initializeTenantUserScope, { immediate: true });

watch(selectedTenant, () => {
  selectedOrganization.value = undefined;
  selectedUserKey.value = undefined;
  selectedUser.value = undefined;
  userDraft.value = createUserDraft(selectedTenant.value, undefined);
  closeUserDetail();
  organizationReloadKey.value += 1;
  userReloadKey.value += 1;
});

watch(selectedOrganization, () => {
  selectedUserKey.value = undefined;
  selectedUser.value = undefined;
  userDraft.value = createUserDraft(selectedTenant.value, selectedOrganization.value);
  closeUserDetail();
  userReloadKey.value += 1;
});

async function loadUserFormDefinition() {
  try {
    const runtimeContext = await userContext.runtime.ready;
    userFormFieldDefinitions.value = resolveRecordFormFields(runtimeContext.uiDescriptor);
  } catch (cause) {
    presentPlatformError(cause, { source: 'user-management', phase: 'load' });
  }
}

function createScopedUserModuleContext(
  context: ModuleContext<UserAccount>,
  tenant: Tenant | undefined,
  organization: Organization | undefined,
): ModuleContext<UserAccount> {
  return {
    ...context,
    crud: {
      ...context.crud,
      query: (request) => context.crud.query(scopedUserQuery(request, tenant, organization)),
    },
  };
}

function scopedUserQuery(
  request: WebQueryRequest | undefined,
  tenant: Tenant | undefined,
  organization: Organization | undefined,
): WebQueryRequest {
  const conditions = [...(request?.conditions ?? [])];
  if (tenant?.id) {
    conditions.push({ fieldName: 'tenantId', operator: 'EQ', values: [tenant.id] });
  }
  if (organization?.id) {
    conditions.push({ fieldName: 'organizationId', operator: 'EQ', values: [organization.id] });
  }
  return { ...request, conditions };
}

function handleTenantsLoaded(records: CrudRecordListBase[]) {
  if (!canBrowseTenants.value || selectedTenant.value) {
    return;
  }
  if (records.length > 0) {
    selectTenant(records[0] as Tenant);
  }
}

function initializeTenantUserScope(record = currentUserTenant.value) {
  if (!record || canBrowseTenants.value || selectedTenant.value) {
    return;
  }
  selectedTenant.value = record;
}

function selectTenant(record: Tenant) {
  if (!canLeaveUserDetailContext()) {
    return;
  }
  selectedTenant.value = record;
}

function selectOrganization(record: Organization) {
  if (!record.id || !canLeaveUserDetailContext()) {
    return;
  }
  selectedOrganization.value = record;
}

function clearOrganizationScope() {
  if (!canLeaveUserDetailContext()) {
    return;
  }
  selectedOrganization.value = undefined;
}

function handleUserListAction(action: RecordActionItem) {
  if (action.key === 'create') {
    startCreateUser();
  }
}

function handleUserRowAction(action: ResolvedRecordActionItem, record: QueryListRecord) {
  if (!canLeaveUserDetailContext()) {
    return;
  }
  if (action.key === 'view') {
    void openUserDetail(record, 'view');
    return;
  }
  if (action.key === 'edit') {
    void openUserDetail(record, 'edit');
    return;
  }
  if (action.key === 'delete') {
    void removeUser(record);
  }
}

function handleUserRowDblclick(record: QueryListRecord) {
  if (!canLeaveUserDetailContext()) {
    return;
  }
  void openUserDetail(record, 'view');
}

function startCreateUser() {
  if (!canLeaveUserDetailContext()) {
    return;
  }
  if (!selectedTenant.value?.id) {
    presentPlatformMessage('请先选择租户', { phase: 'validation' });
    return;
  }
  selectedUser.value = undefined;
  selectedUserKey.value = undefined;
  userDraft.value = createUserDraft(selectedTenant.value, selectedOrganization.value);
  passwordDraft.value = '';
  userDetailMode.value = 'create';
  loadingUserDetail.value = false;
  userDetailLoadFailed.value = false;
  userDetailRequestSeq.value += 1;
  userDetailOpen.value = true;
}

async function openUserDetail(record: QueryListRecord, mode: UserDetailMode) {
  if (!canLeaveUserDetailContext()) {
    return;
  }
  const id = String(record.id ?? '');
  if (!id) {
    return;
  }
  selectedUserKey.value = id;
  userDetailOpen.value = true;
  userDetailMode.value = mode;
  selectedUser.value = undefined;
  userDraft.value = copyUser(record as UserAccount);
  passwordDraft.value = '';
  resetPasswordResult.value = undefined;
  loadingUserDetail.value = true;
  userDetailLoadFailed.value = false;
  const requestSeq = userDetailRequestSeq.value + 1;
  userDetailRequestSeq.value = requestSeq;
  try {
    const fullRecord = await userContext.crud.view(id);
    if (!canCommitUserDetailRequest(id, requestSeq)) {
      return;
    }
    commitUserDetailRecord(fullRecord, mode);
  } catch (cause) {
    if (canCommitUserDetailRequest(id, requestSeq)) {
      userDetailLoadFailed.value = true;
      presentPlatformError(cause, { source: 'user-management', phase: 'load' });
    }
  } finally {
    if (canCommitUserDetailRequest(id, requestSeq)) {
      loadingUserDetail.value = false;
    }
  }
}

function closeUserDetail() {
  if (savingUser.value) {
    return;
  }
  userDetailRequestSeq.value += 1;
  loadingUserDetail.value = false;
  userDetailLoadFailed.value = false;
  userDetailOpen.value = false;
  userDetailMode.value = 'view';
  passwordDraft.value = '';
  resetPasswordResult.value = undefined;
  userDraft.value = selectedUser.value
    ? copyUser(selectedUser.value)
    : createUserDraft(selectedTenant.value, selectedOrganization.value);
}

function cancelUserDetail() {
  if (savingUser.value) {
    return;
  }
  if (!selectedUser.value?.id || userDetailMode.value === 'create') {
    closeUserDetail();
    return;
  }
  userDraft.value = copyUser(selectedUser.value);
  passwordDraft.value = '';
  resetPasswordResult.value = undefined;
  userDetailMode.value = 'view';
  loadingUserDetail.value = false;
  userDetailLoadFailed.value = false;
}

function handleUserDetailAction(action: RecordActionItem) {
  if (action.key === 'cancel') {
    cancelUserDetail();
    return;
  }
  if (action.key === 'save') {
    void saveUser();
    return;
  }
  if (!canLeaveUserDetailContext()) {
    return;
  }
  if (action.key === 'edit' && selectedUser.value) {
    userDraft.value = copyUser(selectedUser.value);
    userDetailMode.value = 'edit';
    return;
  }
  if (action.key === 'resetPassword' && selectedUser.value) {
    passwordDraft.value = '';
    resetPasswordResult.value = undefined;
    userDetailMode.value = 'resetPassword';
    return;
  }
  if (action.key === 'resetGeneratedPassword' && selectedUser.value) {
    void resetUserLoginPassword();
    return;
  }
  if (action.key === 'delete') {
    void removeUser(selectedUser.value);
  }
}

function retryUserDetail() {
  const id = String(userDraft.value.id ?? selectedUserKey.value ?? '');
  if (!id) {
    return;
  }
  const mode = userDetailMode.value === 'create' ? 'view' : userDetailMode.value;
  void openUserDetail({ ...userDraft.value, id } as QueryListRecord, mode);
}

async function saveUser() {
  if (userDetailMode.value === 'resetPassword') {
    await resetUserPassword();
    return;
  }
  await executeStaticFormSave<UserAccount>({
    loading: savingUser,
    mode: userDetailMode.value === 'edit' ? 'edit' : 'create',
    source: 'user-management',
    validateContext: () =>
      userDetailMode.value === 'create' && !selectedTenant.value?.id ? '请先选择租户' : undefined,
    canSave: () => canSaveUser.value,
    deniedMessage: '当前用户无权保存用户',
    createRecord: () =>
      normalizedUserDraft(
        userDraft.value,
        selectedTenant.value,
        selectedOrganization.value,
        userDetailMode.value,
        passwordDraft.value,
      ),
    validateRecord: validateUserDraft,
    save: (draft, mode) =>
      mode === 'edit' && selectedUser.value?.id
        ? userContext.crud.update(selectedUser.value.id, draft)
        : userContext.crud.insert(draft),
    onSaved: ({ record }) => {
      commitUserDetailRecord(record);
      userReloadKey.value += 1;
    },
  });
}

async function resetUserPassword() {
  await executeStaticRecordAction<UserAccount, WebCountResponse>({
    loading: savingUser,
    source: 'user-management',
    record: () => (selectedUser.value?.id ? selectedUser.value : undefined),
    canExecute: () => canSaveUser.value,
    deniedMessage: '当前用户无权重置用户密码',
    execute: (user) =>
      userContext.http.request<WebCountResponse>({
        method: 'POST',
        path: `/iam.user/changePassword/${encodeURIComponent(user.id!)}`,
        body: { password: passwordDraft.value },
      }),
    onExecuted: async (_, user) => {
      const refreshed = await userContext.crud.view(user.id!);
      commitUserDetailRecord(refreshed);
      userReloadKey.value += 1;
    },
  });
}

async function resetUserLoginPassword() {
  await executeStaticRecordAction<UserAccount, ResetPasswordResponse>({
    loading: savingUser,
    source: 'user-management',
    record: () => (selectedUser.value?.id ? selectedUser.value : undefined),
    canExecute: () => userContext.can('resetPassword') === true,
    deniedMessage: '当前用户无权重置用户密码',
    execute: (user) =>
      userContext.http.request<ResetPasswordResponse>({
        method: 'POST',
        path: `/iam.user/resetPassword/${encodeURIComponent(user.id!)}`,
      }),
    onExecuted: async (result, user) => {
      const refreshed = await userContext.crud.view(user.id!);
      commitUserDetailRecord(refreshed);
      resetPasswordResult.value = result;
      userReloadKey.value += 1;
    },
  });
}

async function toggleUserEnabled() {
  await executeStaticRecordAction({
    loading: savingUser,
    source: 'user-management',
    record: () => (selectedUser.value?.id ? selectedUser.value : undefined),
    canExecute: () => canToggleUser.value,
    deniedMessage: '当前用户无权变更用户启停状态',
    execute: (user) =>
      user.enabled === false ? userContext.crud.enable(user.id!) : userContext.crud.disable(user.id!),
    onExecuted: async (_, user) => {
      const refreshed = await userContext.crud.view(user.id!);
      commitUserDetailRecord(refreshed);
      userReloadKey.value += 1;
    },
  });
}

async function removeUser(record: Partial<UserAccount> | QueryListRecord | undefined) {
  await executeStaticRecordAction({
    loading: savingUser,
    source: 'user-management',
    record: () => (record?.id ? record : undefined),
    canExecute: () => userContext.can('delete') === true,
    deniedMessage: '当前用户无权删除用户',
    confirm: (target) =>
      confirmAction({
        title: '删除用户',
        content: `确认删除用户「${userTitle(target)}」？`,
        okText: '删除',
        danger: true,
      }),
    execute: (target) => userContext.crud.delete(String(target.id)),
    onExecuted: (_, target) => {
      if (selectedUserKey.value === String(target.id)) {
        selectedUserKey.value = undefined;
        selectedUser.value = undefined;
        userDraft.value = createUserDraft(selectedTenant.value, selectedOrganization.value);
        userDetailOpen.value = false;
        userDetailMode.value = 'view';
        loadingUserDetail.value = false;
        userDetailLoadFailed.value = false;
        userDetailRequestSeq.value += 1;
      }
      userReloadKey.value += 1;
    },
  });
}

function canLeaveUserDetailContext() {
  return !savingUser.value;
}

function canCommitUserDetailRequest(recordId: string, requestSeq: number) {
  return userDetailRequestSeq.value === requestSeq && selectedUserKey.value === recordId;
}

function commitUserDetailRecord(record: UserAccount, nextMode: UserDetailMode = 'view') {
  selectedUser.value = record;
  selectedUserKey.value = record.id;
  userDraft.value = copyUser(record);
  passwordDraft.value = '';
  userDetailMode.value = nextMode === 'edit' ? 'edit' : 'view';
  userDetailOpen.value = true;
  loadingUserDetail.value = false;
  userDetailLoadFailed.value = false;
  userDetailRequestSeq.value += 1;
}

function createUserDraft(
  tenant: Tenant | undefined,
  organization: Organization | undefined,
): Partial<UserAccount> {
  return {
    tenantId: tenant?.id,
    organizationId: organization?.id,
    enabled: true,
    sortOrder: 100,
  };
}

function copyUser(record: Partial<UserAccount>): Partial<UserAccount> {
  return { ...record, password: undefined };
}

function normalizedUserDraft(
  draft: Partial<UserAccount>,
  tenant: Tenant | undefined,
  organization: Organization | undefined,
  mode: UserDetailMode,
  password: string,
): UserAccount {
  const record = {
    ...draft,
    tenantId: tenant?.id ?? draft.tenantId,
    organizationId: (organization?.id ?? draft.organizationId?.trim()) || undefined,
    username: draft.username?.trim(),
    title: draft.title?.trim() || undefined,
    mobile: draft.mobile?.trim() || undefined,
    email: draft.email?.trim() || undefined,
    enabled: draft.enabled !== false,
    sortOrder: normalizeSortOrder(draft.sortOrder),
    password: mode === 'create' ? password.trim() : undefined,
  } as UserAccount;
  return record;
}

function validateUserDraft(draft: UserAccount) {
  const requiredFields: UserFormFieldName[] = ['username'];
  for (const fieldName of requiredFields) {
    const field = resolveRecordFormFieldState(fieldName, {
      fields: userFormFieldDefinitions.value,
      fallback: userFormFieldFallback.value,
    });
    if (field.visible && field.required && !draft[fieldName]) {
      return `请填写${field.label}`;
    }
  }
  if (userDetailMode.value === 'create' && !draft.password) {
    return '请填写初始密码';
  }
  if (userDetailMode.value === 'resetPassword' && !passwordDraft.value.trim()) {
    return '请填写新密码';
  }
  return undefined;
}

function userFormFieldDisabled(fieldName: string) {
  return fieldName === 'organizationId' || (fieldName === 'username' && userDetailMode.value === 'edit');
}

function updateUserDraftField(fieldName: string, value: string | number | boolean | undefined) {
  userDraft.value = {
    ...userDraft.value,
    [fieldName]: value,
  };
}

function normalizeSortOrder(value: unknown) {
  if (typeof value === 'number') {
    return Number.isFinite(value) ? value : 100;
  }
  const parsed = Number(String(value ?? '').trim());
  return Number.isFinite(parsed) ? parsed : 100;
}

function userToggleActionCode(record: Partial<UserAccount>) {
  return record.enabled === false ? 'enable' : 'disable';
}

function userTitle(record: Partial<UserAccount> | QueryListRecord | undefined) {
  return String(record?.title ?? record?.username ?? record?.id ?? '用户');
}

function tenantTitle(record: Tenant | CrudRecordListBase | undefined) {
  return String(record?.title ?? record?.alias ?? record?.id ?? '未命名租户');
}

function organizationTitle(record: Organization | TreeRecordBase | undefined) {
  return String(record?.title ?? record?.code ?? record?.id ?? '未命名机构');
}

function organizationDisplayValue(value: unknown) {
  if (!value) {
    return '-';
  }
  if (selectedOrganization.value?.id === value) {
    return organizationTitle(selectedOrganization.value);
  }
  return String(value);
}

function userDetailDisplayValue(fieldName: string, value: unknown) {
  if (fieldName === 'organizationId') {
    return organizationDisplayValue(value);
  }
  return undefined;
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
    title: organizationTitle(record),
    secondary: record.code ?? record.id,
    muted: record.enabled === false,
  };
}
</script>

<template>
  <section
    class="user-management-page"
    :class="{ 'user-management-page-no-tenant': !organizationPanelVisible }"
  >
    <RecordExplorerPanel
      class="user-scope-panel"
      title="租户"
      refresh-title="刷新租户列表"
      :search-keyword="tenantSearchKeyword"
      search-placeholder="搜索租户名称、alias 或 ID"
      :searchable="canBrowseTenants"
      @refresh="canBrowseTenants ? (tenantReloadKey += 1) : initializeTenantUserScope()"
      @update:search-keyword="tenantSearchKeyword = $event"
    >
      <button
        v-if="!canBrowseTenants && currentUserTenant"
        class="user-scope-entry"
        type="button"
        @click="selectTenant(currentUserTenant)"
      >
        <UiRecordExplorerItem
          :title="tenantTitle(currentUserTenant)"
          secondary="当前租户"
          clickable
          :selected="selectedTenant?.id === currentUserTenant.id"
        />
      </button>
      <CrudRecordListExplorer
        v-if="canBrowseTenants"
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
      v-if="organizationPanelVisible"
      class="user-scope-panel"
      title="机构"
      refresh-title="刷新机构树"
      :search-keyword="organizationSearchKeyword"
      search-placeholder="搜索机构名称、编码或 ID"
      :searchable="Boolean(selectedTenant)"
      @refresh="organizationReloadKey += 1"
      @update:search-keyword="organizationSearchKeyword = $event"
    >
      <UiEmpty v-if="!selectedTenant" description="请选择左侧租户" />
      <template v-else>
        <button class="user-scope-entry" type="button" @click="clearOrganizationScope">
          <UiRecordExplorerItem
            :title="tenantTitle(selectedTenant)"
            secondary="全部用户"
            clickable
            :selected="!selectedOrganization"
          />
        </button>
        <TreeRecordExplorer
          :context="organizationTreeContext"
          :selected-id="selectedOrganization?.id"
          :reload-key="organizationReloadKey"
          :keyword="organizationSearchKeyword"
          search-mode="none"
          search-trigger="external"
          empty-description="当前租户暂无机构"
          loading-tip="加载机构树"
          fallback-title="未命名机构"
          :item-of="organizationItemOf"
          @select="selectOrganization($event as Organization)"
        />
      </template>
    </RecordExplorerPanel>

    <RecordQueryListPanel
      class="user-list-panel"
      :context="userListContext"
      :title="userListTitle"
      :columns="userListColumns"
      standard-crud-actions
      standard-crud-row-actions
      create-title="新建用户"
      :selected-key="selectedUserKey"
      :reload-key="userReloadKey"
      :ready="userListReady"
      quick-search-placeholder="搜索账号、姓名、手机号或邮箱"
      empty-description="当前范围暂无用户"
      waiting-description="请选择账号范围"
      @action="handleUserListAction"
      @row-action="handleUserRowAction"
      @row-dblclick="handleUserRowDblclick"
      @select="selectedUserKey = String($event.id ?? '')"
    />

    <RecordDetailDrawer
      :open="userDetailOpen"
      :title="userDetailTitle"
      :close-on-outside="userDetailMode === 'view'"
      @close="closeUserDetail"
    >
      <template #status>
        <RecordStatusSwitch
          v-if="userDetailMode === 'view' && selectedUser"
          :enabled="selectedUser.enabled !== false"
          :disabled="savingUser || !canToggleUser"
          :loading="savingUser"
          :show-label="false"
          @change="toggleUserEnabled"
        />
      </template>
      <template #actions>
        <RecordActionBar
          :context="userListContext"
          :actions="userDetailActions"
          @action="handleUserDetailAction"
        />
      </template>

      <UiSpin v-if="loadingUserDetail" class="user-detail-state" tip="加载用户详情" />
      <div v-else-if="userDetailLoadFailed" class="user-detail-state">
        <UiError title="详情加载失败" message="无法加载用户详情，请重试" />
        <UiButton type="primary" icon-name="reload" @click="retryUserDetail">重试</UiButton>
      </div>

      <template v-else-if="userDetailMode === 'view' || userDetailMode === 'create' || selectedUser">
        <RecordDetailFields
          v-if="userDetailMode === 'view'"
          :record="userDraft as RecordFormRecord"
          :fields="userFormFieldDefinitions"
          :fallback="userFormFieldFallback"
          :display-of="userDetailDisplayValue"
        />
        <div
          v-if="userDetailMode === 'view' && resetPasswordResult?.temporaryPassword"
          class="user-password-reset-result"
        >
          <span>临时密码</span>
          <UiInput :value="resetPasswordResult.temporaryPassword" disabled />
          <small v-if="resetPasswordResult.expiresAt">有效期至 {{ resetPasswordResult.expiresAt }}</small>
        </div>

        <form v-else class="user-form" @submit.prevent="saveUser">
          <label>
            <span class="user-form-label">当前租户</span>
            <UiInput :value="tenantTitle(selectedTenant)" disabled />
          </label>
          <RecordFormFields
            v-if="userDetailMode !== 'resetPassword'"
            :record="userDraft as RecordFormRecord"
            :field-names="userFormFieldNames"
            :fields="userFormFieldDefinitions"
            :fallback="userFormFieldFallback"
            :disabled="userFormDisabled"
            :disabled-of="userFormFieldDisabled"
            @update:field="updateUserDraftField"
          />
          <label v-if="userDetailMode === 'create' || userDetailMode === 'resetPassword'">
            <span class="user-form-label">{{ userDetailMode === 'create' ? '初始密码' : '新密码' }}</span>
            <UiInput
              :value="passwordDraft"
              type="password"
              :disabled="userFormDisabled"
              placeholder="请输入密码"
              allow-clear
              @update:value="passwordDraft = $event"
            />
          </label>
        </form>
        <RecordMetaSection
          v-if="userDetailMode !== 'create' && userDetailMode !== 'resetPassword'"
          :record="userDraft"
          show-sort-order
        />
      </template>
    </RecordDetailDrawer>
  </section>
</template>

<style scoped>
.user-management-page {
  position: relative;
  display: grid;
  grid-template-columns: minmax(240px, 300px) minmax(240px, 300px) minmax(0, 1fr);
  gap: 12px;
  height: calc(100vh - 116px);
  overflow: hidden;
}

.user-management-page-no-tenant {
  grid-template-columns: minmax(240px, 300px) minmax(0, 1fr);
}

.user-scope-panel,
.user-list-panel {
  min-width: 0;
  min-height: 0;
}

.user-scope-entry {
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

.user-form {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
}

.user-form > label {
  display: grid;
  gap: 6px;
  color: var(--muyun-text-muted);
  font-size: 13px;
}

.user-form-label {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.user-password-reset-result {
  display: grid;
  gap: 6px;
  margin: 12px 0;
  padding: 12px;
  border: 1px solid var(--muyun-border);
  border-radius: 8px;
  background: var(--muyun-hover-subtle);
  color: var(--muyun-text-muted);
  font-size: 13px;
}

.user-password-reset-result small {
  color: var(--muyun-text-muted);
}

.user-detail-state {
  display: grid;
  justify-items: center;
  gap: 12px;
  padding: 32px 0;
}

@media (max-width: 1180px) {
  .user-management-page {
    grid-template-columns: minmax(220px, 280px) minmax(0, 1fr);
    grid-template-rows: minmax(0, 0.95fr) minmax(0, 1.3fr);
  }

  .user-list-panel {
    grid-column: 1 / -1;
  }
}

@media (max-width: 760px) {
  .user-management-page {
    grid-template-columns: 1fr;
    grid-template-rows: minmax(180px, 0.65fr) minmax(220px, 0.8fr) minmax(360px, 1fr);
    height: auto;
    min-height: calc(100vh - 116px);
    overflow: visible;
  }

  .user-list-panel {
    grid-column: auto;
  }
}
</style>
