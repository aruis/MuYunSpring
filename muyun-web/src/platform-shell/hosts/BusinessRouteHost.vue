<script setup lang="ts">
import type { BusinessRoutePageDescriptor } from '@muyun/web-contracts';
import { computed } from 'vue';

defineOptions({ name: 'BusinessRouteHost' });

const props = defineProps<{
  descriptor: BusinessRoutePageDescriptor;
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
  <section class="page-host">
    <p class="eyebrow">business-route</p>
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
