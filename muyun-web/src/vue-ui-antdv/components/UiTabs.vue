<script setup lang="ts">
import { computed } from 'vue';
import { Tabs as ATabs } from 'ant-design-vue';
import type { UiTabItem } from '../types';

defineOptions({ name: 'UiTabs' });

const props = defineProps<{
  tabs: UiTabItem[];
  activeKey: string;
}>();

const emit = defineEmits<{
  'update:activeKey': [key: string];
  close: [key: string];
}>();

const tabItems = computed(() =>
  props.tabs.map((tab) => ({
    key: tab.key,
    label: tab.title,
    closable: tab.closable ?? true,
  })),
);

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
    :items="tabItems"
    @change="handleChange"
    @edit="handleEditEvent"
  />
</template>
