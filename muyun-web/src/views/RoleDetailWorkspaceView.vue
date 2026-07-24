<script setup lang="ts">
import type { RoleOwnerScopeType } from '@muyun/web-contracts';
import RoleManagementView from './RoleManagementView.vue';

defineOptions({ name: 'RoleDetailWorkspaceView' });

defineProps<{
  recordId: string;
  scopeKind: RoleOwnerScopeType;
  scopeId?: string;
}>();
</script>

<template>
  <RoleManagementView :record-id="recordId" :scope-kind="scopeKind" :scope-id="scopeId" />
</template>
