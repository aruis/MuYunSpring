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
    size?: 'default' | 'compact';
  }>(),
  {
    loading: false,
    size: 'default',
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
  <div class="record-action-bar" :class="{ compact: size === 'compact' }">
    <UiButton
      v-for="action in resolvedActions"
      :key="action.key"
      :type="action.primary ? 'primary' : size === 'compact' && !action.danger ? 'text' : 'default'"
      :disabled="action.disabled"
      :loading="action.loading"
      :danger="action.danger"
      :icon-name="action.iconName"
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

.record-action-bar.compact {
  gap: 4px;
}

.record-action-bar.compact :deep(.ant-btn) {
  min-width: 0;
  height: 26px;
  padding: 0 8px;
  font-size: 12px;
}

.record-action-bar.compact :deep(.ant-btn-icon) {
  font-size: 12px;
}
</style>
