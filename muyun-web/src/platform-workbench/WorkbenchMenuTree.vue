<script setup lang="ts">
import type { MenuNavigationTarget, MenuRecord } from '@muyun/web-contracts';
import type { WorkbenchMenuNode } from './menuTreeModel';

defineOptions({ name: 'WorkbenchMenuTree' });

const props = defineProps<{
  node: WorkbenchMenuNode;
}>();

const emit = defineEmits<{
  selectMenu: [menu: MenuRecord, target: MenuNavigationTarget];
}>();

function handleClick() {
  if (props.node.target) {
    emit('selectMenu', props.node.record, props.node.target);
  }
}

function handleChildSelect(menu: MenuRecord, menuTarget: MenuNavigationTarget) {
  emit('selectMenu', menu, menuTarget);
}
</script>

<template>
  <li class="deep-node">
    <button
      class="deep-node-button"
      :class="{ navigable: node.navigable, branch: node.hasChildren }"
      type="button"
      :disabled="!node.navigable && !node.hasChildren"
      @click="handleClick"
    >
      <span>{{ node.record.title }}</span>
      <small v-if="node.navigable">打开</small>
    </button>

    <ul v-if="node.hasChildren" class="deep-children">
      <WorkbenchMenuTree
        v-for="child in node.children"
        :key="child.record.id"
        :node="child"
        @select-menu="handleChildSelect"
      />
    </ul>
  </li>
</template>

<style scoped>
.deep-node,
.deep-children {
  margin: 0;
  padding: 0;
  list-style: none;
}

.deep-node-button {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  width: 100%;
  min-height: 30px;
  padding: 5px 8px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: #64748b;
  font: inherit;
  font-size: 12px;
  text-align: left;
}

.deep-node-button span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.deep-node-button small {
  flex: 0 0 auto;
  color: #0f766e;
  font-size: 10px;
}

.deep-node-button.navigable {
  color: #1e293b;
  cursor: pointer;
}

.deep-node-button.branch {
  font-weight: 600;
}

.deep-node-button.navigable:hover {
  background: #eaf5f2;
  color: #0f766e;
}

.deep-node-button:disabled {
  cursor: default;
}
</style>
