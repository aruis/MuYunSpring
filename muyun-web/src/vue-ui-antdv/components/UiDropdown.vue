<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue';
import type { UiDropdownItem } from '../types';

defineOptions({ name: 'UiDropdown' });

defineProps<{
  items: UiDropdownItem[];
}>();

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
    <div v-if="open" class="ui-dropdown-popup" role="menu">
      <button
        v-for="item in items"
        :key="item.key"
        class="ui-dropdown-item"
        :class="{ danger: item.danger }"
        :disabled="item.disabled"
        type="button"
        role="menuitem"
        @click="select(item)"
      >
        {{ item.title }}
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
  right: 0;
  z-index: 50;
  min-width: 120px;
  padding: 4px;
  border: 1px solid #d6e0ec;
  border-radius: 6px;
  background: #fff;
  box-shadow: 0 8px 24px rgb(15 23 42 / 14%);
}

.ui-dropdown-item {
  width: 100%;
  padding: 8px 10px;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: #1f2933;
  text-align: left;
  cursor: pointer;
}

.ui-dropdown-item:hover:not(:disabled) {
  background: #f3f6fa;
}

.ui-dropdown-item:disabled {
  color: #9aa6b2;
  cursor: not-allowed;
}

.ui-dropdown-item.danger {
  color: #c2410c;
}
</style>
