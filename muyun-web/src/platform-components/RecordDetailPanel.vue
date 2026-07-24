<script setup lang="ts">
import RecordDetailLayout from './RecordDetailLayout.vue';

defineOptions({ name: 'RecordDetailPanel' });

withDefaults(
  defineProps<{
    title: string;
    subtitle?: string;
    /** Keeps the panel header fixed while the detail content scrolls. */
    scrollableContent?: boolean;
  }>(),
  {
    subtitle: undefined,
    scrollableContent: false,
  },
);
</script>

<template>
  <RecordDetailLayout
    surface="workspace"
    :title="title"
    :subtitle="subtitle"
    :scrollable-content="scrollableContent"
  >
    <template #status>
      <slot name="status" />
    </template>
    <template #actions>
      <slot name="actions" />
    </template>
    <slot />
    <template v-if="$slots.operation" #operation>
      <slot name="operation" />
    </template>
  </RecordDetailLayout>
</template>
