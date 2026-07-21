<script setup lang="ts">
import { computed } from 'vue';

defineOptions({ name: 'ManagementWorkspace' });

const props = withDefaults(
  defineProps<{
    /** Number of explorer columns shown before the detail workspace. */
    explorerCount?: 1 | 2 | 3;
  }>(),
  {
    explorerCount: 1,
  },
);

const workspaceClass = computed(() => `management-workspace--${props.explorerCount}-explorer`);
</script>

<template>
  <section class="management-workspace" :class="workspaceClass">
    <slot />
  </section>
</template>

<style scoped>
.management-workspace {
  --muyun-management-explorer-width: 280px;
  --muyun-management-detail-min-width: 560px;
  --muyun-management-column-gap: 12px;

  display: grid;
  align-items: start;
  gap: var(--muyun-management-column-gap);
  min-height: 100%;
}

.management-workspace--1-explorer {
  grid-template-columns: var(--muyun-management-explorer-width) minmax(
      var(--muyun-management-detail-min-width),
      1fr
    );
  min-width: calc(
    var(--muyun-management-explorer-width) + var(--muyun-management-detail-min-width) +
      var(--muyun-management-column-gap)
  );
}

.management-workspace--2-explorer {
  grid-template-columns: repeat(2, var(--muyun-management-explorer-width)) minmax(
      var(--muyun-management-detail-min-width),
      1fr
    );
  min-width: calc(
    var(--muyun-management-explorer-width) + var(--muyun-management-explorer-width) +
      var(--muyun-management-detail-min-width) + var(--muyun-management-column-gap) +
      var(--muyun-management-column-gap)
  );
}

.management-workspace--3-explorer {
  grid-template-columns: repeat(3, var(--muyun-management-explorer-width)) minmax(
      var(--muyun-management-detail-min-width),
      1fr
    );
  min-width: calc(
    var(--muyun-management-explorer-width) + var(--muyun-management-explorer-width) +
      var(--muyun-management-explorer-width) + var(--muyun-management-detail-min-width) +
      var(--muyun-management-column-gap) + var(--muyun-management-column-gap) +
      var(--muyun-management-column-gap)
  );
}

@media (max-width: 719px) {
  .management-workspace,
  .management-workspace--1-explorer,
  .management-workspace--2-explorer,
  .management-workspace--3-explorer {
    grid-template-columns: minmax(0, 1fr);
    min-width: 0;
  }
}
</style>
