<script setup lang="ts">
import { computed } from 'vue';
import { formatPlatformDateTime, type PlatformDateTimePrecision } from './platformDateTime';
import { usePlatformTimeZoneContext } from './platformTimeZoneContext';

defineOptions({ name: 'DateTimeText' });

const props = withDefaults(
  defineProps<{
    value?: string | number | Date | null;
    emptyText?: string;
    precision?: PlatformDateTimePrecision;
    timeZone?: string;
  }>(),
  {
    value: undefined,
    emptyText: '-',
    precision: 'second',
    timeZone: undefined,
  },
);

const injectedTimeZone = usePlatformTimeZoneContext();
const display = computed(() =>
  formatPlatformDateTime(props.value, {
    emptyText: props.emptyText,
    precision: props.precision,
    timeZone: props.timeZone ?? injectedTimeZone?.value,
  }),
);
</script>

<template>
  <time v-if="display.valid" :datetime="display.datetime" :title="display.title">
    {{ display.text }}
  </time>
  <span v-else :title="display.title">{{ display.text }}</span>
</template>
