<script setup lang="ts">
import { computed } from 'vue';
import { UiActionButton, type UiSidePanelScope } from '@muyun/vue-ui-antdv';
import RecordDetailDrawer from './RecordDetailDrawer.vue';
import RecordExternalChangeNotice from './RecordExternalChangeNotice.vue';
import type { DrawerPromotion } from './drawerPromotion';

defineOptions({ name: 'RecordModeDrawer' });

const props = withDefaults(
  defineProps<{
    open: boolean;
    title: string;
    /** Secondary business identity rendered by the platform detail header. */
    subtitle?: string;
    width?: number | string;
    scope?: UiSidePanelScope;
    mode: string;
    viewMode?: string;
    formModes?: string[];
    loading?: boolean;
    loadFailed?: boolean;
    closeOnOutside?: boolean;
    closeTitle?: string;
    promotion?: DrawerPromotion;
    errorTitle?: string;
    errorMessage?: string;
    retryTitle?: string;
    externallyChanged?: boolean;
    externalChangeTitle?: string;
    externalChangeMessage?: string;
    externalChangeReloadTitle?: string;
    externalChangeDismissTitle?: string;
  }>(),
  {
    subtitle: undefined,
    width: 520,
    scope: 'tab',
    viewMode: 'view',
    formModes: () => ['edit', 'create'],
    loading: false,
    loadFailed: false,
    closeOnOutside: undefined,
    closeTitle: '关闭',
    promotion: undefined,
    errorTitle: '详情加载失败',
    errorMessage: '无法加载详情，请重试',
    retryTitle: '重试',
    externallyChanged: false,
    externalChangeTitle: undefined,
    externalChangeMessage: undefined,
    externalChangeReloadTitle: undefined,
    externalChangeDismissTitle: undefined,
  },
);

defineSlots<{
  status(): unknown;
  loading(): unknown;
  error(): unknown;
  externalChangeNotice(): unknown;
  view(): unknown;
  form(): unknown;
  default(): unknown;
  operation(): unknown;
}>();

const emit = defineEmits<{
  close: [];
  retry: [];
  reloadExternalChange: [];
  dismissExternalChange: [];
}>();

const viewModeActive = computed(() => props.mode === props.viewMode);
const formModeActive = computed(() => props.formModes.includes(props.mode));
const actualCloseOnOutside = computed(() => props.closeOnOutside ?? viewModeActive.value);
</script>

<template>
  <RecordDetailDrawer
    :open="open"
    :title="title"
    :subtitle="subtitle"
    :width="width"
    :scope="scope"
    :close-on-outside="actualCloseOnOutside"
    :close-title="closeTitle"
    :promotion="promotion"
    @close="emit('close')"
  >
    <template #status>
      <slot name="status" />
    </template>
    <template v-if="$slots.operation" #operation>
      <slot name="operation" />
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
          <UiActionButton emphasis="primary" icon-name="reload" @click="emit('retry')">
            {{ retryTitle }}
          </UiActionButton>
        </div>
      </slot>
    </template>
    <template v-else-if="viewModeActive">
      <slot name="view" />
    </template>
    <template v-else-if="formModeActive">
      <slot v-if="externallyChanged" name="externalChangeNotice">
        <RecordExternalChangeNotice
          :title="externalChangeTitle"
          :message="externalChangeMessage"
          :reload-title="externalChangeReloadTitle"
          :dismiss-title="externalChangeDismissTitle"
          @reload="emit('reloadExternalChange')"
          @dismiss="emit('dismissExternalChange')"
        />
      </slot>
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
