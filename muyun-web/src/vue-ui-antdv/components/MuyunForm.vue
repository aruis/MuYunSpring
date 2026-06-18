<script setup lang="ts">
import { computed } from 'vue';
import { Button as AButton, Form as AForm, FormItem as AFormItem } from 'ant-design-vue';
import { resolveMuyunFieldComponent } from '../registry';
import type {
  MuyunFieldCondition,
  MuyunFieldContract,
  MuyunFormContract,
  MuyunPrimitive,
  MuyunRecord,
} from '@muyun/web-contracts';

defineOptions({ name: 'MuyunForm' });

const props = defineProps<{
  contract: MuyunFormContract;
  modelValue: MuyunRecord;
  submitText?: string;
}>();

const emit = defineEmits<{
  'update:modelValue': [value: MuyunRecord];
  submit: [value: MuyunRecord];
}>();

const formModel = computed({
  get: () => props.modelValue,
  set: (value: MuyunRecord) => emit('update:modelValue', value),
});

function matchCondition(condition: MuyunFieldCondition | undefined) {
  if (!condition) {
    return false;
  }

  const actual = props.modelValue[condition.field];
  if ('equals' in condition) {
    return actual === condition.equals;
  }
  if ('notEquals' in condition) {
    return actual !== condition.notEquals;
  }
  return false;
}

function isVisible(field: MuyunFieldContract) {
  return field.visibleWhen ? matchCondition(field.visibleWhen) : true;
}

function isReadonly(field: MuyunFieldContract) {
  return Boolean(field.disabled || matchCondition(field.readonlyWhen));
}

function isRequired(field: MuyunFieldContract) {
  return Boolean(field.required || matchCondition(field.requiredWhen));
}

function updateField(name: string, value: MuyunPrimitive) {
  emit('update:modelValue', {
    ...props.modelValue,
    [name]: value,
  });
}

function mergePatch(patch: Record<string, MuyunPrimitive>) {
  emit('update:modelValue', {
    ...props.modelValue,
    ...patch,
  });
}

const visibleFields = computed(() => props.contract.fields.filter(isVisible));
</script>

<template>
  <AForm :model="formModel" layout="vertical" @finish="emit('submit', modelValue)">
    <AFormItem
      v-for="field in visibleFields"
      :key="field.name"
      :label="field.label"
      :name="field.name"
      :rules="isRequired(field) ? [{ required: true, message: `请填写${field.label}` }] : undefined"
    >
      <component
        :is="resolveMuyunFieldComponent(field.kind)"
        :value="modelValue[field.name]"
        :placeholder="field.placeholder"
        :disabled="isReadonly(field)"
        :options="field.options ?? []"
        :dictionary-alias="field.dictionaryAlias"
        :reference="field.reference"
        @update:value="updateField(field.name, $event)"
        @fill-back="mergePatch"
      />
    </AFormItem>

    <AFormItem>
      <AButton type="primary" html-type="submit">{{ submitText ?? '保存' }}</AButton>
    </AFormItem>
  </AForm>
</template>
