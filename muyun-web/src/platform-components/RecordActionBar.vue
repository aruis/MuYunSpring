<script setup lang="ts">
import { computed } from 'vue';
import type { ModuleContext } from '@muyun/web-core';
import { UiButton } from '@muyun/vue-ui-antdv';
import { resolveRecordActions, type RecordActionItem } from './recordActionBarModel';

defineOptions({ name: 'RecordActionBar' });

const props = withDefaults(
  defineProps<{
    context: ModuleContext<unknown>;
    actions: RecordActionItem[];
    loading?: boolean;
  }>(),
  {
    loading: false,
  },
);

const emit = defineEmits<{
  action: [action: RecordActionItem, event: MouseEvent];
}>();

const resolvedActions = computed(() => resolveRecordActions(props.context, props.actions, props.loading));

function handleClick(action: RecordActionItem, event: MouseEvent) {
  emit('action', action, event);
}
</script>

<template>
  <div class="record-action-bar">
    <UiButton
      v-for="action in resolvedActions"
      :key="action.key"
      :type="action.primary ? 'primary' : 'default'"
      :disabled="action.disabled"
      :loading="action.loading"
      :danger="action.danger"
      @click="handleClick(action, $event)"
    >
      {{ action.title }}
    </UiButton>
  </div>
</template>

<style scoped>
.record-action-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
