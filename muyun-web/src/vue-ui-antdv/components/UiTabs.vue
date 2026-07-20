<script setup lang="ts">
import { Tabs as ATabs, TabPane as ATabPane } from 'ant-design-vue';
import type { UiTabItem } from '../types';

defineOptions({ name: 'UiTabs', inheritAttrs: false });

defineProps<{
  tabs: UiTabItem[];
  activeKey: string;
}>();

const emit = defineEmits<{
  'update:activeKey': [key: string];
  close: [key: string];
}>();

function handleChange(key: string | number) {
  emit('update:activeKey', String(key));
}

function handleEditEvent(targetKey: string | number | MouseEvent | KeyboardEvent, action: 'add' | 'remove') {
  if (action === 'remove' && (typeof targetKey === 'string' || typeof targetKey === 'number')) {
    emit('close', String(targetKey));
  }
}
</script>

<template>
  <ATabs
    type="editable-card"
    hide-add
    :active-key="activeKey"
    :class="$attrs.class"
    :style="$attrs.style"
    @change="handleChange"
    @edit="handleEditEvent"
  >
    <ATabPane v-for="tab in tabs" :key="tab.key" :tab="tab.title" :closable="tab.closable ?? true" />
  </ATabs>
</template>
