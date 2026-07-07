<script setup lang="ts">
import { computed } from 'vue';
import { UiButton } from '@muyun/vue-ui-antdv';
import RecordDetailDrawer from './RecordDetailDrawer.vue';

defineOptions({ name: 'RecordModeDrawer' });

const props = withDefaults(
  defineProps<{
    open: boolean;
    title: string;
    mode: string;
    viewMode?: string;
    formModes?: string[];
    loading?: boolean;
    loadFailed?: boolean;
    closeOnOutside?: boolean;
    closeTitle?: string;
    errorTitle?: string;
    errorMessage?: string;
    retryTitle?: string;
  }>(),
  {
    viewMode: 'view',
    formModes: () => ['edit', 'create'],
    loading: false,
    loadFailed: false,
    closeOnOutside: undefined,
    closeTitle: '关闭',
    errorTitle: '详情加载失败',
    errorMessage: '无法加载详情，请重试',
    retryTitle: '重试',
  },
);

defineSlots<{
  status(): unknown;
  actions(): unknown;
  loading(): unknown;
  error(): unknown;
  view(): unknown;
  form(): unknown;
  default(): unknown;
}>();

const emit = defineEmits<{
  close: [];
  retry: [];
}>();

const viewModeActive = computed(() => props.mode === props.viewMode);
const formModeActive = computed(() => props.formModes.includes(props.mode));
const actualCloseOnOutside = computed(() => props.closeOnOutside ?? viewModeActive.value);
</script>

<template>
  <RecordDetailDrawer
    :open="open"
    :title="title"
    :close-on-outside="actualCloseOnOutside"
    :close-title="closeTitle"
    @close="emit('close')"
  >
    <template #status>
      <slot name="status" />
    </template>
    <template #actions>
      <slot name="actions" />
    </template>

    <slot />

    <template v-if="loading">
      <slot name="loading" />
    </template>
    <template v-else-if="loadFailed">
      <slot name="error">
        <div class="record-mode-drawer-state">
          <strong>{{ errorTitle }}</strong>
          <span>{{ errorMessage }}</span>
          <UiButton type="primary" icon-name="reload" @click="emit('retry')">{{ retryTitle }}</UiButton>
        </div>
      </slot>
    </template>
    <template v-else-if="viewModeActive">
      <slot name="view" />
    </template>
    <template v-else-if="formModeActive">
      <slot name="form" />
    </template>
  </RecordDetailDrawer>
</template>

<style scoped>
.record-mode-drawer-state {
  display: grid;
  justify-items: start;
  gap: 10px;
  color: var(--muyun-text);
}

.record-mode-drawer-state span {
  color: #64748b;
  font-size: 13px;
}
</style>
