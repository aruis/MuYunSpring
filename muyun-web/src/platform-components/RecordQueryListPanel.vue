<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { UiButton, UiEmpty, UiInput, UiSelect, UiSpin } from '@muyun/vue-ui-antdv';
import type {
  Option,
  OptionValue,
  QueryOperator,
  QuerySchema,
  QuerySchemaField,
  WebQueryCondition,
  WebQueryRequest,
  WebSort,
} from '@muyun/web-contracts';
import type { ModuleContext } from '@muyun/web-core';
import { presentPlatformError } from './platformErrorFeedback';

defineOptions({ name: 'RecordQueryListPanel' });

export type QueryListRecord = Record<string, unknown> & { id?: string; enabled?: boolean };

export interface RecordQueryListColumn {
  key: string;
  title: string;
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

const props = withDefaults(
  defineProps<{
    context: ModuleContext<QueryListRecord>;
    title: string;
    columns: RecordQueryListColumn[];
    rowKey?: string;
    selectedKey?: string;
    reloadKey?: number;
    pageSize?: number;
    ready?: boolean;
    externalQueryValues?: Record<string, unknown>;
    quickSearchPlaceholder?: string;
    emptyDescription?: string;
    waitingDescription?: string;
  }>(),
  {
    rowKey: 'id',
    selectedKey: undefined,
    reloadKey: undefined,
    pageSize: 20,
    ready: true,
    externalQueryValues: undefined,
    quickSearchPlaceholder: '搜索',
    emptyDescription: '暂无记录',
    waitingDescription: '请选择查询范围',
  },
);

const emit = defineEmits<{
  select: [record: QueryListRecord];
  loaded: [records: QueryListRecord[]];
}>();

const loading = ref(false);
const schema = ref<QuerySchema>();
const records = ref<QueryListRecord[]>([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(props.pageSize);
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
  try {
    await props.context.runtime.ready;
    const nextSchema = await props.context.crud.querySchema();
    if (requestSeq !== schemaRequestSeq) {
      return;
    }
    schema.value = nextSchema;
    activeConditions.value = [];
    resetConditionDrafts();
    await loadRecords(false);
  } catch (cause) {
    if (requestSeq !== schemaRequestSeq) {
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

function refresh() {
  void loadRecords();
}

function submitQuickSearch() {
  appliedQuickSearch.value = quickSearchKeyword.value;
  pageNum.value = 1;
  void loadRecords();
}

function clearQuickSearch() {
  quickSearchKeyword.value = '';
  appliedQuickSearch.value = '';
  pageNum.value = 1;
  void loadRecords();
}

function toggleConditions() {
  conditionsExpanded.value = !conditionsExpanded.value;
}

function addCondition() {
  conditionDrafts.value.push(createConditionDraft());
}

function removeCondition(key: number) {
  conditionDrafts.value = conditionDrafts.value.filter((draft) => draft.key !== key);
  if (conditionDrafts.value.length === 0) {
    conditionDrafts.value.push(createConditionDraft());
  }
}

function applyConditions() {
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
  return column.render?.(record) ?? String(record[column.key] ?? '');
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
      <h2>{{ title }}</h2>
      <div class="record-query-list-actions">
        <UiInput
          v-model:value="quickSearchKeyword"
          class="record-query-list-search"
          :disabled="quickSearchDisabled"
          :placeholder="quickSearchPlaceholder"
          @keydown.enter="submitQuickSearch"
        />
        <UiButton icon-name="search" :disabled="quickSearchDisabled" @click="submitQuickSearch">
          查询
        </UiButton>
        <UiButton v-if="appliedQuickSearch" type="text" @click="clearQuickSearch">清除</UiButton>
        <UiButton type="text" icon-name="settings" @click="toggleConditions">
          条件<span v-if="conditionCount"> {{ conditionCount }}</span>
        </UiButton>
        <UiButton type="text" icon-name="reload" :disabled="queryActionsDisabled" @click="refresh">
          刷新
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
        <UiButton type="dashed" icon-name="plus" @click="addCondition">添加条件</UiButton>
        <UiButton type="primary" :disabled="queryActionsDisabled" @click="applyConditions">应用条件</UiButton>
        <UiButton type="text" :disabled="queryActionsDisabled" @click="clearConditions">重置</UiButton>
      </div>
    </section>

    <UiSpin v-if="loading" tip="加载列表" />
    <UiEmpty v-else-if="!queryReady" :description="waitingDescription" />
    <UiEmpty v-else-if="records.length === 0" :description="emptyDescription" />
    <div v-else class="record-query-list-table-wrap">
      <table class="record-query-list-table">
        <thead>
          <tr>
            <th
              v-for="column in columns"
              :key="column.key"
              :style="{ width: column.width, textAlign: column.align ?? 'left' }"
            >
              {{ column.title }}
            </th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="record in records"
            :key="recordKey(record)"
            :class="{ selected: selectedKey === recordKey(record), muted: record.enabled === false }"
            @click="emit('select', record)"
          >
            <td v-for="column in columns" :key="column.key" :style="{ textAlign: column.align ?? 'left' }">
              {{ cellValue(record, column) }}
            </td>
          </tr>
        </tbody>
      </table>
    </div>

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
  align-content: start;
  gap: 12px;
  min-width: 0;
  min-height: 0;
  padding: 14px;
  border: 1px solid var(--muyun-border);
  border-radius: 8px;
  background: var(--muyun-surface);
}

.record-query-list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-width: 0;
}

.record-query-list-header h2 {
  margin: 0;
  overflow: hidden;
  color: var(--muyun-text);
  font-size: 16px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.record-query-list-actions,
.record-query-condition-actions,
.record-query-list-pagination {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.record-query-list-search {
  width: 220px;
}

.record-query-conditions {
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

.record-query-list-table-wrap {
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

.record-query-list-pagination {
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
