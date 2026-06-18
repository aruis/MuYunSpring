<script setup lang="ts">
import { computed } from 'vue';
import { Menu as AMenu } from 'ant-design-vue';
import type { ItemType } from 'ant-design-vue';
import type { UiMenuItem } from '../types';

defineOptions({ name: 'UiMenu' });

const props = withDefaults(
  defineProps<{
    items: UiMenuItem[];
    selectedKey?: string;
    openKeys: string[];
    mode?: 'inline' | 'vertical' | 'horizontal';
  }>(),
  {
    mode: 'inline',
    selectedKey: undefined,
  },
);

const emit = defineEmits<{
  select: [key: string];
  'update:openKeys': [keys: string[]];
}>();

const selectedKeys = computed(() => (props.selectedKey ? [props.selectedKey] : []));

const menuItems = computed<ItemType[]>(() => props.items.map(toMenuItem));

function toMenuItem(item: UiMenuItem): ItemType {
  const children = item.children?.map(toMenuItem);
  return {
    key: item.key,
    label: item.title,
    disabled: item.disabled,
    children: children && children.length > 0 ? children : undefined,
  } as ItemType;
}

function handleClick(event: { key: string | number }) {
  emit('select', String(event.key));
}

function handleOpenChange(keys: (string | number)[]) {
  emit('update:openKeys', keys.map(String));
}
</script>

<template>
  <AMenu
    :items="menuItems"
    :mode="mode"
    :selected-keys="selectedKeys"
    :open-keys="openKeys"
    @click="handleClick"
    @open-change="handleOpenChange"
  />
</template>
