<script setup lang="ts">
import RecordDetailPanel from './RecordDetailPanel.vue';
import RecordExplorerPanel from './RecordExplorerPanel.vue';
import ManagementExplorerColumn from './ManagementExplorerColumn.vue';
import ManagementWorkspace from './ManagementWorkspace.vue';

defineOptions({ name: 'StaticManagementLayout' });

withDefaults(
  defineProps<{
    sidebarTitle: string;
    refreshTitle: string;
    mode: 'view' | 'edit' | 'create';
    cardTitle: string;
    mutedMessage?: string;
    sidebarSearchKeyword?: string;
    sidebarSearchPlaceholder?: string;
    sidebarSearchable?: boolean;
  }>(),
  {
    mutedMessage: undefined,
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
  <ManagementWorkspace class="static-management-page">
    <ManagementExplorerColumn>
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
    </ManagementExplorerColumn>

    <RecordDetailPanel class="static-management-card" :title="cardTitle">
      <template #status>
        <slot name="card-status" />
      </template>
      <template #actions>
        <slot name="card-actions" />
      </template>

      <div v-if="mutedMessage" class="message muted">{{ mutedMessage }}</div>

      <slot />
    </RecordDetailPanel>
  </ManagementWorkspace>
</template>

<style scoped>
.static-management-page {
  --muyun-management-explorer-width: 280px;
  --muyun-management-detail-min-width: 560px;
}

.static-management-sidebar,
.static-management-card {
  min-width: 0;
}

.static-management-sidebar {
  border: 1px solid var(--muyun-border);
  border-radius: 8px;
  background: var(--muyun-surface);
  min-height: 0;
}

.static-management-sidebar :deep(.record-explorer-panel-actions) {
  display: flex;
  align-items: center;
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
  :deep(.static-record-form) {
    grid-template-columns: 1fr;
  }
}
</style>
