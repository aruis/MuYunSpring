<script setup lang="ts">
import { computed, nextTick, ref } from 'vue';
import { UiButton, UiInput } from '@muyun/vue-ui-antdv';

defineOptions({ name: 'RecordExplorerPanel' });

const props = withDefaults(
  defineProps<{
    title: string;
    refreshTitle?: string;
    searchKeyword?: string;
    searchPlaceholder?: string;
    searchable?: boolean;
  }>(),
  {
    refreshTitle: undefined,
    searchKeyword: '',
    searchPlaceholder: '搜索名称、编码或 ID',
    searchable: true,
  },
);

const emit = defineEmits<{
  'update:searchKeyword': [keyword: string];
  refresh: [];
}>();

const searchExpanded = ref(false);
const searchRoot = ref<HTMLElement>();
const searchVisible = computed(
  () => props.searchable && (searchExpanded.value || props.searchKeyword.trim().length > 0),
);

function toggleSearch() {
  if (searchVisible.value) {
    emit('update:searchKeyword', '');
    searchExpanded.value = false;
    return;
  }
  searchExpanded.value = true;
  focusSearchInput();
}

function handleSearchBlur() {
  if (!props.searchKeyword.trim()) {
    searchExpanded.value = false;
  }
}

function handleSearchEscape() {
  emit('update:searchKeyword', '');
  searchExpanded.value = false;
}

async function focusSearchInput() {
  await nextTick();
  searchRoot.value?.querySelector('input')?.focus();
}
</script>

<template>
  <section class="record-explorer-panel">
    <header class="record-explorer-panel-header">
      <UiButton
        class="record-explorer-panel-title"
        icon-name="reload"
        icon-position="end"
        type="text"
        :title="refreshTitle ?? `刷新${title}`"
        @click="emit('refresh')"
      >
        <span class="record-explorer-panel-title-text">
          <span>{{ title }}</span>
        </span>
      </UiButton>
      <div v-if="$slots['title-extra']" class="record-explorer-panel-title-extra">
        <slot name="title-extra" />
      </div>
      <div class="record-explorer-panel-actions">
        <UiButton
          v-if="searchable"
          icon-name="search"
          type="text"
          :title="`搜索${title}`"
          @mousedown.prevent
          @click="toggleSearch"
        />
        <slot name="actions" />
      </div>
    </header>

    <Transition name="record-explorer-search">
      <div v-if="searchVisible" ref="searchRoot" class="record-explorer-search">
        <UiInput
          :value="searchKeyword"
          allow-clear
          :placeholder="searchPlaceholder"
          autofocus
          @update:value="emit('update:searchKeyword', $event)"
          @blur="handleSearchBlur"
          @keydown.esc="handleSearchEscape"
        />
      </div>
    </Transition>

    <div class="record-explorer-panel-content">
      <slot />
    </div>

    <footer v-if="$slots.footer" class="record-explorer-panel-footer">
      <slot name="footer" />
    </footer>

    <slot name="editor" />
  </section>
</template>

<style scoped>
.record-explorer-panel {
  position: relative;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  padding: 12px;
  border: 1px solid var(--muyun-border);
  border-radius: 8px;
  background: var(--muyun-surface);
  overflow: hidden;
}

.record-explorer-panel-header {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.record-explorer-panel-title {
  margin: -4px 0 -4px -6px;
  padding: 4px 6px;
  color: var(--muyun-text);
  font-size: 16px;
  font-weight: 700;
}

.record-explorer-panel-title-text {
  display: inline-grid;
  justify-items: start;
  gap: 2px;
}

.record-explorer-panel-title-extra {
  display: inline-flex;
  flex: 1 1 auto;
  align-items: center;
  min-width: 0;
}

.record-explorer-panel-title :deep(.ui-button-trailing-icon) {
  width: 0;
  margin-inline-start: 0;
  margin-inline-end: 0;
  color: var(--muyun-text-muted);
  opacity: 0;
  overflow: hidden;
  transition:
    width 0.12s ease,
    margin-inline-start 0.12s ease,
    opacity 0.12s ease;
}

.record-explorer-panel-title:hover :deep(.ui-button-trailing-icon),
.record-explorer-panel-title:focus-visible :deep(.ui-button-trailing-icon) {
  width: 1em;
  margin-inline-start: 6px;
  opacity: 1;
}

.record-explorer-panel-actions {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 4px;
}

.record-explorer-search {
  display: flex;
  flex: 0 0 auto;
  min-width: 0;
  margin-bottom: 10px;
  overflow: hidden;
}

.record-explorer-search-enter-active,
.record-explorer-search-leave-active {
  max-height: 40px;
  transition:
    max-height 0.16s ease,
    margin-bottom 0.16s ease,
    opacity 0.16s ease,
    transform 0.16s ease;
}

.record-explorer-search-enter-from,
.record-explorer-search-leave-to {
  max-height: 0;
  margin-bottom: 0;
  opacity: 0;
  transform: translateY(-4px);
}

.record-explorer-search-enter-to,
.record-explorer-search-leave-from {
  max-height: 40px;
  margin-bottom: 10px;
  opacity: 1;
  transform: translateY(0);
}

.record-explorer-search :deep(.ant-input) {
  width: 100%;
}

.record-explorer-panel-content {
  flex: 1 1 auto;
  min-height: 0;
  overflow: hidden;
}

.record-explorer-panel-footer {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  min-height: 32px;
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px solid var(--muyun-border-subtle);
}
</style>
