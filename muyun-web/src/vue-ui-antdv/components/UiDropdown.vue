<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue';
import UiIcon from './UiIcon.vue';
import type { UiDropdownItem } from '../types';

defineOptions({ name: 'UiDropdown' });

withDefaults(
  defineProps<{
    items: UiDropdownItem[];
    selectedKey?: string;
    align?: 'start' | 'end';
  }>(),
  {
    selectedKey: undefined,
    align: 'end',
  },
);

defineSlots<{
  default(props: { toggle: () => void }): unknown;
}>();

const emit = defineEmits<{
  select: [key: string];
}>();

const open = ref(false);
const root = ref<HTMLElement>();

function toggle() {
  open.value = !open.value;
}

function select(item: UiDropdownItem) {
  if (item.disabled) {
    return;
  }
  open.value = false;
  emit('select', item.key);
}

function handleDocumentClick(event: MouseEvent) {
  if (!root.value?.contains(event.target as Node)) {
    open.value = false;
  }
}

function handleDocumentKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    open.value = false;
  }
}

onMounted(() => {
  document.addEventListener('click', handleDocumentClick);
  document.addEventListener('keydown', handleDocumentKeydown);
});

onBeforeUnmount(() => {
  document.removeEventListener('click', handleDocumentClick);
  document.removeEventListener('keydown', handleDocumentKeydown);
});
</script>

<template>
  <div ref="root" class="ui-dropdown">
    <span class="ui-dropdown-trigger" @click.stop="toggle">
      <slot :toggle="toggle" />
    </span>
    <div v-if="open" class="ui-dropdown-popup" :class="`align-${align}`" role="menu">
      <button
        v-for="item in items"
        :key="item.key"
        class="ui-dropdown-item"
        :class="{ danger: item.danger, selected: item.key === selectedKey }"
        :disabled="item.disabled"
        type="button"
        role="menuitem"
        @click="select(item)"
      >
        <span class="ui-dropdown-item-title">{{ item.title }}</span>
        <UiIcon v-if="item.key === selectedKey" name="check" class="ui-dropdown-item-check" />
      </button>
    </div>
  </div>
</template>

<style scoped>
.ui-dropdown {
  position: relative;
  display: inline-flex;
}

.ui-dropdown-trigger {
  display: inline-flex;
}

.ui-dropdown-popup {
  position: absolute;
  top: calc(100% + 6px);
  z-index: 50;
  min-width: max(100%, 112px);
  padding: 5px;
  border: 1px solid var(--muyun-border-subtle);
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 8px 18px rgb(15 23 42 / 10%);
}

.ui-dropdown-popup.align-start {
  left: 0;
}

.ui-dropdown-popup.align-end {
  right: 0;
}

.ui-dropdown-item {
  display: flex;
  width: 100%;
  min-height: 30px;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 5px 8px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: var(--muyun-text-body);
  font-size: 13px;
  line-height: 18px;
  text-align: left;
  cursor: pointer;
}

.ui-dropdown-item:hover:not(:disabled) {
  background: var(--muyun-hover-subtle);
  color: var(--muyun-text);
}

.ui-dropdown-item.selected {
  background: var(--muyun-selected);
  color: var(--muyun-text);
}

.ui-dropdown-item-title {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ui-dropdown-item-check {
  flex: 0 0 auto;
  color: var(--muyun-text-muted);
  font-size: 11px;
}

.ui-dropdown-item:disabled {
  color: var(--muyun-text-muted);
  cursor: not-allowed;
  opacity: 0.7;
}

.ui-dropdown-item.danger {
  color: #c2410c;
}
</style>
