<script setup lang="ts">
import { Dropdown as ADropdown, Menu as AMenu, MenuItem as AMenuItem } from 'ant-design-vue';
import type { UiDropdownItem } from '../types';

defineOptions({ name: 'UiDropdown' });

defineProps<{
  items: UiDropdownItem[];
}>();

const emit = defineEmits<{
  select: [key: string];
}>();

function handleSelect(event: { key: string | number }) {
  emit('select', String(event.key));
}
</script>

<template>
  <ADropdown>
    <slot />
    <template #overlay>
      <AMenu @click="handleSelect">
        <AMenuItem v-for="item in items" :key="item.key" :disabled="item.disabled" :danger="item.danger">
          {{ item.title }}
        </AMenuItem>
      </AMenu>
    </template>
  </ADropdown>
</template>
