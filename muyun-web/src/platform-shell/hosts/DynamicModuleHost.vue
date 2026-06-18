<script setup lang="ts">
import type { DynamicModulePageDescriptor } from '@muyun/web-contracts';

defineOptions({ name: 'DynamicModuleHost' });

defineProps<{
  descriptor: DynamicModulePageDescriptor;
}>();
</script>

<template>
  <section class="page-host">
    <p class="eyebrow">dynamic-module</p>
    <h2>{{ descriptor.title ?? descriptor.target.moduleAlias }}</h2>
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
