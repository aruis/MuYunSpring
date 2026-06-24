<script setup lang="ts">
defineOptions({ name: 'RecordStatusTag' });

defineProps<{
  enabled?: boolean;
}>();
</script>

<template>
  <span class="record-status-tag" :class="{ 'record-status-tag-disabled': enabled === false }">
    {{ enabled === false ? '停用' : '启用' }}
  </span>
</template>

<style scoped>
.record-status-tag {
  flex: 0 0 auto;
  padding: 1px 6px;
  border: 1px solid #9fd4bf;
  border-radius: 4px;
  background: #f1fbf7;
  color: #0f6b57;
  font-size: 12px;
  line-height: 18px;
}

.record-status-tag-disabled {
  border-color: #d7dde5;
  background: #f8fafc;
  color: #697588;
}
</style>
