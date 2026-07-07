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
import { UiButton, UiError, UiInput, UiSpin } from '@muyun/vue-ui-antdv';
import type {
  ResetPasswordResponse,
  UserAccount,
  WebCountResponse,
  WebQueryRequest,
} from '@muyun/web-contracts';
import { useModuleContext, type ModuleContext } from '@muyun/web-core';

defineOptions({ name: 'SystemUserManagementView' });

type SystemUserDetailMode = 'view' | 'edit' | 'resetPassword';
type SystemUserFormFieldName = 'username' | 'title' | 'mobile' | 'email' | 'enabled' | 'sortOrder';

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

const systemUserContext = computed(
  () => createSystemUserModuleContext(userContext) as ModuleContext<QueryListRecord>,
);
const columns = computed<RecordQueryListColumn[]>(() => [
  { key: 'username', title: '账号', width: '18%' },
  { key: 'title', title: '姓名', width: '18%' },
  { key: 'mobile', title: '手机号', width: '16%' },
  { key: 'email', title: '邮箱', width: '18%' },
  { key: 'enabled', title: '状态', type: 'enabledStatus', width: '10%' },
  { key: 'sortOrder', title: '排序', width: '10%' },
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
    return Boolean(selectedUser.value?.id) && userContext.can('changePassword') === true;
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
  title: { label: '姓名', visible: true, placeholder: '请输入姓名或显示名' },
  mobile: { label: '手机号', visible: true, placeholder: '请输入手机号' },
  email: { label: '邮箱', visible: true, placeholder: '请输入邮箱' },
  enabled: { label: '启用状态', visible: true, controlType: 'enabledStatus' },
  sortOrder: { label: '排序号', visible: true, placeholder: '请输入排序号' },
}));
const formFieldNames = computed<SystemUserFormFieldName[]>(() => [
  'username',
  'title',
  'mobile',
  'email',
  'enabled',
  'sortOrder',
]);

onMounted(loadFormDefinition);

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
    detailMode.value = 'edit';
    return;
  }
  if (action.key === 'resetPassword' && selectedUser.value) {
    passwordDraft.value = '';
    resetPasswordResult.value = undefined;
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
  await executeStaticRecordAction<UserAccount, WebCountResponse>({
    loading: savingUser,
    source: 'system-user-management',
    record: () => (selectedUser.value?.id ? selectedUser.value : undefined),
    canExecute: () => canSaveUser.value,
    deniedMessage: '当前用户无权重置系统账号密码',
    execute: (user) =>
      userContext.http.request<WebCountResponse>({
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
    canExecute: () => userContext.can('resetPassword') === true,
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
  detailOpen.value = true;
  loadingDetail.value = false;
  detailLoadFailed.value = false;
  detailRequestSeq.value += 1;
}

function createSystemUserDraft(): Partial<UserAccount> {
  return {
    enabled: true,
    sortOrder: 100,
  };
}

function copySystemUser(record: Partial<UserAccount>): Partial<UserAccount> {
  return { ...record, password: undefined };
}

function normalizedSystemUserDraft(draft: Partial<UserAccount>): UserAccount {
  return {
    ...draft,
    tenantId: undefined,
    organizationId: undefined,
    username: draft.username?.trim(),
    title: draft.title?.trim() || undefined,
    mobile: draft.mobile?.trim() || undefined,
    email: draft.email?.trim() || undefined,
    enabled: draft.enabled !== false,
    sortOrder: normalizeSortOrder(draft.sortOrder),
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

function normalizeSortOrder(value: unknown) {
  if (typeof value === 'number') {
    return Number.isFinite(value) ? value : 100;
  }
  const parsed = Number(String(value ?? '').trim());
  return Number.isFinite(parsed) ? parsed : 100;
}

function systemUserToggleActionCode(record: Partial<UserAccount>) {
  return record.enabled === false ? 'enable' : 'disable';
}

function systemUserTitle(record: Partial<UserAccount> | QueryListRecord | undefined) {
  return String(record?.title ?? record?.username ?? record?.id ?? '系统账号');
}
</script>

<template>
  <section class="system-user-management-page">
    <RecordQueryListPanel
      class="system-user-list-panel"
      :context="systemUserContext"
      title="系统账号"
      :columns="columns"
      :row-actions-of="rowActionsOf"
      :selected-key="selectedUserKey"
      :reload-key="reloadKey"
      :ready="true"
      quick-search-placeholder="搜索账号、姓名、手机号或邮箱"
      empty-description="暂无系统账号"
      @row-action="handleRowAction"
      @row-dblclick="handleRowDblclick"
      @select="selectedUserKey = String($event.id ?? '')"
    />

    <RecordModeDrawer
      :open="detailOpen"
      :title="detailTitle"
      :mode="detailMode"
      :form-modes="['edit', 'resetPassword']"
      :loading="loadingDetail"
      :load-failed="detailLoadFailed"
      error-title="详情加载失败"
      error-message="无法加载系统账号详情，请重试"
      @close="closeDetail"
      @retry="retryDetail"
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
        <RecordActionBar :context="systemUserContext" :actions="detailActions" @action="handleDetailAction" />
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
        <RecordMetaSection :record="userDraft" show-sort-order />
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
        <RecordMetaSection v-if="detailMode !== 'resetPassword'" :record="userDraft" show-sort-order />
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

.system-user-detail-state {
  display: grid;
  place-items: center;
  gap: 12px;
  min-height: 180px;
}
</style>
