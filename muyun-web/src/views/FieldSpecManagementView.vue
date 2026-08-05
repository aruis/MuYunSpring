<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
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
import type { FieldSpec, FieldUiControl, Option } from '@muyun/web-contracts';
import { useModuleContext } from '@muyun/web-core';
import { confirmAction, UiInput, UiSelect } from '@muyun/vue-ui-antdv';
import { createFieldSpecManagementState } from './fieldSpecManagementState';

defineOptions({ name: 'FieldSpecManagementView' });

const fieldSpecContext = useModuleContext<FieldSpec>();
const fieldUiControlContext = useModuleContext<FieldUiControl>({ moduleAlias: 'platform.field_ui_control' });
const explorerSearchKeyword = ref('');
const fieldUiControls = ref<FieldUiControl[]>([]);
const {
  selected,
  draft,
  mode,
  reloadKey,
  saving,
  cardTitle,
  readonly,
  specAliasReadonly,
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
} = createFieldSpecManagementState(fieldSpecContext, confirmAction);

const fieldTypeOptions: Option[] = [
  'STRING',
  'TEXT',
  'INTEGER',
  'LONG',
  'BOOLEAN',
  'DATE',
  'TIMESTAMP',
  'ZONED_TIMESTAMP',
  'DECIMAL',
  'JSON',
].map((value) => ({ label: value, value }));
const queryOperatorOptions: Option[] = [
  'EQ',
  'NOT_EQUAL',
  'GT',
  'GTE',
  'LT',
  'LTE',
  'LIKE',
  'IN',
  'NOT_IN',
  'NULL',
  'NOT_NULL',
  'BETWEEN',
].map((value) => ({ label: value, value }));
const uiControlOptions = computed<Option[]>(() =>
  fieldUiControls.value.map((control) => ({
    label: control.title
      ? `${control.title} (${control.alias ?? control.id})`
      : (control.alias ?? control.id ?? '未命名控件'),
    value: control.alias ?? control.id ?? '',
    disabled: control.enabled === false,
  })),
);
const recycleBinExplorer = useRecycleBinExplorerMode({
  context: fieldSpecContext,
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

function fieldSpecItemOf(record: CrudRecordListBase): RecordExplorerItemDescriptor {
  return {
    title: record.title ?? record.alias ?? record.id ?? '未命名字段规格',
    secondary: record.alias ?? record.id,
    muted: record.enabled === false,
  };
}

function handleLoaded(records: CrudRecordListBase[]) {
  if (recycleBinExplorer.active.value) {
    handleReadonlyListLoaded(records as FieldSpec[]);
    return;
  }
  handleListLoaded(records as FieldSpec[]);
}

function handleFieldSpecSelect(record: CrudRecordListBase) {
  handleSelect(record as FieldSpec);
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

onMounted(async () => {
  await fieldUiControlContext.runtime.ready;
  const response = await fieldUiControlContext.abilities
    .crud()
    .query({ page: { pageNum: 1, pageSize: 200 } });
  fieldUiControls.value = response.records;
});

function numberValue(value: string) {
  const number = Number(value);
  return Number.isFinite(number) ? number : undefined;
}
</script>

<template>
  <StaticManagementLayout
    v-model:explorer-search-keyword="explorerSearchKeyword"
    :explorer-title="recycleBinExplorer.active.value ? '回收站' : '字段规格列表'"
    :refresh-title="recycleBinExplorer.active.value ? '刷新回收站' : '刷新字段规格列表'"
    explorer-search-placeholder="搜索字段规格名称或 alias"
    :explorer-searchable="!recycleBinExplorer.active.value"
    :mode="mode"
    :detail-title="cardTitle"
    @refresh="recycleBinExplorer.refresh"
  >
    <template #explorer-actions>
      <ModuleActionButton
        v-if="!recycleBinExplorer.active.value"
        class="record-panel-create-button"
        :context="fieldSpecContext"
        action-code="create"
        title="新建字段规格"
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
        :context="fieldSpecContext"
        :selected-id="selected?.id"
        :reload-key="recycleBinExplorer.reloadKey.value"
        :mode="recycleBinExplorer.mode.value"
        :keyword="explorerSearchKeyword"
        :empty-description="recycleBinExplorer.active.value ? '回收站为空' : '暂无字段规格'"
        :loading-tip="recycleBinExplorer.active.value ? '加载回收站' : '加载字段规格列表'"
        fallback-title="未命名字段规格"
        :item-of="fieldSpecItemOf"
        @recycle-bin-summary="recycleBinExplorer.updateSummary"
        @select="handleFieldSpecSelect"
        @loaded="handleLoaded"
      />
    </template>

    <template #detail-actions>
      <RecordActionBar :context="fieldSpecContext" :actions="cardActions" @action="handleCardAction" />
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
        <span>规格 alias</span>
        <UiInput v-model:value="draft.alias" :disabled="specAliasReadonly" />
      </label>
      <label>
        <span>规格名称</span>
        <UiInput v-model:value="draft.title" :disabled="readonly" />
      </label>
      <label>
        <span>运行时类型</span>
        <UiSelect v-model:value="draft.fieldType" :options="fieldTypeOptions" :disabled="readonly" />
      </label>
      <label>
        <span>默认长度</span>
        <UiInput
          :value="draft.defaultLength"
          type="number"
          :disabled="readonly"
          @update:value="draft.defaultLength = numberValue($event)"
        />
      </label>
      <label>
        <span>默认精度</span>
        <UiInput
          :value="draft.defaultPrecision"
          type="number"
          :disabled="readonly"
          @update:value="draft.defaultPrecision = numberValue($event)"
        />
      </label>
      <label>
        <span>默认小数位</span>
        <UiInput
          :value="draft.defaultScale"
          type="number"
          :disabled="readonly"
          @update:value="draft.defaultScale = numberValue($event)"
        />
      </label>
      <label>
        <span>默认查询操作符</span>
        <UiSelect
          v-model:value="draft.defaultQueryOperator"
          :options="queryOperatorOptions"
          :disabled="readonly"
        />
      </label>
      <label>
        <span>允许查询操作符</span>
        <UiSelect
          v-model:value="draft.queryOperators"
          mode="multiple"
          :options="queryOperatorOptions"
          :disabled="readonly"
        />
      </label>
      <label>
        <span>默认 UI 控件</span>
        <UiSelect
          v-model:value="draft.defaultUiControlAlias"
          :options="uiControlOptions"
          :disabled="readonly"
        />
      </label>
      <label>
        <span>允许 UI 控件</span>
        <UiSelect
          v-model:value="draft.uiControlAliases"
          mode="multiple"
          :options="uiControlOptions"
          :disabled="readonly"
        />
      </label>
    </form>

    <RecordMetaSection :record="draft" show-sort-order />
  </StaticManagementLayout>
</template>
