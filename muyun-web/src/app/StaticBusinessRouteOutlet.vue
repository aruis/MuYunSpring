<script setup lang="ts">
import { computed } from 'vue';
import { ModuleContextProvider } from '@muyun/web-core';
import type { BusinessRoutePageDescriptor } from '@muyun/web-contracts';
import { resolveStaticBusinessRoute } from './businessRoutes';

defineOptions({ name: 'StaticBusinessRouteOutlet' });

const props = defineProps<{
  descriptor: BusinessRoutePageDescriptor;
}>();

const route = computed(() => resolveStaticBusinessRoute(props.descriptor));
const moduleAlias = computed(() => props.descriptor.target.moduleAlias ?? route.value?.moduleAlias);
</script>

<template>
  <ModuleContextProvider v-if="route && moduleAlias" :module-alias="moduleAlias">
    <component :is="route.component" />
  </ModuleContextProvider>
</template>
