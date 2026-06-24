<script setup lang="ts">
import type { Organization } from '@muyun/web-contracts';
import type { ModuleContext } from '@muyun/web-core';
import TreeRecordExplorer from './TreeRecordExplorer.vue';
import type { TreeRecordBase } from './treeRecordModel';

defineOptions({ name: 'OrganizationTree' });

defineProps<{
  context: ModuleContext<Organization>;
  selectedId?: string;
  reloadKey?: number;
}>();

const emit = defineEmits<{
  select: [organization: Organization];
  loaded: [organizations: Organization[]];
}>();

function organizationTitle(record: TreeRecordBase) {
  return record.title ?? record.code ?? record.id ?? '未命名机构';
}
</script>

<template>
  <TreeRecordExplorer
    :context="context"
    :selected-id="selectedId"
    :reload-key="reloadKey"
    search-placeholder="搜索机构名称、编码或 ID"
    empty-description="暂无机构"
    loading-tip="加载机构树"
    fallback-title="未命名机构"
    :title-of="organizationTitle"
    @select="emit('select', $event as Organization)"
    @loaded="emit('loaded', $event as Organization[])"
  />
</template>
