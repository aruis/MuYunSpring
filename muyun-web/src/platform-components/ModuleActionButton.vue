<script setup lang="ts">
import { computed } from 'vue';
import type { ModuleContext } from '@muyun/web-core';

defineOptions({ name: 'ModuleActionButton' });

const props = withDefaults(
  defineProps<{
    context: ModuleContext<unknown>;
    actionCode: string;
    type?: 'button' | 'submit' | 'reset';
    disabled?: boolean;
    loading?: boolean;
    primary?: boolean;
    danger?: boolean;
    title?: string;
  }>(),
  {
    type: 'button',
    disabled: false,
    loading: false,
    primary: false,
    danger: false,
    title: undefined,
  },
);

const emit = defineEmits<{
  click: [event: MouseEvent];
}>();

const action = computed(() => props.context.action(props.actionCode));
const authorized = computed(() => props.context.can(props.actionCode) === true);
const buttonDisabled = computed(() => props.loading || props.disabled || !authorized.value);
const buttonTitle = computed(() => props.title ?? action.value?.title);

function handleClick(event: MouseEvent) {
  if (buttonDisabled.value) {
    event.preventDefault();
    return;
  }
  emit('click', event);
}
</script>

<template>
  <button
    :type="type"
    class="module-action-button"
    :class="{ primary, danger }"
    :disabled="buttonDisabled"
    :title="buttonTitle"
    @click="handleClick"
  >
    <slot>{{ action?.title ?? actionCode }}</slot>
  </button>
</template>

<style scoped>
.module-action-button {
  height: 32px;
  padding: 0 10px;
  border: 1px solid #cfd9e5;
  border-radius: 6px;
  background: #fff;
  color: #243447;
  cursor: pointer;
}

.module-action-button:disabled {
  color: #9aa7b5;
  cursor: not-allowed;
}

.module-action-button.primary {
  border-color: #0f766e;
  background: #0f766e;
  color: #fff;
}

.module-action-button.danger {
  border-color: #f3c6c6;
  color: #b42318;
}
</style>
