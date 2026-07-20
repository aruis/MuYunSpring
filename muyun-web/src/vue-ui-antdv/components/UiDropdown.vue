<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import UiIcon from './UiIcon.vue';
import { resolveDropdownPopupLayout } from '../dropdownPosition';
import type { UiDropdownItem } from '../types';

defineOptions({ name: 'UiDropdown' });

const props = withDefaults(
  defineProps<{
    items: UiDropdownItem[];
    selectedKey?: string;
    align?: 'start' | 'end';
    trigger?: 'click' | 'hover';
  }>(),
  {
    selectedKey: undefined,
    align: 'end',
    trigger: 'click',
  },
);

defineSlots<{
  default(props: { toggle: () => void }): unknown;
}>();

const emit = defineEmits<{
  select: [key: string];
}>();

const open = ref(false);
const root = ref<HTMLElement>();
const popup = ref<HTMLElement>();
const popupPosition = ref({ top: 0, left: 0, minWidth: 112, maxWidth: 0, maxHeight: 0 });
let closeTimer: ReturnType<typeof setTimeout> | undefined;

const popupStyle = computed(() => ({
  top: `${popupPosition.value.top}px`,
  left: `${popupPosition.value.left}px`,
  minWidth: `${popupPosition.value.minWidth}px`,
  maxWidth: popupPosition.value.maxWidth > 0 ? `${popupPosition.value.maxWidth}px` : undefined,
  maxHeight: popupPosition.value.maxHeight > 0 ? `${popupPosition.value.maxHeight}px` : undefined,
}));

watch(open, async (visible) => {
  if (!visible) {
    return;
  }
  await nextTick();
  updatePopupPosition();
});

function toggle() {
  clearCloseTimer();
  open.value = !open.value;
}

function openDropdown() {
  clearCloseTimer();
  open.value = true;
}

function closeDropdown() {
  clearCloseTimer();
  open.value = false;
}

function scheduleCloseDropdown() {
  clearCloseTimer();
  closeTimer = setTimeout(() => {
    closeTimer = undefined;
    open.value = false;
  }, 120);
}

function clearCloseTimer() {
  if (!closeTimer) {
    return;
  }
  clearTimeout(closeTimer);
  closeTimer = undefined;
}

function handleTriggerClick() {
  if (props.trigger === 'click') {
    toggle();
    return;
  }
  openDropdown();
}

function handleMouseEnter() {
  if (props.trigger === 'hover') {
    openDropdown();
  }
}

function handleMouseLeave() {
  if (props.trigger === 'hover') {
    scheduleCloseDropdown();
  }
}

function select(item: UiDropdownItem) {
  if (item.disabled) {
    return;
  }
  emit('select', item.key);
  closeDropdown();
}

function handleDocumentClick(event: MouseEvent) {
  const target = event.target as Node;
  if (!root.value?.contains(target) && !popup.value?.contains(target)) {
    closeDropdown();
  }
}

function handleDocumentKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    closeDropdown();
  }
}

onMounted(() => {
  document.addEventListener('click', handleDocumentClick);
  document.addEventListener('keydown', handleDocumentKeydown);
  window.addEventListener('resize', updatePopupPosition);
  window.addEventListener('scroll', updatePopupPosition, true);
});

onBeforeUnmount(() => {
  clearCloseTimer();
  document.removeEventListener('click', handleDocumentClick);
  document.removeEventListener('keydown', handleDocumentKeydown);
  window.removeEventListener('resize', updatePopupPosition);
  window.removeEventListener('scroll', updatePopupPosition, true);
});

function updatePopupPosition() {
  if (!open.value || !root.value) {
    return;
  }
  const rect = root.value.getBoundingClientRect();
  const layout = resolveDropdownPopupLayout({
    trigger: rect,
    popupWidth: Math.max(popup.value?.offsetWidth ?? 0, popup.value?.scrollWidth ?? 0),
    popupHeight: popup.value?.scrollHeight ?? 0,
    viewportWidth: window.innerWidth,
    viewportHeight: window.innerHeight,
    align: props.align,
  });
  popupPosition.value = layout;
}
</script>

<template>
  <div ref="root" class="ui-dropdown" @mouseenter="handleMouseEnter" @mouseleave="handleMouseLeave">
    <span class="ui-dropdown-trigger" @click.stop="handleTriggerClick">
      <slot :toggle="toggle" />
    </span>
  </div>
  <Teleport to="body">
    <div
      v-if="open"
      ref="popup"
      class="ui-dropdown-popup"
      role="menu"
      :style="popupStyle"
      @mouseenter="handleMouseEnter"
      @mouseleave="handleMouseLeave"
    >
      <button
        v-for="item in items"
        :key="item.key"
        class="ui-dropdown-item"
        :class="{ danger: item.danger, selected: item.key === selectedKey }"
        :disabled="item.disabled"
        type="button"
        role="menuitem"
        @click="select(item)"
      >
        <span class="ui-dropdown-item-title">{{ item.title }}</span>
        <UiIcon v-if="item.key === selectedKey" name="check" class="ui-dropdown-item-check" />
      </button>
    </div>
  </Teleport>
</template>

<style scoped>
.ui-dropdown {
  position: relative;
  display: inline-flex;
}

.ui-dropdown-trigger {
  display: inline-flex;
}

.ui-dropdown-popup {
  position: fixed;
  z-index: 3000;
  padding: 5px;
  border: 1px solid var(--muyun-border-subtle);
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 8px 18px rgb(15 23 42 / 10%);
  box-sizing: border-box;
  overflow-x: hidden;
  overflow-y: auto;
}

.ui-dropdown-item {
  display: flex;
  width: 100%;
  min-height: 30px;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 5px 8px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: var(--muyun-text-body);
  font-size: 13px;
  line-height: 18px;
  text-align: left;
  cursor: pointer;
}

.ui-dropdown-item:hover:not(:disabled) {
  background: var(--muyun-hover-subtle);
  color: var(--muyun-text);
}

.ui-dropdown-item.selected {
  background: var(--muyun-selected);
  color: var(--muyun-text);
}

.ui-dropdown-item-title {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ui-dropdown-item-check {
  flex: 0 0 auto;
  color: var(--muyun-text-muted);
  font-size: 11px;
}

.ui-dropdown-item:disabled {
  color: var(--muyun-text-muted);
  cursor: not-allowed;
  opacity: 0.7;
}

.ui-dropdown-item.danger {
  color: #c2410c;
}
</style>
