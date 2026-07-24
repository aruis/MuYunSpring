<script setup lang="ts">
import { computed, ref } from 'vue';
import {
  ModuleActionButton,
  RecordActionBar,
  parentRecordConstraints,
  RecordMetaSection,
  RecordPicker,
  RecordStatusSwitch,
  StaticManagementLayout,
  TreeRecordExplorer,
  type RecordActionItem,
  type RecordExplorerItemDescriptor,
  type RecordPickerRecord,
  type TreeRecordBase,
} from '@muyun/platform-components';
import type { Organization } from '@muyun/web-contracts';
import { useModuleContext } from '@muyun/web-core';
import { confirmAction, UiInput } from '@muyun/vue-ui-antdv';
import { createOrganizationManagementState } from './organizationManagementState';

defineOptions({ name: 'OrganizationManagementView' });

const organizationContext = useModuleContext<Organization>();
const explorerSearchKeyword = ref('');
const {
  selected,
  draft,
  mode,
  reloadKey,
  saving,
  cardTitle,
  readonly,
  canEnable,
  handleTreeLoaded,
  handleSelect,
  startCreateRoot,
  startCreateChild,
  startEdit,
  cancelEdit,
  save,
  toggleEnabled,
  removeSelected,
} = createOrganizationManagementState(organizationContext, confirmAction);

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
    { key: 'create-child', actionCode: 'create', title: '新建下级', disabled: !selected.value },
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

function organizationTitle(record: RecordPickerRecord) {
  return record.title ?? record.code ?? record.id ?? '未命名机构';
}

function organizationItemOf(record: TreeRecordBase): RecordExplorerItemDescriptor {
  return {
    title: record.title ?? record.code ?? record.id ?? '未命名机构',
    secondary: record.code ?? record.id,
    muted: record.enabled === false,
  };
}

function handleCardAction(action: RecordActionItem) {
  if (action.key === 'edit') {
    startEdit();
    return;
  }
  if (action.key === 'create-child') {
    startCreateChild();
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
    v-model:explorer-search-keyword="explorerSearchKeyword"
    explorer-title="机构树"
    refresh-title="刷新机构树"
    explorer-search-placeholder="搜索机构名称、编码或 ID"
    :mode="mode"
    :detail-title="cardTitle"
    @refresh="reloadKey += 1"
  >
    <template #explorer-actions>
      <ModuleActionButton
        class="record-panel-create-button"
        :context="organizationContext"
        action-code="create"
        title="新建根机构"
        icon-only
        @click="startCreateRoot"
      />
    </template>

    <template #explorer>
      <TreeRecordExplorer
        :context="organizationContext"
        :selected-id="selected?.id"
        :reload-key="reloadKey"
        :keyword="explorerSearchKeyword"
        search-mode="none"
        search-placeholder="搜索机构名称、编码或 ID"
        empty-description="暂无机构"
        loading-tip="加载机构树"
        fallback-title="未命名机构"
        :item-of="organizationItemOf"
        @select="handleSelect"
        @loaded="handleTreeLoaded($event as Organization[])"
      />
    </template>

    <template #detail-actions>
      <RecordActionBar :context="organizationContext" :actions="cardActions" @action="handleCardAction" />
    </template>
    <template #detail-status>
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

    <form class="static-record-form" @submit.prevent="save">
      <label>
        <span>机构名称</span>
        <UiInput v-model:value="draft.title" :disabled="readonly" />
      </label>
      <label>
        <span>机构编码</span>
        <UiInput v-model:value="draft.code" :disabled="readonly" />
      </label>
      <label>
        <span>上级机构</span>
        <RecordPicker
          v-model:value="draft.parentId"
          :context="organizationContext"
          :disabled="readonly"
          :constraints="parentRecordConstraints(draft.id)"
          :title-of="organizationTitle"
          placeholder="根机构留空"
        />
      </label>
    </form>

    <RecordMetaSection :record="draft" />
  </StaticManagementLayout>
</template>
