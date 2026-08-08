<script setup lang="ts">
import { computed } from 'vue';

defineOptions({ name: 'RecordTagList' });

interface TagItem {
  id: string;
  title: string;
  color?: string;
}

const props = defineProps<{ items: unknown; maxVisible?: number }>();

const tags = computed<TagItem[]>(() =>
  (Array.isArray(props.items) ? props.items : [])
    .filter((item): item is Record<string, unknown> => typeof item === 'object' && item !== null)
    .map((item) => ({
      id: String(item.id ?? item.title ?? ''),
      title: String(item.title ?? ''),
      color: safeColor(item.color),
    }))
    .filter((item) => item.title.trim().length > 0),
);
const visibleTags = computed(() => tags.value.slice(0, props.maxVisible ?? 3));
const overflow = computed(() => Math.max(0, tags.value.length - visibleTags.value.length));

function safeColor(value: unknown) {
  return typeof value === 'string' && /^#[0-9a-f]{6}$/i.test(value) ? value.toUpperCase() : undefined;
}
</script>

<template>
  <span v-if="tags.length" class="record-tag-list">
    <span
      v-for="tag in visibleTags"
      :key="tag.id"
      class="record-tag-list-item"
      :style="
        tag.color
          ? { color: tag.color, borderColor: tag.color, backgroundColor: `${tag.color}24` }
          : undefined
      "
      >{{ tag.title }}</span
    >
    <span
      v-if="overflow"
      class="record-tag-list-overflow"
      :title="
        tags
          .slice(visibleTags.length)
          .map((tag) => tag.title)
          .join('、')
      "
    >
      +{{ overflow }}
    </span>
  </span>
  <span v-else>-</span>
</template>

<style scoped>
.record-tag-list {
  display: inline-flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
}
.record-tag-list-item,
.record-tag-list-overflow {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
  padding: 0 7px;
  border: 1px solid #d7deea;
  border-radius: 4px;
  color: #526071;
  font-size: 12px;
  line-height: 20px;
  white-space: nowrap;
}
.record-tag-list-overflow {
  color: #697588;
  border-style: dashed;
}
</style>
