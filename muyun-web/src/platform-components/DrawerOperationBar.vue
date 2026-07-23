<script setup lang="ts">
defineOptions({ name: 'DrawerOperationBar' });

withDefaults(
  defineProps<{
    label?: string;
  }>(),
  {
    label: '操作区域',
  },
);
</script>

<template>
  <section class="drawer-operation-bar" :aria-label="label">
    <slot />
  </section>
</template>

<style scoped>
.drawer-operation-bar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  min-width: 0;
  flex-wrap: wrap;
}

.drawer-operation-bar :deep(.record-action-bar) {
  justify-content: flex-end;
}

.drawer-operation-bar :deep(.ant-btn-primary) {
  order: 1;
}

@media (max-width: 480px) {
  .drawer-operation-bar,
  .drawer-operation-bar :deep(.record-action-bar) {
    width: 100%;
  }

  .drawer-operation-bar :deep(.ant-btn) {
    flex: 1 1 auto;
  }
}
</style>
