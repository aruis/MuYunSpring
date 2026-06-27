<script setup lang="ts">
defineOptions({ name: 'RecordStatusTag' });

defineProps<{
  enabled?: boolean;
}>();
</script>

<template>
  <span class="record-status-tag" :class="{ 'record-status-tag-disabled': enabled === false }">
    <span class="record-status-tag-dot" />
    <span>{{ enabled === false ? '停用' : '启用' }}</span>
  </span>
</template>

<style scoped>
.record-status-tag {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: #0f6b57;
  font-size: 12px;
  font-weight: 500;
  line-height: 20px;
  white-space: nowrap;
}

.record-status-tag-dot {
  width: 6px;
  height: 6px;
  border-radius: 999px;
  background: #22a06b;
}

.record-status-tag-disabled {
  color: #697588;
}

.record-status-tag-disabled .record-status-tag-dot {
  background: #94a3b8;
}
</style>
