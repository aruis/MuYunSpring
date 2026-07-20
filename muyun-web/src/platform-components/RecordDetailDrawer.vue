<script setup lang="ts">
import { UiActionButton, UiSidePanel } from '@muyun/vue-ui-antdv';
import RecordDetailPanel from './RecordDetailPanel.vue';

defineOptions({ name: 'RecordDetailDrawer' });

withDefaults(
  defineProps<{
    open: boolean;
    title: string;
    closeOnOutside?: boolean;
    closeTitle?: string;
  }>(),
  {
    closeOnOutside: false,
    closeTitle: '关闭',
  },
);

defineSlots<{
  status(): unknown;
  actions(): unknown;
  default(): unknown;
}>();

const emit = defineEmits<{
  close: [];
}>();
</script>

<template>
  <UiSidePanel :open="open" :close-on-outside="closeOnOutside" @close="emit('close')">
    <RecordDetailPanel class="record-detail-drawer-panel" :title="title">
      <template #status>
        <slot name="status" />
      </template>
      <template #actions>
        <slot name="actions" />
        <UiActionButton emphasis="quiet" icon-name="close" :title="closeTitle" @click="emit('close')" />
      </template>
      <slot />
    </RecordDetailPanel>
  </UiSidePanel>
</template>

<style scoped>
.record-detail-drawer-panel {
  height: 100%;
  overflow: auto;
  border-radius: 8px;
}
</style>
