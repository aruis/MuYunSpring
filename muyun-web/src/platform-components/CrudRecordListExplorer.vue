<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { UiSpin, type UiRecordInlineAction } from '@muyun/vue-ui-antdv';
import type { ModuleContext } from '@muyun/web-core';
import RecordListExplorer, { type RecordListExplorerRecord } from './RecordListExplorer.vue';
import type { RecordExplorerItemDescriptor } from './recordExplorerItemModel';
import {
  defaultCrudRecordListMatches,
  defaultCrudRecordListTitle,
  type CrudRecordListBase,
} from './crudRecordListModel';
import { presentPlatformError } from './platformErrorFeedback';

defineOptions({ name: 'CrudRecordListExplorer' });

const props = withDefaults(
  defineProps<{
    context: ModuleContext<CrudRecordListBase>;
    selectedId?: string;
    reloadKey?: number;
    keyword?: string;
    emptyDescription?: string;
    loadingTip?: string;
    fallbackTitle?: string;
    titleOf?: (record: CrudRecordListBase) => string;
    subtitleOf?: (record: CrudRecordListBase) => string | undefined;
    itemOf?: (record: CrudRecordListBase) => RecordExplorerItemDescriptor | undefined;
    actionsOf?: (record: CrudRecordListBase) => UiRecordInlineAction[];
    filterOption?: (record: CrudRecordListBase, normalizedKeyword: string) => boolean;
    tagOf?: (record: CrudRecordListBase) => string | undefined;
    mutedOf?: (record: CrudRecordListBase) => boolean;
  }>(),
  {
    selectedId: undefined,
    reloadKey: undefined,
    keyword: '',
    emptyDescription: '暂无记录',
    loadingTip: '加载记录列表',
    fallbackTitle: '未命名记录',
    titleOf: undefined,
    subtitleOf: undefined,
    itemOf: undefined,
    actionsOf: undefined,
    filterOption: undefined,
    tagOf: undefined,
    mutedOf: undefined,
  },
);

const emit = defineEmits<{
  select: [record: CrudRecordListBase];
  action: [action: UiRecordInlineAction, record: CrudRecordListBase];
  loaded: [records: CrudRecordListBase[]];
}>();

const loading = ref(false);
const records = ref<CrudRecordListBase[]>([]);

const listRecords = computed<RecordListExplorerRecord[]>(() => records.value);

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
  try {
    await props.context.runtime.ready;
    const response = await props.context.abilities.crud().query({ page: { pageNum: 1, pageSize: 200 } });
    records.value = response.records;
    emit('loaded', response.records);
  } catch (cause) {
    records.value = [];
    emit('loaded', []);
    presentPlatformError(cause, { source: 'crud-record-list-explorer', phase: 'load' });
  } finally {
    loading.value = false;
  }
}

function recordTitle(record: CrudRecordListBase) {
  const item = props.itemOf?.(record);
  return item?.title ?? props.titleOf?.(record) ?? defaultCrudRecordListTitle(record, props.fallbackTitle);
}

function recordCode(record: CrudRecordListBase) {
  const item = props.itemOf?.(record);
  return item
    ? item.secondary
    : props.subtitleOf
      ? props.subtitleOf(record)
      : (record.alias ?? record.code ?? record.id);
}

function matchesKeyword(record: CrudRecordListBase, normalized: string) {
  return (
    props.filterOption?.(record, normalized) ??
    defaultCrudRecordListMatches(record, normalized, recordTitle, recordCode)
  );
}

function handleSelect(record: CrudRecordListBase) {
  emit('select', record);
}

function handleAction(action: UiRecordInlineAction, record: CrudRecordListBase) {
  emit('action', action, record);
}
</script>

<template>
  <div class="crud-record-list-explorer">
    <UiSpin v-if="loading" :tip="loadingTip" />
    <RecordListExplorer
      v-else
      :records="listRecords"
      :selected-id="selectedId"
      :keyword="keyword"
      :empty-description="emptyDescription"
      :title-of="(record) => recordTitle(record as CrudRecordListBase)"
      :code-of="(record) => recordCode(record as CrudRecordListBase)"
      :item-of="(record) => itemOf?.(record as CrudRecordListBase)"
      :filter-option="(record, normalized) => matchesKeyword(record as CrudRecordListBase, normalized)"
      :actions-of="(record) => actionsOf?.(record as CrudRecordListBase) ?? []"
      :tag-of="(record) => tagOf?.(record as CrudRecordListBase)"
      :muted-of="(record) => mutedOf?.(record as CrudRecordListBase) ?? record.enabled === false"
      @select="handleSelect($event as CrudRecordListBase)"
      @action="(action, record) => handleAction(action, record as CrudRecordListBase)"
    />
  </div>
</template>

<style scoped>
.crud-record-list-explorer {
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}
</style>
