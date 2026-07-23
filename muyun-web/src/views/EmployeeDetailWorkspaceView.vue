<script setup lang="ts">
import EmployeeManagementView from './EmployeeManagementView.vue';

defineOptions({ name: 'EmployeeDetailWorkspaceView' });

defineProps<{ recordId: string }>();
</script>

<template>
  <EmployeeManagementView :record-id="recordId" />
</template>
