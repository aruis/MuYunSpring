<script setup lang="ts">
defineOptions({ name: 'RecordDetailPanel' });

withDefaults(
  defineProps<{
    title: string;
    /** Keeps the panel header fixed while the detail content scrolls. */
    scrollableContent?: boolean;
  }>(),
  {
    scrollableContent: false,
  },
);
</script>

<template>
  <main class="record-detail-panel" :class="{ 'record-detail-panel--scrollable': scrollableContent }">
    <header class="record-detail-panel-header">
      <div class="record-detail-panel-title-group">
        <h2>{{ title }}</h2>
        <slot name="status" />
      </div>
      <div v-if="$slots.actions" class="record-detail-panel-actions">
        <slot name="actions" />
      </div>
    </header>
    <div v-if="scrollableContent" class="record-detail-panel-content">
      <slot />
    </div>
    <slot v-else />
  </main>
</template>

<style scoped>
.record-detail-panel {
  display: grid;
  align-content: start;
  gap: 12px;
  min-width: 0;
  min-height: 0;
  padding: 14px;
  border: 1px solid var(--muyun-border);
  border-radius: 8px;
  background: var(--muyun-surface);
}

.record-detail-panel--scrollable {
  grid-template-rows: auto minmax(0, 1fr);
  overflow: hidden;
}

.record-detail-panel-content {
  min-height: 0;
  overflow: auto;
}

.record-detail-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-width: 0;
}

.record-detail-panel-title-group {
  display: inline-flex;
  flex: 1 1 auto;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.record-detail-panel-title-group h2 {
  margin: 0;
  overflow: hidden;
  color: var(--muyun-text);
  font-size: 16px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.record-detail-panel-actions {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  min-width: 0;
  max-width: 100%;
}

.record-detail-panel-actions :deep(.record-action-bar) {
  justify-content: flex-end;
}

@media (max-width: 720px) {
  .record-detail-panel-header {
    display: grid;
    grid-template-columns: minmax(0, 1fr);
  }

  .record-detail-panel-actions {
    width: 100%;
  }
}
</style>
