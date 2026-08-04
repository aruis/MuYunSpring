<script setup lang="ts">
import RecordDetailLayout from './RecordDetailLayout.vue';
import { usePageLayout } from './pageLayoutContext';

defineOptions({ name: 'RecordDetailPanel' });

withDefaults(
  defineProps<{
    title: string;
    subtitle?: string;
  }>(),
  { subtitle: undefined },
);
const pageLayout = usePageLayout();
</script>

<template>
  <RecordDetailLayout
    surface="workspace"
    :title="title"
    :subtitle="subtitle"
    :scrollable-content="pageLayout === 'workspace'"
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
