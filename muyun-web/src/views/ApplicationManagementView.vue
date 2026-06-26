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
import type { Application } from '@muyun/web-contracts';
import { useModuleContext } from '@muyun/web-core';
import { confirmAction, UiInput } from '@muyun/vue-ui-antdv';
import { createApplicationManagementState } from './applicationManagementState';

defineOptions({ name: 'ApplicationManagementView' });

const applicationContext = useModuleContext<Application>();
const explorerSearchKeyword = ref('');
const {
  selected,
  draft,
  mode,
  reloadKey,
  saving,
  actionError,
  actionMessage,
  cardTitle,
  readonly,
  aliasReadonly,
  handleListLoaded,
  handleSelect,
  startCreate,
  startEdit,
  cancelEdit,
  save,
  toggleEnabled,
  removeSelected,
} = createApplicationManagementState(applicationContext, confirmAction);

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
      disabled: !selected.value,
      loading: saving.value,
    },
    {
      key: 'delete',
      actionCode: 'delete',
      title: '删除',
      disabled: !selected.value,
      loading: saving.value,
      danger: true,
    },
  ];
});

function applicationTitle(record: CrudRecordListBase) {
  return record.title ?? record.alias ?? record.id ?? '未命名应用';
}

function applicationSubtitle(record: CrudRecordListBase) {
  return record.alias ?? record.id;
}

function handleLoaded(records: CrudRecordListBase[]) {
  handleListLoaded(records as Application[]);
}

function handleApplicationSelect(record: CrudRecordListBase) {
  handleSelect(record as Application);
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
    sidebar-title="应用列表"
    refresh-title="刷新应用列表"
    v-model:sidebar-search-keyword="explorerSearchKeyword"
    sidebar-search-placeholder="搜索应用名称、alias 或 ID"
    :mode="mode"
    :card-title="cardTitle"
    :action-error="actionError"
    :action-message="actionMessage"
    :show-status="Boolean(selected && mode === 'view')"
    :enabled="selected?.enabled"
    @refresh="reloadKey += 1"
  >
    <template #sidebar-actions>
      <ModuleActionButton
        class="record-panel-create-button"
        :context="applicationContext"
        action-code="create"
        title="新建应用"
        icon-only
        @click="startCreate"
      />
    </template>

    <template #explorer>
      <CrudRecordListExplorer
        :context="applicationContext"
        :selected-id="selected?.id"
        :reload-key="reloadKey"
        :keyword="explorerSearchKeyword"
        empty-description="暂无应用"
        loading-tip="加载应用列表"
        fallback-title="未命名应用"
        :title-of="applicationTitle"
        :subtitle-of="applicationSubtitle"
        @select="handleApplicationSelect"
        @loaded="handleLoaded"
      />
    </template>

    <template #card-actions>
      <RecordActionBar :context="applicationContext" :actions="cardActions" @action="handleCardAction" />
    </template>

    <form class="static-record-form" @submit.prevent="save">
      <label>
        <span>应用 alias</span>
        <UiInput v-model:value="draft.alias" :disabled="aliasReadonly" />
      </label>
      <label>
        <span>应用名称</span>
        <UiInput v-model:value="draft.title" :disabled="readonly" />
      </label>
      <label>
        <span>启用状态</span>
        <EnabledSelect v-model:value="draft.enabled" :disabled="readonly" />
      </label>
    </form>

    <RecordMetaSection :record="draft" show-sort-order />
  </StaticManagementLayout>
</template>
