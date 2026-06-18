<script setup lang="ts">
import { Button as AButton, Space as ASpace, Tooltip as ATooltip } from 'ant-design-vue';
import type { ActionContract } from '@muyun/web-contracts';

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
      <AButton
        :type="action.level === 'primary' ? 'primary' : 'default'"
        :danger="action.level === 'danger'"
        :disabled="action.disabled"
        @click="emit('execute', action)"
      >
        {{ action.title }}
      </AButton>
    </ATooltip>
  </ASpace>
</template>
