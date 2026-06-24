<script setup lang="ts">
import { computed } from 'vue';
import {
  ModuleActionButton,
  RecordActionBar,
  RecordStatusTag,
  type RecordActionItem,
} from '@muyun/platform-components';
import type { Application } from '@muyun/web-contracts';
import { useModuleContext } from '@muyun/web-core';
import { confirmAction, UiButton, UiInput, UiSelect } from '@muyun/vue-ui-antdv';
import ListRecordExplorer from './application-management/ListRecordExplorer.vue';
import type { ListRecordBase } from './application-management/listRecordModel';
import { createApplicationManagementState } from './applicationManagementState';

defineOptions({ name: 'ApplicationManagementView' });

const applicationContext = useModuleContext<Application>();
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

const enabledOptions = [
  { label: '启用', value: 'true' },
  { label: '停用', value: 'false' },
];

const enabledValue = computed({
  get: () => (draft.value.enabled === false ? 'false' : 'true'),
  set: (value) => {
    draft.value.enabled = value !== 'false';
  },
});

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

function applicationTitle(record: ListRecordBase) {
  return record.title ?? record.alias ?? record.id ?? '未命名应用';
}

function applicationSubtitle(record: ListRecordBase) {
  return record.alias ?? record.id;
}

function handleLoaded(records: ListRecordBase[]) {
  handleListLoaded(records as Application[]);
}

function handleApplicationSelect(record: ListRecordBase) {
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
  <section class="application-page">
    <aside class="application-sidebar">
      <div class="sidebar-header">
        <div>
          <p>平台配置</p>
          <h2>应用列表</h2>
        </div>
        <UiButton title="刷新应用列表" @click="reloadKey += 1">刷新</UiButton>
      </div>
      <div class="sidebar-actions">
        <ModuleActionButton :context="applicationContext" action-code="create" @click="startCreate">
          新建应用
        </ModuleActionButton>
      </div>
      <ListRecordExplorer
        :context="applicationContext"
        :selected-id="selected?.id"
        :reload-key="reloadKey"
        search-placeholder="搜索应用名称、alias 或 ID"
        empty-description="暂无应用"
        loading-tip="加载应用列表"
        fallback-title="未命名应用"
        :title-of="applicationTitle"
        :subtitle-of="applicationSubtitle"
        @select="handleApplicationSelect"
        @loaded="handleLoaded"
      />
    </aside>

    <main class="application-card">
      <header class="card-header">
        <div>
          <p>{{ mode === 'view' ? '查看' : mode === 'edit' ? '编辑' : '新建' }}</p>
          <div class="title-line">
            <h2>{{ cardTitle }}</h2>
            <RecordStatusTag v-if="selected && mode === 'view'" :enabled="selected.enabled" />
          </div>
        </div>
        <RecordActionBar :context="applicationContext" :actions="cardActions" @action="handleCardAction" />
      </header>

      <div v-if="actionError" class="message error">{{ actionError }}</div>
      <div v-else-if="actionMessage" class="message success">{{ actionMessage }}</div>

      <form class="record-form" @submit.prevent="save">
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
          <UiSelect
            v-model:value="enabledValue"
            :options="enabledOptions"
            :disabled="readonly"
            :allow-clear="false"
          />
        </label>
      </form>

      <section class="record-meta">
        <h3>系统信息</h3>
        <dl>
          <div>
            <dt>ID</dt>
            <dd>{{ draft.id ?? '-' }}</dd>
          </div>
          <div>
            <dt>版本</dt>
            <dd>{{ draft.version ?? '-' }}</dd>
          </div>
          <div>
            <dt>排序号</dt>
            <dd>{{ draft.sortOrder ?? '-' }}</dd>
          </div>
          <div>
            <dt>创建时间</dt>
            <dd>{{ draft.createdAt ?? '-' }}</dd>
          </div>
          <div>
            <dt>更新时间</dt>
            <dd>{{ draft.updatedAt ?? '-' }}</dd>
          </div>
        </dl>
      </section>
    </main>
  </section>
</template>

<style scoped>
.application-page {
  display: grid;
  grid-template-columns: minmax(260px, 320px) minmax(0, 1fr);
  gap: 12px;
  min-height: calc(100vh - 116px);
}

.application-sidebar,
.application-card {
  min-width: 0;
  border: 1px solid #d8e1ea;
  border-radius: 8px;
  background: #fff;
}

.application-sidebar {
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr);
  gap: 10px;
  padding: 12px;
  overflow: hidden;
}

.sidebar-header,
.card-header,
.sidebar-actions {
  display: flex;
  align-items: center;
}

.sidebar-header,
.card-header {
  justify-content: space-between;
  gap: 12px;
}

.title-line {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.sidebar-header p,
.card-header p,
h2,
h3 {
  margin: 0;
}

.sidebar-header p,
.card-header p {
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
}

h2 {
  color: #172033;
  font-size: 16px;
}

.sidebar-actions {
  gap: 8px;
  flex-wrap: wrap;
}

.application-card {
  display: grid;
  align-content: start;
  gap: 14px;
  padding: 16px;
}

.record-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(220px, 1fr));
  gap: 14px;
}

label {
  display: grid;
  gap: 6px;
  color: #465569;
  font-size: 13px;
}

.message {
  padding: 9px 10px;
  border-radius: 6px;
  font-size: 13px;
}

.message.error {
  border: 1px solid #f4b8b8;
  background: #fff5f5;
  color: #b42318;
}

.message.success {
  border: 1px solid #a9d7c8;
  background: #f1fbf7;
  color: #0f6b57;
}

.record-meta {
  display: grid;
  gap: 10px;
  padding-top: 4px;
}

.record-meta h3 {
  color: #334155;
  font-size: 14px;
}

dl {
  display: grid;
  grid-template-columns: repeat(2, minmax(220px, 1fr));
  gap: 10px 16px;
  margin: 0;
}

dl div {
  min-width: 0;
}

dt {
  color: #64748b;
  font-size: 12px;
}

dd {
  overflow: hidden;
  margin: 3px 0 0;
  color: #243447;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 900px) {
  .application-page {
    grid-template-columns: 1fr;
  }

  .record-form,
  dl {
    grid-template-columns: 1fr;
  }
}
</style>
