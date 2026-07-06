<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { UiButton, UiDropdown, UiEmpty, UiInput, UiSelect, UiSpin } from '@muyun/vue-ui-antdv';
import type { UiDropdownItem } from '@muyun/vue-ui-antdv';
import type {
  Option,
  OptionValue,
  QueryOperator,
  QuerySchema,
  QuerySchemaField,
  ResolvedViewDescriptor,
  WebQueryCondition,
  WebQueryRequest,
  WebSort,
} from '@muyun/web-contracts';
import { normalizeError, type ModuleContext } from '@muyun/web-core';
import { presentPlatformError, presentPlatformMessage } from './platformErrorFeedback';
import RecordActionBar from './RecordActionBar.vue';
import RecordStatusTag from './RecordStatusTag.vue';
import {
  resolveRecordActions,
  type RecordActionItem,
  type ResolvedRecordActionItem,
} from './recordActionBarModel';

defineOptions({ name: 'RecordQueryListPanel' });

export type QueryListRecord = Record<string, unknown> & { id?: string; enabled?: boolean };

export interface RecordQueryListColumn {
  key: string;
  title: string;
  type?: 'text' | 'enabledStatus';
  width?: string;
  align?: 'left' | 'center' | 'right';
  render?: (record: QueryListRecord) => string;
}

interface ConditionDraft {
  key: number;
  fieldName?: string;
  operator?: QueryOperator;
  rawValue: string;
  booleanValue?: OptionValue | null;
}

interface QueryListRow {
  key: string;
  record: QueryListRecord;
  primaryAction?: ResolvedRecordActionItem;
  secondaryActions: ResolvedRecordActionItem[];
  dropdownItems: UiDropdownItem[];
}

const props = withDefaults(
  defineProps<{
    context: ModuleContext<QueryListRecord>;
    title: string;
    columns?: RecordQueryListColumn[];
    actions?: RecordActionItem[];
    standardCrudActions?: boolean;
    createTitle?: string;
    standardCrudRowActions?: boolean;
    rowActionsOf?: (record: QueryListRecord) => RecordActionItem[];
    rowActionsTitle?: string;
    rowKey?: string;
    selectedKey?: string;
    reloadKey?: number;
    refreshTitle?: string;
    pageSize?: number;
    uiConfigId?: string;
    queryTemplateId?: string;
    ready?: boolean;
    externalQueryValues?: Record<string, unknown>;
    quickSearchPlaceholder?: string;
    emptyDescription?: string;
    waitingDescription?: string;
  }>(),
  {
    rowKey: 'id',
    columns: () => [],
    actions: () => [],
    standardCrudActions: false,
    createTitle: undefined,
    standardCrudRowActions: false,
    rowActionsOf: undefined,
    rowActionsTitle: '操作',
    selectedKey: undefined,
    reloadKey: undefined,
    refreshTitle: undefined,
    pageSize: 20,
    uiConfigId: undefined,
    queryTemplateId: undefined,
    ready: true,
    externalQueryValues: undefined,
    quickSearchPlaceholder: '搜索',
    emptyDescription: '暂无记录',
    waitingDescription: '请选择查询范围',
  },
);

const emit = defineEmits<{
  select: [record: QueryListRecord];
  rowDblclick: [record: QueryListRecord, event: MouseEvent];
  loaded: [records: QueryListRecord[]];
  action: [action: RecordActionItem, event: MouseEvent];
  rowAction: [action: ResolvedRecordActionItem, record: QueryListRecord, event?: MouseEvent];
}>();

const loading = ref(false);
const schema = ref<QuerySchema>();
const records = ref<QueryListRecord[]>([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(props.pageSize);
const runtimeViews = ref<ResolvedViewDescriptor[]>([]);
const descriptorLoadError = ref(false);
const quickSearchKeyword = ref('');
const appliedQuickSearch = ref('');
const conditionsExpanded = ref(false);
const conditionSeq = ref(0);
const conditionDrafts = ref<ConditionDraft[]>([]);
const activeConditions = ref<WebQueryCondition[]>([]);
let schemaRequestSeq = 0;
let recordsRequestSeq = 0;

const pages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)));
const queryReady = computed(() => props.ready);
const queryFields = computed(() => schema.value?.fields ?? []);
const fieldOptions = computed<Option[]>(() =>
  queryFields.value.map((field) => ({
    label: field.title ?? field.name,
    value: field.name,
  })),
);
const conditionCount = computed(() => activeConditions.value.length);
const quickSearchEnabled = computed(() => schema.value?.quickSearch.enabled === true);
const quickSearchDisabled = computed(() => !queryReady.value || !quickSearchEnabled.value);
const queryActionsDisabled = computed(() => !queryReady.value);
const conditionsDisabled = computed(() => !queryReady.value || queryFields.value.length === 0);
const panelActions = computed<RecordActionItem[]>(() => {
  if (props.actions && props.actions.length > 0) {
    return props.actions;
  }
  if (!props.standardCrudActions) {
    return [];
  }
  return [
    {
      key: 'create',
      actionCode: 'create',
      title: props.createTitle ?? '新建',
      primary: true,
      disabled: !queryReady.value,
    },
  ];
});
const rowActionsProvider = computed(
  () => props.rowActionsOf ?? (props.standardCrudRowActions ? standardCrudRowActionsOf : undefined),
);
const hasRowActions = computed(() => rowActionsProvider.value !== undefined);
const rows = computed<QueryListRow[]>(() => records.value.map(resolveRow));
const tableColumns = computed<RecordQueryListColumn[]>(() => {
  if (props.columns && props.columns.length > 0) {
    return props.columns;
  }
  return columnsFromRuntimeListView(runtimeViews.value);
});
const pageSizeOptions: Option[] = [
  { label: '10 条/页', value: 10 },
  { label: '20 条/页', value: 20 },
  { label: '50 条/页', value: 50 },
];
const booleanOptions: Option[] = [
  { label: '是', value: 'true' },
  { label: '否', value: 'false' },
];

onMounted(loadSchemaAndRecords);

watch(
  () => props.reloadKey,
  () => refresh(),
);

watch(
  () => props.context,
  () => loadSchemaAndRecords(),
);

watch(
  () => props.ready,
  (ready) => {
    pageNum.value = 1;
    if (ready) {
      void loadRecords();
      return;
    }
    records.value = [];
    total.value = 0;
    emit('loaded', []);
  },
);

watch(
  () => props.externalQueryValues,
  () => {
    pageNum.value = 1;
    void loadRecords();
  },
  { deep: true },
);

watch(
  () => props.pageSize,
  (value) => {
    pageSize.value = value;
  },
);

async function loadSchemaAndRecords() {
  const requestSeq = ++schemaRequestSeq;
  loading.value = true;
  descriptorLoadError.value = false;
  try {
    runtimeViews.value = await loadRuntimeViews();
    const nextSchema = await props.context.crud.querySchema({
      uiConfigId: props.uiConfigId,
    });
    if (requestSeq !== schemaRequestSeq) {
      return;
    }
    schema.value = nextSchema;
    activeConditions.value = [];
    conditionsExpanded.value = false;
    resetConditionDrafts();
    await loadRecords(false);
  } catch (cause) {
    if (requestSeq !== schemaRequestSeq) {
      return;
    }
    if (isUnsupportedQuerySchemaError(cause)) {
      schema.value = emptyQuerySchema(props.context.moduleAlias);
      activeConditions.value = [];
      conditionsExpanded.value = false;
      resetConditionDrafts();
      await loadRecords(false);
      return;
    }
    schema.value = undefined;
    records.value = [];
    total.value = 0;
    emit('loaded', []);
    presentPlatformError(cause, { source: 'record-query-list-panel', phase: 'load' });
  } finally {
    if (requestSeq === schemaRequestSeq) {
      loading.value = false;
    }
  }
}

async function loadRuntimeViews(): Promise<ResolvedViewDescriptor[]> {
  if (props.columns && props.columns.length > 0) {
    return [];
  }
  try {
    const runtimeContext = await props.context.runtime.ready;
    return runtimeContext.uiDescriptor?.views ?? [];
  } catch (cause) {
    descriptorLoadError.value = true;
    throw cause;
  }
}

async function loadRecords(updateLoading = true) {
  const requestSeq = ++recordsRequestSeq;
  if (!queryReady.value) {
    records.value = [];
    total.value = 0;
    emit('loaded', []);
    if (updateLoading) {
      loading.value = false;
    }
    return;
  }
  if (updateLoading) {
    loading.value = true;
  }
  try {
    const response = await props.context.crud.query(buildQueryRequest());
    if (requestSeq !== recordsRequestSeq) {
      return;
    }
    records.value = response.records;
    total.value = response.total;
    pageNum.value = response.pageNum;
    pageSize.value = response.pageSize;
    emit('loaded', response.records);
  } catch (cause) {
    if (requestSeq !== recordsRequestSeq) {
      return;
    }
    records.value = [];
    total.value = 0;
    emit('loaded', []);
    presentPlatformError(cause, { source: 'record-query-list-panel', phase: 'load' });
  } finally {
    if (updateLoading && requestSeq === recordsRequestSeq) {
      loading.value = false;
    }
  }
}

function buildQueryRequest(): WebQueryRequest {
  const quickSearch = appliedQuickSearch.value.trim();
  const request: WebQueryRequest = {
    page: { pageNum: pageNum.value, pageSize: pageSize.value },
    conditions: activeConditions.value,
    sorts: defaultSorts(),
  };
  if (props.uiConfigId) {
    request.uiConfigId = props.uiConfigId;
  }
  if (props.queryTemplateId) {
    request.queryTemplateId = props.queryTemplateId;
  }
  if (quickSearch && quickSearchEnabled.value) {
    request.quickSearch = quickSearch;
    request.quickSearchFields = schema.value?.quickSearch.fields ?? [];
  }
  if (props.externalQueryValues && Object.keys(props.externalQueryValues).length > 0) {
    request.externalQueryValues = props.externalQueryValues;
  }
  return request;
}

function defaultSorts(): WebSort[] {
  return (schema.value?.defaultSorts ?? []).map((sort) => ({
    field: sort.field,
    desc: sort.desc,
  }));
}

function emptyQuerySchema(scopeName: string): QuerySchema {
  return {
    scopeName,
    quickSearch: { enabled: false, fields: [], fieldSchemas: [] },
    fields: [],
    externalCriteria: [],
    defaultSorts: [],
  };
}

function isUnsupportedQuerySchemaError(cause: unknown) {
  const error = normalizeError(cause);
  return error.message.includes('query schema is not supported by');
}

function refresh() {
  void loadRecords();
}

function handleAction(action: RecordActionItem, event: MouseEvent) {
  emit('action', action, event);
}

function resolveRow(record: QueryListRecord): QueryListRow {
  const actions = resolveRecordActions(props.context, rowActionsProvider.value?.(record) ?? []);
  const secondaryActions = actions.slice(1);
  return {
    key: recordKey(record),
    record,
    primaryAction: actions[0],
    secondaryActions,
    dropdownItems: secondaryActions.map(rowActionDropdownItem),
  };
}

function standardCrudRowActionsOf(): RecordActionItem[] {
  return [
    { key: 'view', title: '查看' },
    { key: 'edit', actionCode: 'update', title: '修改', iconName: 'edit' },
    { key: 'delete', actionCode: 'delete', title: '删除', iconName: 'delete', danger: true },
  ];
}

function rowActionDropdownItem(action: ResolvedRecordActionItem): UiDropdownItem {
  return {
    key: action.key,
    title: action.title,
    disabled: action.disabled,
    danger: action.danger,
  };
}

function handlePrimaryRowAction(row: QueryListRow, event: MouseEvent) {
  if (!row.primaryAction || row.primaryAction.disabled) {
    return;
  }
  emit('rowAction', row.primaryAction, row.record, event);
}

function handleSecondaryRowAction(row: QueryListRow, key: string) {
  const action = row.secondaryActions.find((item) => item.key === key);
  if (!action || action.disabled) {
    return;
  }
  emit('rowAction', action, row.record);
}

function submitQuickSearch() {
  appliedQuickSearch.value = quickSearchKeyword.value;
  pageNum.value = 1;
  void loadRecords();
}

function handleQuickSearchInput(value: string) {
  quickSearchKeyword.value = value;
  if (value || !appliedQuickSearch.value) {
    return;
  }
  appliedQuickSearch.value = '';
  pageNum.value = 1;
  void loadRecords();
}

function toggleConditions() {
  if (conditionsDisabled.value) {
    return;
  }
  conditionsExpanded.value = !conditionsExpanded.value;
}

function addCondition() {
  if (conditionsDisabled.value) {
    return;
  }
  conditionDrafts.value.push(createConditionDraft());
}

function removeCondition(key: number) {
  conditionDrafts.value = conditionDrafts.value.filter((draft) => draft.key !== key);
  if (conditionDrafts.value.length === 0) {
    conditionDrafts.value.push(createConditionDraft());
  }
}

function applyConditions() {
  if (conditionsDisabled.value) {
    return;
  }
  const validationMessage = validateConditionDrafts();
  if (validationMessage) {
    presentPlatformMessage(validationMessage, { phase: 'validation' });
    return;
  }
  activeConditions.value = conditionDrafts.value.flatMap(conditionOfDraft);
  pageNum.value = 1;
  void loadRecords();
}

function clearConditions() {
  activeConditions.value = [];
  resetConditionDrafts();
  pageNum.value = 1;
  void loadRecords();
}

function resetConditionDrafts() {
  conditionDrafts.value = [createConditionDraft()];
}

function createConditionDraft(): ConditionDraft {
  conditionSeq.value += 1;
  return {
    key: conditionSeq.value,
    fieldName: queryFields.value[0]?.name,
    operator: queryFields.value[0]?.defaultOperator ?? queryFields.value[0]?.operators[0],
    rawValue: '',
    booleanValue: null,
  };
}

function handleFieldChange(draft: ConditionDraft, fieldName: OptionValue | null) {
  const field = fieldByName(String(fieldName ?? ''));
  draft.fieldName = field?.name;
  draft.operator = field?.defaultOperator ?? field?.operators[0];
  draft.rawValue = '';
  draft.booleanValue = null;
}

function handleOperatorChange(draft: ConditionDraft, operator: OptionValue | null) {
  draft.operator = String(operator ?? '') as QueryOperator;
}

function conditionOfDraft(draft: ConditionDraft): WebQueryCondition[] {
  const field = fieldByName(draft.fieldName);
  const operator = draft.operator ?? field?.defaultOperator;
  if (!field || !operator) {
    return [];
  }
  const values = valuesOfDraft(field, operator, draft);
  if (!valueLessOperator(operator) && values.length === 0) {
    return [];
  }
  return [{ fieldName: field.name, operator, values }];
}

function validateConditionDrafts() {
  for (const draft of conditionDrafts.value) {
    const field = fieldByName(draft.fieldName);
    const operator = draft.operator ?? field?.defaultOperator;
    if (!field || !operator || valueLessOperator(operator)) {
      continue;
    }
    if (operator === 'BETWEEN' && valuesOfDraft(field, operator, draft).length !== 2) {
      return `${field.title ?? field.name} 需要填写起始和结束两个值`;
    }
  }
  return undefined;
}

function valuesOfDraft(field: QuerySchemaField, operator: QueryOperator, draft: ConditionDraft): unknown[] {
  if (valueLessOperator(operator)) {
    return [];
  }
  if (field.valueType === 'BOOLEAN') {
    if (draft.booleanValue !== 'true' && draft.booleanValue !== 'false') {
      return [];
    }
    return [draft.booleanValue === 'true'];
  }
  const raw = draft.rawValue.trim();
  if (!raw) {
    return [];
  }
  if (operator === 'IN' || operator === 'NOT_IN' || operator === 'BETWEEN') {
    return raw
      .split(',')
      .map((item) => item.trim())
      .filter(Boolean);
  }
  return [raw];
}

function valueLessOperator(operator: QueryOperator) {
  return operator === 'NULL' || operator === 'NOT_NULL';
}

function operatorOptions(draft: ConditionDraft): Option[] {
  const field = fieldByName(draft.fieldName);
  return (field?.operators ?? []).map((operator) => ({
    label: operatorLabel(operator),
    value: operator,
  }));
}

function fieldByName(fieldName?: string) {
  return queryFields.value.find((field) => field.name === fieldName);
}

function operatorLabel(operator: QueryOperator) {
  const labels: Record<QueryOperator, string> = {
    EQ: '等于',
    NOT_EQUAL: '不等于',
    LIKE: '包含',
    IN: '属于',
    NOT_IN: '不属于',
    GT: '大于',
    GTE: '大于等于',
    LT: '小于',
    LTE: '小于等于',
    BETWEEN: '介于',
    NULL: '为空',
    NOT_NULL: '不为空',
  };
  return labels[operator] ?? operator;
}

function conditionPlaceholder(draft: ConditionDraft) {
  if (draft.operator === 'BETWEEN') {
    return '起始, 结束';
  }
  if (draft.operator === 'IN' || draft.operator === 'NOT_IN') {
    return '多个值用逗号分隔';
  }
  return '请输入条件值';
}

function recordKey(record: QueryListRecord) {
  return String(record[props.rowKey] ?? record.id ?? '');
}

function cellValue(record: QueryListRecord, column: RecordQueryListColumn) {
  return column.render?.(record) ?? displayRecordFieldValue(record, column.key);
}

function displayRecordFieldValue(record: QueryListRecord, fieldName: string) {
  const titleValue = record[`${fieldName}Title`];
  if (typeof titleValue === 'string' && titleValue.trim()) {
    return titleValue;
  }
  const value = record[fieldName];
  if (typeof value === 'boolean') {
    return value ? '是' : '否';
  }
  return String(value ?? '');
}

function columnsFromRuntimeListView(views: ResolvedViewDescriptor[] | undefined): RecordQueryListColumn[] {
  const view =
    views?.find((item) => item.viewKind === 'LIST' && item.viewCode === 'default_list') ??
    views?.find((item) => item.viewKind === 'LIST');
  if (!view) {
    return [];
  }
  return view.fields
    .filter((field) => field.visible?.constant !== false)
    .map((field) => ({
      key: field.fieldRef.fieldName,
      title: field.label ?? field.fieldRef.fieldName,
      type: field.uiType === 'enabledStatus' ? 'enabledStatus' : 'text',
      width: field.width,
      align: columnAlign(field.align),
    }));
}

function columnAlign(align: string | undefined): RecordQueryListColumn['align'] {
  return align === 'center' || align === 'right' ? align : 'left';
}

function goPage(nextPage: number) {
  pageNum.value = Math.min(Math.max(1, nextPage), pages.value);
  void loadRecords();
}

function handlePageSizeChange(value: OptionValue | null) {
  pageSize.value = typeof value === 'number' ? value : Number(value ?? props.pageSize);
  pageNum.value = 1;
  void loadRecords();
}

defineExpose({ refresh });
</script>

<template>
  <main class="record-query-list-panel">
    <header class="record-query-list-header">
      <UiButton
        class="record-query-list-title"
        icon-name="reload"
        icon-position="end"
        type="text"
        :disabled="queryActionsDisabled"
        :title="refreshTitle ?? `刷新${title}`"
        @click="refresh"
      >
        <span>{{ title }}</span>
      </UiButton>
      <div class="record-query-list-actions">
        <RecordActionBar
          v-if="panelActions.length > 0"
          :context="context"
          :actions="panelActions"
          @action="handleAction"
        />
        <UiInput
          :value="quickSearchKeyword"
          class="record-query-list-search"
          allow-clear
          :disabled="quickSearchDisabled"
          :placeholder="quickSearchPlaceholder"
          @update:value="handleQuickSearchInput"
          @keydown.enter="submitQuickSearch"
        />
        <UiButton icon-name="search" :disabled="quickSearchDisabled" @click="submitQuickSearch">
          查询
        </UiButton>
        <UiButton type="text" icon-name="settings" :disabled="conditionsDisabled" @click="toggleConditions">
          条件<span v-if="conditionCount"> {{ conditionCount }}</span>
        </UiButton>
      </div>
    </header>

    <section v-if="conditionsExpanded" class="record-query-conditions">
      <div v-for="draft in conditionDrafts" :key="draft.key" class="record-query-condition-row">
        <UiSelect
          class="record-query-condition-field"
          :value="draft.fieldName"
          :options="fieldOptions"
          placeholder="字段"
          @update:value="handleFieldChange(draft, $event)"
        />
        <UiSelect
          class="record-query-condition-operator"
          :value="draft.operator"
          :options="operatorOptions(draft)"
          placeholder="关系"
          @update:value="handleOperatorChange(draft, $event)"
        />
        <UiSelect
          v-if="fieldByName(draft.fieldName)?.valueType === 'BOOLEAN' && !valueLessOperator(draft.operator!)"
          class="record-query-condition-value"
          :value="draft.booleanValue"
          :options="booleanOptions"
          placeholder="选择"
          @update:value="draft.booleanValue = $event"
        />
        <UiInput
          v-else-if="!valueLessOperator(draft.operator!)"
          v-model:value="draft.rawValue"
          class="record-query-condition-value"
          :placeholder="conditionPlaceholder(draft)"
        />
        <div v-else class="record-query-condition-value muted">无需输入值</div>
        <UiButton type="text" icon-name="delete" danger @click="removeCondition(draft.key)" />
      </div>
      <div class="record-query-condition-actions">
        <UiButton type="dashed" icon-name="plus" :disabled="conditionsDisabled" @click="addCondition">
          添加条件
        </UiButton>
        <UiButton type="primary" :disabled="conditionsDisabled" @click="applyConditions">应用条件</UiButton>
        <UiButton type="text" :disabled="conditionsDisabled" @click="clearConditions">重置</UiButton>
      </div>
    </section>

    <section class="record-query-list-body">
      <UiSpin v-if="loading" tip="加载列表" />
      <UiEmpty v-else-if="!queryReady" :description="waitingDescription" />
      <UiEmpty v-else-if="descriptorLoadError" description="列表声明加载失败，请稍后重试" />
      <UiEmpty v-else-if="records.length === 0" :description="emptyDescription" />
      <div v-else class="record-query-list-table-wrap">
        <table class="record-query-list-table">
          <thead>
            <tr>
              <th
                v-for="column in tableColumns"
                :key="column.key"
                :style="{ width: column.width, textAlign: column.align ?? 'left' }"
              >
                {{ column.title }}
              </th>
              <th v-if="hasRowActions" class="record-query-list-action-head">
                {{ rowActionsTitle }}
              </th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="row in rows"
              :key="row.key"
              :class="{ selected: selectedKey === row.key, muted: row.record.enabled === false }"
              @click="emit('select', row.record)"
              @dblclick="emit('rowDblclick', row.record, $event)"
            >
              <td
                v-for="column in tableColumns"
                :key="column.key"
                :style="{ textAlign: column.align ?? 'left' }"
              >
                <RecordStatusTag
                  v-if="column.type === 'enabledStatus'"
                  :enabled="row.record[column.key] !== false"
                />
                <template v-else>
                  {{ cellValue(row.record, column) }}
                </template>
              </td>
              <td v-if="hasRowActions" class="record-query-list-row-actions" @click.stop>
                <UiButton
                  v-if="row.primaryAction"
                  class="record-query-list-primary-action"
                  type="text"
                  :disabled="row.primaryAction.disabled"
                  :icon-name="row.primaryAction.iconName"
                  @click="handlePrimaryRowAction(row, $event)"
                >
                  {{ row.primaryAction.title }}
                </UiButton>
                <UiDropdown
                  v-if="row.secondaryActions.length > 0"
                  :items="row.dropdownItems"
                  trigger="hover"
                  @select="handleSecondaryRowAction(row, $event)"
                >
                  <UiButton
                    class="record-query-list-more-action"
                    type="text"
                    icon-name="down"
                    title="更多"
                    aria-label="更多"
                  />
                </UiDropdown>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <footer class="record-query-list-pagination">
      <span>共 {{ total }} 条</span>
      <UiSelect
        class="record-query-list-page-size"
        :value="pageSize"
        :options="pageSizeOptions"
        :allow-clear="false"
        :disabled="queryActionsDisabled"
        @update:value="handlePageSizeChange"
      />
      <UiButton :disabled="queryActionsDisabled || pageNum <= 1" @click="goPage(pageNum - 1)">
        上一页
      </UiButton>
      <span>第 {{ pageNum }} / {{ pages }} 页</span>
      <UiButton :disabled="queryActionsDisabled || pageNum >= pages" @click="goPage(pageNum + 1)">
        下一页
      </UiButton>
    </footer>
  </main>
</template>

<style scoped>
.record-query-list-panel {
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr) auto;
  grid-template-areas:
    'header'
    'conditions'
    'body'
    'pagination';
  align-content: stretch;
  gap: 12px;
  min-width: 0;
  min-height: 0;
  height: 100%;
  padding: 14px;
  border: 1px solid var(--muyun-border);
  border-radius: 8px;
  background: var(--muyun-surface);
}

.record-query-list-header {
  grid-area: header;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-width: 0;
}

.record-query-list-title {
  flex: 0 0 auto;
  margin: -4px 0 -4px -6px;
  padding: 4px 6px;
  color: var(--muyun-text);
  font-size: 16px;
  font-weight: 700;
}

.record-query-list-title :deep(.ui-button-trailing-icon) {
  width: 0;
  margin-inline-start: 0;
  margin-inline-end: 0;
  color: var(--muyun-text-muted);
  opacity: 0;
  transition:
    width 0.16s ease,
    margin 0.16s ease,
    opacity 0.16s ease;
}

.record-query-list-title:hover :deep(.ui-button-trailing-icon),
.record-query-list-title:focus-visible :deep(.ui-button-trailing-icon) {
  width: 14px;
  margin-inline-start: 6px;
  opacity: 1;
}

.record-query-list-actions,
.record-query-condition-actions,
.record-query-list-pagination {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.record-query-list-actions {
  flex: 1 1 auto;
  justify-content: flex-end;
  flex-wrap: wrap;
}

.record-query-list-search {
  width: clamp(150px, 20vw, 220px);
}

.record-query-conditions {
  grid-area: conditions;
  display: grid;
  gap: 8px;
  padding: 10px;
  border: 1px solid var(--muyun-border-subtle);
  border-radius: 8px;
  background: var(--muyun-hover-subtle);
}

.record-query-condition-row {
  display: grid;
  grid-template-columns: minmax(140px, 0.8fr) minmax(120px, 0.6fr) minmax(180px, 1fr) 32px;
  gap: 8px;
  align-items: center;
  min-width: 0;
}

.record-query-condition-field,
.record-query-condition-operator,
.record-query-condition-value,
.record-query-list-page-size {
  min-width: 0;
}

.record-query-condition-value.muted {
  height: 32px;
  padding: 5px 11px;
  border: 1px solid var(--muyun-border);
  border-radius: 6px;
  background: #fff;
  color: var(--muyun-text-muted);
  font-size: 14px;
}

.record-query-list-body {
  grid-area: body;
  display: grid;
  min-height: 0;
}

.record-query-list-table-wrap {
  align-self: stretch;
  min-height: 0;
  overflow: auto;
  border: 1px solid var(--muyun-border-subtle);
  border-radius: 8px;
}

.record-query-list-table {
  width: 100%;
  min-width: 720px;
  border-collapse: collapse;
  font-size: 13px;
}

.record-query-list-table th,
.record-query-list-table td {
  padding: 9px 10px;
  border-bottom: 1px solid var(--muyun-border-subtle);
  color: var(--muyun-text-body);
  line-height: 1.4;
}

.record-query-list-table th {
  position: sticky;
  top: 0;
  z-index: 1;
  background: var(--muyun-hover-subtle);
  color: var(--muyun-text);
  font-weight: 700;
}

.record-query-list-action-head {
  width: 92px;
  color: var(--muyun-text-muted);
  font-weight: 500;
  text-align: center;
}

.record-query-list-table tbody tr {
  cursor: pointer;
}

.record-query-list-table tbody tr:hover {
  background: var(--muyun-hover);
}

.record-query-list-table tbody tr.selected {
  background: var(--muyun-selected);
}

.record-query-list-table tbody tr.muted td {
  color: var(--muyun-text-muted);
}

.record-query-list-row-actions {
  width: 92px;
  text-align: right;
  white-space: nowrap;
}

.record-query-list-row-actions :deep(.ui-dropdown) {
  margin-left: 2px;
}

.record-query-list-row-actions :deep(.ant-btn) {
  min-width: 0;
  height: 24px;
  padding: 0 4px;
  color: var(--muyun-text-muted);
  font-size: 12px;
}

.record-query-list-row-actions :deep(.ant-btn:hover),
.record-query-list-row-actions :deep(.ant-btn:focus-visible) {
  color: var(--muyun-primary);
}

.record-query-list-primary-action :deep(.ant-btn-icon) {
  display: none;
}

.record-query-list-more-action {
  width: 24px;
  opacity: 0;
  transition: opacity 0.14s ease;
}

.record-query-list-table tbody tr:hover .record-query-list-more-action,
.record-query-list-row-actions:focus-within .record-query-list-more-action {
  opacity: 1;
}

.record-query-list-pagination {
  grid-area: pagination;
  justify-content: flex-end;
  color: var(--muyun-text-muted);
  font-size: 13px;
}

.record-query-list-page-size {
  width: 112px;
}

@media (max-width: 900px) {
  .record-query-list-header,
  .record-query-list-actions,
  .record-query-list-pagination {
    display: grid;
    grid-template-columns: 1fr;
    justify-items: stretch;
  }

  .record-query-list-search,
  .record-query-list-page-size {
    width: 100%;
  }

  .record-query-condition-row {
    grid-template-columns: 1fr;
  }
}
</style>
