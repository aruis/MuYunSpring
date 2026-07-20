<script setup lang="ts">
import { Space as ASpace, Tooltip as ATooltip } from 'ant-design-vue';
import type { ActionContract } from '@muyun/web-contracts';
import UiActionButton from './UiActionButton.vue';

defineOptions({ name: 'ActionBar', inheritAttrs: false });

defineProps<{
  actions: ActionContract[];
}>();

const emit = defineEmits<{
  execute: [action: ActionContract];
}>();
</script>

<template>
  <ASpace wrap :class="$attrs.class" :style="$attrs.style">
    <ATooltip
      v-for="action in actions"
      :key="action.actionCode"
      :title="action.disabled ? action.disabledReason : undefined"
    >
      <UiActionButton
        :emphasis="action.level === 'primary' ? 'primary' : 'secondary'"
        :intent="action.level === 'danger' ? 'danger' : 'normal'"
        :disabled="action.disabled"
        @click="emit('execute', action)"
      >
        {{ action.title }}
      </UiActionButton>
    </ATooltip>
  </ASpace>
</template>
