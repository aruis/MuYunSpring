<script setup lang="ts">
import type { BusinessRoutePageDescriptor, PlatformRoutePageDescriptor } from '@muyun/web-contracts';
import { computed } from 'vue';

defineOptions({ name: 'PlatformRouteHost' });

const props = defineProps<{
  descriptor: PlatformRoutePageDescriptor | BusinessRoutePageDescriptor;
}>();

const title = computed(
  () =>
    props.descriptor.title ??
    props.descriptor.target.route ??
    props.descriptor.target.routeName ??
    props.descriptor.target.pageKey,
);
</script>

<template>
  <RouterView v-if="props.descriptor.target.route === '/'" />
  <section v-else class="page-host">
    <p class="eyebrow">{{ props.descriptor.pageType }}</p>
    <h2>{{ title }}</h2>
  </section>
</template>

<style scoped>
.page-host {
  min-height: 240px;
  padding: 24px;
  border: 1px solid #dbe4ef;
  border-radius: 8px;
  background: #fff;
}

.eyebrow {
  margin: 0 0 8px;
  color: #6b7788;
  font-size: 12px;
}

h2 {
  margin: 0;
  color: #1f2933;
  font-size: 18px;
}
</style>
