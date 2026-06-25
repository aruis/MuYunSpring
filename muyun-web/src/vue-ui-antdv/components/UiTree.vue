<script setup lang="ts">
import { computed } from 'vue';
import { Tree as ATree } from 'ant-design-vue';
import UiIcon from './UiIcon.vue';
import type { UiRecordInlineAction, UiTreeNode } from '../types';

defineOptions({ name: 'UiTree' });

const props = defineProps<{
  nodes: UiTreeNode[];
  selectedKey?: string;
  expandedKeys?: string[];
}>();

const emit = defineEmits<{
  select: [node: UiTreeNode];
  action: [action: UiRecordInlineAction, node: UiTreeNode];
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

function handleAction(event: MouseEvent, action: UiRecordInlineAction, nodeKey: string) {
  event.stopPropagation();
  if (action.disabled) {
    event.preventDefault();
    return;
  }
  const node = findNode(props.nodes, nodeKey);
  if (node) {
    emit('action', action, node);
  }
}

function actionFallbackLabel(action: UiRecordInlineAction) {
  return action.title.trim().slice(0, 1);
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
    <template #title="{ key, title, tag, muted, actions }">
      <span class="ui-tree-title" :class="{ 'ui-tree-title-muted': muted }">
        <span class="ui-tree-title-main">
          <span class="ui-tree-title-text">{{ title }}</span>
          <span v-if="tag" class="ui-tree-title-tag">{{ tag }}</span>
        </span>
        <span v-if="actions?.length" class="ui-tree-node-actions">
          <button
            v-for="action in actions"
            :key="action.key"
            class="ui-tree-node-action"
            :class="{ danger: action.danger }"
            :title="action.title"
            :aria-label="action.title"
            :disabled="action.disabled"
            type="button"
            @click="handleAction($event, action, key)"
          >
            <UiIcon v-if="action.iconName" :name="action.iconName" />
            <span v-else class="ui-tree-node-action-label">{{ actionFallbackLabel(action) }}</span>
          </button>
        </span>
      </span>
    </template>
  </ATree>
</template>

<style scoped>
.ui-tree-title {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  width: 100%;
}

.ui-tree-title-muted {
  color: #8a97a8;
}

.ui-tree-title-main {
  display: inline-flex;
  flex: 1 1 auto;
  align-items: center;
  gap: 6px;
  min-width: 0;
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

.ui-tree-node-actions {
  display: inline-flex;
  flex: 0 0 auto;
  gap: 2px;
  opacity: 0;
  transition: opacity 0.12s ease;
}

.ui-tree-title:hover .ui-tree-node-actions,
:deep(.ant-tree-node-selected) .ui-tree-node-actions {
  opacity: 1;
}

.ui-tree-node-action {
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

.ui-tree-node-action:hover:not(:disabled) {
  background: var(--muyun-hover);
  box-shadow:
    inset 0 0 0 1px var(--muyun-border-subtle),
    0 1px 2px rgb(15 23 42 / 8%);
}

.ui-tree-node-action.danger {
  color: var(--muyun-danger-text);
}

.ui-tree-node-action-label {
  font-size: 12px;
  line-height: 1;
}

.ui-tree-node-action:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}
</style>
