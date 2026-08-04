<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import {
  CrudRecordListExplorer,
  RecordActionBar,
  RecordDetailDrawer,
  RecordDetailPanel,
  RecordExplorerPanel,
  RecordQueryListPanel,
  RecordStatusSwitch,
  UserSessionExpandedSubtable,
  executeStaticFormSave,
  executeStaticRecordAction,
  normalizeRecordDraft,
  presentPlatformError,
  presentPlatformMessage,
  resolveRecordFormFieldState,
  resolveRecordFormFields,
  type CrudRecordListBase,
  type QueryListRecord,
  type RecordActionItem,
  type RecordExplorerItemDescriptor,
  type RecordFormFieldFallback,
  type RecordQueryListColumn,
  type ResolvedRecordActionItem,
} from '@muyun/platform-components';
import { UiRecordExplorerItem, confirmAction } from '@muyun/vue-ui-antdv';
import type {
  ResetPasswordResponse,
  Tenant,
  UserAccount,
  UserEmployeeBindingView,
  UserSessionView,
  WebQueryRequest,
} from '@muyun/web-contracts';
import { useModuleContext, type ModuleContext } from '@muyun/web-core';
import { useCurrentUserContext } from '../app/currentUserContext';
import { createBackendHttpClient } from '../app/backendHttp';
import { usePageBusinessEventHandler } from '../app/pageRealtime';
import { useWorkspaceViewHost } from '../app/workspaceViewHost';
import { useWorkspaceViewPromotion } from '../app/useWorkspaceViewPromotion';
import UserDetailContent from './UserDetailContent.vue';
import { userDetailWorkspaceView } from './userDetailWorkspaceView';
import {
  handOffUserDetailWorkspaceSession,
  registerUserDetailWorkspaceHandoffRecipient,
  takeUserDetailWorkspaceSession,
  type UserDetailWorkspaceSession,
} from './userDetailWorkspaceSession';
import { useUserSessionRows } from './useUserSessionRows';
import {
  canSwitchUserDetailContext,
  persistedUserDetailMode,
  shouldCommitUserDetailRequest,
  type UserDetailMode,
} from './userDetailStateModel';

defineOptions({ name: 'UserManagementView' });

const props = defineProps<{
  /** Present only when this component is restored as a workbench task. */
  recordId?: string;
  mode?: 'view' | 'edit';
}>();

type UserFormFieldName = 'username' | 'enabled';

const tenantContext = useModuleContext<Tenant>({ moduleAlias: 'iam.tenant' });
const userContext = useModuleContext<UserAccount>({ moduleAlias: 'iam.user' });
const currentUser = useCurrentUserContext();
const workspaceViewHost = useWorkspaceViewHost();
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
const {
  expandedUserKeys,
  handleUserListLoaded,
  handleUserRowExpand,
  handleUserSessionBusinessEvent,
  loadUserSessions,
  resetUserSessionRows,
  userOnlineStatusTitle,
  userSessionState,
} = useUserSessionRows({ context: userContext, source: 'user-management' });

const tenantListContext = computed(() => tenantContext as unknown as ModuleContext<CrudRecordListBase>);
const isWorkspaceTask = computed(() => Boolean(props.recordId));
const isDrawerWorkspaceTask = computed(
  () => isWorkspaceTask.value && workspaceViewHost?.presentation === 'drawer',
);
const shouldRenderUserDetailDrawer = computed(() => !isWorkspaceTask.value || isDrawerWorkspaceTask.value);
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
const userListColumns = computed<RecordQueryListColumn[]>(() => [
  { key: 'username', title: '账号', width: '180px' },
  { key: 'onlineStatus', title: '在线状态', width: '100px', align: 'center' },
  { key: 'enabled', title: '状态', type: 'enabledStatus', width: '90px', align: 'center' },
  { key: 'passwordStatus', title: '密码状态', width: '120px' },
  { key: 'employeeNo', title: '职员工号', width: '150px' },
  { key: 'employeeTitle', title: '职员姓名', width: '150px' },
  { key: 'lastLoginAt', title: '最后登录时间', type: 'datetime', width: '180px' },
]);
const userListReady = computed(() => Boolean(selectedTenant.value?.id));
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
  return userPrimaryTitle(selectedUser.value ?? userDraft.value);
});
const userDetailSubtitle = computed(() => {
  if (userDetailMode.value === 'create') return undefined;
  return userEmployeeSubtitle(selectedUser.value ?? userDraft.value);
});
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
const userDetailOperationActions = computed(() => userDetailActions.value);
const userWorkspaceOperationActions = computed<RecordActionItem[]>(() => {
  if (userDetailMode.value === 'view') {
    return selectedUser.value?.id
      ? [{ key: 'edit', actionCode: 'update', title: '编辑', iconName: 'edit', disabled: savingUser.value }]
      : [];
  }
  return userDetailMode.value === 'edit'
    ? [
        { key: 'cancel', title: '取消', iconName: 'close', disabled: savingUser.value },
        {
          key: 'save',
          actionCode: 'update',
          title: '保存',
          iconName: 'save',
          primary: true,
          loading: savingUser.value,
          disabled: !canSaveUser.value,
        },
      ]
    : [];
});
const userDetailPromotion = useWorkspaceViewPromotion({
  view: userDetailWorkspaceView,
  input: computed(() => {
    const recordId = selectedUser.value?.id;
    const mode = userDetailMode.value;
    return recordId && (mode === 'view' || mode === 'edit') ? { recordId } : undefined;
  }),
  title: computed(() => userPrimaryTitle(selectedUser.value)),
  eligibility: computed(() => ({
    hasStableIdentity: Boolean(selectedUser.value?.id) && !loadingUserDetail.value,
    busy: savingUser.value,
  })),
  beforePromote: async (input) => {
    const selected = selectedUser.value;
    if (!selected) return;
    return (
      (await handOffUserDetailWorkspaceSession(input, {
        selectedUser: selected,
        draft: userDraft.value,
        tenant: selectedTenant.value,
        mode: userDetailMode.value === 'edit' ? 'edit' : 'view',
        password: passwordDraft.value,
        resetPasswordResult: resetPasswordResult.value,
      })) === 'accepted'
    );
  },
  onPromoted: closeUserDetail,
});

watch(
  selectedUser,
  (user) => {
    if (!isWorkspaceTask.value || !user) return;
    workspaceViewHost?.setTitle(userPrimaryTitle(user));
  },
  { immediate: true },
);
const userFormFieldFallback = computed<Record<UserFormFieldName, RecordFormFieldFallback>>(() => ({
  username: { label: '账号', required: true, visible: true, placeholder: '请输入登录账号' },
  enabled: { label: '允许登录', visible: true, controlType: 'enabledStatus' },
}));
const userFormFieldNames = computed<UserFormFieldName[]>(() => ['username', 'enabled']);

let disposeUserWorkspaceHandoffRecipient: (() => void) | undefined;

onMounted(() => {
  void loadUserFormDefinition();
  if (props.recordId) {
    const input = { recordId: props.recordId } as const;
    if (!isDrawerWorkspaceTask.value) {
      disposeUserWorkspaceHandoffRecipient = registerUserDetailWorkspaceHandoffRecipient(
        input,
        receiveUserDetailWorkspaceSession,
      );
    }
    const session = takeUserDetailWorkspaceSession(input);
    if (session) {
      restoreUserDetailWorkspaceSession(session);
      return;
    }
    void openUserDetail({ id: props.recordId }, props.mode ?? 'view');
  }
});

onBeforeUnmount(() => disposeUserWorkspaceHandoffRecipient?.());

function receiveUserDetailWorkspaceSession(session: UserDetailWorkspaceSession) {
  if (userDetailMode.value !== 'view') return false;
  restoreUserDetailWorkspaceSession(session);
  return true;
}

usePageBusinessEventHandler(handleUserSessionBusinessEvent);

watch(currentUserTenant, initializeTenantUserScope, { immediate: true });

watch(selectedTenant, () => {
  if (isWorkspaceTask.value) {
    return;
  }
  selectedUserKey.value = undefined;
  selectedUser.value = undefined;
  resetUserSessionRows();
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
  if (isWorkspaceTask.value || !record || canBrowseTenants.value || selectedTenant.value) {
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
    if (!fullRecord?.id) {
      userDetailLoadFailed.value = true;
      presentPlatformMessage('未找到指定用户', { source: 'user-management', phase: 'load' });
      return;
    }
    const binding = await loadUserEmployeeBinding(id);
    if (!canCommitUserDetailRequest(id, requestSeq)) {
      return;
    }
    commitUserDetailRecord({ ...fullRecord, ...binding }, mode);
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

function loadUserEmployeeBinding(userId: string) {
  return createBackendHttpClient().request<UserEmployeeBindingView>({
    path: `/iam.user/${encodeURIComponent(userId)}/employee-binding`,
  });
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
  userDraft.value = selectedUser.value ? copyUser(selectedUser.value) : createUserDraft(selectedTenant.value);
  if (isDrawerWorkspaceTask.value) {
    workspaceViewHost?.dismiss();
  }
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
  await executeStaticRecordAction<UserAccount, number>({
    loading: savingUser,
    source: 'user-management',
    record: () => (selectedUser.value?.id ? selectedUser.value : undefined),
    canExecute: () => canSaveUser.value,
    deniedMessage: '当前用户无权重置用户密码',
    execute: (user) =>
      userContext.http.request<number>({
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

async function revokeUserSession(record: Partial<UserAccount> | QueryListRecord, session: UserSessionView) {
  await executeStaticRecordAction<UserAccount, number>({
    loading: savingUser,
    source: 'user-management',
    record: () => (record?.id ? (record as UserAccount) : undefined),
    canExecute: (user) => userContext.can('revokeSession', user.id) === true && !session.current,
    deniedMessage: '当前用户无权下线该登录会话',
    confirm: (user) =>
      confirmAction({
        title: '下线登录会话',
        content: `确认下线用户「${userTitle(user)}」的该登录会话？`,
        okText: '下线',
        danger: true,
      }),
    execute: (user) =>
      userContext.http.request<number>({
        method: 'POST',
        path: `/iam.user/${encodeURIComponent(user.id!)}/sessions/${encodeURIComponent(session.id)}/revoke`,
      }),
    onExecuted: (_, user) => {
      void loadUserSessions(user.id);
      userReloadKey.value += 1;
    },
  });
}

async function revokeAllUserSessions(record: Partial<UserAccount> | QueryListRecord) {
  const userId = String(record.id ?? '');
  const sessionIds = revokableUserSessions(userId).map((session) => session.id);
  if (sessionIds.length === 0) {
    presentPlatformMessage('当前没有可下线的登录会话', { source: 'user-management', phase: 'validation' });
    return;
  }
  await executeStaticRecordAction<UserAccount, number>({
    loading: savingUser,
    source: 'user-management',
    record: () => (record?.id ? (record as UserAccount) : undefined),
    canExecute: (user) => userContext.can('revokeSessions', user.id) === true,
    deniedMessage: '当前用户无权批量下线登录会话',
    confirm: (user) =>
      confirmAction({
        title: '批量下线登录会话',
        content: `确认下线用户「${userTitle(user)}」的 ${sessionIds.length} 个登录会话？`,
        okText: '全部下线',
        danger: true,
      }),
    execute: (user) =>
      userContext.http.request<number>({
        method: 'POST',
        path: `/iam.user/${encodeURIComponent(user.id!)}/sessions/revoke`,
        body: { sessionIds },
      }),
    onExecuted: (_, user) => {
      void loadUserSessions(user.id);
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
      user.enabled === false
        ? userContext.crud.enable(user.id!, { version: user.version! })
        : userContext.crud.disable(user.id!, { version: user.version! }),
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
    execute: (target) =>
      userContext.crud.delete(String(target.id), { version: (target as { version: number }).version }),
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
  return canSwitchUserDetailContext(savingUser.value);
}

function canCommitUserDetailRequest(recordId: string, requestSeq: number) {
  return shouldCommitUserDetailRequest({
    activeRequestSeq: userDetailRequestSeq.value,
    requestSeq,
    selectedUserKey: selectedUserKey.value,
    recordId,
  });
}

function commitUserDetailRecord(record: UserAccount, nextMode: UserDetailMode = 'view') {
  selectedUser.value = record;
  selectedUserKey.value = record.id;
  userDraft.value = copyUser(record);
  passwordDraft.value = '';
  userDetailMode.value = persistedUserDetailMode(nextMode);
  userDetailOpen.value = true;
  loadingUserDetail.value = false;
  userDetailLoadFailed.value = false;
  userDetailRequestSeq.value += 1;
  const requestSeq = userDetailRequestSeq.value;
  return requestSeq;
}

function restoreUserDetailWorkspaceSession(session: UserDetailWorkspaceSession) {
  selectedUser.value = session.selectedUser;
  selectedUserKey.value = session.selectedUser.id;
  selectedTenant.value = session.tenant;
  userDraft.value = session.draft;
  userDetailMode.value = session.mode;
  passwordDraft.value = session.password;
  resetPasswordResult.value = session.resetPasswordResult;
  userDetailOpen.value = true;
  loadingUserDetail.value = false;
  userDetailLoadFailed.value = false;
  userDetailRequestSeq.value += 1;
}

function revokableUserSessions(userId: string | undefined) {
  if (!userId || userContext.can('revokeSession', userId) !== true) {
    return [];
  }
  return userSessionState(userId).records.filter((session) => !session.current);
}

function canRevokeUserSession(userId: string | undefined, session: UserSessionView) {
  return Boolean(userId) && !session.current && userContext.can('revokeSession', userId) === true;
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
  return normalizeRecordDraft<UserAccount>(draft, {
    tenantId: tenant?.id ?? draft.tenantId,
    username: draft.username?.trim(),
    enabled: draft.enabled !== false,
    password: mode === 'create' ? password.trim() : undefined,
  });
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

function updateUserDraftField(
  fieldName: string,
  value: import('@muyun/platform-components').RecordFormFieldValue,
) {
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

function userPrimaryTitle(record: Partial<UserAccount> | QueryListRecord | undefined) {
  return userTitle(record);
}

function userEmployeeSubtitle(record: Partial<UserAccount> | QueryListRecord | undefined) {
  const employeeTitle = String(record?.employeeTitle ?? '').trim();
  return employeeTitle ? `职员：${employeeTitle}` : '未关联职员';
}

function tenantTitle(record: Tenant | CrudRecordListBase | undefined) {
  return String(record?.title ?? record?.alias ?? record?.id ?? '未命名租户');
}

const userDetailDisplayValue = () => undefined;

function tenantItemOf(record: CrudRecordListBase): RecordExplorerItemDescriptor {
  return {
    title: tenantTitle(record),
    secondary: record.alias ?? record.id,
    muted: record.enabled === false,
  };
}
</script>

<template>
  <section
    class="user-management-page"
    :class="{ 'user-management-page--task': isWorkspaceTask && !isDrawerWorkspaceTask }"
  >
    <RecordExplorerPanel
      v-if="!isWorkspaceTask || isDrawerWorkspaceTask"
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
      v-if="!isWorkspaceTask || isDrawerWorkspaceTask"
      class="user-list-panel"
      :context="userListContext"
      :title="userListTitle"
      :columns="userListColumns"
      standard-crud-actions
      standard-crud-row-actions
      :selected-key="selectedUserKey"
      :expanded-row-keys="expandedUserKeys"
      :cell-renderers="{ onlineStatus: userOnlineStatusTitle }"
      :reload-key="userReloadKey"
      :ready="userListReady"
      quick-search-placeholder="搜索账号"
      empty-description="当前租户暂无账号"
      waiting-description="请选择租户"
      @action="handleUserListAction"
      @row-action="handleUserRowAction"
      @row-dblclick="handleUserRowDblclick"
      @row-expand="handleUserRowExpand"
      @loaded="handleUserListLoaded"
      @select="selectedUserKey = String($event.id ?? '')"
    >
      <template #expandedRow="{ record }">
        <UserSessionExpandedSubtable
          :sessions="userSessionState(String(record.id ?? '')).records"
          :loading="userSessionState(String(record.id ?? '')).loading"
          :error="userSessionState(String(record.id ?? '')).error"
          :actions-disabled="savingUser"
          :can-revoke="(session) => canRevokeUserSession(String(record.id ?? ''), session)"
          :can-revoke-all="revokableUserSessions(String(record.id ?? '')).length > 1"
          @refresh="loadUserSessions(String(record.id ?? ''))"
          @revoke="revokeUserSession(record, $event)"
          @revoke-all="revokeAllUserSessions(record)"
        />
      </template>
    </RecordQueryListPanel>

    <RecordDetailDrawer
      v-if="shouldRenderUserDetailDrawer"
      :open="userDetailOpen"
      :title="userDetailTitle"
      :subtitle="userDetailSubtitle"
      :close-on-outside="userDetailMode === 'view'"
      :promotion="userDetailPromotion"
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
      <template #operation>
        <RecordActionBar
          :context="userListContext"
          :actions="userDetailOperationActions"
          :record-id="selectedUser?.id"
          @action="handleUserDetailAction"
        />
      </template>

      <UserDetailContent
        :mode="userDetailMode"
        :draft="userDraft"
        :selected-user="selectedUser"
        :loading="loadingUserDetail"
        :load-failed="userDetailLoadFailed"
        :saving="savingUser || loadingUserDetail"
        :tenant-title="tenantTitle(selectedTenant)"
        :fields="userFormFieldDefinitions"
        :fallback="userFormFieldFallback"
        :field-names="userFormFieldNames"
        :password="passwordDraft"
        :reset-password-result="resetPasswordResult"
        :display-of="userDetailDisplayValue"
        :disabled-of="userFormFieldDisabled"
        @retry="retryUserDetail"
        @save="saveUser"
        @update:field="updateUserDraftField"
        @update:password="passwordDraft = $event"
      />
    </RecordDetailDrawer>

    <RecordDetailPanel v-else :title="userDetailTitle" :subtitle="userDetailSubtitle">
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
      <template #operation>
        <RecordActionBar
          :context="userContext"
          :actions="userWorkspaceOperationActions"
          :record-id="selectedUser?.id"
          @action="handleUserDetailAction"
        />
      </template>
      <UserDetailContent
        :mode="userDetailMode"
        :draft="userDraft"
        :selected-user="selectedUser"
        :loading="loadingUserDetail"
        :load-failed="userDetailLoadFailed"
        :saving="savingUser || loadingUserDetail"
        :tenant-title="tenantTitle(selectedTenant ?? ({ id: userDraft.tenantId } as Tenant))"
        :fields="userFormFieldDefinitions"
        :fallback="userFormFieldFallback"
        :field-names="userFormFieldNames"
        :password="passwordDraft"
        :reset-password-result="resetPasswordResult"
        :display-of="userDetailDisplayValue"
        :disabled-of="userFormFieldDisabled"
        @retry="retryUserDetail"
        @save="saveUser"
        @update:field="updateUserDraftField"
        @update:password="passwordDraft = $event"
      />
    </RecordDetailPanel>
  </section>
</template>

<style scoped>
.user-management-page {
  position: relative;
  display: grid;
  grid-template-columns: minmax(240px, 300px) minmax(0, 1fr);
  gap: 12px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.user-management-page--task {
  display: block;
  height: 100%;
}

.user-management-page--task :deep(.record-detail-layout) {
  height: 100%;
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

@media (max-width: 980px) {
  .user-management-page {
    height: auto;
    overflow: visible;
  }
}

@media (max-width: 760px) {
  .user-management-page {
    grid-template-columns: 1fr;
    grid-template-rows: minmax(180px, 0.65fr) minmax(360px, 1fr);
  }

  .user-list-panel {
    grid-column: auto;
  }
}
</style>
