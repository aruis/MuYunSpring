<script setup lang="ts">
import SystemUserManagementView from './SystemUserManagementView.vue';

defineOptions({ name: 'SystemUserDetailWorkspaceView' });

defineProps<{
  recordId: string;
  title?: string;
}>();
</script>

<template>
  <SystemUserManagementView :record-id="recordId" />
</template>
