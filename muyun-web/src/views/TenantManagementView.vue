<script setup lang="ts">
import { computed, ref } from 'vue';
import {
  CrudRecordListExplorer,
  type CrudRecordListBase,
  ModuleActionButton,
  RecordActionBar,
  RecordMetaSection,
  RecordStatusSwitch,
  StaticManagementLayout,
  type RecordActionItem,
  type RecordExplorerItemDescriptor,
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
  cardTitle,
  readonly,
  aliasReadonly,
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

const enabledReadonly = computed(() => false);

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
      key: 'delete',
      actionCode: 'delete',
      title: '删除',
      disabled: !selected.value || !canDelete.value,
      loading: saving.value,
      danger: true,
    },
  ];
});

function tenantItemOf(record: CrudRecordListBase): RecordExplorerItemDescriptor {
  return {
    title: record.title ?? record.alias ?? record.id ?? '未命名租户',
    secondary: record.alias ?? record.id,
    muted: record.enabled === false,
  };
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
        :item-of="tenantItemOf"
        @select="handleTenantSelect"
        @loaded="handleLoaded"
      />
    </template>

    <template #card-actions>
      <RecordActionBar :context="tenantContext" :actions="cardActions" @action="handleCardAction" />
    </template>
    <template #card-status>
      <RecordStatusSwitch
        v-if="mode !== 'view'"
        :enabled="draft.enabled"
        :disabled="enabledReadonly"
        :show-label="false"
        @change="draft.enabled = $event"
      />
      <RecordStatusSwitch
        v-else-if="selected"
        :enabled="selected.enabled"
        :disabled="saving || !canEnable"
        :loading="saving"
        :show-label="false"
        @change="toggleEnabled"
      />
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
    </form>

    <RecordMetaSection :record="draft" show-sort-order />
  </StaticManagementLayout>
</template>
