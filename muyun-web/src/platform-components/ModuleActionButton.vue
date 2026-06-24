<script setup lang="ts">
import { computed } from 'vue';
import type { ModuleContext } from '@muyun/web-core';
import { UiButton } from '@muyun/vue-ui-antdv';

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
  <UiButton
    :html-type="type"
    :type="primary ? 'primary' : 'default'"
    :disabled="buttonDisabled"
    :loading="loading"
    :danger="danger"
    :title="buttonTitle"
    @click="handleClick"
  >
    <slot>{{ action?.title ?? actionCode }}</slot>
  </UiButton>
</template>
