<script setup lang="ts">
import { computed, ref } from 'vue';
import {
  CrudRecordListExplorer,
  type CrudRecordListBase,
  EnabledSelect,
  ModuleActionButton,
  RecordActionBar,
  RecordMetaSection,
  StaticManagementLayout,
  type RecordActionItem,
} from '@muyun/platform-components';
import type { Tenant } from '@muyun/web-contracts';
import { useModuleContext } from '@muyun/web-core';
import { confirmAction, UiInput } from '@muyun/vue-ui-antdv';
import { createTenantManagementState } from './tenantManagementState';

defineOptions({ name: 'TenantManagementView' });

const tenantContext = useModuleContext<Tenant>();
const explorerSearchKeyword = ref('');
const {
  selected,
  draft,
  mode,
  reloadKey,
  saving,
  actionMessage,
  cardTitle,
  readonly,
  aliasReadonly,
  isPlatformTenant,
  canDelete,
  canEnable,
  handleListLoaded,
  handleSelect,
  startCreate,
  startEdit,
  cancelEdit,
  save,
  toggleEnabled,
  removeSelected,
} = createTenantManagementState(tenantContext, confirmAction);

const enabledReadonly = computed(
  () => readonly.value || (isPlatformTenant.value && draft.value.enabled !== false),
);

const cardActions = computed<RecordActionItem[]>(() => {
  if (mode.value !== 'view') {
    return [
      { key: 'cancel', title: '取消', disabled: saving.value },
      {
        key: 'save',
        actionCode: mode.value === 'create' ? 'create' : 'update',
        title: saving.value ? '保存中' : '保存',
        loading: saving.value,
        primary: true,
      },
    ];
  }
  return [
    { key: 'edit', actionCode: 'update', title: '编辑', disabled: !selected.value },
    {
      key: 'toggle-enabled',
      actionCode: selected.value?.enabled === false ? 'enable' : 'disable',
      title: selected.value?.enabled === false ? '启用' : '停用',
      disabled: !selected.value || !canEnable.value,
      loading: saving.value,
    },
    {
      key: 'delete',
      actionCode: 'delete',
      title: '删除',
      disabled: !selected.value || !canDelete.value,
      loading: saving.value,
      danger: true,
    },
  ];
});

function tenantTitle(record: CrudRecordListBase) {
  return record.title ?? record.alias ?? record.id ?? '未命名租户';
}

function tenantSubtitle(record: CrudRecordListBase) {
  return record.alias ?? record.id;
}

function handleLoaded(records: CrudRecordListBase[]) {
  handleListLoaded(records as Tenant[]);
}

function handleTenantSelect(record: CrudRecordListBase) {
  handleSelect(record as Tenant);
}

function handleCardAction(action: RecordActionItem) {
  if (action.key === 'edit') {
    startEdit();
    return;
  }
  if (action.key === 'toggle-enabled') {
    void toggleEnabled();
    return;
  }
  if (action.key === 'delete') {
    void removeSelected();
    return;
  }
  if (action.key === 'cancel') {
    cancelEdit();
    return;
  }
  if (action.key === 'save') {
    void save();
  }
}
</script>

<template>
  <StaticManagementLayout
    v-model:sidebar-search-keyword="explorerSearchKeyword"
    sidebar-title="租户列表"
    refresh-title="刷新租户列表"
    sidebar-search-placeholder="搜索租户名称、alias 或 ID"
    :mode="mode"
    :card-title="cardTitle"
    :action-message="actionMessage"
    :muted-message="isPlatformTenant ? '平台租户是系统内置身份根，不能删除或停用。' : undefined"
    :show-status="Boolean(selected && mode === 'view')"
    :enabled="selected?.enabled"
    @refresh="reloadKey += 1"
  >
    <template #sidebar-actions>
      <ModuleActionButton
        class="record-panel-create-button"
        :context="tenantContext"
        action-code="create"
        title="新建租户"
        icon-only
        @click="startCreate"
      />
    </template>

    <template #explorer>
      <CrudRecordListExplorer
        :context="tenantContext"
        :selected-id="selected?.id"
        :reload-key="reloadKey"
        :keyword="explorerSearchKeyword"
        empty-description="暂无租户"
        loading-tip="加载租户列表"
        fallback-title="未命名租户"
        :title-of="tenantTitle"
        :subtitle-of="tenantSubtitle"
        @select="handleTenantSelect"
        @loaded="handleLoaded"
      />
    </template>

    <template #card-actions>
      <RecordActionBar :context="tenantContext" :actions="cardActions" @action="handleCardAction" />
    </template>

    <form class="static-record-form" @submit.prevent="save">
      <label>
        <span>租户 alias</span>
        <UiInput v-model:value="draft.alias" :disabled="aliasReadonly" />
      </label>
      <label>
        <span>租户名称</span>
        <UiInput v-model:value="draft.title" :disabled="readonly" />
      </label>
      <label>
        <span>启用状态</span>
        <EnabledSelect v-model:value="draft.enabled" :disabled="enabledReadonly" />
      </label>
    </form>

    <RecordMetaSection :record="draft" show-sort-order />
  </StaticManagementLayout>
</template>
