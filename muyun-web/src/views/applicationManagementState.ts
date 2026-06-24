import { computed, ref } from 'vue';
import type { Application } from '@muyun/web-contracts';
import { normalizeError, type ModuleContext } from '@muyun/web-core';
import type { UiConfirmOptions } from '@muyun/vue-ui-antdv';

type CardMode = 'view' | 'edit' | 'create';
type ConfirmAction = (options: UiConfirmOptions) => Promise<boolean>;

export function createApplicationManagementState(
  applicationContext: ModuleContext<Application>,
  confirmAction: ConfirmAction,
) {
  const selected = ref<Application>();
  const draft = ref<Application>(emptyDraft());
  const mode = ref<CardMode>('view');
  const reloadKey = ref(0);
  const saving = ref(false);
  const actionError = ref<string>();
  const actionMessage = ref<string>();

  const cardTitle = computed(() => {
    if (mode.value === 'create') {
      return '新建应用';
    }
    return selected.value?.title ?? selected.value?.alias ?? selected.value?.id ?? '应用详情';
  });
  const readonly = computed(() => mode.value === 'view');
  const aliasReadonly = computed(() => mode.value !== 'create');
  const canCreate = computed(() => applicationContext.can('create') === true);
  const canUpdate = computed(() => Boolean(selected.value?.id) && applicationContext.can('update') === true);
  const canDelete = computed(() => Boolean(selected.value?.id) && applicationContext.can('delete') === true);
  const canEnable = computed(() => {
    const actionCode = selected.value?.enabled === false ? 'enable' : 'disable';
    return Boolean(selected.value?.id) && applicationContext.can(actionCode) === true;
  });
  const canMutate = computed(() => canUpdate.value || canDelete.value || canEnable.value);

  function handleListLoaded(records: Application[]) {
    if (selected.value?.id && records.some((item) => item.id === selected.value?.id)) {
      return;
    }
    const first = records[0];
    selected.value = first;
    draft.value = first ? copyRecord(first) : emptyDraft();
    mode.value = first || !canCreate.value ? 'view' : 'create';
  }

  function handleSelect(application: Application) {
    selected.value = application;
    draft.value = copyRecord(application);
    mode.value = 'view';
    clearFeedback();
  }

  function startCreate() {
    if (!canCreate.value) {
      actionError.value = '当前用户无权新建应用';
      return;
    }
    selected.value = undefined;
    draft.value = emptyDraft();
    mode.value = 'create';
    clearFeedback();
  }

  function startEdit() {
    if (!selected.value) {
      return;
    }
    draft.value = copyRecord(selected.value);
    mode.value = 'edit';
    clearFeedback();
  }

  function cancelEdit() {
    draft.value = selected.value ? copyRecord(selected.value) : emptyDraft();
    mode.value = selected.value ? 'view' : 'create';
    clearFeedback();
  }

  async function save() {
    if (mode.value === 'view') {
      return;
    }
    if (mode.value === 'create' ? !canCreate.value : !canUpdate.value) {
      actionError.value = '当前用户无权保存应用';
      return;
    }
    clearFeedback();
    const validDraft = normalizedDraft(draft.value, selected.value, mode.value);
    if (!validDraft.id || !validDraft.title) {
      actionError.value = '应用 alias 和应用名称不能为空';
      return;
    }

    saving.value = true;
    try {
      await applicationContext.runtime.ready;
      const crud = applicationContext.abilities.crud();
      const saved =
        mode.value === 'create'
          ? await crud.insert(validDraft)
          : await crud.update(requiredId(validDraft), validDraft);
      selected.value = saved;
      draft.value = copyRecord(saved);
      mode.value = 'view';
      actionMessage.value = '已保存';
      reloadKey.value += 1;
    } catch (cause) {
      actionError.value = normalizeError(cause).message;
    } finally {
      saving.value = false;
    }
  }

  async function toggleEnabled() {
    if (!selected.value?.id) {
      return;
    }
    if (!canEnable.value) {
      actionError.value = '当前用户无权变更应用启停状态';
      return;
    }
    clearFeedback();
    saving.value = true;
    try {
      await applicationContext.runtime.ready;
      const crud = applicationContext.abilities.crud();
      const enable = applicationContext.abilities.enable();
      if (selected.value.enabled === false) {
        await enable.enable(selected.value.id);
      } else {
        await enable.disable(selected.value.id);
      }
      const refreshed = await crud.view(selected.value.id);
      selected.value = refreshed;
      draft.value = copyRecord(refreshed);
      actionMessage.value = refreshed.enabled === false ? '已停用' : '已启用';
      reloadKey.value += 1;
    } catch (cause) {
      actionError.value = normalizeError(cause).message;
    } finally {
      saving.value = false;
    }
  }

  async function removeSelected() {
    if (!selected.value?.id) {
      return;
    }
    if (!canDelete.value) {
      actionError.value = '当前用户无权删除应用';
      return;
    }
    const confirmed = await confirmAction({
      title: '删除应用',
      content: `确认删除应用「${selected.value.title ?? selected.value.alias ?? selected.value.id}」？`,
      okText: '删除',
      danger: true,
    });
    if (!confirmed) {
      return;
    }
    clearFeedback();
    saving.value = true;
    try {
      await applicationContext.runtime.ready;
      const crud = applicationContext.abilities.crud();
      await crud.delete(selected.value.id);
      selected.value = undefined;
      draft.value = emptyDraft();
      mode.value = canCreate.value ? 'create' : 'view';
      actionMessage.value = '已删除';
      reloadKey.value += 1;
    } catch (cause) {
      actionError.value = normalizeError(cause).message;
    } finally {
      saving.value = false;
    }
  }

  function clearFeedback() {
    actionError.value = undefined;
    actionMessage.value = undefined;
  }

  return {
    selected,
    draft,
    mode,
    reloadKey,
    saving,
    actionError,
    actionMessage,
    cardTitle,
    readonly,
    aliasReadonly,
    canCreate,
    canUpdate,
    canDelete,
    canEnable,
    canMutate,
    handleListLoaded,
    handleSelect,
    startCreate,
    startEdit,
    cancelEdit,
    save,
    toggleEnabled,
    removeSelected,
  };
}

function copyRecord(record: Application): Application {
  const alias = applicationAliasOf(record);
  return { ...record, alias };
}

function emptyDraft(): Application {
  return {
    alias: '',
    title: '',
    enabled: true,
  };
}

function normalizedDraft(
  record: Application,
  selected: Application | undefined,
  mode: CardMode,
): Application {
  const alias = mode === 'create' ? record.alias?.trim() : applicationAliasOf(selected ?? record);
  return {
    ...record,
    id: alias,
    alias,
    title: record.title?.trim(),
  };
}

function applicationAliasOf(record: Application) {
  return record.alias?.trim() || record.id?.trim();
}

function requiredId(record: Application) {
  if (!record.id) {
    throw new Error('应用 ID 不能为空');
  }
  return record.id;
}
