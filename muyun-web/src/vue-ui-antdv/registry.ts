import type { Component } from 'vue';
import DictionarySelect from './components/DictionarySelect.vue';
import UiInput from './components/UiInput.vue';
import ReferenceSelect from './components/ReferenceSelect.vue';
import UiSelect from './components/UiSelect.vue';
import type { FieldKind } from '@muyun/web-contracts';

const fieldComponents: Record<FieldKind, Component> = {
  input: UiInput,
  select: UiSelect,
  'dictionary-select': DictionarySelect,
  'reference-select': ReferenceSelect,
};

export function resolveFieldComponent(kind: FieldKind): Component {
  return fieldComponents[kind];
}
