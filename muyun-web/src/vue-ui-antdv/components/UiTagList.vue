<script setup lang="ts">
import { Tag as ATag, Tooltip as ATooltip } from 'ant-design-vue';
import { computed } from 'vue';

defineOptions({ name: 'UiTagList', inheritAttrs: false });

export interface UiTagListItem {
  key: string;
  label: string;
  color?: string;
}

const props = withDefaults(
  defineProps<{
    items?: UiTagListItem[];
    maxVisible?: number;
    emptyText?: string;
  }>(),
  {
    items: () => [],
    maxVisible: 3,
    emptyText: '-',
  },
);

const visibleLimit = computed(() =>
  Number.isFinite(props.maxVisible) ? Math.max(0, Math.floor(props.maxVisible)) : 0,
);
const visibleItems = computed(() => props.items.slice(0, visibleLimit.value));
const overflowItems = computed(() => props.items.slice(visibleLimit.value));
const overflowTitle = computed(() => overflowItems.value.map((item) => item.label).join('、'));
</script>

<template>
  <span v-if="items.length" class="ui-tag-list" :class="$attrs.class" :style="$attrs.style">
    <ATag v-for="item in visibleItems" :key="item.key" :color="item.color" class="ui-tag-list-item">
      {{ item.label }}
    </ATag>
    <ATooltip v-if="overflowItems.length" :title="overflowTitle">
      <ATag class="ui-tag-list-overflow">+{{ overflowItems.length }}</ATag>
    </ATooltip>
  </span>
  <span v-else class="ui-tag-list-empty" :class="$attrs.class" :style="$attrs.style">{{ emptyText }}</span>
</template>

<style scoped>
.ui-tag-list {
  display: inline-flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
}

.ui-tag-list :deep(.ant-tag) {
  margin-inline-end: 0;
}

.ui-tag-list-overflow {
  color: #697588;
  border-style: dashed;
}
</style>
