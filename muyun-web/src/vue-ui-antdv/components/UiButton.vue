<script setup lang="ts">
import { Button as AButton } from 'ant-design-vue';
import UiIcon, { type UiIconName } from './UiIcon.vue';

defineOptions({ name: 'UiButton' });

withDefaults(
  defineProps<{
    type?: 'default' | 'primary' | 'dashed' | 'link' | 'text';
    htmlType?: 'button' | 'submit' | 'reset';
    disabled?: boolean;
    loading?: boolean;
    danger?: boolean;
    iconName?: UiIconName;
    iconPosition?: 'start' | 'end';
  }>(),
  {
    type: 'default',
    htmlType: 'button',
    disabled: false,
    loading: false,
    danger: false,
    iconName: undefined,
    iconPosition: 'start',
  },
);

const emit = defineEmits<{
  click: [event: MouseEvent];
}>();
</script>

<template>
  <AButton
    :type="type"
    :html-type="htmlType"
    :disabled="disabled"
    :loading="loading"
    :danger="danger"
    @click="emit('click', $event)"
  >
    <template v-if="iconName && iconPosition === 'start'" #icon>
      <UiIcon :name="iconName" />
    </template>
    <slot />
    <UiIcon v-if="iconName && iconPosition === 'end'" class="ui-button-trailing-icon" :name="iconName" />
  </AButton>
</template>

<style scoped>
.ui-button-trailing-icon {
  margin-inline-start: 8px;
}
</style>
