<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { UiEmpty, UiError, UiSpin, UiTree, type UiTreeNode } from '@muyun/vue-ui-antdv';
import type { Organization, WebTreeNode } from '@muyun/web-contracts';
import { normalizeError, type ModuleContext } from '@muyun/web-core';

defineOptions({ name: 'OrganizationTree' });

const props = defineProps<{
  context: ModuleContext<Organization>;
  selectedId?: string;
  reloadKey?: number;
}>();

const emit = defineEmits<{
  select: [organization: Organization];
  loaded: [organizations: Organization[]];
}>();

const loading = ref(false);
const error = ref<string>();
const tree = ref<WebTreeNode<Organization>[]>([]);
const expandedKeys = ref<string[]>([]);

const nodes = computed(() => tree.value.map(toUiTreeNode));
const records = computed(() => flattenRecords(tree.value));

onMounted(loadTree);

watch(
  () => props.reloadKey,
  () => loadTree(),
);

async function loadTree() {
  loading.value = true;
  error.value = undefined;
  try {
    await props.context.runtime.ready;
    const treeCapability = props.context.abilities.tree();
    const response = await treeCapability.tree();
    tree.value = response.records;
    expandedKeys.value = firstTwoLevels(response.records);
    emit('loaded', flattenRecords(response.records));
  } catch (cause) {
    error.value = normalizeError(cause).message;
  } finally {
    loading.value = false;
  }
}

function handleSelect(node: UiTreeNode) {
  const organization = records.value.find((item) => item.id === node.key);
  if (organization) {
    emit('select', organization);
  }
}

function toUiTreeNode(node: WebTreeNode<Organization>): UiTreeNode {
  const record = node.record;
  return {
    key: record.id ?? '',
    title: record.title ?? record.code ?? record.id ?? '未命名机构',
    tag: record.enabled === false ? '停用' : undefined,
    muted: record.enabled === false,
    children: node.children.map(toUiTreeNode),
  };
}

function flattenRecords(nodesToFlatten: WebTreeNode<Organization>[]): Organization[] {
  return nodesToFlatten.flatMap((node) => [node.record, ...flattenRecords(node.children)]);
}

function firstTwoLevels(nodesToExpand: WebTreeNode<Organization>[]) {
  return nodesToExpand.flatMap((node) => [
    ...(node.record.id ? [node.record.id] : []),
    ...node.children.flatMap((child) => (child.record.id ? [child.record.id] : [])),
  ]);
}
</script>

<template>
  <div class="organization-tree">
    <UiSpin v-if="loading" tip="加载机构树" />
    <UiError v-else-if="error" :message="error" />
    <UiEmpty v-else-if="nodes.length === 0" description="暂无机构" />
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
.organization-tree {
  min-height: 0;
}
</style>
