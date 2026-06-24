<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { UiEmpty, UiError, UiInput, UiSpin } from '@muyun/vue-ui-antdv';
import { normalizeError, type ModuleContext } from '@muyun/web-core';
import {
  defaultListRecordMatches,
  defaultListRecordSubtitle,
  defaultListRecordTitle,
  type ListRecordBase,
} from './listRecordModel';

defineOptions({ name: 'ListRecordExplorer' });

const props = withDefaults(
  defineProps<{
    context: ModuleContext<ListRecordBase>;
    selectedId?: string;
    reloadKey?: number;
    searchPlaceholder?: string;
    emptyDescription?: string;
    loadingTip?: string;
    fallbackTitle?: string;
    titleOf?: (record: ListRecordBase) => string;
    subtitleOf?: (record: ListRecordBase) => string | undefined;
    filterOption?: (record: ListRecordBase, normalizedKeyword: string) => boolean;
    tagOf?: (record: ListRecordBase) => string | undefined;
    mutedOf?: (record: ListRecordBase) => boolean;
  }>(),
  {
    selectedId: undefined,
    reloadKey: undefined,
    searchPlaceholder: '搜索名称、别名或 ID',
    emptyDescription: '暂无记录',
    loadingTip: '加载记录列表',
    fallbackTitle: '未命名记录',
    titleOf: undefined,
    subtitleOf: undefined,
    filterOption: undefined,
    tagOf: undefined,
    mutedOf: undefined,
  },
);

const emit = defineEmits<{
  select: [record: ListRecordBase];
  loaded: [records: ListRecordBase[]];
}>();

const loading = ref(false);
const error = ref<string>();
const keyword = ref('');
const records = ref<ListRecordBase[]>([]);

const filteredRecords = computed(() =>
  records.value.filter((record) => matchesKeyword(record, keyword.value.trim().toLowerCase())),
);

onMounted(loadRecords);

watch(
  () => props.reloadKey,
  () => loadRecords(),
);

watch(
  () => props.context,
  () => loadRecords(),
);

async function loadRecords() {
  loading.value = true;
  error.value = undefined;
  try {
    await props.context.runtime.ready;
    const response = await props.context.abilities.crud().query({ page: { pageNum: 1, pageSize: 200 } });
    records.value = response.records;
    emit('loaded', response.records);
  } catch (cause) {
    error.value = normalizeError(cause).message;
  } finally {
    loading.value = false;
  }
}

function recordTitle(record: ListRecordBase) {
  return props.titleOf?.(record) ?? defaultListRecordTitle(record, props.fallbackTitle);
}

function recordSubtitle(record: ListRecordBase) {
  return props.subtitleOf?.(record) ?? defaultListRecordSubtitle(record);
}

function matchesKeyword(record: ListRecordBase, normalized: string) {
  if (!normalized) {
    return true;
  }
  return (
    props.filterOption?.(record, normalized) ??
    defaultListRecordMatches(record, normalized, recordTitle, recordSubtitle)
  );
}

function handleSelect(record: ListRecordBase) {
  emit('select', record);
}
</script>

<template>
  <div class="list-record-explorer">
    <UiInput v-model:value="keyword" :placeholder="searchPlaceholder" />
    <UiSpin v-if="loading" :tip="loadingTip" />
    <UiError v-else-if="error" :message="error" />
    <UiEmpty v-else-if="filteredRecords.length === 0" :description="emptyDescription" />
    <ul v-else class="list-record-items">
      <li v-for="record in filteredRecords" :key="record.id">
        <button
          type="button"
          :class="{
            selected: record.id === selectedId,
            muted: mutedOf?.(record) ?? record.enabled === false,
          }"
          @click="handleSelect(record)"
        >
          <span class="list-record-main">
            <span class="list-record-title">{{ recordTitle(record) }}</span>
            <span v-if="tagOf?.(record) ?? record.enabled === false" class="list-record-tag">
              {{ tagOf?.(record) ?? '停用' }}
            </span>
          </span>
          <span v-if="recordSubtitle(record)" class="list-record-subtitle">
            {{ recordSubtitle(record) }}
          </span>
        </button>
      </li>
    </ul>
  </div>
</template>

<style scoped>
.list-record-explorer {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 10px;
  min-height: 0;
  overflow: hidden;
}

.list-record-items {
  display: grid;
  align-content: start;
  gap: 4px;
  min-height: 0;
  margin: 0;
  padding: 0;
  overflow: auto;
  list-style: none;
}

.list-record-items button {
  display: grid;
  width: 100%;
  min-height: 42px;
  gap: 3px;
  padding: 7px 9px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: #172033;
  text-align: left;
  cursor: pointer;
}

.list-record-items button:hover {
  background: #f1f5f9;
}

.list-record-items button.selected {
  background: #e8f3ff;
}

.list-record-items button.muted {
  color: #8a97a8;
}

.list-record-main {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.list-record-title,
.list-record-subtitle {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.list-record-title {
  flex: 1 1 auto;
  min-width: 0;
}

.list-record-subtitle {
  color: #64748b;
  font-size: 12px;
}

.list-record-tag {
  flex: 0 0 auto;
  padding: 1px 5px;
  border: 1px solid #d7dde5;
  border-radius: 4px;
  color: #697588;
  font-size: 11px;
  line-height: 16px;
}
</style>
