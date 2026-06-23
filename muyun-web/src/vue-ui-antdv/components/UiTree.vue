<script setup lang="ts">
import { computed } from 'vue';
import { Tree as ATree } from 'ant-design-vue';
import type { UiTreeNode } from '../types';

defineOptions({ name: 'UiTree' });

const props = defineProps<{
  nodes: UiTreeNode[];
  selectedKey?: string;
  expandedKeys?: string[];
}>();

const emit = defineEmits<{
  select: [node: UiTreeNode];
  'update:expandedKeys': [keys: string[]];
}>();

const selectedKeys = computed(() => (props.selectedKey ? [props.selectedKey] : []));

function handleSelect(keys: unknown[]) {
  const selected = keys[0];
  if (typeof selected !== 'string') {
    return;
  }
  const node = findNode(props.nodes, selected);
  if (node) {
    emit('select', node);
  }
}

function handleExpand(keys: unknown[]) {
  emit(
    'update:expandedKeys',
    keys.filter((key): key is string => typeof key === 'string'),
  );
}

function findNode(nodes: UiTreeNode[], key: string): UiTreeNode | undefined {
  for (const node of nodes) {
    if (node.key === key) {
      return node;
    }
    const child = node.children ? findNode(node.children, key) : undefined;
    if (child) {
      return child;
    }
  }
  return undefined;
}
</script>

<template>
  <ATree
    block-node
    :tree-data="nodes"
    :selected-keys="selectedKeys"
    :expanded-keys="expandedKeys"
    @select="handleSelect"
    @expand="handleExpand"
  >
    <template #title="{ title, tag, muted }">
      <span class="ui-tree-title" :class="{ 'ui-tree-title-muted': muted }">
        <span class="ui-tree-title-text">{{ title }}</span>
        <span v-if="tag" class="ui-tree-title-tag">{{ tag }}</span>
      </span>
    </template>
  </ATree>
</template>

<style scoped>
.ui-tree-title {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  max-width: 100%;
}

.ui-tree-title-muted {
  color: #8a97a8;
}

.ui-tree-title-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ui-tree-title-tag {
  flex: 0 0 auto;
  padding: 1px 5px;
  border: 1px solid #d7dde5;
  border-radius: 4px;
  color: #697588;
  font-size: 11px;
  line-height: 16px;
}
</style>
