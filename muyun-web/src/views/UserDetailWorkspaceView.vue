<script setup lang="ts">
import UserManagementView from './UserManagementView.vue';

defineOptions({ name: 'UserDetailWorkspaceView' });

defineProps<{
  recordId: string;
  title?: string;
}>();
</script>

<template>
  <UserManagementView :record-id="recordId" />
</template>
