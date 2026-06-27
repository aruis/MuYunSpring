<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue';
import { UiButton } from '@muyun/vue-ui-antdv';
import RecordDetailPanel from './RecordDetailPanel.vue';

defineOptions({ name: 'RecordDetailDrawer' });

const props = withDefaults(
  defineProps<{
    open: boolean;
    title: string;
    closeOnOutside?: boolean;
    closeTitle?: string;
  }>(),
  {
    closeOnOutside: false,
    closeTitle: '关闭',
  },
);

defineSlots<{
  status(): unknown;
  actions(): unknown;
  default(): unknown;
}>();

const emit = defineEmits<{
  close: [];
}>();

const root = ref<HTMLElement>();

function handleDocumentPointerDown(event: PointerEvent) {
  if (!props.open || !props.closeOnOutside) {
    return;
  }
  if (root.value?.contains(event.target as Node)) {
    return;
  }
  emit('close');
}

onMounted(() => {
  document.addEventListener('pointerdown', handleDocumentPointerDown, true);
});

onBeforeUnmount(() => {
  document.removeEventListener('pointerdown', handleDocumentPointerDown, true);
});
</script>

<template>
  <Transition name="record-detail-drawer">
    <aside v-if="open" ref="root" class="record-detail-drawer" role="dialog" aria-modal="false">
      <RecordDetailPanel class="record-detail-drawer-panel" :title="title">
        <template #status>
          <slot name="status" />
        </template>
        <template #actions>
          <slot name="actions" />
          <UiButton type="text" icon-name="close" :title="closeTitle" @click="emit('close')" />
        </template>
        <slot />
      </RecordDetailPanel>
    </aside>
  </Transition>
</template>

<style scoped>
.record-detail-drawer {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  z-index: 6;
  width: min(520px, 100%);
  min-width: min(420px, 100%);
  background: var(--muyun-surface);
  box-shadow: -12px 0 28px rgb(15 23 42 / 10%);
}

.record-detail-drawer-panel {
  height: 100%;
  overflow: auto;
  border-radius: 8px;
}

.record-detail-drawer-enter-active,
.record-detail-drawer-leave-active {
  transition:
    opacity 0.18s ease,
    transform 0.18s ease;
}

.record-detail-drawer-enter-from,
.record-detail-drawer-leave-to {
  opacity: 0;
  transform: translateX(16px);
}
</style>
