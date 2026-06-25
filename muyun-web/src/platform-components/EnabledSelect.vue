<script setup lang="ts">
import { computed } from 'vue';
import { UiSelect } from '@muyun/vue-ui-antdv';

defineOptions({ name: 'EnabledSelect' });

const props = withDefaults(
  defineProps<{
    value?: boolean;
    disabled?: boolean;
  }>(),
  {
    value: true,
    disabled: false,
  },
);

const emit = defineEmits<{
  'update:value': [value: boolean];
}>();

const enabledOptions = [
  { label: '启用', value: 'true' },
  { label: '停用', value: 'false' },
];

const enabledValue = computed({
  get: () => (props.value === false ? 'false' : 'true'),
  set: (value) => {
    emit('update:value', value !== 'false');
  },
});
</script>

<template>
  <UiSelect
    v-model:value="enabledValue"
    :options="enabledOptions"
    :disabled="disabled"
    :allow-clear="false"
  />
</template>
