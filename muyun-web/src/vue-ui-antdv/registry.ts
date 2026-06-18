import type { Component } from 'vue';
import MuyunDictionarySelect from './components/MuyunDictionarySelect.vue';
import MuyunInput from './components/MuyunInput.vue';
import MuyunReferenceSelect from './components/MuyunReferenceSelect.vue';
import MuyunSelect from './components/MuyunSelect.vue';
import type { MuyunFieldKind } from '@muyun/web-contracts';

const fieldComponents: Record<MuyunFieldKind, Component> = {
  input: MuyunInput,
  select: MuyunSelect,
  'dictionary-select': MuyunDictionarySelect,
  'reference-select': MuyunReferenceSelect,
};

export function resolveMuyunFieldComponent(kind: MuyunFieldKind): Component {
  return fieldComponents[kind];
}
