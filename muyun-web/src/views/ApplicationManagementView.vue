<script setup lang="ts">
import { computed, ref } from 'vue';
import {
  CrudRecordListExplorer,
  type CrudRecordListBase,
  ModuleActionButton,
  RecordActionBar,
  RecordMetaSection,
  RecordStatusSwitch,
  RecycleBinModeButton,
  StaticManagementLayout,
  useRecycleBinExplorerMode,
  type RecordActionItem,
  type RecordExplorerItemDescriptor,
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
  cardTitle,
  readonly,
  aliasReadonly,
  canEnable,
  handleListLoaded,
  handleReadonlyListLoaded,
  handleSelect,
  startCreate,
  startEdit,
  cancelEdit,
  save,
  toggleEnabled,
  removeSelected,
} = createApplicationManagementState(applicationContext, confirmAction);
const recycleBinExplorer = useRecycleBinExplorerMode({
  context: applicationContext,
  listReloadKey: reloadKey,
  searchKeyword: explorerSearchKeyword,
  resetSelection,
});

const cardActions = computed<RecordActionItem[]>(() => {
  if (recycleBinExplorer.active.value) return [];
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
      disabled: !selected.value,
      loading: saving.value,
      danger: true,
    },
  ];
});

function applicationItemOf(record: CrudRecordListBase): RecordExplorerItemDescriptor {
  return {
    title: record.title ?? record.alias ?? record.id ?? '未命名应用',
    secondary: record.alias ?? record.id,
    muted: record.enabled === false,
  };
}

function handleLoaded(records: CrudRecordListBase[]) {
  if (recycleBinExplorer.active.value) {
    handleReadonlyListLoaded(records as Application[]);
    return;
  }
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

function resetSelection() {
  selected.value = undefined;
  draft.value = { alias: '', title: '', enabled: true };
  mode.value = 'view';
}
</script>

<template>
  <StaticManagementLayout
    v-model:explorer-search-keyword="explorerSearchKeyword"
    :explorer-title="recycleBinExplorer.active.value ? '回收站' : '应用列表'"
    :refresh-title="recycleBinExplorer.active.value ? '刷新回收站' : '刷新应用列表'"
    explorer-search-placeholder="搜索应用名称、alias 或 ID"
    :explorer-searchable="!recycleBinExplorer.active.value"
    :mode="mode"
    :detail-title="cardTitle"
    @refresh="recycleBinExplorer.refresh"
  >
    <template #explorer-actions>
      <ModuleActionButton
        v-if="!recycleBinExplorer.active.value"
        class="record-panel-create-button"
        :context="applicationContext"
        action-code="create"
        title="新建应用"
        icon-only
        @click="startCreate"
      />
    </template>
    <template #explorer-footer>
      <RecycleBinModeButton
        v-if="recycleBinExplorer.buttonVisible.value"
        :active="recycleBinExplorer.active.value"
        :has-records="recycleBinExplorer.hasRecords.value"
        :count="recycleBinExplorer.total.value"
        @click="recycleBinExplorer.toggle"
      />
    </template>

    <template #explorer>
      <CrudRecordListExplorer
        :context="applicationContext"
        :selected-id="selected?.id"
        :reload-key="recycleBinExplorer.reloadKey.value"
        :mode="recycleBinExplorer.mode.value"
        :keyword="explorerSearchKeyword"
        :empty-description="recycleBinExplorer.active.value ? '回收站为空' : '暂无应用'"
        :loading-tip="recycleBinExplorer.active.value ? '加载回收站' : '加载应用列表'"
        fallback-title="未命名应用"
        :item-of="applicationItemOf"
        @recycle-bin-summary="recycleBinExplorer.updateSummary"
        @select="handleApplicationSelect"
        @loaded="handleLoaded"
      />
    </template>

    <template #detail-actions>
      <RecordActionBar :context="applicationContext" :actions="cardActions" @action="handleCardAction" />
    </template>
    <template #detail-status>
      <template v-if="!recycleBinExplorer.active.value">
        <RecordStatusSwitch
          v-if="mode !== 'view'"
          :enabled="draft.enabled"
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
    </form>

    <RecordMetaSection :record="draft" show-sort-order />
  </StaticManagementLayout>
</template>
