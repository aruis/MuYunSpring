<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import {
  handlePlatformActionSuccess,
  presentPlatformError,
  presentPlatformMessage,
} from '@muyun/platform-components';
import { UiButton, UiEmpty, UiError, UiSelect, UiSpin, confirmAction } from '@muyun/vue-ui-antdv';
import type { AccountRoleGrant, ManagementScopeType, Role, UserSelectorItem } from '@muyun/web-contracts';
import type { ModuleContext } from '@muyun/web-core';
import { createRoleGrantClient } from './roleGrantClient';

defineOptions({ name: 'RoleAccountGrantPanel' });

const props = defineProps<{
  context: ModuleContext<Role>;
  role: Role;
  editable?: boolean;
}>();

const grants = ref<AccountRoleGrant[]>([]);
const usersById = ref<Record<string, UserSelectorItem>>({});
const userOptions = ref<UserSelectorItem[]>([]);
const selectedUserId = ref<string>();
const loading = ref(false);
const loadingUsers = ref(false);
const saving = ref(false);
const loadFailed = ref(false);

const client = computed(() => createRoleGrantClient(props.context.http));
const roleId = computed(() => props.role.id);
const canManage = computed(() => {
  const id = roleId.value;
  if (!id || props.role.systemManaged || props.editable !== true) {
    return false;
  }
  return props.context.can('accountRoleGrants', id) !== false;
});
const defaultManagementScopeType = computed<ManagementScopeType>(() => {
  if (props.role.ownerScopeType === 'platform') {
    return 'platform';
  }
  if (props.role.ownerScopeType === 'organization') {
    return 'organization';
  }
  return 'tenant';
});
const defaultManagementScopeId = computed(() =>
  defaultManagementScopeType.value === 'platform' ? undefined : props.role.ownerScopeId,
);
const scopeTitle = computed(() => managementScopeTitle(defaultManagementScopeType.value));
const selectedUserAlreadyBound = computed(() =>
  selectedUserId.value ? grants.value.some((grant) => grant.userId === selectedUserId.value) : false,
);
const grantRows = computed(() =>
  grants.value.map((grant) => ({
    grant,
    user: grant.userId ? usersById.value[grant.userId] : undefined,
  })),
);
const selectOptions = computed(() =>
  userOptions.value.map((user) => ({
    label: userTitle(user),
    value: user.id,
    disabled: grants.value.some((grant) => grant.userId === user.id),
  })),
);

onMounted(load);

watch(
  () => props.role.id,
  () => {
    selectedUserId.value = undefined;
    void load();
  },
);

async function load() {
  const id = roleId.value;
  if (!id) {
    grants.value = [];
    usersById.value = {};
    return;
  }
  loading.value = true;
  loadFailed.value = false;
  try {
    const [nextGrants, boundUsers] = await Promise.all([
      client.value.accountRoleGrants(id),
      client.value.userSelector({
        roleId: id,
        enabledOnly: false,
        page: { pageNum: 0, pageSize: 200 },
      }),
    ]);
    grants.value = nextGrants;
    usersById.value = mergeUsers(boundUsers.records);
    userOptions.value = boundUsers.records;
  } catch (cause) {
    loadFailed.value = true;
    presentPlatformError(cause, { source: 'role-account-grants', phase: 'load' });
  } finally {
    loading.value = false;
  }
}

async function searchUsers(keyword = '') {
  loadingUsers.value = true;
  try {
    const response = await client.value.userSelector({
      keyword,
      enabledOnly: true,
      page: { pageNum: 0, pageSize: 20 },
    });
    userOptions.value = response.records;
    usersById.value = mergeUsers([...Object.values(usersById.value), ...response.records]);
  } catch (cause) {
    presentPlatformError(cause, { source: 'role-account-grants', phase: 'load' });
  } finally {
    loadingUsers.value = false;
  }
}

async function grantSelectedUser() {
  const id = roleId.value;
  if (!id || !selectedUserId.value) {
    presentPlatformMessage('请选择用户账号', { source: 'role-account-grants', phase: 'validation' });
    return;
  }
  if (!canManage.value) {
    presentPlatformMessage('当前用户无权维护账号角色授权', {
      source: 'role-account-grants',
      phase: 'validation',
    });
    return;
  }
  if (selectedUserAlreadyBound.value) {
    presentPlatformMessage('该用户已经绑定当前角色', {
      source: 'role-account-grants',
      phase: 'validation',
    });
    return;
  }
  saving.value = true;
  try {
    const result = await client.value.grantAccountRole(id, {
      userId: selectedUserId.value,
      managementScopeType: defaultManagementScopeType.value,
      managementScopeId: defaultManagementScopeId.value,
    });
    selectedUserId.value = undefined;
    await load();
    await handlePlatformActionSuccess(result, {
      source: 'role-account-grants',
      phase: 'action',
      fallbackMessage: '账号角色已授权',
    });
  } catch (cause) {
    presentPlatformError(cause, { source: 'role-account-grants', phase: 'action' });
  } finally {
    saving.value = false;
  }
}

async function deleteGrant(grant: AccountRoleGrant) {
  const id = roleId.value;
  if (!id || !grant.id || !canManage.value) {
    return;
  }
  const confirmed = await confirmAction({
    title: '撤销账号授权',
    content: `确认撤销用户「${grantUserTitle(grant)}」的当前角色？`,
    okText: '撤销',
    danger: true,
  });
  if (!confirmed) {
    return;
  }
  saving.value = true;
  try {
    const result = await client.value.deleteAccountRoleGrant(id, grant.id);
    await load();
    await handlePlatformActionSuccess(result, {
      source: 'role-account-grants',
      phase: 'action',
      fallbackMessage: '账号角色授权已撤销',
    });
  } catch (cause) {
    presentPlatformError(cause, { source: 'role-account-grants', phase: 'action' });
  } finally {
    saving.value = false;
  }
}

function mergeUsers(users: UserSelectorItem[]) {
  const next: Record<string, UserSelectorItem> = { ...usersById.value };
  for (const user of users) {
    if (user.id) {
      next[user.id] = user;
    }
  }
  return next;
}

function grantUserTitle(grant: AccountRoleGrant) {
  return grant.userId ? userTitle(usersById.value[grant.userId]) : '未知用户';
}

function userTitle(user: UserSelectorItem | undefined) {
  return String(user?.username ?? user?.id ?? '未知用户');
}

function managementScopeTitle(type: ManagementScopeType | undefined) {
  if (type === 'platform') {
    return '平台级';
  }
  if (type === 'organization') {
    return '机构级';
  }
  return '租户级';
}

function handleSelectUser(value: unknown) {
  selectedUserId.value = typeof value === 'string' ? value : undefined;
}
</script>

<template>
  <section class="role-account-grant-section">
    <div class="role-account-grant-header">
      <div>
        <strong>账号授权</strong>
        <span>{{ scopeTitle }}</span>
      </div>
      <UiButton icon-name="reload" :disabled="loading || saving" @click="load">刷新</UiButton>
    </div>

    <UiSpin v-if="loading" class="role-account-grant-state" tip="加载账号授权" />
    <div v-else-if="loadFailed" class="role-account-grant-state">
      <UiError title="账号授权加载失败" message="无法加载当前角色的账号授权" />
      <UiButton type="primary" icon-name="reload" @click="load">重试</UiButton>
    </div>
    <template v-else>
      <form v-if="editable" class="role-account-grant-form" @submit.prevent="grantSelectedUser">
        <UiSelect
          show-search
          :filter-option="false"
          :value="selectedUserId"
          :options="selectOptions"
          :loading="loadingUsers"
          :disabled="saving || !canManage"
          placeholder="搜索并选择用户账号"
          @search="searchUsers"
          @focus="searchUsers('')"
          @update:value="handleSelectUser"
        />
        <UiButton
          type="primary"
          html-type="submit"
          icon-name="plus"
          :loading="saving"
          :disabled="!selectedUserId || selectedUserAlreadyBound || !canManage"
        >
          授权
        </UiButton>
      </form>

      <UiEmpty v-if="grantRows.length === 0" description="暂无绑定用户" />
      <div v-else class="role-account-grant-list">
        <div v-for="{ grant } in grantRows" :key="grant.id ?? grant.userId" class="role-account-grant-row">
          <div>
            <strong>{{ grantUserTitle(grant) }}</strong>
            <span>{{ grant.userId }}</span>
          </div>
          <span class="role-account-grant-scope">{{ managementScopeTitle(grant.managementScopeType) }}</span>
          <UiButton
            v-if="editable"
            danger
            icon-name="delete"
            :disabled="saving || !canManage"
            @click="deleteGrant(grant)"
          >
            撤销
          </UiButton>
        </div>
      </div>
    </template>
  </section>
</template>

<style scoped>
.role-account-grant-section {
  display: grid;
  gap: 12px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--muyun-border-subtle);
}

.role-account-grant-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.role-account-grant-header > div {
  display: grid;
  gap: 2px;
}

.role-account-grant-header strong {
  color: var(--muyun-text);
  font-size: 14px;
}

.role-account-grant-header span,
.role-account-grant-row span {
  color: var(--muyun-text-muted);
  font-size: 12px;
}

.role-account-grant-form {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  align-items: center;
}

.role-account-grant-list {
  display: grid;
  gap: 8px;
}

.role-account-grant-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  gap: 10px;
  align-items: center;
  padding: 10px 12px;
  border: 1px solid var(--muyun-border-subtle);
  border-radius: 8px;
}

.role-account-grant-row > div {
  display: grid;
  min-width: 0;
  gap: 2px;
}

.role-account-grant-row strong,
.role-account-grant-row span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.role-account-grant-scope {
  padding: 2px 8px;
  border-radius: 999px;
  background: var(--muyun-hover-subtle);
}

.role-account-grant-state {
  display: grid;
  justify-items: center;
  gap: 12px;
  padding: 24px 0;
}

@media (max-width: 640px) {
  .role-account-grant-form,
  .role-account-grant-row {
    grid-template-columns: 1fr;
    align-items: stretch;
  }
}
</style>
