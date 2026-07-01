import { computed, ref } from 'vue';
import type {
  CurrentUser,
  MenuOpenMode,
  MenuPageMode,
  MenuRecord,
  MenuScheme,
  MenuType,
} from '@muyun/web-contracts';
import type { ModuleContext } from '@muyun/web-core';
import type { UiConfirmOptions } from '@muyun/vue-ui-antdv';
import { executeStaticFormSave, executeStaticRecordAction } from '@muyun/platform-components';

export type MenuSchemeMode = 'view' | 'edit' | 'create';
export type MenuNodeMode = 'view' | 'edit' | 'create-root' | 'create-child';
type ConfirmAction = (options: UiConfirmOptions) => Promise<boolean>;
type MenuContextProvider = () => ModuleContext<MenuRecord>;
type CurrentUserProvider = () => CurrentUser | undefined;

export interface MenuManagementStateOptions {
  currentUser?: CurrentUserProvider;
}

export function createMenuManagementState(
  schemeContext: ModuleContext<MenuScheme>,
  menuContextProvider: MenuContextProvider,
  confirmAction: ConfirmAction,
  options: MenuManagementStateOptions = {},
) {
  const schemeReloadKey = ref(0);
  const menuReloadKey = ref(0);
  const schemes = ref<MenuScheme[]>([]);
  const selectedScheme = ref<MenuScheme>();
  const selectedMenu = ref<MenuRecord>();
  const schemeDraft = ref<MenuScheme>(emptySchemeDraft(currentUser()));
  const menuDraft = ref<MenuRecord>(emptyMenuDraft());
  const schemeMode = ref<MenuSchemeMode>('view');
  const menuMode = ref<MenuNodeMode>('view');
  const savingScheme = ref(false);
  const savingMenu = ref(false);

  const selectedSchemeId = computed(() => selectedScheme.value?.id);
  const selectedSchemeTitle = computed(() => schemeTitleOf(selectedScheme.value));
  const selectedMenuTitle = computed(() => menuTitleOf(selectedMenu.value));
  const schemeReadonly = computed(() => schemeMode.value === 'view');
  const menuReadonly = computed(() => menuMode.value === 'view');
  const canCreateScheme = computed(() => schemeContext.can('create') === true);
  const canUpdateScheme = computed(
    () => Boolean(selectedScheme.value?.id) && schemeContext.can('update') === true,
  );
  const canDeleteScheme = computed(
    () => Boolean(selectedScheme.value?.id) && schemeContext.can('delete') === true,
  );
  const canToggleScheme = computed(() => {
    if (!selectedScheme.value?.id) {
      return false;
    }
    return schemeContext.can(selectedScheme.value.enabled === false ? 'enable' : 'disable') === true;
  });
  const canCreateMenu = computed(
    () => Boolean(selectedSchemeId.value) && menuContext().can('create') === true,
  );
  const canUpdateMenu = computed(
    () => Boolean(selectedMenu.value?.id) && menuContext().can('update') === true,
  );
  const canDeleteMenu = computed(
    () => Boolean(selectedMenu.value?.id) && menuContext().can('delete') === true,
  );
  const canToggleMenu = computed(() => {
    if (!selectedMenu.value?.id) {
      return false;
    }
    return menuContext().can(selectedMenu.value.enabled === false ? 'enable' : 'disable') === true;
  });
  const schemeCardTitle = computed(() => {
    if (schemeMode.value === 'create') {
      return '新建菜单方案';
    }
    return selectedScheme.value ? schemeTitleOf(selectedScheme.value) : '菜单方案';
  });
  const menuCardTitle = computed(() => {
    if (menuMode.value === 'create-root') {
      return '新建根菜单';
    }
    if (menuMode.value === 'create-child') {
      return `新建下级：${selectedMenuTitle.value}`;
    }
    return selectedMenu.value ? selectedMenuTitle.value : '菜单详情';
  });

  function handleSchemesLoaded(records: MenuScheme[]) {
    schemes.value = records;
    if (!selectedScheme.value?.id || !records.some((item) => item.id === selectedScheme.value?.id)) {
      selectedScheme.value = records[0];
      resetMenusForScheme();
      return;
    }
    selectedScheme.value = records.find((item) => item.id === selectedScheme.value?.id);
    if (schemeMode.value === 'view') {
      schemeDraft.value = selectedScheme.value ? copyScheme(selectedScheme.value) : emptySchemeDraft();
    }
  }

  function selectScheme(record: MenuScheme) {
    if (selectedScheme.value?.id === record.id) {
      return;
    }
    selectedScheme.value = record;
    schemeDraft.value = copyScheme(record);
    schemeMode.value = 'view';
    resetMenusForScheme();
  }

  function handleMenusLoaded(records: MenuRecord[]) {
    if (!selectedMenu.value?.id || !records.some((item) => item.id === selectedMenu.value?.id)) {
      selectedMenu.value = records[0];
    } else {
      selectedMenu.value = records.find((item) => item.id === selectedMenu.value?.id);
    }
    menuDraft.value = selectedMenu.value
      ? copyMenu(selectedMenu.value)
      : emptyMenuDraft(selectedSchemeId.value);
    menuMode.value = 'view';
  }

  function selectMenu(record: MenuRecord) {
    selectedMenu.value = record;
    menuDraft.value = copyMenu(record);
    menuMode.value = 'view';
  }

  function startCreateScheme() {
    if (!canCreateScheme.value) {
      return;
    }
    selectedScheme.value = undefined;
    schemeDraft.value = emptySchemeDraft(currentUser());
    schemeMode.value = 'create';
  }

  function startEditScheme() {
    if (!selectedScheme.value || !canUpdateScheme.value) {
      return;
    }
    schemeDraft.value = copyScheme(selectedScheme.value);
    schemeMode.value = 'edit';
  }

  function cancelSchemeEdit() {
    schemeDraft.value = selectedScheme.value
      ? copyScheme(selectedScheme.value)
      : emptySchemeDraft(currentUser());
    schemeMode.value = selectedScheme.value ? 'view' : 'create';
  }

  function startCreateRootMenu() {
    if (!selectedSchemeId.value || !canCreateMenu.value) {
      return;
    }
    selectedMenu.value = undefined;
    menuDraft.value = emptyMenuDraft(selectedSchemeId.value);
    menuMode.value = 'create-root';
  }

  function startCreateChildMenu(parent?: MenuRecord) {
    const current = parent ?? selectedMenu.value;
    if (!selectedSchemeId.value || !current?.id || !canCreateMenu.value) {
      return;
    }
    selectedMenu.value = current;
    menuDraft.value = {
      ...emptyMenuDraft(selectedSchemeId.value),
      parentId: current.id,
    };
    menuMode.value = 'create-child';
  }

  function startEditMenu() {
    if (!selectedMenu.value || !canUpdateMenu.value) {
      return;
    }
    menuDraft.value = copyMenu(selectedMenu.value);
    menuMode.value = 'edit';
  }

  function cancelMenuEdit() {
    menuDraft.value = selectedMenu.value
      ? copyMenu(selectedMenu.value)
      : emptyMenuDraft(selectedSchemeId.value);
    menuMode.value = 'view';
  }

  async function saveScheme() {
    await executeStaticFormSave<MenuScheme>({
      loading: savingScheme,
      mode: schemeMode.value === 'create' ? 'create' : 'edit',
      source: 'menu-scheme-management',
      validateContext: () => (schemeMode.value === 'view' ? '请选择编辑或新建菜单方案' : undefined),
      canSave: () => (schemeMode.value === 'create' ? canCreateScheme.value : canUpdateScheme.value),
      deniedMessage: '当前用户无权保存菜单方案',
      createRecord: () => normalizeSchemeDraft(schemeDraft.value, selectedScheme.value, schemeMode.value),
      validateRecord: (record) => (isValidScheme(record) ? undefined : '方案 alias、名称和 scope 不能为空'),
      save: async (record, saveMode) => {
        await schemeContext.runtime.ready;
        const crud = schemeContext.abilities.crud();
        return saveMode === 'edit' && record.id ? crud.update(record.id, record) : crud.insert(record);
      },
      onSaved: ({ record }) => {
        selectedScheme.value = record;
        schemeDraft.value = copyScheme(record);
        schemeMode.value = 'view';
        schemeReloadKey.value += 1;
      },
    });
  }

  async function saveMenu() {
    await executeStaticFormSave<MenuRecord>({
      loading: savingMenu,
      mode: menuMode.value === 'edit' ? 'edit' : 'create',
      source: 'menu-management',
      validateContext: () => {
        if (menuMode.value === 'view') {
          return '请选择编辑或新建菜单';
        }
        return selectedSchemeId.value ? undefined : '请先选择菜单方案';
      },
      canSave: () => (menuMode.value.startsWith('create') ? canCreateMenu.value : canUpdateMenu.value),
      deniedMessage: '当前用户无权保存菜单',
      createRecord: () => normalizeMenuDraft(menuDraft.value, selectedSchemeId.value),
      validateRecord: validateMenu,
      save: async (record, saveMode) => {
        const context = menuContext();
        await context.runtime.ready;
        const crud = context.abilities.crud();
        return saveMode === 'edit' && record.id ? crud.update(record.id, record) : crud.insert(record);
      },
      onSaved: ({ record }) => {
        selectedMenu.value = record;
        menuDraft.value = copyMenu(record);
        menuMode.value = 'view';
        menuReloadKey.value += 1;
      },
    });
  }

  async function toggleSchemeEnabled() {
    await executeStaticRecordAction({
      loading: savingScheme,
      source: 'menu-scheme-management',
      record: () => (selectedScheme.value?.id ? selectedScheme.value : undefined),
      canExecute: () => canToggleScheme.value,
      deniedMessage: '当前用户无权变更菜单方案启停状态',
      execute: async (scheme) => {
        await schemeContext.runtime.ready;
        const enable = schemeContext.abilities.enable();
        return scheme.enabled === false ? enable.enable(scheme.id!) : enable.disable(scheme.id!);
      },
      onExecuted: async (_, scheme) => {
        const refreshed = await schemeContext.abilities.crud().view(scheme.id!);
        selectedScheme.value = refreshed;
        schemeDraft.value = copyScheme(refreshed);
        schemeReloadKey.value += 1;
      },
    });
  }

  async function toggleMenuEnabled() {
    await executeStaticRecordAction({
      loading: savingMenu,
      source: 'menu-management',
      record: () => (selectedMenu.value?.id ? selectedMenu.value : undefined),
      canExecute: () => canToggleMenu.value,
      deniedMessage: '当前用户无权变更菜单启停状态',
      execute: async (menu) => {
        const context = menuContext();
        await context.runtime.ready;
        const enable = context.abilities.enable();
        return menu.enabled === false ? enable.enable(menu.id) : enable.disable(menu.id);
      },
      onExecuted: async (_, menu) => {
        const refreshed = await menuContext().abilities.crud().view(menu.id);
        selectedMenu.value = refreshed;
        menuDraft.value = copyMenu(refreshed);
        menuReloadKey.value += 1;
      },
    });
  }

  async function removeSelectedScheme() {
    await executeStaticRecordAction({
      loading: savingScheme,
      source: 'menu-scheme-management',
      record: () => (selectedScheme.value?.id ? selectedScheme.value : undefined),
      canExecute: () => canDeleteScheme.value,
      deniedMessage: '当前用户无权删除菜单方案',
      confirm: (scheme) =>
        confirmAction({
          title: '删除菜单方案',
          content: `确认删除菜单方案「${schemeTitleOf(scheme)}」？`,
          okText: '删除',
          danger: true,
        }),
      execute: (scheme) => schemeContext.abilities.crud().delete(scheme.id!),
      onExecuted: () => {
        selectedScheme.value = undefined;
        schemeDraft.value = emptySchemeDraft(currentUser());
        schemeMode.value = canCreateScheme.value ? 'create' : 'view';
        resetMenusForScheme();
        schemeReloadKey.value += 1;
      },
    });
  }

  async function removeSelectedMenu() {
    await executeStaticRecordAction({
      loading: savingMenu,
      source: 'menu-management',
      record: () => (selectedMenu.value?.id ? selectedMenu.value : undefined),
      canExecute: () => canDeleteMenu.value,
      deniedMessage: '当前用户无权删除菜单',
      confirm: (menu) =>
        confirmAction({
          title: '删除菜单',
          content: `确认删除菜单「${menuTitleOf(menu)}」？`,
          okText: '删除',
          danger: true,
        }),
      execute: (menu) => menuContext().abilities.crud().delete(menu.id),
      onExecuted: () => {
        selectedMenu.value = undefined;
        menuDraft.value = emptyMenuDraft(selectedSchemeId.value);
        menuMode.value = 'view';
        menuReloadKey.value += 1;
      },
    });
  }

  function resetMenusForScheme() {
    selectedMenu.value = undefined;
    menuDraft.value = emptyMenuDraft(selectedSchemeId.value);
    menuMode.value = 'view';
    menuReloadKey.value += 1;
  }

  function menuContext() {
    return menuContextProvider();
  }

  function currentUser() {
    return options.currentUser?.();
  }

  return {
    schemeReloadKey,
    menuReloadKey,
    schemes,
    selectedScheme,
    selectedMenu,
    schemeDraft,
    menuDraft,
    schemeMode,
    menuMode,
    savingScheme,
    savingMenu,
    selectedSchemeId,
    selectedSchemeTitle,
    selectedMenuTitle,
    schemeReadonly,
    menuReadonly,
    canCreateScheme,
    canUpdateScheme,
    canDeleteScheme,
    canToggleScheme,
    canCreateMenu,
    canUpdateMenu,
    canDeleteMenu,
    canToggleMenu,
    schemeCardTitle,
    menuCardTitle,
    handleSchemesLoaded,
    selectScheme,
    handleMenusLoaded,
    selectMenu,
    startCreateScheme,
    startEditScheme,
    cancelSchemeEdit,
    startCreateRootMenu,
    startCreateChildMenu,
    startEditMenu,
    cancelMenuEdit,
    saveScheme,
    saveMenu,
    toggleSchemeEnabled,
    toggleMenuEnabled,
    removeSelectedScheme,
    removeSelectedMenu,
  };
}

export function schemeTitleOf(record: MenuScheme | undefined) {
  return record?.title ?? record?.alias ?? record?.id ?? '菜单方案';
}

export function menuTitleOf(record: MenuRecord | undefined) {
  return (
    record?.title ?? record?.moduleAlias ?? record?.route ?? record?.externalUrl ?? record?.id ?? '菜单详情'
  );
}

export function emptySchemeDraft(currentUser?: CurrentUser): MenuScheme {
  return {
    alias: '',
    title: '',
    ...defaultMenuSchemeScope(currentUser),
    enabled: true,
  };
}

export function defaultMenuSchemeScope(
  currentUser?: CurrentUser,
): Pick<MenuScheme, 'tenantId' | 'scopeType' | 'scopeId'> {
  if (currentUser?.system) {
    return {
      tenantId: undefined,
      scopeType: 'system',
      scopeId: 'system',
    };
  }
  if (currentUser?.tenantId) {
    return {
      tenantId: currentUser.tenantId,
      scopeType: 'tenant',
      scopeId: currentUser.tenantId,
    };
  }
  return {
    scopeType: 'tenant',
  };
}

export function emptyMenuDraft(schemeId?: string): MenuRecord {
  return {
    id: '',
    schemeId: schemeId ?? '',
    title: '',
    menuType: 'group',
    openMode: undefined,
    enabled: true,
  };
}

export function copyScheme(record: MenuScheme): MenuScheme {
  return { ...record };
}

export function copyMenu(record: MenuRecord): MenuRecord {
  return { ...record };
}

export function normalizeSchemeDraft(
  record: MenuScheme,
  selected: MenuScheme | undefined,
  mode: MenuSchemeMode,
): MenuScheme {
  const alias = mode === 'create' ? record.alias?.trim() : (selected?.alias ?? record.alias?.trim());
  return {
    ...record,
    id: mode === 'create' ? alias : record.id,
    alias,
    title: record.title?.trim(),
    scopeType: normalizeScopeType(record.scopeType),
    scopeId: normalizeBlank(record.scopeId),
  };
}

export function normalizeMenuDraft(record: MenuRecord, schemeId: string | undefined): MenuRecord {
  const menuType = normalizeMenuType(record.menuType);
  const normalized: MenuRecord = {
    ...record,
    id: record.id?.trim(),
    schemeId: record.schemeId || schemeId || '',
    parentId: normalizeBlank(record.parentId),
    title: record.title?.trim() ?? '',
    menuType,
    openMode: menuType === 'group' ? undefined : normalizeOpenMode(record.openMode),
    moduleAlias: menuType === 'group' ? undefined : normalizeBlank(record.moduleAlias),
    route: undefined,
    externalUrl: undefined,
    pageMode: menuType === 'module' ? normalizePageMode(record.pageMode) : undefined,
    defaultUiConfigId: menuType === 'module' ? normalizeBlank(record.defaultUiConfigId) : undefined,
    defaultQueryTemplateId: menuType === 'module' ? normalizeBlank(record.defaultQueryTemplateId) : undefined,
    entryParamsJson:
      menuType === 'module' || menuType === 'route' ? normalizeBlank(record.entryParamsJson) : undefined,
  };
  return normalized;
}

export function isValidScheme(record: MenuScheme) {
  return Boolean(record.id && record.alias && record.title && record.scopeType);
}

export function validateMenu(record: MenuRecord) {
  if (!record.schemeId || !record.title || !record.menuType) {
    return '所属方案、菜单名称和菜单类型不能为空';
  }
  if (record.menuType !== 'group' && !record.openMode) {
    return '非分组菜单必须选择打开方式';
  }
  if (record.menuType === 'module' && !record.moduleAlias) {
    return '模块菜单必须选择模块';
  }
  if (record.menuType !== 'group' && !record.moduleAlias) {
    return '非分组菜单必须选择模块入口';
  }
  return undefined;
}

function normalizeScopeType(value: MenuScheme['scopeType']) {
  return value === 'system' || value === 'organization' ? value : 'tenant';
}

function normalizeMenuType(value: MenuType | undefined): MenuType {
  if (value === 'module' || value === 'route' || value === 'link') {
    return value;
  }
  return 'group';
}

function normalizeOpenMode(value: MenuOpenMode | undefined): MenuOpenMode {
  return value === 'window' ? 'window' : 'tab';
}

function normalizePageMode(value: MenuPageMode | undefined): MenuPageMode | undefined {
  if (value === 'FORM' || value === 'DETAIL') {
    return value;
  }
  return value === 'LIST' ? 'LIST' : undefined;
}

function normalizeBlank(value: string | undefined) {
  const normalized = value?.trim();
  return normalized ? normalized : undefined;
}
