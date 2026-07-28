import { computed, ref, toValue, type MaybeRefOrGetter, type Ref } from 'vue';
import { canQueryRecycleBin, hasRecycleBinAbility, type ModuleContext } from '@muyun/web-core';

export type RecycleBinExplorerMode = 'normal' | 'recycleBin';

export interface RecycleBinExplorerModeOptions<TRecord> {
  context: MaybeRefOrGetter<ModuleContext<TRecord>>;
  listReloadKey: Ref<number>;
  searchKeyword?: Ref<string>;
  canChange?: () => boolean;
  resetSelection?: () => void;
}

/** Shared mode controller for standard and micro-list recycle-bin explorers. */
export function useRecycleBinExplorerMode<TRecord>(options: RecycleBinExplorerModeOptions<TRecord>) {
  const mode = ref<RecycleBinExplorerMode>('normal');
  const total = ref<number>();
  const recycleBinReloadKey = ref(0);
  const enabled = computed(() => hasRecycleBinAbility(toValue(options.context)));
  const canEnter = computed(() => canQueryRecycleBin(toValue(options.context)));
  const active = computed(() => mode.value === 'recycleBin');
  const buttonVisible = computed(() => active.value || (enabled.value && canEnter.value));
  const hasRecords = computed<boolean | undefined>(() =>
    total.value === undefined ? undefined : total.value > 0,
  );
  const reloadKey = computed(() => (active.value ? recycleBinReloadKey.value : options.listReloadKey.value));

  function enter() {
    if (!canEnter.value) return;
    switchMode('recycleBin');
  }

  function leave() {
    switchMode('normal');
  }

  function toggle() {
    if (active.value) leave();
    else enter();
  }

  function refresh() {
    if (active.value) recycleBinReloadKey.value += 1;
    else options.listReloadKey.value += 1;
  }

  function updateSummary(nextTotal: number | undefined) {
    total.value = nextTotal;
  }

  function switchMode(nextMode: RecycleBinExplorerMode) {
    if (mode.value === nextMode) return;
    if (options.canChange && !options.canChange()) return;
    options.resetSelection?.();
    if (nextMode === 'recycleBin' && options.searchKeyword) {
      options.searchKeyword.value = '';
    }
    mode.value = nextMode;
  }

  return {
    mode,
    total,
    enabled,
    canEnter,
    active,
    buttonVisible,
    hasRecords,
    reloadKey,
    enter,
    leave,
    toggle,
    refresh,
    updateSummary,
  };
}
