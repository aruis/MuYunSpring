<script setup lang="ts">
import { onMounted } from 'vue';
import type { ModuleContext } from '@muyun/web-core';
import type { RecycleBinItem } from '@muyun/web-contracts';
import { confirmAction, UiButton, UiEmpty, UiSpin } from '@muyun/vue-ui-antdv';
import { useRecycleBinState } from './recycleBinState';
import { formatPlatformDateTime } from './platformDateTime';

defineOptions({ name: 'RecycleBinPanel' });

const props = withDefaults(
  defineProps<{
    context: ModuleContext<unknown>;
    recordTitle?: (record: unknown) => string;
    title?: string;
    emptyDescription?: string;
  }>(),
  {
    recordTitle: undefined,
    title: '回收站',
    emptyDescription: '回收站为空',
  },
);

const emit = defineEmits<{
  restored: [];
  purged: [];
}>();

const state = useRecycleBinState({
  context: props.context,
  recordTitle: props.recordTitle,
});

onMounted(() => void state.load());

function deletedAtText(item: RecycleBinItem<unknown>): string {
  return formatPlatformDateTime(item.deletedAt, { precision: 'minute' }).text;
}

async function handleRestore(item: RecycleBinItem<unknown>) {
  const title = state.recordTitleOf(item);
  const confirmed = await confirmAction({
    title: '恢复记录',
    content: `确认恢复「${title}」及其关联资源？`,
    okText: '恢复',
  });
  if (!confirmed) return;
  const report = await state.restore(item);
  if (report) emit('restored');
}

async function handlePurge(item: RecycleBinItem<unknown>) {
  const title = state.recordTitleOf(item);
  const confirmed = await confirmAction({
    title: '彻底删除',
    content: `彻底删除后数据不可恢复。确认彻底删除「${title}」及其关联资源？`,
    okText: '彻底删除',
    danger: true,
    requiredText: title,
  });
  if (!confirmed) return;
  const report = await state.purge(item);
  if (report) emit('purged');
}
</script>

<template>
  <section class="recycle-bin-panel">
    <header class="recycle-bin-panel-header">
      <UiButton
        class="recycle-bin-panel-title"
        icon-name="reload"
        icon-position="end"
        type="text"
        :title="`刷新${title}`"
        @click="state.refresh()"
      >
        <span class="recycle-bin-panel-title-text">{{ title }}</span>
      </UiButton>
      <span class="recycle-bin-panel-count">{{ state.items.value.length }} 项</span>
    </header>

    <div class="recycle-bin-panel-content">
      <UiSpin :spinning="state.loading.value" tip="加载回收站">
        <UiEmpty v-if="state.isEmpty.value" :description="emptyDescription" />
        <ul v-else class="recycle-bin-list">
          <li v-for="item in state.items.value" :key="item.sourceDeleteOperationId" class="recycle-bin-item">
            <div class="recycle-bin-item-info">
              <span class="recycle-bin-item-title">{{ state.recordTitleOf(item) }}</span>
              <span class="recycle-bin-item-meta">
                删除于 {{ deletedAtText(item) }}
                <template v-if="!item.restorable && item.unavailableReason">
                  · {{ item.unavailableReason }}
                </template>
              </span>
            </div>
            <div class="recycle-bin-item-actions">
              <UiButton
                size="small"
                :disabled="!item.restorable || state.acting.value"
                :loading="
                  state.acting.value && state.actingOperationId.value === item.sourceDeleteOperationId
                "
                @click="handleRestore(item)"
              >
                恢复
              </UiButton>
              <UiButton
                size="small"
                danger
                :disabled="state.acting.value"
                :loading="
                  state.acting.value && state.actingOperationId.value === item.sourceDeleteOperationId
                "
                @click="handlePurge(item)"
              >
                彻底删除
              </UiButton>
            </div>
          </li>
        </ul>
      </UiSpin>
    </div>
  </section>
</template>

<style scoped>
.recycle-bin-panel {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
}

.recycle-bin-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.recycle-bin-panel-title {
  margin: -4px 0 -4px -6px;
  padding: 4px 6px;
  color: var(--muyun-text);
  font-size: 16px;
  font-weight: 700;
}

.recycle-bin-panel-title-text {
  display: inline-block;
}

.recycle-bin-panel-title :deep(.ui-button-trailing-icon) {
  width: 0;
  margin-inline-start: 0;
  color: var(--muyun-text-muted);
  opacity: 0;
  overflow: hidden;
  transition:
    width 0.12s ease,
    margin-inline-start 0.12s ease,
    opacity 0.12s ease;
}

.recycle-bin-panel-title:hover :deep(.ui-button-trailing-icon),
.recycle-bin-panel-title:focus-visible :deep(.ui-button-trailing-icon) {
  width: 1em;
  margin-inline-start: 6px;
  opacity: 1;
}

.recycle-bin-panel-count {
  color: var(--muyun-text-muted);
  font-size: 13px;
}

.recycle-bin-panel-content {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
}

.recycle-bin-list {
  display: grid;
  gap: 8px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.recycle-bin-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border: 1px solid var(--muyun-border);
  border-radius: 6px;
  background: var(--muyun-surface);
}

.recycle-bin-item-info {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.recycle-bin-item-title {
  color: var(--muyun-text);
  font-size: 14px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recycle-bin-item-meta {
  color: var(--muyun-text-muted);
  font-size: 12px;
}

.recycle-bin-item-actions {
  display: flex;
  flex: 0 0 auto;
  gap: 8px;
}
</style>
