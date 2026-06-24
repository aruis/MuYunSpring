<script setup lang="ts">
import { Space as ASpace, Tooltip as ATooltip } from 'ant-design-vue';
import type { ActionContract } from '@muyun/web-contracts';
import UiButton from './UiButton.vue';

defineOptions({ name: 'ActionBar' });

defineProps<{
  actions: ActionContract[];
}>();

const emit = defineEmits<{
  execute: [action: ActionContract];
}>();
</script>

<template>
  <ASpace wrap>
    <ATooltip
      v-for="action in actions"
      :key="action.actionCode"
      :title="action.disabled ? action.disabledReason : undefined"
    >
      <UiButton
        :type="action.level === 'primary' ? 'primary' : 'default'"
        :danger="action.level === 'danger'"
        :disabled="action.disabled"
        @click="emit('execute', action)"
      >
        {{ action.title }}
      </UiButton>
    </ATooltip>
  </ASpace>
</template>
