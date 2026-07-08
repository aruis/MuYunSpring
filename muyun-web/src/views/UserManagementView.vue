<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
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
} from '@muyun/platform-components';
import { UiButton, UiError, UiInput, UiRecordExplorerItem, UiSpin, confirmAction } from '@muyun/vue-ui-antdv';
import type {
  ResetPasswordResponse,
  Tenant,
  UserAccount,
  UserEmployeeBindingView,
  WebCountResponse,
  WebQueryRequest,
} from '@muyun/web-contracts';
import { useModuleContext, type ModuleContext } from '@muyun/web-core';
import { useCurrentUserContext } from '../app/currentUserContext';

defineOptions({ name: 'UserManagementView' });

type UserDetailMode = 'view' | 'create' | 'edit' | 'resetPassword';
type UserFormFieldName = 'username' | 'enabled';

const tenantContext = useModuleContext<Tenant>({ moduleAlias: 'iam.tenant' });
const userContext = useModuleContext<UserAccount>({ moduleAlias: 'iam.user' });
const currentUser = useCurrentUserContext();
const router = useRouter();
const tenantSearchKeyword = ref('');
const tenantReloadKey = ref(0);
const userReloadKey = ref(0);
const selectedTenant = ref<Tenant>();
const selectedUserKey = ref<string>();
const selectedUser = ref<UserAccount>();
const userDetailOpen = ref(false);
const userDetailMode = ref<UserDetailMode>('view');
const loadingUserDetail = ref(false);
const userDetailLoadFailed = ref(false);
const savingUser = ref(false);
const userDetailRequestSeq = ref(0);
const userDraft = ref<Partial<UserAccount>>(createUserDraft(undefined));
const passwordDraft = ref('');
const resetPasswordResult = ref<ResetPasswordResponse>();
const userFormFieldDefinitions = ref(resolveRecordFormFields(undefined));
const userEmployeeBinding = ref<UserEmployeeBindingView>();
const loadingUserEmployeeBinding = ref(false);
const userEmployeeBindingLoadFailed = ref(false);

const tenantListContext = computed(() => tenantContext as unknown as ModuleContext<CrudRecordListBase>);
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
const userListContext = computed(
  () => createScopedUserModuleContext(userContext, selectedTenant.value) as ModuleContext<QueryListRecord>,
);
const userListReady = computed(() => Boolean(selectedTenant.value?.id));
const userListColumns = computed<RecordQueryListColumn[]>(() => [
  { key: 'username', title: '账号', width: '26%' },
  { key: 'passwordStatusTitle', title: '密码状态', width: '18%' },
  { key: 'lastLoginAt', title: '最后登录', width: '26%' },
  { key: 'enabled', title: '登录状态', type: 'enabledStatus', width: '14%' },
]);
const userListTitle = computed(() => {
  if (!selectedTenant.value) {
    return '用户列表';
  }
  return `用户列表 - ${tenantTitle(selectedTenant.value)}`;
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
    const userId = selectedUser.value?.id;
    return Boolean(userId) && userContext.can('changePassword', userId) === true;
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
  enabled: { label: '允许登录', visible: true, controlType: 'enabledStatus' },
}));
const userFormFieldNames = computed<UserFormFieldName[]>(() => ['username', 'enabled']);

onMounted(loadUserFormDefinition);

watch(currentUserTenant, initializeTenantUserScope, { immediate: true });

watch(selectedTenant, () => {
  selectedUserKey.value = undefined;
  selectedUser.value = undefined;
  userDraft.value = createUserDraft(selectedTenant.value);
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
): ModuleContext<UserAccount> {
  return {
    ...context,
    crud: {
      ...context.crud,
      query: (request) => context.crud.query(scopedUserQuery(request, tenant)),
    },
  };
}

function scopedUserQuery(request: WebQueryRequest | undefined, tenant: Tenant | undefined): WebQueryRequest {
  const conditions = [...(request?.conditions ?? [])];
  if (tenant?.id) {
    conditions.push({ fieldName: 'tenantId', operator: 'EQ', values: [tenant.id] });
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
  userDraft.value = createUserDraft(selectedTenant.value);
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
  resetUserEmployeeBinding();
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
    const detailSeq = commitUserDetailRecord(fullRecord, mode);
    if (mode === 'view') {
      void loadUserEmployeeBinding(fullRecord, detailSeq);
    }
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
  resetUserEmployeeBinding();
  passwordDraft.value = '';
  resetPasswordResult.value = undefined;
  userDraft.value = selectedUser.value ? copyUser(selectedUser.value) : createUserDraft(selectedTenant.value);
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
  void loadUserEmployeeBinding(selectedUser.value, userDetailRequestSeq.value);
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
      normalizedUserDraft(userDraft.value, selectedTenant.value, userDetailMode.value, passwordDraft.value),
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
    canExecute: (user) => userContext.can('resetPassword', user.id) === true,
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
        userDraft.value = createUserDraft(selectedTenant.value);
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
  const requestSeq = userDetailRequestSeq.value;
  if (userDetailMode.value !== 'view') {
    resetUserEmployeeBinding();
  }
  return requestSeq;
}

async function loadUserEmployeeBinding(
  record: Partial<UserAccount> = selectedUser.value ?? userDraft.value,
  requestSeq = userDetailRequestSeq.value,
) {
  const userId = record.id;
  if (!userId) {
    resetUserEmployeeBinding();
    return;
  }
  loadingUserEmployeeBinding.value = true;
  userEmployeeBindingLoadFailed.value = false;
  try {
    const binding = await userContext.http.request<UserEmployeeBindingView>({
      path: `/iam.user/${encodeURIComponent(userId)}/employee-binding`,
    });
    if (canCommitUserDetailRequest(userId, requestSeq)) {
      userEmployeeBinding.value = binding;
    }
  } catch (cause) {
    if (canCommitUserDetailRequest(userId, requestSeq)) {
      userEmployeeBindingLoadFailed.value = true;
      presentPlatformError(cause, { source: 'user-management', phase: 'load' });
    }
  } finally {
    if (canCommitUserDetailRequest(userId, requestSeq)) {
      loadingUserEmployeeBinding.value = false;
    }
  }
}

function resetUserEmployeeBinding() {
  userEmployeeBinding.value = undefined;
  loadingUserEmployeeBinding.value = false;
  userEmployeeBindingLoadFailed.value = false;
}

function createUserDraft(tenant: Tenant | undefined): Partial<UserAccount> {
  return {
    tenantId: tenant?.id,
    enabled: true,
  };
}

function copyUser(record: Partial<UserAccount>): Partial<UserAccount> {
  return { ...record, password: undefined };
}

function normalizedUserDraft(
  draft: Partial<UserAccount>,
  tenant: Tenant | undefined,
  mode: UserDetailMode,
  password: string,
): UserAccount {
  const record = {
    tenantId: tenant?.id ?? draft.tenantId,
    username: draft.username?.trim(),
    enabled: draft.enabled !== false,
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
  return fieldName === 'username' && userDetailMode.value === 'edit';
}

function updateUserDraftField(fieldName: string, value: string | number | boolean | undefined) {
  userDraft.value = {
    ...userDraft.value,
    [fieldName]: value,
  };
}

function userToggleActionCode(record: Partial<UserAccount>) {
  return record.enabled === false ? 'enable' : 'disable';
}

function userTitle(record: Partial<UserAccount> | QueryListRecord | undefined) {
  return String(record?.username ?? record?.id ?? '用户');
}

function tenantTitle(record: Tenant | CrudRecordListBase | undefined) {
  return String(record?.title ?? record?.alias ?? record?.id ?? '未命名租户');
}

const userDetailDisplayValue = () => undefined;

function userEmployeeBindingTitle(binding: UserEmployeeBindingView | undefined) {
  if (!binding?.employeeId) {
    return '未绑定职员';
  }
  return String(binding.employeeTitle ?? binding.employeeNo ?? binding.employeeId);
}

function userEmployeeBindingDescription(binding: UserEmployeeBindingView | undefined) {
  if (!binding?.employeeId) {
    return '账号未关联职员身份';
  }
  return [
    binding.employeeNo ? `编号 ${binding.employeeNo}` : undefined,
    binding.organizationId ? `机构 ${binding.organizationId}` : undefined,
    binding.departmentId ? `部门 ${binding.departmentId}` : undefined,
  ]
    .filter(Boolean)
    .join(' / ');
}

function openEmployeeManagement() {
  void router.push('/iam/employees');
}

function tenantItemOf(record: CrudRecordListBase): RecordExplorerItemDescriptor {
  return {
    title: tenantTitle(record),
    secondary: record.alias ?? record.id,
    muted: record.enabled === false,
  };
}
</script>

<template>
  <section class="user-management-page">
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
      quick-search-placeholder="搜索账号"
      empty-description="当前租户暂无账号"
      waiting-description="请选择租户"
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
          :record-id="selectedUser?.id"
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
        <section v-if="userDetailMode === 'view'" class="user-employee-binding">
          <div class="user-employee-binding-header">
            <strong>绑定职员</strong>
            <UiButton type="text" icon-name="search" @click="openEmployeeManagement">到职员管理查看</UiButton>
          </div>
          <UiSpin v-if="loadingUserEmployeeBinding" tip="加载绑定职员" />
          <div v-else-if="userEmployeeBindingLoadFailed" class="user-employee-binding-state">
            <UiError message="无法加载绑定职员" />
            <UiButton icon-name="reload" @click="loadUserEmployeeBinding()">重试</UiButton>
          </div>
          <div v-else class="user-employee-binding-card">
            <span>{{ userEmployeeBindingTitle(userEmployeeBinding) }}</span>
            <small>{{ userEmployeeBindingDescription(userEmployeeBinding) }}</small>
          </div>
        </section>
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
        />
      </template>
    </RecordDetailDrawer>
  </section>
</template>

<style scoped>
.user-management-page {
  position: relative;
  display: grid;
  grid-template-columns: minmax(240px, 300px) minmax(0, 1fr);
  gap: 12px;
  height: calc(100vh - 116px);
  overflow: hidden;
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

.user-employee-binding {
  display: grid;
  gap: 10px;
  margin: 14px 0;
  padding: 12px;
  border: 1px solid var(--muyun-border);
  border-radius: 8px;
  background: var(--muyun-hover-subtle);
}

.user-employee-binding-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.user-employee-binding-header strong {
  color: var(--muyun-text);
  font-size: 14px;
}

.user-employee-binding-card,
.user-employee-binding-state {
  display: grid;
  gap: 6px;
}

.user-employee-binding-card span {
  color: var(--muyun-text);
  font-weight: 600;
}

.user-employee-binding-card small {
  color: var(--muyun-text-muted);
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
    grid-template-rows: minmax(180px, 0.65fr) minmax(360px, 1fr);
    height: auto;
    min-height: calc(100vh - 116px);
    overflow: visible;
  }

  .user-list-panel {
    grid-column: auto;
  }
}
</style>
