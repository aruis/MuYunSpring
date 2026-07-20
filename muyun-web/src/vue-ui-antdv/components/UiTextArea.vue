<script setup lang="ts">
import { Input as AInput } from 'ant-design-vue';

defineOptions({ name: 'UiTextArea', inheritAttrs: false });

withDefaults(
  defineProps<{
    value?: string;
    placeholder?: string;
    disabled?: boolean;
    rows?: number;
    maxlength?: number;
  }>(),
  {
    value: '',
    placeholder: undefined,
    disabled: false,
    rows: 4,
    maxlength: undefined,
  },
);

const emit = defineEmits<{
  'update:value': [value: string];
}>();
</script>

<template>
  <AInput.TextArea
    :value="value"
    :placeholder="placeholder"
    :disabled="disabled"
    :rows="rows"
    :maxlength="maxlength"
    :class="$attrs.class"
    :style="$attrs.style"
    @update:value="emit('update:value', $event)"
  />
</template>
