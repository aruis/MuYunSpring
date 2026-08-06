<script setup lang="ts">
import type { PageDescriptor } from '@muyun/web-contracts';
import WorkbenchOutlet from '../platform-workbench/WorkbenchOutlet.vue';
import { isPlatformAdminRoutePage } from '../platform-admin-runtime/platformAdminRoutes';
import PlatformAdminRouteOutlet from '../platform-admin-runtime/PlatformAdminOutlet.vue';

defineOptions({ name: 'PlatformAdminOutlet' });

defineProps<{
  descriptor?: PageDescriptor;
}>();
</script>

<template>
  <PlatformAdminRouteOutlet v-if="isPlatformAdminRoutePage(descriptor)" :descriptor="descriptor" />
  <WorkbenchOutlet v-else :descriptor="descriptor" />
</template>
