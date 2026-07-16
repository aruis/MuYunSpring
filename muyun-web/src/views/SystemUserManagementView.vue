<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import {
  RecordActionBar,
  RecordDetailFields,
  RecordFormFields,
  RecordMetaSection,
  RecordModeDrawer,
  RecordQueryListPanel,
  RecordStatusSwitch,
  executeStaticFormSave,
  executeStaticRecordAction,
  presentPlatformError,
  presentPlatformMessage,
  resolveRecordFormFieldState,
  resolveRecordFormFields,
  type QueryListRecord,
  type RecordActionItem,
  type RecordFormFieldFallback,
  type RecordFormRecord,
  type RecordQueryListColumn,
  type ResolvedRecordActionItem,
} from '@muyun/platform-components';
import { UiButton, UiError, UiInput, UiSpin, confirmAction } from '@muyun/vue-ui-antdv';
import type {
  ResetPasswordResponse,
  UserAccount,
  UserSessionView,
  WebQueryRequest,
} from '@muyun/web-contracts';
import { useModuleContext, type ModuleContext } from '@muyun/web-core';
import { usePageBusinessEventHandler, usePageRecordExternalChange } from '../app/pageRealtime';
import { useUserSessionRows } from './useUserSessionRows';

defineOptions({ name: 'SystemUserManagementView' });

type SystemUserDetailMode = 'view' | 'edit' | 'resetPassword';
type SystemUserFormFieldName = 'username' | 'enabled';

const userContext = useModuleContext<UserAccount>({ moduleAlias: 'iam.user' });
const selectedUserKey = ref<string>();
const selectedUser = ref<UserAccount>();
const detailOpen = ref(false);
const detailMode = ref<SystemUserDetailMode>('view');
const loadingDetail = ref(false);
const detailLoadFailed = ref(false);
const savingUser = ref(false);
const detailRequestSeq = ref(0);
const reloadKey = ref(0);
const userDraft = ref<Partial<UserAccount>>(createSystemUserDraft());
const passwordDraft = ref('');
const resetPasswordResult = ref<ResetPasswordResponse>();
const formFieldDefinitions = ref(resolveRecordFormFields(undefined));
const {
  expandedUserKeys,
  handleUserListLoaded,
  handleUserRowExpand,
  handleUserSessionBusinessEvent,
  loadUserSessions,
  userOnlineStatusTitle,
  userSessionState,
} = useUserSessionRows({ context: userContext, source: 'system-user-management' });

const userExternalChange = usePageRecordExternalChange({
  moduleAlias: 'iam.user',
  recordId: () => selectedUser.value?.id,
  editing: () => detailMode.value === 'edit',
  saving: () => savingUser.value,
});

const systemUserContext = computed(
  () => createSystemUserModuleContext(userContext) as ModuleContext<QueryListRecord>,
);
const columns = computed<RecordQueryListColumn[]>(() => [
  { key: 'username', title: '账号', width: '24%' },
  { key: 'onlineStatus', title: '在线状态', width: '14%', align: 'center' },
  { key: 'passwordStatusTitle', title: '密码状态', width: '18%' },
  { key: 'lastLoginAt', title: '最后登录', width: '24%' },
  { key: 'enabled', title: '登录状态', type: 'enabledStatus', width: '14%' },
]);
const detailTitle = computed(() => {
  if (detailMode.value === 'resetPassword') {
    return `修改密码 - ${systemUserTitle(selectedUser.value ?? userDraft.value)}`;
  }
  return systemUserTitle(selectedUser.value ?? userDraft.value);
});
const formDisabled = computed(() => savingUser.value || loadingDetail.value);
const canSaveUser = computed(() => {
  if (loadingDetail.value) {
    return false;
  }
  if (detailMode.value === 'edit') {
    return Boolean(selectedUser.value?.id) && userContext.can('update') === true;
  }
  if (detailMode.value === 'resetPassword') {
    const userId = selectedUser.value?.id;
    return Boolean(userId) && userContext.can('changePassword', userId) === true;
  }
  return false;
});
const canToggleUser = computed(() => {
  if (!selectedUser.value?.id || loadingDetail.value) {
    return false;
  }
  return userContext.can(systemUserToggleActionCode(selectedUser.value)) === true;
});
const detailActions = computed<RecordActionItem[]>(() => {
  if (detailMode.value === 'view') {
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
    ];
  }
  return [
    { key: 'cancel', title: '取消', iconName: 'close', disabled: savingUser.value },
    {
      key: 'save',
      actionCode: detailMode.value === 'resetPassword' ? 'changePassword' : 'update',
      title: '保存',
      iconName: 'save',
      primary: true,
      loading: savingUser.value,
      disabled: !canSaveUser.value,
    },
  ];
});
const formFieldFallback = computed<Record<SystemUserFormFieldName, RecordFormFieldFallback>>(() => ({
  username: { label: '账号', required: true, visible: true, placeholder: '请输入登录账号' },
  enabled: { label: '允许登录', visible: true, controlType: 'enabledStatus' },
}));
const formFieldNames = computed<SystemUserFormFieldName[]>(() => ['username', 'enabled']);

onMounted(() => {
  void loadFormDefinition();
});

usePageBusinessEventHandler(handleUserSessionBusinessEvent);

async function loadFormDefinition() {
  try {
    const runtimeContext = await userContext.runtime.ready;
    formFieldDefinitions.value = resolveRecordFormFields(runtimeContext.uiDescriptor);
  } catch (cause) {
    presentPlatformError(cause, { source: 'system-user-management', phase: 'load' });
  }
}

function createSystemUserModuleContext(context: ModuleContext<UserAccount>): ModuleContext<UserAccount> {
  return {
    ...context,
    crud: {
      ...context.crud,
      query: (request) => context.crud.query(systemUserQuery(request)),
    },
  };
}

function systemUserQuery(request: WebQueryRequest | undefined): WebQueryRequest {
  return {
    ...request,
    conditions: [...(request?.conditions ?? []), { fieldName: 'tenantId', operator: 'NULL', values: [] }],
  };
}

function rowActionsOf(): RecordActionItem[] {
  return [
    { key: 'view', actionCode: 'view', title: '查看' },
    { key: 'edit', actionCode: 'update', title: '编辑', iconName: 'edit' },
  ];
}

function handleRowAction(action: ResolvedRecordActionItem, record: QueryListRecord) {
  if (!canLeaveDetailContext()) {
    return;
  }
  if (action.key === 'view') {
    void openDetail(record, 'view');
    return;
  }
  if (action.key === 'edit') {
    void openDetail(record, 'edit');
  }
}

function handleRowDblclick(record: QueryListRecord) {
  if (!canLeaveDetailContext()) {
    return;
  }
  void openDetail(record, 'view');
}

async function openDetail(record: QueryListRecord, mode: SystemUserDetailMode) {
  if (!canLeaveDetailContext()) {
    return;
  }
  const id = String(record.id ?? '');
  if (!id) {
    return;
  }
  selectedUserKey.value = id;
  userExternalChange.clearExternalChanged();
  detailOpen.value = true;
  detailMode.value = mode;
  selectedUser.value = undefined;
  userDraft.value = copySystemUser(record as UserAccount);
  passwordDraft.value = '';
  resetPasswordResult.value = undefined;
  loadingDetail.value = true;
  detailLoadFailed.value = false;
  const requestSeq = detailRequestSeq.value + 1;
  detailRequestSeq.value = requestSeq;
  try {
    const fullRecord = await userContext.crud.view(id);
    if (!canCommitDetailRequest(id, requestSeq)) {
      return;
    }
    commitDetailRecord(fullRecord, mode);
  } catch (cause) {
    if (canCommitDetailRequest(id, requestSeq)) {
      detailLoadFailed.value = true;
      presentPlatformError(cause, { source: 'system-user-management', phase: 'load' });
    }
  } finally {
    if (canCommitDetailRequest(id, requestSeq)) {
      loadingDetail.value = false;
    }
  }
}

function closeDetail() {
  if (savingUser.value) {
    return;
  }
  detailRequestSeq.value += 1;
  loadingDetail.value = false;
  detailLoadFailed.value = false;
  detailOpen.value = false;
  detailMode.value = 'view';
  passwordDraft.value = '';
  resetPasswordResult.value = undefined;
  userExternalChange.clearExternalChanged();
  userDraft.value = selectedUser.value ? copySystemUser(selectedUser.value) : createSystemUserDraft();
}

function cancelDetail() {
  if (savingUser.value) {
    return;
  }
  if (!selectedUser.value?.id) {
    closeDetail();
    return;
  }
  userDraft.value = copySystemUser(selectedUser.value);
  passwordDraft.value = '';
  resetPasswordResult.value = undefined;
  userExternalChange.clearExternalChanged();
  detailMode.value = 'view';
  loadingDetail.value = false;
  detailLoadFailed.value = false;
}

function handleDetailAction(action: RecordActionItem) {
  if (action.key === 'cancel') {
    cancelDetail();
    return;
  }
  if (action.key === 'save') {
    void saveUser();
    return;
  }
  if (!canLeaveDetailContext()) {
    return;
  }
  if (action.key === 'edit' && selectedUser.value) {
    userDraft.value = copySystemUser(selectedUser.value);
    userExternalChange.clearExternalChanged();
    detailMode.value = 'edit';
    return;
  }
  if (action.key === 'resetPassword' && selectedUser.value) {
    passwordDraft.value = '';
    resetPasswordResult.value = undefined;
    userExternalChange.clearExternalChanged();
    detailMode.value = 'resetPassword';
    return;
  }
  if (action.key === 'resetGeneratedPassword' && selectedUser.value) {
    void resetUserLoginPassword();
  }
}

function retryDetail() {
  const id = String(userDraft.value.id ?? selectedUserKey.value ?? '');
  if (!id) {
    return;
  }
  void openDetail({ ...userDraft.value, id } as QueryListRecord, detailMode.value);
}

function reloadExternalUserChange() {
  const id = String(
    userExternalChange.externalChangedRecordId.value ?? userDraft.value.id ?? selectedUserKey.value ?? '',
  );
  if (!id) {
    return;
  }
  userExternalChange.clearExternalChanged();
  void openDetail({ ...userDraft.value, id } as QueryListRecord, 'edit');
}

async function saveUser() {
  if (detailMode.value === 'resetPassword') {
    await resetUserPassword();
    return;
  }
  await executeStaticFormSave<UserAccount>({
    loading: savingUser,
    mode: 'edit',
    source: 'system-user-management',
    canSave: () => canSaveUser.value,
    deniedMessage: '当前用户无权保存系统账号',
    createRecord: () => normalizedSystemUserDraft(userDraft.value),
    validateRecord: validateSystemUserDraft,
    save: (draft) => userContext.crud.update(selectedUser.value!.id!, draft),
    onSaved: ({ record }) => {
      commitDetailRecord(record);
      reloadKey.value += 1;
    },
  });
}

async function resetUserPassword() {
  if (!passwordDraft.value.trim()) {
    presentPlatformMessage('请填写新密码', { source: 'system-user-management', phase: 'validation' });
    return;
  }
  await executeStaticRecordAction<UserAccount, number>({
    loading: savingUser,
    source: 'system-user-management',
    record: () => (selectedUser.value?.id ? selectedUser.value : undefined),
    canExecute: () => canSaveUser.value,
    deniedMessage: '当前用户无权重置系统账号密码',
    execute: (user) =>
      userContext.http.request<number>({
        method: 'POST',
        path: `/iam.user/changePassword/${encodeURIComponent(user.id!)}`,
        body: { password: passwordDraft.value },
      }),
    onExecuted: async (_, user) => {
      const refreshed = await userContext.crud.view(user.id!);
      commitDetailRecord(refreshed);
      reloadKey.value += 1;
    },
  });
}

async function resetUserLoginPassword() {
  await executeStaticRecordAction<UserAccount, ResetPasswordResponse>({
    loading: savingUser,
    source: 'system-user-management',
    record: () => (selectedUser.value?.id ? selectedUser.value : undefined),
    canExecute: (user) => userContext.can('resetPassword', user.id) === true,
    deniedMessage: '当前用户无权重置系统账号密码',
    execute: (user) =>
      userContext.http.request<ResetPasswordResponse>({
        method: 'POST',
        path: `/iam.user/resetPassword/${encodeURIComponent(user.id!)}`,
      }),
    onExecuted: async (result, user) => {
      const refreshed = await userContext.crud.view(user.id!);
      commitDetailRecord(refreshed);
      resetPasswordResult.value = result;
      reloadKey.value += 1;
    },
  });
}

async function revokeUserSession(record: Partial<UserAccount> | QueryListRecord, session: UserSessionView) {
  await executeStaticRecordAction<UserAccount, number>({
    loading: savingUser,
    source: 'system-user-management',
    record: () => (record?.id ? (record as UserAccount) : undefined),
    canExecute: (user) => userContext.can('revokeSession', user.id) === true && !session.current,
    deniedMessage: '当前用户无权下线该登录会话',
    confirm: (user) =>
      confirmAction({
        title: '下线登录会话',
        content: `确认下线系统账号「${systemUserTitle(user)}」的该登录会话？`,
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
      reloadKey.value += 1;
    },
  });
}

async function revokeAllUserSessions(record: Partial<UserAccount> | QueryListRecord) {
  const userId = String(record.id ?? '');
  const sessionIds = revokableUserSessions(userId).map((session) => session.id);
  if (sessionIds.length === 0) {
    presentPlatformMessage('当前没有可下线的登录会话', {
      source: 'system-user-management',
      phase: 'validation',
    });
    return;
  }
  await executeStaticRecordAction<UserAccount, number>({
    loading: savingUser,
    source: 'system-user-management',
    record: () => (record?.id ? (record as UserAccount) : undefined),
    canExecute: (user) => userContext.can('revokeSessions', user.id) === true,
    deniedMessage: '当前用户无权批量下线登录会话',
    confirm: (user) =>
      confirmAction({
        title: '批量下线登录会话',
        content: `确认下线系统账号「${systemUserTitle(user)}」的 ${sessionIds.length} 个登录会话？`,
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
      reloadKey.value += 1;
    },
  });
}

async function toggleUserEnabled() {
  await executeStaticRecordAction({
    loading: savingUser,
    source: 'system-user-management',
    record: () => (selectedUser.value?.id ? selectedUser.value : undefined),
    canExecute: () => canToggleUser.value,
    deniedMessage: '当前用户无权变更系统账号启停状态',
    execute: (user) =>
      user.enabled === false ? userContext.crud.enable(user.id!) : userContext.crud.disable(user.id!),
    onExecuted: async (_, user) => {
      const refreshed = await userContext.crud.view(user.id!);
      commitDetailRecord(refreshed);
      reloadKey.value += 1;
    },
  });
}

function canLeaveDetailContext() {
  return !savingUser.value;
}

function canCommitDetailRequest(recordId: string, requestSeq: number) {
  return detailRequestSeq.value === requestSeq && selectedUserKey.value === recordId;
}

function commitDetailRecord(record: UserAccount, nextMode: SystemUserDetailMode = 'view') {
  selectedUser.value = record;
  selectedUserKey.value = record.id;
  userDraft.value = copySystemUser(record);
  passwordDraft.value = '';
  detailMode.value = nextMode === 'edit' ? 'edit' : 'view';
  userExternalChange.clearExternalChanged();
  detailOpen.value = true;
  loadingDetail.value = false;
  detailLoadFailed.value = false;
  detailRequestSeq.value += 1;
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

function sessionTitle(session: UserSessionView) {
  return session.loginUserAgent || session.loginIp || session.id;
}

function sessionTerminalTitle(session: UserSessionView) {
  const terminal = session.terminalTypeTitle || '其他终端';
  const platform = session.platformTypeTitle;
  return platform ? `${terminal} / ${platform}` : terminal;
}

function sessionTime(value: string | undefined) {
  return value ?? '-';
}

function createSystemUserDraft(): Partial<UserAccount> {
  return {
    enabled: true,
  };
}

function copySystemUser(record: Partial<UserAccount>): Partial<UserAccount> {
  return { ...record, password: undefined };
}

function normalizedSystemUserDraft(draft: Partial<UserAccount>): UserAccount {
  return {
    ...draft,
    tenantId: undefined,
    username: draft.username?.trim(),
    enabled: draft.enabled !== false,
    password: undefined,
  } as UserAccount;
}

function validateSystemUserDraft(draft: UserAccount) {
  const field = resolveRecordFormFieldState('username', {
    fields: formFieldDefinitions.value,
    fallback: formFieldFallback.value,
  });
  if (field.visible && field.required && !draft.username) {
    return `请填写${field.label}`;
  }
  if (detailMode.value === 'resetPassword' && !passwordDraft.value.trim()) {
    return '请填写新密码';
  }
  return undefined;
}

function systemUserFormFieldDisabled(fieldName: string) {
  return fieldName === 'username';
}

function updateUserDraftField(fieldName: string, value: string | number | boolean | undefined) {
  userDraft.value = {
    ...userDraft.value,
    [fieldName]: value,
  };
}

function systemUserToggleActionCode(record: Partial<UserAccount>) {
  return record.enabled === false ? 'enable' : 'disable';
}

function systemUserTitle(record: Partial<UserAccount> | QueryListRecord | undefined) {
  return String(record?.username ?? record?.id ?? '系统账号');
}
</script>

<template>
  <section class="system-user-management-page">
    <RecordQueryListPanel
      class="system-user-list-panel"
      :context="systemUserContext"
      title="系统账号"
      :columns="columns"
      :cell-renderers="{ onlineStatus: userOnlineStatusTitle }"
      :row-actions-of="rowActionsOf"
      :selected-key="selectedUserKey"
      :expanded-row-keys="expandedUserKeys"
      :reload-key="reloadKey"
      :ready="true"
      quick-search-placeholder="搜索账号、姓名、手机号或邮箱"
      empty-description="暂无系统账号"
      @row-action="handleRowAction"
      @row-dblclick="handleRowDblclick"
      @row-expand="handleUserRowExpand"
      @loaded="handleUserListLoaded"
      @select="selectedUserKey = String($event.id ?? '')"
    >
      <template #expandedRow="{ record }">
        <section class="system-user-session-section">
          <div class="system-user-session-header">
            <h3>在线会话</h3>
            <div class="system-user-session-actions">
              <UiButton
                type="text"
                icon-name="reload"
                :disabled="userSessionState(String(record.id ?? '')).loading"
                @click="loadUserSessions(String(record.id ?? ''))"
              >
                刷新
              </UiButton>
              <UiButton
                v-if="revokableUserSessions(String(record.id ?? '')).length > 1"
                danger
                icon-name="power"
                :disabled="savingUser || userSessionState(String(record.id ?? '')).loading"
                @click="revokeAllUserSessions(record)"
              >
                全部下线
              </UiButton>
            </div>
          </div>
          <UiSpin
            v-if="userSessionState(String(record.id ?? '')).loading"
            class="system-user-session-state"
            tip="加载在线会话"
          />
          <UiError
            v-else-if="userSessionState(String(record.id ?? '')).error"
            title="在线会话加载失败"
            :message="userSessionState(String(record.id ?? '')).error ?? '无法加载在线会话，请重试'"
          />
          <p
            v-else-if="userSessionState(String(record.id ?? '')).records.length === 0"
            class="system-user-session-empty"
          >
            当前无在线会话
          </p>
          <div v-else class="system-user-session-list">
            <article
              v-for="session in userSessionState(String(record.id ?? '')).records"
              :key="session.id"
              class="system-user-session-item"
            >
              <div class="system-user-session-main">
                <strong :title="sessionTitle(session)">{{ sessionTitle(session) }}</strong>
                <span v-if="session.current" class="system-user-session-badge">当前会话</span>
              </div>
              <dl class="system-user-session-meta">
                <div>
                  <dt>登录</dt>
                  <dd :title="sessionTime(session.issuedAt)">{{ sessionTime(session.issuedAt) }}</dd>
                </div>
                <div>
                  <dt>活跃</dt>
                  <dd :title="sessionTime(session.lastSeenAt)">{{ sessionTime(session.lastSeenAt) }}</dd>
                </div>
                <div>
                  <dt>IP</dt>
                  <dd :title="session.loginIp || '-'">{{ session.loginIp || '-' }}</dd>
                </div>
                <div>
                  <dt>终端</dt>
                  <dd :title="sessionTerminalTitle(session)">{{ sessionTerminalTitle(session) }}</dd>
                </div>
              </dl>
              <UiButton
                danger
                icon-name="power"
                :disabled="savingUser || !canRevokeUserSession(String(record.id ?? ''), session)"
                @click="revokeUserSession(record, session)"
              >
                下线
              </UiButton>
            </article>
          </div>
        </section>
      </template>
    </RecordQueryListPanel>

    <RecordModeDrawer
      :open="detailOpen"
      :title="detailTitle"
      :mode="detailMode"
      :form-modes="['edit', 'resetPassword']"
      :loading="loadingDetail"
      :load-failed="detailLoadFailed"
      :externally-changed="userExternalChange.externallyChanged.value"
      error-title="详情加载失败"
      error-message="无法加载系统账号详情，请重试"
      @close="closeDetail"
      @retry="retryDetail"
      @reload-external-change="reloadExternalUserChange"
      @dismiss-external-change="userExternalChange.clearExternalChanged"
    >
      <template #status>
        <RecordStatusSwitch
          v-if="detailMode === 'view' && selectedUser"
          :enabled="selectedUser.enabled !== false"
          :disabled="savingUser || !canToggleUser"
          :loading="savingUser"
          :show-label="false"
          @change="toggleUserEnabled"
        />
      </template>
      <template #actions>
        <RecordActionBar
          :context="systemUserContext"
          :actions="detailActions"
          :record-id="selectedUser?.id"
          @action="handleDetailAction"
        />
      </template>

      <template #loading>
        <UiSpin class="system-user-detail-state" tip="加载系统账号详情" />
      </template>
      <template #error>
        <div class="system-user-detail-state">
          <UiError title="详情加载失败" message="无法加载系统账号详情，请重试" />
          <UiButton type="primary" icon-name="reload" @click="retryDetail">重试</UiButton>
        </div>
      </template>

      <template #view>
        <RecordDetailFields
          :record="userDraft as RecordFormRecord"
          :fields="formFieldDefinitions"
          :fallback="formFieldFallback"
        />
        <div
          v-if="detailMode === 'view' && resetPasswordResult?.temporaryPassword"
          class="system-user-password-reset-result"
        >
          <span>临时密码</span>
          <UiInput :value="resetPasswordResult.temporaryPassword" disabled />
          <small v-if="resetPasswordResult.expiresAt">有效期至 {{ resetPasswordResult.expiresAt }}</small>
        </div>
        <RecordMetaSection :record="userDraft" />
      </template>

      <template #form>
        <form class="system-user-form" @submit.prevent="saveUser">
          <RecordFormFields
            v-if="detailMode !== 'resetPassword'"
            :record="userDraft as RecordFormRecord"
            :field-names="formFieldNames"
            :fields="formFieldDefinitions"
            :fallback="formFieldFallback"
            :disabled="formDisabled"
            :disabled-of="systemUserFormFieldDisabled"
            @update:field="updateUserDraftField"
          />
          <label v-else>
            <span class="system-user-form-label">新密码</span>
            <UiInput
              :value="passwordDraft"
              type="password"
              :disabled="formDisabled"
              placeholder="请输入密码"
              allow-clear
              @update:value="passwordDraft = $event"
            />
          </label>
        </form>
        <RecordMetaSection v-if="detailMode !== 'resetPassword'" :record="userDraft" />
      </template>
    </RecordModeDrawer>
  </section>
</template>

<style scoped>
.system-user-management-page {
  position: relative;
  display: grid;
  height: calc(100vh - 116px);
  min-height: 0;
  overflow: hidden;
}

.system-user-list-panel {
  min-width: 0;
  min-height: 0;
}

.system-user-form {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
}

.system-user-form > label {
  display: grid;
  gap: 6px;
  color: var(--muyun-text-muted);
  font-size: 13px;
}

.system-user-form-label {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.system-user-password-reset-result {
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

.system-user-password-reset-result small {
  color: var(--muyun-text-muted);
}

.system-user-session-section {
  display: grid;
  gap: 8px;
  padding: 12px 16px 14px 46px;
  border-top: 1px solid var(--muyun-border-subtle);
  background: #fbfcfe;
}

.system-user-session-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.system-user-session-header h3 {
  margin: 0;
  color: var(--muyun-text);
  font-size: 13px;
  font-weight: 700;
}

.system-user-session-actions {
  display: inline-flex;
  gap: 4px;
}

.system-user-session-state {
  min-height: 56px;
}

.system-user-session-empty {
  margin: 0;
  color: var(--muyun-text-muted);
  font-size: 13px;
}

.system-user-session-list {
  display: grid;
  gap: 0;
  border: 1px solid var(--muyun-border-subtle);
  border-radius: 6px;
  background: var(--muyun-surface);
}

.system-user-session-item {
  display: grid;
  grid-template-columns: minmax(180px, 1.2fr) minmax(360px, 2fr) auto;
  gap: 10px;
  align-items: center;
  padding: 10px 12px;
  border-bottom: 1px solid var(--muyun-border-subtle);
}

.system-user-session-item:last-child {
  border-bottom: 0;
}

.system-user-session-main {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 8px;
}

.system-user-session-main strong {
  min-width: 0;
  overflow: hidden;
  color: var(--muyun-text);
  font-size: 13px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.system-user-session-badge {
  flex: none;
  padding: 1px 6px;
  border-radius: 999px;
  background: var(--muyun-hover-subtle);
  color: var(--muyun-text-muted);
  font-size: 12px;
}

.system-user-session-meta {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin: 0;
}

.system-user-session-meta div {
  min-width: 0;
}

.system-user-session-meta dt {
  color: var(--muyun-text-muted);
  font-size: 11px;
  line-height: 1.2;
}

.system-user-session-meta dd {
  margin: 2px 0 0;
  overflow: hidden;
  overflow-wrap: anywhere;
  color: var(--muyun-text);
  font-size: 12px;
  line-height: 1.25;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.system-user-session-item > :deep(.ant-btn) {
  min-width: 64px;
  height: 28px;
  padding: 0 10px;
  font-size: 12px;
}

.system-user-detail-state {
  display: grid;
  place-items: center;
  gap: 12px;
  min-height: 180px;
}
</style>
