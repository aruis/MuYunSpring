<script setup lang="ts">
import RecordExplorerPanel from './RecordExplorerPanel.vue';
import RecordStatusTag from './RecordStatusTag.vue';

defineOptions({ name: 'StaticManagementLayout' });

withDefaults(
  defineProps<{
    groupTitle: string;
    sidebarTitle: string;
    refreshTitle: string;
    mode: 'view' | 'edit' | 'create';
    cardTitle: string;
    actionError?: string;
    actionMessage?: string;
    mutedMessage?: string;
    showStatus?: boolean;
    enabled?: boolean;
    sidebarSearchKeyword?: string;
    sidebarSearchPlaceholder?: string;
    sidebarSearchable?: boolean;
  }>(),
  {
    actionError: undefined,
    actionMessage: undefined,
    mutedMessage: undefined,
    showStatus: false,
    enabled: undefined,
    sidebarSearchKeyword: '',
    sidebarSearchPlaceholder: '搜索名称、编码或 ID',
    sidebarSearchable: true,
  },
);

const emit = defineEmits<{
  refresh: [];
  'update:sidebarSearchKeyword': [keyword: string];
}>();
</script>

<template>
  <section class="static-management-page">
    <RecordExplorerPanel
      class="static-management-sidebar"
      :title="sidebarTitle"
      :refresh-title="refreshTitle"
      :search-keyword="sidebarSearchKeyword"
      :search-placeholder="sidebarSearchPlaceholder"
      :searchable="sidebarSearchable"
      @update:search-keyword="emit('update:sidebarSearchKeyword', $event)"
      @refresh="emit('refresh')"
    >
      <template #actions>
        <slot name="sidebar-actions" />
      </template>
      <slot name="explorer" />
    </RecordExplorerPanel>

    <main class="static-management-card">
      <header class="card-header">
        <div>
          <p>{{ mode === 'view' ? '查看' : mode === 'edit' ? '编辑' : '新建' }}</p>
          <div class="title-line">
            <h2>{{ cardTitle }}</h2>
            <RecordStatusTag v-if="showStatus" :enabled="enabled" />
          </div>
        </div>
        <slot name="card-actions" />
      </header>

      <div v-if="actionError" class="message error">{{ actionError }}</div>
      <div v-else-if="actionMessage" class="message success">{{ actionMessage }}</div>
      <div v-else-if="mutedMessage" class="message muted">{{ mutedMessage }}</div>

      <slot />
    </main>
  </section>
</template>

<style scoped>
.static-management-page {
  display: grid;
  grid-template-columns: minmax(260px, 320px) minmax(0, 1fr);
  gap: 12px;
  min-height: calc(100vh - 116px);
}

.static-management-sidebar,
.static-management-card {
  min-width: 0;
  border: 1px solid var(--muyun-border);
  border-radius: 8px;
  background: var(--muyun-surface);
}

.static-management-sidebar {
  min-height: 0;
}

.card-header,
.static-management-sidebar :deep(.record-explorer-panel-actions) {
  display: flex;
  align-items: center;
}

.card-header {
  justify-content: space-between;
  gap: 12px;
}

.title-line {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.card-header p,
h2 {
  margin: 0;
}

.card-header p {
  color: var(--muyun-text-muted);
  font-size: 12px;
  font-weight: 700;
}

h2 {
  color: var(--muyun-text);
  font-size: 16px;
}

.static-management-card {
  display: grid;
  align-content: start;
  gap: 14px;
  padding: 16px;
}

.static-management-sidebar :deep(.record-panel-create-button) {
  width: 28px;
  height: 28px;
  padding: 0;
  border-radius: 999px;
}

.message {
  padding: 9px 10px;
  border-radius: 6px;
  font-size: 13px;
}

.message.error {
  border: 1px solid var(--muyun-danger-border);
  background: var(--muyun-danger-bg);
  color: var(--muyun-danger-text);
}

.message.success {
  border: 1px solid var(--muyun-success-border);
  background: var(--muyun-success-bg);
  color: var(--muyun-success-text);
}

.message.muted {
  border: 1px solid var(--muyun-border);
  background: var(--muyun-hover-subtle);
  color: var(--muyun-text-muted);
}

:deep(.static-record-form) {
  display: grid;
  grid-template-columns: repeat(2, minmax(220px, 1fr));
  gap: 14px;
}

:deep(.static-record-form label) {
  display: grid;
  gap: 6px;
  color: var(--muyun-text-body);
  font-size: 13px;
}

@media (max-width: 900px) {
  .static-management-page,
  :deep(.static-record-form) {
    grid-template-columns: 1fr;
  }
}
</style>
