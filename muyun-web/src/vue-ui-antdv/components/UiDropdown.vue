<script setup lang="ts">
import { computed, ref } from 'vue';
import { Dropdown as ADropdown, Menu as AMenu } from 'ant-design-vue';
import type { ItemType, MenuProps } from 'ant-design-vue';
import type { UiDropdownItem } from '../types';

defineOptions({ name: 'UiDropdown', inheritAttrs: false });

const props = withDefaults(
  defineProps<{
    items: UiDropdownItem[];
    selectedKey?: string;
    align?: 'start' | 'end';
    trigger?: 'click' | 'hover';
  }>(),
  {
    selectedKey: undefined,
    align: 'end',
    trigger: 'click',
  },
);

defineSlots<{
  default(props: { toggle: () => void }): unknown;
}>();

const emit = defineEmits<{
  select: [key: string];
}>();

const open = ref(false);
const menuItems = computed<ItemType[]>(() =>
  props.items.map((item) => ({
    key: item.key,
    label: item.title,
    disabled: item.disabled,
    danger: item.danger,
  })),
);
const selectedKeys = computed(() => (props.selectedKey ? [props.selectedKey] : []));
const placement = computed(() => (props.align === 'start' ? 'bottomLeft' : 'bottomRight'));
const triggers = computed(() => [props.trigger]);

function toggle() {
  open.value = !open.value;
}

function handleOpenChange(nextOpen: boolean) {
  open.value = nextOpen;
}

function handleMenuClick(event: Parameters<NonNullable<MenuProps['onClick']>>[0]) {
  emit('select', String(event.key));
  open.value = false;
}
</script>

<template>
  <ADropdown
    :open="open"
    :trigger="triggers"
    :placement="placement"
    :overlay-class-name="'muyun-ui-dropdown-overlay'"
    @open-change="handleOpenChange"
  >
    <span class="ui-dropdown" :class="$attrs.class" :style="$attrs.style">
      <slot :toggle="toggle" />
    </span>
    <template #overlay>
      <AMenu :items="menuItems" :selected-keys="selectedKeys" @click="handleMenuClick" />
    </template>
  </ADropdown>
</template>
