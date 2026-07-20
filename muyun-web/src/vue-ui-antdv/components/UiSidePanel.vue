<script setup lang="ts">
import { Drawer as ADrawer } from 'ant-design-vue';

defineOptions({ name: 'UiSidePanel', inheritAttrs: false });

withDefaults(
  defineProps<{
    open: boolean;
    width?: number | string;
    closeOnOutside?: boolean;
  }>(),
  {
    width: 520,
    closeOnOutside: false,
  },
);

const emit = defineEmits<{
  close: [];
}>();
</script>

<template>
  <ADrawer
    :open="open"
    placement="right"
    :width="width"
    :get-container="false"
    :mask="closeOnOutside"
    :mask-closable="closeOnOutside"
    :mask-style="{ background: 'transparent' }"
    :keyboard="closeOnOutside"
    :closable="false"
    :header-style="{ display: 'none' }"
    :body-style="{ height: '100%', padding: 0 }"
    :root-style="{ position: 'absolute', inset: 0, zIndex: 6 }"
    :class="$attrs.class"
    :style="$attrs.style"
    @close="emit('close')"
  >
    <slot />
  </ADrawer>
</template>
