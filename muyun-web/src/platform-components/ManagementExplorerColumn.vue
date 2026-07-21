<script setup lang="ts">
defineOptions({ name: 'ManagementExplorerColumn' });
</script>

<template>
  <section class="management-explorer-column">
    <slot />
  </section>
</template>

<style scoped>
.management-explorer-column {
  display: flex;
  align-self: stretch;
  min-width: 0;
  min-height: 0;
}

.management-explorer-column :slotted(*) {
  flex: 1 1 auto;
  min-width: 0;
  min-height: 0;
}
</style>
