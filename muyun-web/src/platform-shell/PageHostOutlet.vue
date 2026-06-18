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
import { UiEmpty } from '@muyun/vue-ui-antdv';
import DynamicModuleHost from './hosts/DynamicModuleHost.vue';
import ExternalPageHost from './hosts/ExternalPageHost.vue';
import PlatformRouteHost from './hosts/PlatformRouteHost.vue';
import { resolvePageHostComponentName } from './pageHostRegistry';

defineOptions({ name: 'PageHostOutlet' });

const props = defineProps<{
  descriptor?: PageDescriptor;
}>();

const pageHostComponentName = computed(() =>
  props.descriptor ? resolvePageHostComponentName(props.descriptor.hostType) : undefined,
);
const routeDescriptor = computed(() =>
  pageHostComponentName.value === 'PlatformRouteHost'
    ? (props.descriptor as PlatformRoutePageDescriptor | BusinessRoutePageDescriptor)
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
  <DynamicModuleHost v-else-if="dynamicDescriptor" :descriptor="dynamicDescriptor" />
  <ExternalPageHost v-else-if="externalDescriptor" :descriptor="externalDescriptor" />
  <UiEmpty v-else description="暂无页面" />
</template>
