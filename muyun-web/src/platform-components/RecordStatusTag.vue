<script setup lang="ts">
defineOptions({ name: 'RecordStatusTag' });

defineProps<{
  enabled?: boolean | null;
  enabledLabel?: string;
  disabledLabel?: string;
  unknownLabel?: string;
  enabledTone?: 'SUCCESS' | 'NEUTRAL' | 'WARNING' | 'DANGER';
  disabledTone?: 'SUCCESS' | 'NEUTRAL' | 'WARNING' | 'DANGER';
}>();
</script>

<template>
  <span
    class="record-status-tag"
    :class="`record-status-tag--${enabled === true ? (enabledTone ?? 'SUCCESS').toLowerCase() : enabled === false ? (disabledTone ?? 'NEUTRAL').toLowerCase() : 'neutral'}`"
  >
    <span class="record-status-tag-dot" />
    <span>{{
      enabled === true
        ? (enabledLabel ?? '启用')
        : enabled === false
          ? (disabledLabel ?? '停用')
          : (unknownLabel ?? '-')
    }}</span>
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

.record-status-tag--neutral {
  color: #697588;
}

.record-status-tag--neutral .record-status-tag-dot {
  background: #94a3b8;
}

.record-status-tag--warning {
  color: #9a6700;
}
.record-status-tag--warning .record-status-tag-dot {
  background: #d97706;
}
.record-status-tag--danger {
  color: #b42318;
}
.record-status-tag--danger .record-status-tag-dot {
  background: #d92d20;
}
</style>
