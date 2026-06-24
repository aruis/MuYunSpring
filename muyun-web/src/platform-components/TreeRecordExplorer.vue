<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { UiEmpty, UiError, UiInput, UiSpin, UiTree, type UiTreeNode } from '@muyun/vue-ui-antdv';
import { normalizeError, type ModuleContext } from '@muyun/web-core';
import type { WebTreeNode } from '@muyun/web-contracts';
import {
  defaultTreeRecordMatches,
  defaultTreeRecordTitle,
  expandAllTreeRecords,
  filterTreeRecords,
  firstTwoTreeLevels,
  flattenTreeRecords,
  type TreeRecordBase,
} from './treeRecordModel';

defineOptions({ name: 'TreeRecordExplorer' });

const props = withDefaults(
  defineProps<{
    context: ModuleContext<TreeRecordBase>;
    selectedId?: string;
    reloadKey?: number;
    searchPlaceholder?: string;
    emptyDescription?: string;
    loadingTip?: string;
    fallbackTitle?: string;
    titleOf?: (record: TreeRecordBase) => string;
    filterOption?: (record: TreeRecordBase, normalizedKeyword: string) => boolean;
    tagOf?: (record: TreeRecordBase) => string | undefined;
    mutedOf?: (record: TreeRecordBase) => boolean;
  }>(),
  {
    selectedId: undefined,
    reloadKey: undefined,
    searchPlaceholder: '搜索名称、编码或 ID',
    emptyDescription: '暂无记录',
    loadingTip: '加载树形记录',
    fallbackTitle: '未命名记录',
    titleOf: undefined,
    filterOption: undefined,
    tagOf: undefined,
    mutedOf: undefined,
  },
);

const emit = defineEmits<{
  select: [record: TreeRecordBase];
  loaded: [records: TreeRecordBase[]];
}>();

const loading = ref(false);
const error = ref<string>();
const keyword = ref('');
const tree = ref<WebTreeNode<TreeRecordBase>[]>([]);
const expandedKeys = ref<string[]>([]);

const filteredTree = computed(() =>
  filterTreeRecords(tree.value, keyword.value, (record, normalized) => matchesKeyword(record, normalized)),
);
const nodes = computed(() => filteredTree.value.map(toUiTreeNode));
const records = computed(() => flattenTreeRecords(tree.value));

onMounted(loadTree);

watch(
  () => props.reloadKey,
  () => loadTree(),
);

watch(
  () => props.context,
  () => loadTree(),
);

watch(keyword, () => {
  if (keyword.value.trim()) {
    expandedKeys.value = filteredTree.value.flatMap(expandAllTreeRecords);
  }
});

async function loadTree() {
  loading.value = true;
  error.value = undefined;
  try {
    await props.context.runtime.ready;
    const treeCapability = props.context.abilities.tree();
    const response = await treeCapability.tree();
    tree.value = response.records;
    expandedKeys.value = firstTwoTreeLevels(response.records);
    emit('loaded', flattenTreeRecords(response.records));
  } catch (cause) {
    error.value = normalizeError(cause).message;
  } finally {
    loading.value = false;
  }
}

function recordTitle(record: TreeRecordBase) {
  return props.titleOf?.(record) ?? defaultTreeRecordTitle(record, props.fallbackTitle);
}

function matchesKeyword(record: TreeRecordBase, normalized: string) {
  return props.filterOption?.(record, normalized) ?? defaultTreeRecordMatches(record, normalized, recordTitle);
}

function handleSelect(node: UiTreeNode) {
  const record = records.value.find((item) => item.id === node.key);
  if (record) {
    emit('select', record);
  }
}

function toUiTreeNode(node: WebTreeNode<TreeRecordBase>): UiTreeNode {
  const record = node.record;
  return {
    key: record.id ?? '',
    title: recordTitle(record),
    tag: props.tagOf?.(record) ?? (record.enabled === false ? '停用' : undefined),
    muted: props.mutedOf?.(record) ?? record.enabled === false,
    children: node.children.map(toUiTreeNode),
  };
}
</script>

<template>
  <div class="tree-record-explorer">
    <UiInput v-model:value="keyword" :placeholder="searchPlaceholder" />
    <UiSpin v-if="loading" :tip="loadingTip" />
    <UiError v-else-if="error" :message="error" />
    <UiEmpty v-else-if="nodes.length === 0" :description="emptyDescription" />
    <UiTree
      v-else
      v-model:expanded-keys="expandedKeys"
      :nodes="nodes"
      :selected-key="selectedId"
      @select="handleSelect"
    />
  </div>
</template>

<style scoped>
.tree-record-explorer {
  display: grid;
  gap: 10px;
  min-height: 0;
}
</style>
