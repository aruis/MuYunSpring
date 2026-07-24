import { computed, ref } from 'vue';
import type { ModuleEntryType, PlatformModule } from '@muyun/web-contracts';
import type { ModuleContext } from '@muyun/web-core';
import type { UiConfirmOptions } from '@muyun/vue-ui-antdv';
import {
  createRecordEditorSessionState,
  executeStaticFormSave,
  executeStaticRecordAction,
} from '@muyun/platform-components';

export type ModuleManagementMode = 'view' | 'edit' | 'create-root' | 'create-child';
type ConfirmAction = (options: UiConfirmOptions) => Promise<boolean>;

export function createModuleManagementState(
  moduleContext: ModuleContext<PlatformModule>,
  applicationAlias: () => string | undefined,
  confirmAction: ConfirmAction,
) {
  const moduleReloadKey = ref(0);
  const selectedApplicationAlias = computed(applicationAlias);
  const modules = ref<PlatformModule[]>([]);
  const moduleEditor = createRecordEditorSessionState<PlatformModule, ModuleManagementMode>({
    viewMode: 'view',
    createMode: 'create-root',
    editMode: 'edit',
    emptyDraft: () => emptyModuleDraft(selectedApplicationAlias.value),
    copyRecord: copyModule,
  });
  const selectedModule = moduleEditor.selected;
  const draft = moduleEditor.draft;
  const mode = moduleEditor.mode;
  const saving = ref(false);

  const readonly = moduleEditor.readonly;
  const aliasReadonly = computed(() => mode.value === 'edit');
  const canCreate = computed(() => moduleContext.can('create') === true);
  const canUpdate = computed(() => moduleContext.can('update') === true);
  const canDelete = computed(() => moduleContext.can('delete') === true);
  const canToggle = computed(() => {
    const record = selectedModule.value;
    return Boolean(record?.id && moduleContext.can(record.enabled === false ? 'enable' : 'disable') === true);
  });
  const cardTitle = computed(() => {
    if (mode.value === 'create-root') return '新建模块';
    if (mode.value === 'create-child') return `新建下级：${moduleTitleOf(selectedModule.value)}`;
    return moduleTitleOf(selectedModule.value);
  });

  function handleModulesLoaded(records: PlatformModule[]) {
    modules.value = records;
    const matched = selectedModule.value?.id
      ? records.find((item) => item.id === selectedModule.value?.id)
      : undefined;
    if (mode.value !== 'view') {
      if (matched) moduleEditor.replaceSelected(matched);
      return;
    }
    if (matched ?? records[0]) {
      moduleEditor.select(matched ?? records[0]!);
    } else {
      moduleEditor.clearSelection();
    }
  }

  function selectModule(record: PlatformModule) {
    moduleEditor.select(record);
  }

  function startCreateRoot() {
    if (!selectedApplicationAlias.value || !canCreate.value) return;
    moduleEditor.startCreate({ mode: 'create-root', preserveSelection: true });
  }

  function startCreateChild(parent?: PlatformModule) {
    const current = parent ?? selectedModule.value;
    if (!selectedApplicationAlias.value || !current?.id || !canCreate.value) return;
    moduleEditor.startCreate({
      mode: 'create-child',
      selectedRecord: current,
      draft: () => ({ ...emptyModuleDraft(selectedApplicationAlias.value), parentId: current.id }),
    });
  }

  function startEdit() {
    if (selectedModule.value && canUpdate.value && selectedModule.value.systemManaged !== true) {
      moduleEditor.startEdit();
    }
  }

  function cancelEdit() {
    moduleEditor.cancel();
  }

  async function save() {
    await executeStaticFormSave<PlatformModule>({
      loading: saving,
      mode: mode.value === 'edit' ? 'edit' : 'create',
      source: 'module-management',
      validateContext: () => (selectedApplicationAlias.value ? undefined : '请先选择应用'),
      canSave: () => (mode.value.startsWith('create') ? canCreate.value : canUpdate.value),
      deniedMessage: '当前用户无权保存模块',
      createRecord: () => normalizeModuleDraft(draft.value, selectedApplicationAlias.value),
      validateRecord: moduleValidationMessage,
      save: async (record, saveMode) => {
        await moduleContext.runtime.ready;
        const crud = moduleContext.abilities.crud();
        return saveMode === 'edit' && record.id ? crud.update(record.id, record) : crud.insert(record);
      },
      onSaved: ({ record }) => {
        moduleEditor.select(record);
        mode.value = 'view';
        moduleReloadKey.value += 1;
      },
    });
  }

  async function toggleEnabled() {
    await executeStaticRecordAction({
      loading: saving,
      source: 'module-management',
      record: () => (selectedModule.value?.id ? selectedModule.value : undefined),
      canExecute: () => canToggle.value,
      deniedMessage: '当前用户无权变更模块启停状态',
      execute: async (record) => {
        await moduleContext.runtime.ready;
        const enable = moduleContext.abilities.enable();
        return record.enabled === false
          ? enable.enable(record.id!, { version: record.version! })
          : enable.disable(record.id!, { version: record.version! });
      },
      onExecuted: async (_, record) => {
        moduleEditor.select(await moduleContext.abilities.crud().view(record.id!));
        moduleReloadKey.value += 1;
      },
    });
  }

  async function removeSelected() {
    await executeStaticRecordAction({
      loading: saving,
      source: 'module-management',
      record: () => (selectedModule.value?.id ? selectedModule.value : undefined),
      canExecute: () => canDelete.value && selectedModule.value?.systemManaged !== true,
      deniedMessage: '当前用户无权删除模块',
      confirm: (record) =>
        confirmAction({
          title: '删除模块',
          content: `确认删除模块「${moduleTitleOf(record)}」？`,
          okText: '删除',
          danger: true,
        }),
      execute: (record) => moduleContext.abilities.crud().delete(record.id!, { version: record.version! }),
      onExecuted: () => {
        moduleEditor.clearSelection();
        mode.value = 'view';
        moduleReloadKey.value += 1;
      },
    });
  }

  function resetForApplication() {
    modules.value = [];
    moduleEditor.clearSelection();
    mode.value = 'view';
    moduleReloadKey.value += 1;
  }

  return {
    moduleReloadKey,
    selectedModule,
    draft,
    mode,
    saving,
    readonly,
    aliasReadonly,
    canCreate,
    canToggle,
    cardTitle,
    handleModulesLoaded,
    selectModule,
    startCreateRoot,
    startCreateChild,
    startEdit,
    cancelEdit,
    save,
    toggleEnabled,
    removeSelected,
    resetForApplication,
  };
}

export function moduleTitleOf(record: PlatformModule | undefined) {
  return record?.title ?? record?.alias ?? record?.id ?? '模块详情';
}

export function emptyModuleDraft(applicationAlias?: string): PlatformModule {
  return {
    applicationAlias,
    parentId: undefined,
    alias: '',
    title: '',
    moduleKind: 'static',
    entryType: 'module',
    enabled: true,
  };
}

export function copyModule(record: PlatformModule): PlatformModule {
  return { ...record, alias: record.alias ?? record.id };
}

export function normalizeModuleDraft(
  record: PlatformModule,
  applicationAlias: string | undefined,
): PlatformModule {
  const entryType = (record.entryType ?? 'module') as ModuleEntryType;
  return {
    ...record,
    id: record.id ?? record.alias?.trim(),
    alias: record.alias?.trim(),
    applicationAlias: applicationAlias ?? record.applicationAlias?.trim(),
    parentId: normalizeBlank(record.parentId),
    title: record.title?.trim(),
    entryType,
    entryRoute: entryType === 'route' ? normalizeBlank(record.entryRoute) : undefined,
    entryExternalUrl: entryType === 'link' ? normalizeBlank(record.entryExternalUrl) : undefined,
  };
}

export function isValidModule(record: PlatformModule) {
  return moduleValidationMessage(record) === undefined;
}

export function moduleValidationMessage(record: PlatformModule) {
  if (!record.applicationAlias || !(record.alias ?? record.id) || !record.title) {
    return '模块 alias 和模块名称不能为空';
  }
  if (record.entryType === 'route' && !record.entryRoute) {
    return '请输入内部路由';
  }
  if (record.entryType === 'link' && !record.entryExternalUrl) {
    return '请输入外部链接';
  }
  return undefined;
}

function normalizeBlank(value: string | undefined) {
  const normalized = value?.trim();
  return normalized ? normalized : undefined;
}
