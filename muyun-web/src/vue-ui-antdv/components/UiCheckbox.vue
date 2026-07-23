<script setup lang="ts">
import { Checkbox as ACheckbox } from 'ant-design-vue';

defineOptions({ name: 'UiCheckbox', inheritAttrs: false });

withDefaults(
  defineProps<{
    checked?: boolean;
    indeterminate?: boolean;
    disabled?: boolean;
    ariaLabel?: string;
  }>(),
  {
    checked: false,
    indeterminate: false,
    disabled: false,
    ariaLabel: undefined,
  },
);

const emit = defineEmits<{
  'update:checked': [checked: boolean];
  change: [checked: boolean];
}>();

function handleChange(event: { target?: { checked?: boolean } }) {
  const checked = event.target?.checked === true;
  emit('update:checked', checked);
  emit('change', checked);
}
</script>

<template>
  <ACheckbox
    :checked="checked"
    :indeterminate="indeterminate"
    :disabled="disabled"
    :aria-label="ariaLabel"
    :class="$attrs.class"
    :style="$attrs.style"
    @change="handleChange"
  >
    <slot />
  </ACheckbox>
</template>
