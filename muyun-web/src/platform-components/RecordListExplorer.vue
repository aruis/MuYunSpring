<script setup lang="ts">
import { computed } from 'vue';
import { UiEmpty, UiIcon, type UiRecordInlineAction } from '@muyun/vue-ui-antdv';
import RecordStatusTag from './RecordStatusTag.vue';

defineOptions({ name: 'RecordListExplorer' });

export interface RecordListExplorerRecord {
  id?: string;
  code?: string;
  title?: string;
  name?: string;
  enabled?: boolean;
}

const props = withDefaults(
  defineProps<{
    records: RecordListExplorerRecord[];
    selectedId?: string;
    keyword?: string;
    emptyDescription?: string;
    titleOf?: (record: RecordListExplorerRecord) => string;
    codeOf?: (record: RecordListExplorerRecord) => string | undefined;
    actionsOf?: (record: RecordListExplorerRecord) => UiRecordInlineAction[];
    filterOption?: (record: RecordListExplorerRecord, normalizedKeyword: string) => boolean;
    mutedOf?: (record: RecordListExplorerRecord) => boolean;
  }>(),
  {
    selectedId: undefined,
    keyword: '',
    emptyDescription: '暂无记录',
    titleOf: undefined,
    codeOf: undefined,
    actionsOf: undefined,
    filterOption: undefined,
    mutedOf: undefined,
  },
);

const emit = defineEmits<{
  select: [record: RecordListExplorerRecord];
  action: [action: UiRecordInlineAction, record: RecordListExplorerRecord];
}>();

const normalizedKeyword = computed(() => props.keyword.trim().toLowerCase());
const filteredRecords = computed(() => {
  if (!normalizedKeyword.value) {
    return props.records;
  }
  return props.records.filter((record) => matchesKeyword(record, normalizedKeyword.value));
});

function recordTitle(record: RecordListExplorerRecord) {
  return props.titleOf?.(record) ?? record.title ?? record.name ?? record.code ?? record.id ?? '未命名记录';
}

function recordCode(record: RecordListExplorerRecord) {
  return props.codeOf?.(record) ?? record.code ?? record.id;
}

function recordMuted(record: RecordListExplorerRecord) {
  return props.mutedOf?.(record) ?? record.enabled === false;
}

function matchesKeyword(record: RecordListExplorerRecord, keyword: string) {
  if (props.filterOption) {
    return props.filterOption(record, keyword);
  }
  return [recordTitle(record), recordCode(record), record.id].some((value) =>
    value?.toLowerCase().includes(keyword),
  );
}

function handleAction(event: MouseEvent, action: UiRecordInlineAction, record: RecordListExplorerRecord) {
  event.stopPropagation();
  if (action.disabled) {
    event.preventDefault();
    return;
  }
  emit('action', action, record);
}

function actionFallbackLabel(action: UiRecordInlineAction) {
  return action.title.trim().slice(0, 1);
}
</script>

<template>
  <UiEmpty v-if="filteredRecords.length === 0" :description="emptyDescription" />
  <ul v-else class="record-list-explorer">
    <li v-for="record in filteredRecords" :key="record.id">
      <div
        role="button"
        tabindex="0"
        class="record-list-item"
        :class="{ selected: record.id === selectedId, muted: recordMuted(record) }"
        @click="emit('select', record)"
        @keydown.enter.prevent="emit('select', record)"
        @keydown.space.prevent="emit('select', record)"
      >
        <span class="record-list-item-main">
          <span class="record-list-item-title">{{ recordTitle(record) }}</span>
          <span class="record-list-item-code">{{ recordCode(record) }}</span>
          <RecordStatusTag v-if="record.enabled === false" :enabled="record.enabled" />
        </span>
        <span v-if="actionsOf?.(record)?.length" class="record-list-item-actions">
          <button
            v-for="action in actionsOf(record)"
            :key="action.key"
            class="record-list-item-action"
            :class="{ danger: action.danger }"
            :title="action.title"
            :aria-label="action.title"
            :disabled="action.disabled"
            type="button"
            @click="handleAction($event, action, record)"
          >
            <UiIcon v-if="action.iconName" :name="action.iconName" />
            <span v-else class="record-list-item-action-label">{{ actionFallbackLabel(action) }}</span>
          </button>
        </span>
      </div>
    </li>
  </ul>
</template>

<style scoped>
.record-list-explorer {
  display: grid;
  align-content: start;
  gap: 5px;
  min-height: 0;
  margin: 0;
  padding: 0;
  overflow: auto;
  list-style: none;
}

.record-list-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  width: 100%;
  min-height: 42px;
  padding: 8px 10px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: var(--muyun-text-body);
  text-align: left;
  cursor: pointer;
}

.record-list-item:hover,
.record-list-item.selected {
  background: var(--muyun-hover);
}

.record-list-item.muted {
  color: var(--muyun-text-muted);
}

.record-list-item-main {
  display: inline-flex;
  flex: 1 1 auto;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.record-list-item-title {
  overflow: hidden;
  color: inherit;
  font-size: 14px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.record-list-item-code {
  flex: 0 0 auto;
  color: var(--muyun-text-muted);
  font-size: 12px;
}

.record-list-item-actions {
  display: inline-flex;
  flex: 0 0 auto;
  gap: 2px;
  opacity: 0;
  transition: opacity 0.12s ease;
}

.record-list-item:hover .record-list-item-actions,
.record-list-item.selected .record-list-item-actions {
  opacity: 1;
}

.record-list-item-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  padding: 0;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: var(--muyun-text-muted);
  cursor: pointer;
}

.record-list-item-action:hover:not(:disabled) {
  background: var(--muyun-hover);
  box-shadow:
    inset 0 0 0 1px var(--muyun-border-subtle),
    0 1px 2px rgb(15 23 42 / 8%);
}

.record-list-item-action.danger {
  color: var(--muyun-danger-text);
}

.record-list-item-action-label {
  font-size: 12px;
  line-height: 1;
}

.record-list-item-action:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}
</style>
