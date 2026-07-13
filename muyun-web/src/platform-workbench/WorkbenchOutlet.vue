<script setup lang="ts">
import { computed } from 'vue';
import type {
  BusinessRoutePageDescriptor,
  DynamicModulePageDescriptor,
  ExternalLinkPageDescriptor,
  PageDescriptor,
  PlatformRoutePageDescriptor,
  RemoteUrlPageDescriptor,
} from '@muyun/web-contracts';
import { DynamicModuleHost } from '@muyun/dynamic-page-runtime';
import { UiEmpty } from '@muyun/vue-ui-antdv';
import BusinessRouteHost from './hosts/BusinessRouteHost.vue';
import ExternalPageHost from './hosts/ExternalPageHost.vue';
import PlatformRouteHost from './hosts/PlatformRouteHost.vue';
import { resolvePageHostComponentName } from './pageHostRegistry';

defineOptions({ name: 'WorkbenchOutlet' });

const props = defineProps<{
  descriptor?: PageDescriptor;
}>();

const pageHostComponentName = computed(() =>
  props.descriptor ? resolvePageHostComponentName(props.descriptor.hostType) : undefined,
);
const routeDescriptor = computed(() =>
  pageHostComponentName.value === 'PlatformRouteHost'
    ? (props.descriptor as PlatformRoutePageDescriptor)
    : undefined,
);
const businessRouteDescriptor = computed(() =>
  pageHostComponentName.value === 'BusinessRouteHost'
    ? (props.descriptor as BusinessRoutePageDescriptor)
    : undefined,
);
const dynamicDescriptor = computed(() =>
  pageHostComponentName.value === 'DynamicModuleHost'
    ? (props.descriptor as DynamicModulePageDescriptor)
    : undefined,
);
const externalDescriptor = computed(() =>
  pageHostComponentName.value === 'ExternalPageHost'
    ? (props.descriptor as RemoteUrlPageDescriptor | ExternalLinkPageDescriptor)
    : undefined,
);
</script>

<template>
  <PlatformRouteHost v-if="routeDescriptor" :descriptor="routeDescriptor" />
  <BusinessRouteHost v-else-if="businessRouteDescriptor" :descriptor="businessRouteDescriptor" />
  <DynamicModuleHost v-else-if="dynamicDescriptor" :descriptor="dynamicDescriptor" />
  <ExternalPageHost v-else-if="externalDescriptor" :descriptor="externalDescriptor" />
  <UiEmpty v-else description="暂无页面" />
</template>
