import { computed, ref } from 'vue';
import type { DictionaryCategory, DictionaryCategoryKind, DictionaryItem } from '@muyun/web-contracts';
import {
  isUnexpectedPlatformError,
  normalizeError,
  type AppError,
  type ModuleContext,
  type StaticModuleTreeClient,
} from '@muyun/web-core';
import type { UiConfirmOptions } from '@muyun/vue-ui-antdv';
import {
  createRecordEditorSessionState,
  handlePlatformActionSuccess,
  presentPlatformError,
  presentPlatformMessage,
} from '@muyun/platform-components';

export type DictionaryCategoryMode = 'view' | 'edit' | 'create-root' | 'create-child';
export type DictionaryItemMode = 'view' | 'edit' | 'create';
type ConfirmAction = (options: UiConfirmOptions) => Promise<boolean>;

export function createDictionaryManagementState(
  categoryContext: ModuleContext<DictionaryCategory>,
  categoryClientOf: () => StaticModuleTreeClient<DictionaryCategory>,
  itemClientOf: (categoryId: string) => StaticModuleTreeClient<DictionaryItem>,
  currentApplicationAlias: () => string | undefined,
  confirmAction: ConfirmAction,
) {
  const categoryReloadKey = ref(0);
  const itemReloadKey = ref(0);
  const categories = ref<DictionaryCategory[]>([]);
  const categoryEditor = createRecordEditorSessionState<DictionaryCategory, DictionaryCategoryMode>({
    viewMode: 'view',
    createMode: 'create-root',
    editMode: 'edit',
    emptyDraft: () => emptyDictionaryCategoryDraft(undefined, currentApplicationAlias()),
    copyRecord: copyDictionaryCategory,
  });
  const selectedCategory = categoryEditor.selected;
  const categoryDraft = categoryEditor.draft;
  const categoryMode = categoryEditor.mode;
  const categorySaving = ref(false);
  const categoryError = ref<string>();

  const items = ref<DictionaryItem[]>([]);
  const itemEditor = createRecordEditorSessionState<DictionaryItem, DictionaryItemMode>({
    viewMode: 'view',
    createMode: 'create',
    editMode: 'edit',
    emptyDraft: () => emptyDictionaryItemDraft(selectedCategory.value),
    copyRecord: copyDictionaryItem,
  });
  const selectedItem = itemEditor.selected;
  const itemDraft = itemEditor.draft;
  const itemMode = itemEditor.mode;
  const itemLoading = ref(false);
  const itemSaving = ref(false);
  const itemError = ref<string>();

  const selectedCategoryId = computed(() => selectedCategory.value?.id);
  const selectedCategoryTitle = computed(() => dictionaryCategoryTitleOf(selectedCategory.value));
  const selectedCategoryIsDictionary = computed(() => isDictionaryCategory(selectedCategory.value));
  const canCreateCategory = computed(() => categoryContext.can('create') === true);
  const canUpdateCategory = computed(() => categoryContext.can('update') === true);
  const canDeleteCategory = computed(() => categoryContext.can('delete') === true);
  const canToggleCategory = computed(() => {
    if (!selectedCategory.value?.id) {
      return false;
    }
    return categoryContext.can(categoryToggleActionCode(selectedCategory.value)) === true;
  });
  const canQueryItem = computed(() => categoryContext.can('item_query') === true);
  const canTreeItem = computed(() => categoryContext.can('item_tree') === true);
  const canCreateItem = computed(() => categoryContext.can('item_create') === true);
  const canUpdateItem = computed(() => categoryContext.can('item_update') === true);
  const canDeleteItem = computed(() => categoryContext.can('item_delete') === true);
  const canToggleItem = computed(() => {
    if (!selectedItem.value?.id) {
      return false;
    }
    return categoryContext.can(itemToggleActionCode(selectedItem.value)) === true;
  });
  const categoryReadonly = categoryEditor.readonly;
  const itemReadonly = itemEditor.readonly;
  const categoryEditorTitle = computed(() => {
    if (categoryMode.value === 'create-root') {
      return '新建字典类目';
    }
    if (categoryMode.value === 'create-child') {
      return `新建下级：${selectedCategoryTitle.value}`;
    }
    return dictionaryCategoryTitleOf(selectedCategory.value);
  });
  const itemCardTitle = computed(() =>
    itemMode.value === 'create' ? '新建字典项' : dictionaryItemTitleOf(selectedItem.value),
  );

  function handleCategoriesLoaded(records: DictionaryCategory[]) {
    categories.value = records;
    const matched = selectedCategory.value?.id
      ? records.find((item) => item.id === selectedCategory.value?.id)
      : undefined;
    if (categoryMode.value !== 'view') {
      if (matched) {
        categoryEditor.replaceSelected(matched);
      }
      return;
    }
    const next = matched ?? records[0];
    if (next) {
      categoryEditor.select(next);
    } else {
      categoryEditor.clearSelection();
    }
    categoryMode.value = 'view';
    resetItemsForCategory();
  }

  function handleSelectCategory(record: DictionaryCategory) {
    categoryEditor.select(record);
    resetItemsForCategory();
    clearCategoryFeedback();
    clearItemFeedback();
  }

  function startCreateRootCategory() {
    if (!canCreateCategory.value) {
      presentCategoryMessage('当前用户无权新增字典类目');
      return;
    }
    categoryEditor.startCreate({
      preserveSelection: true,
      draft: () => emptyDictionaryCategoryDraft(undefined, currentApplicationAlias()),
    });
    clearCategoryFeedback();
  }

  function startCreateChildCategory() {
    if (!canCreateCategory.value) {
      presentCategoryMessage('当前用户无权新增字典类目');
      return;
    }
    if (!selectedCategory.value?.id) {
      presentCategoryMessage('请先选择上级类目');
      return;
    }
    const parentId = selectedCategory.value.id;
    categoryEditor.startCreate({
      mode: 'create-child',
      preserveSelection: true,
      draft: () => emptyDictionaryCategoryDraft(parentId, currentApplicationAlias()),
    });
    clearCategoryFeedback();
  }

  function startEditCategory() {
    if (!selectedCategory.value) {
      return;
    }
    if (!canUpdateCategory.value) {
      presentCategoryMessage('当前用户无权编辑字典类目');
      return;
    }
    categoryEditor.startEdit();
    clearCategoryFeedback();
  }

  function cancelCategoryEdit() {
    categoryEditor.cancel();
    clearCategoryFeedback();
  }

  async function saveCategory() {
    if (categorySaving.value || categoryMode.value === 'view') {
      return;
    }
    if (categoryMode.value.startsWith('create') ? !canCreateCategory.value : !canUpdateCategory.value) {
      presentCategoryMessage('当前用户无权保存字典类目');
      return;
    }
    const validDraft = normalizeDictionaryCategoryDraft(categoryDraft.value, currentApplicationAlias());
    if (!isValidDictionaryCategory(validDraft)) {
      presentCategoryMessage('应用 alias、类目 alias 和类目名称不能为空');
      return;
    }
    clearCategoryFeedback();
    categorySaving.value = true;
    try {
      await categoryContext.runtime.ready;
      const crud = categoryClientOf();
      const result =
        categoryMode.value === 'edit' && validDraft.id
          ? await crud.update(validDraft.id, validDraft)
          : await crud.insert(validDraft);
      const saved = result.record;
      categoryEditor.select(saved);
      categoryMode.value = 'view';
      await presentCategorySuccess(result);
      categoryReloadKey.value += 1;
      resetItemsForCategory();
    } catch (cause) {
      handleCategoryError(cause);
    } finally {
      categorySaving.value = false;
    }
  }

  async function toggleCategory() {
    if (!selectedCategory.value?.id || categorySaving.value) {
      return;
    }
    if (!canToggleCategory.value) {
      presentCategoryMessage('当前用户无权变更字典类目启停状态');
      return;
    }
    clearCategoryFeedback();
    categorySaving.value = true;
    try {
      await categoryContext.runtime.ready;
      const enable = categoryClientOf();
      const result =
        selectedCategory.value.enabled === false
          ? await enable.enable(selectedCategory.value.id)
          : await enable.disable(selectedCategory.value.id);
      categoryEditor.select(await categoryClientOf().view(selectedCategory.value.id));
      await presentCategorySuccess(result);
      categoryReloadKey.value += 1;
    } catch (cause) {
      handleCategoryError(cause);
    } finally {
      categorySaving.value = false;
    }
  }

  async function deleteCategory() {
    if (!selectedCategory.value?.id || categorySaving.value) {
      return;
    }
    if (!canDeleteCategory.value) {
      presentCategoryMessage('当前用户无权删除字典类目');
      return;
    }
    const confirmed = await confirmAction({
      title: '删除字典类目',
      content: `确认删除类目「${dictionaryCategoryTitleOf(selectedCategory.value)}」？`,
      okText: '删除',
      danger: true,
    });
    if (!confirmed) {
      return;
    }
    clearCategoryFeedback();
    categorySaving.value = true;
    try {
      await categoryContext.runtime.ready;
      const result = await categoryClientOf().delete(selectedCategory.value.id);
      categoryEditor.clearSelection();
      categoryMode.value = 'view';
      await presentCategorySuccess(result);
      categoryReloadKey.value += 1;
      resetItemsForCategory();
    } catch (cause) {
      handleCategoryError(cause);
    } finally {
      categorySaving.value = false;
    }
  }

  async function loadItems() {
    if (!selectedCategoryId.value || !selectedCategoryIsDictionary.value) {
      items.value = [];
      syncSelectedItem();
      return;
    }
    if (!canTreeItem.value) {
      items.value = [];
      itemError.value = undefined;
      syncSelectedItem();
      return;
    }
    itemLoading.value = true;
    itemError.value = undefined;
    try {
      const response = await itemClient().treeFlat();
      items.value = response.records;
      syncSelectedItem();
    } catch (cause) {
      handleItemError(cause);
    } finally {
      itemLoading.value = false;
    }
  }

  function handleItemsLoaded(records: DictionaryItem[]) {
    items.value = records;
    syncSelectedItem();
  }

  function selectItem(record: DictionaryItem) {
    itemEditor.select(record);
    clearItemFeedback();
  }

  function startCreateItem() {
    if (!selectedCategoryId.value || !selectedCategoryIsDictionary.value) {
      presentItemMessage('请先选择可绑定的字典类目');
      return;
    }
    if (!canCreateItem.value) {
      presentItemMessage('当前用户无权新增字典项');
      return;
    }
    itemEditor.startCreate({ preserveSelection: true });
    clearItemFeedback();
  }

  function startCreateChildItem(parent: DictionaryItem) {
    if (!selectedCategoryId.value || !selectedCategoryIsDictionary.value) {
      presentItemMessage('请先选择可绑定的字典类目');
      return;
    }
    if (!parent.id) {
      return;
    }
    if (!canCreateItem.value) {
      presentItemMessage('当前用户无权新增字典项');
      return;
    }
    itemEditor.startCreate({
      selectedRecord: parent,
      draft: () => ({
        ...emptyDictionaryItemDraft(selectedCategory.value),
        parentId: parent.id,
      }),
    });
    clearItemFeedback();
  }

  function startEditItem() {
    if (!selectedItem.value) {
      return;
    }
    if (!canUpdateItem.value) {
      presentItemMessage('当前用户无权编辑字典项');
      return;
    }
    itemEditor.startEdit();
    clearItemFeedback();
  }

  function cancelItemEdit() {
    itemEditor.cancel();
    clearItemFeedback();
  }

  async function saveItem() {
    if (itemSaving.value || itemMode.value === 'view') {
      return;
    }
    if (itemMode.value === 'create' ? !canCreateItem.value : !canUpdateItem.value) {
      presentItemMessage('当前用户无权保存字典项');
      return;
    }
    const validDraft = normalizeDictionaryItemDraft(itemDraft.value, selectedCategory.value);
    if (!isValidDictionaryItem(validDraft)) {
      presentItemMessage('字典项编码和名称不能为空');
      return;
    }
    clearItemFeedback();
    itemSaving.value = true;
    try {
      const result =
        itemMode.value === 'edit' && validDraft.id
          ? await itemClient().update(validDraft.id, validDraft)
          : await itemClient().insert(validDraft);
      const saved = result.record;
      itemEditor.select(saved);
      itemMode.value = 'view';
      await presentItemSuccess(result);
      itemReloadKey.value += 1;
    } catch (cause) {
      handleItemError(cause);
    } finally {
      itemSaving.value = false;
    }
  }

  async function toggleItem() {
    if (!selectedItem.value?.id || itemSaving.value) {
      return;
    }
    if (!canToggleItem.value) {
      presentItemMessage('当前用户无权变更字典项启停状态');
      return;
    }
    clearItemFeedback();
    itemSaving.value = true;
    try {
      const result =
        selectedItem.value.enabled === false
          ? await itemClient().enable(selectedItem.value.id)
          : await itemClient().disable(selectedItem.value.id);
      itemEditor.select(await itemClient().view(selectedItem.value.id));
      await presentItemSuccess(result);
      itemReloadKey.value += 1;
    } catch (cause) {
      handleItemError(cause);
    } finally {
      itemSaving.value = false;
    }
  }

  async function deleteItem() {
    if (!selectedItem.value?.id || itemSaving.value) {
      return;
    }
    if (!canDeleteItem.value) {
      presentItemMessage('当前用户无权删除字典项');
      return;
    }
    const confirmed = await confirmAction({
      title: '删除字典项',
      content: `确认删除字典项「${dictionaryItemTitleOf(selectedItem.value)}」？`,
      okText: '删除',
      danger: true,
    });
    if (!confirmed) {
      return;
    }
    clearItemFeedback();
    itemSaving.value = true;
    try {
      const result = await itemClient().delete(selectedItem.value.id);
      itemEditor.clearSelection();
      if (selectedCategoryIsDictionary.value && canCreateItem.value) {
        itemEditor.startCreate();
      } else {
        itemMode.value = 'view';
      }
      await presentItemSuccess(result);
      itemReloadKey.value += 1;
    } catch (cause) {
      handleItemError(cause);
    } finally {
      itemSaving.value = false;
    }
  }

  function resetItemsForCategory() {
    items.value = [];
    itemEditor.clearSelection();
    itemMode.value = 'view';
  }

  function resetForApplication() {
    categories.value = [];
    categoryEditor.clearSelection();
    categoryMode.value = 'view';
    resetItemsForCategory();
    clearCategoryFeedback();
    clearItemFeedback();
  }

  function syncSelectedItem() {
    const matched = selectedItem.value?.id
      ? items.value.find((item) => item.id === selectedItem.value?.id)
      : undefined;
    const next = matched ?? items.value[0];
    if (itemMode.value === 'view') {
      if (next) {
        itemEditor.select(next);
      } else {
        itemEditor.clearSelection();
      }
      return;
    }
    if (matched) {
      itemEditor.replaceSelected(matched);
    }
  }

  function itemClient() {
    if (!selectedCategoryId.value) {
      throw new Error('Dictionary item client requires selected category');
    }
    return itemClientOf(selectedCategoryId.value);
  }

  function clearCategoryFeedback() {
    categoryError.value = undefined;
  }

  function clearItemFeedback() {
    itemError.value = undefined;
  }

  function handleCategoryError(cause: unknown) {
    const error = normalizeError(cause);
    if (presentGlobalError(error, true)) {
      categoryError.value = undefined;
      return;
    }
    categoryError.value = error.message;
  }

  function handleItemError(cause: unknown) {
    const error = normalizeError(cause);
    if (presentGlobalError(error, true)) {
      itemError.value = undefined;
      return;
    }
    itemError.value = error.message;
  }

  function presentGlobalError(error: AppError, forceGlobal: boolean) {
    if (!forceGlobal && !isUnexpectedPlatformError(error)) {
      return false;
    }
    presentPlatformError(error, { source: 'dictionary-management-action', phase: 'action' });
    return true;
  }

  function presentCategoryMessage(message: string) {
    categoryError.value = message;
    presentPlatformMessage(message, { source: 'dictionary-category-action', phase: 'action' });
  }

  function presentItemMessage(message: string) {
    itemError.value = message;
    presentPlatformMessage(message, { source: 'dictionary-item-action', phase: 'action' });
  }

  function presentCategorySuccess(result: unknown) {
    return handlePlatformActionSuccess(result, {
      source: 'dictionary-category-action',
      phase: 'action',
    });
  }

  function presentItemSuccess(result: unknown) {
    return handlePlatformActionSuccess(result, {
      source: 'dictionary-item-action',
      phase: 'action',
    });
  }

  return {
    categoryReloadKey,
    itemReloadKey,
    categories,
    selectedCategory,
    categoryDraft,
    categoryMode,
    categorySaving,
    categoryError,
    items,
    selectedItem,
    itemDraft,
    itemMode,
    itemLoading,
    itemSaving,
    itemError,
    selectedCategoryId,
    selectedCategoryTitle,
    selectedCategoryIsDictionary,
    canCreateCategory,
    canUpdateCategory,
    canDeleteCategory,
    canToggleCategory,
    canQueryItem,
    canTreeItem,
    canCreateItem,
    canUpdateItem,
    canDeleteItem,
    canToggleItem,
    categoryReadonly,
    itemReadonly,
    categoryEditorTitle,
    itemCardTitle,
    handleCategoriesLoaded,
    handleSelectCategory,
    startCreateRootCategory,
    startCreateChildCategory,
    startEditCategory,
    cancelCategoryEdit,
    saveCategory,
    toggleCategory,
    deleteCategory,
    handleItemsLoaded,
    loadItems,
    selectItem,
    startCreateItem,
    startCreateChildItem,
    startEditItem,
    cancelItemEdit,
    saveItem,
    toggleItem,
    deleteItem,
    resetForApplication,
  };
}

export function dictionaryCategoryTitleOf(record: DictionaryCategory | undefined) {
  return record?.title ?? record?.alias ?? record?.id ?? '字典类目';
}

export function dictionaryItemTitleOf(record: DictionaryItem | undefined) {
  return record?.title ?? record?.code ?? record?.id ?? '字典项';
}

export function dictionaryCategoryKindTitle(record: DictionaryCategory | undefined) {
  return isFolderCategory(record) ? '目录' : '字典';
}

export function isDictionaryCategory(record: DictionaryCategory | undefined) {
  return Boolean(record) && normalizedCategoryKind(record?.categoryKind) === 'dictionary';
}

export function isFolderCategory(record: DictionaryCategory | undefined) {
  return normalizedCategoryKind(record?.categoryKind) === 'folder';
}

export function emptyDictionaryCategoryDraft(
  parentId?: string,
  applicationAlias = 'platform',
): DictionaryCategory {
  return {
    applicationAlias,
    alias: '',
    categoryKind: 'DICTIONARY',
    parentId,
    title: '',
    enabled: true,
  };
}

export function copyDictionaryCategory(record: DictionaryCategory): DictionaryCategory {
  return { ...record };
}

export function normalizeDictionaryCategoryDraft(
  record: DictionaryCategory,
  applicationAlias = record.applicationAlias,
): DictionaryCategory {
  return {
    ...record,
    applicationAlias: applicationAlias?.trim(),
    alias: record.alias?.trim(),
    parentId: normalizeBlank(record.parentId),
    title: record.title?.trim(),
    categoryKind: normalizeCategoryKind(record.categoryKind),
  };
}

export function isValidDictionaryCategory(record: DictionaryCategory) {
  return Boolean(record.applicationAlias && record.alias && record.title);
}

export function emptyDictionaryItemDraft(category?: DictionaryCategory): DictionaryItem {
  return {
    categoryId: category?.id,
    categoryAlias: category?.alias,
    code: '',
    title: '',
    enabled: true,
  };
}

export function copyDictionaryItem(record: DictionaryItem): DictionaryItem {
  return { ...record };
}

export function normalizeDictionaryItemDraft(
  record: DictionaryItem,
  category: DictionaryCategory | undefined,
): DictionaryItem {
  return {
    ...record,
    categoryId: category?.id ?? record.categoryId,
    categoryAlias: category?.alias ?? record.categoryAlias,
    parentId: normalizeBlank(record.parentId),
    code: record.code?.trim(),
    title: record.title?.trim(),
  };
}

export function isValidDictionaryItem(record: DictionaryItem) {
  return Boolean(record.categoryId && record.code && record.title);
}

function normalizeCategoryKind(kind: DictionaryCategoryKind | undefined): DictionaryCategoryKind {
  return normalizedCategoryKind(kind) === 'folder' ? 'FOLDER' : 'DICTIONARY';
}

function normalizedCategoryKind(kind: DictionaryCategoryKind | undefined) {
  return String(kind ?? 'DICTIONARY').toLowerCase();
}

function categoryToggleActionCode(record: DictionaryCategory) {
  return record.enabled === false ? 'enable' : 'disable';
}

function itemToggleActionCode(record: DictionaryItem) {
  return record.enabled === false ? 'item_enable' : 'item_disable';
}

function normalizeBlank(value: string | undefined) {
  const normalized = value?.trim();
  return normalized ? normalized : undefined;
}
