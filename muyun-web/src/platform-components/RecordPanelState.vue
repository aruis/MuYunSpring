<script setup lang="ts">
import { UiEmpty, UiSpin } from '@muyun/vue-ui-antdv';

defineOptions({ name: 'RecordPanelState' });

withDefaults(
  defineProps<{
    loading?: boolean;
    description: string;
    loadingTip?: string;
  }>(),
  {
    loading: false,
    loadingTip: '加载中',
  },
);
</script>

<template>
  <UiSpin v-if="loading" :tip="loadingTip" />
  <UiEmpty v-else :description="description" />
</template>
