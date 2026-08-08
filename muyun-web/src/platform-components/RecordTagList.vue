<script setup lang="ts">
import { computed } from 'vue';
import { UiTagList, type UiTagListItem } from '@muyun/vue-ui-antdv';

defineOptions({ name: 'RecordTagList' });

type TagItem = UiTagListItem;

const props = defineProps<{ items: unknown; maxVisible?: number }>();

const tags = computed<TagItem[]>(() =>
  (Array.isArray(props.items) ? props.items : [])
    .filter((item): item is Record<string, unknown> => typeof item === 'object' && item !== null)
    .map((item) => ({
      key: String(item.id ?? item.title ?? ''),
      label: String(item.title ?? ''),
      color: safeColor(item.color),
    }))
    .filter((item) => item.label.trim().length > 0),
);

function safeColor(value: unknown) {
  return typeof value === 'string' && /^#[0-9a-f]{6}$/i.test(value) ? value.toUpperCase() : undefined;
}
</script>

<template>
  <UiTagList :items="tags" :max-visible="maxVisible" />
</template>
