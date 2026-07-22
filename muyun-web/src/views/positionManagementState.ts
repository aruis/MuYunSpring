import { computed, ref } from 'vue';
import type { Position, PositionCategory, WebQueryCondition } from '@muyun/web-contracts';
import { normalizeError, type ModuleContext, type StaticModuleCrudClient } from '@muyun/web-core';
import type { UiConfirmOptions } from '@muyun/vue-ui-antdv';
import {
  createPlatformActionResultReactionHandlers,
  createRecordEditorSessionState,
  handlePlatformActionSuccess,
  mergePlatformActionResultReactionHandlers,
  platformActionResultReactions,
  presentPlatformError,
  presentPlatformMessage,
  type PlatformActionResultReaction,
  type PlatformActionResultReactionHandler,
  withPlatformActionResultReactions,
} from '@muyun/platform-components';

export type PositionCardMode = 'view' | 'edit' | 'create';
export type CategoryCardMode = 'view' | 'edit' | 'create-root' | 'create-child';
type ConfirmAction = (options: UiConfirmOptions) => Promise<boolean>;

export interface PositionManagementStateOptions {
  categoryActionResultReactionHandlers?: Record<string, PlatformActionResultReactionHandler | undefined>;
  positionActionResultReactionHandlers?: Record<string, PlatformActionResultReactionHandler | undefined>;
}

export function createPositionManagementState(
  categoryContext: ModuleContext<PositionCategory>,
  positionClient: StaticModuleCrudClient<Position>,
  confirmAction: ConfirmAction,
  options: PositionManagementStateOptions = {},
) {
  const categoryReloadKey = ref(0);
  const positionReloadKey = ref(0);
  const categories = ref<PositionCategory[]>([]);
  const categoryEditor = createRecordEditorSessionState<PositionCategory, CategoryCardMode>({
    viewMode: 'view',
    createMode: 'create-root',
    editMode: 'edit',
    emptyDraft: () => emptyCategoryDraft(),
    copyRecord: copyCategory,
  });
  const selectedCategory = categoryEditor.selected;
  const categoryDraft = categoryEditor.draft;
  const categoryMode = categoryEditor.mode;
  const categorySaving = ref(false);
  const categoryError = ref<string>();
  const selectedCategoryId = computed(() => selectedCategory.value?.id);
  const categoryActionResultReactionHandlers = createCategoryActionReactionHandlers();

  const positions = ref<Position[]>([]);
  const positionEditor = createRecordEditorSessionState<Position, PositionCardMode>({
    viewMode: 'view',
    createMode: 'create',
    editMode: 'edit',
    emptyDraft: () => emptyPositionDraft(selectedCategoryId.value ?? ''),
    copyRecord: copyPosition,
  });
  const selectedPosition = positionEditor.selected;
  const positionDraft = positionEditor.draft;
  const positionMode = positionEditor.mode;
  const positionLoading = ref(false);
  const positionSaving = ref(false);
  const positionError = ref<string>();
  const positionActionResultReactionHandlers = createPositionActionReactionHandlers();

  const selectedCategoryTitle = computed(() => positionCategoryTitleOf(selectedCategory.value));
  const filteredPositions = computed(() =>
    positions.value.filter((record) => positionMatchesCategory(record, selectedCategoryId.value)),
  );
  const canCreateCategory = computed(() => categoryContext.can('create') === true);
  const canUpdateCategory = computed(() => categoryContext.can('update') === true);
  const canDeleteCategory = computed(() => categoryContext.can('delete') === true);
  const canToggleCategory = computed(() => {
    if (!selectedCategory.value?.id) {
      return false;
    }
    return categoryContext.can(categoryToggleActionCode(selectedCategory.value)) === true;
  });
  const canQueryPosition = computed(() => categoryContext.can('position_query') === true);
  const canCreatePosition = computed(() => categoryContext.can('position_create') === true);
  const canUpdatePosition = computed(() => categoryContext.can('position_update') === true);
  const canDeletePosition = computed(() => categoryContext.can('position_delete') === true);
  const canTogglePosition = computed(() => {
    if (!selectedPosition.value?.id) {
      return false;
    }
    return categoryContext.can(positionToggleActionCode(selectedPosition.value)) === true;
  });
  const positionReadonly = positionEditor.readonly;
  const categoryReadonly = categoryEditor.readonly;
  const positionCardTitle = computed(() =>
    positionMode.value === 'create' ? '新建岗位' : positionTitleOf(selectedPosition.value),
  );
  const categoryEditorTitle = computed(() => {
    if (categoryMode.value === 'create-root') {
      return '新建分类';
    }
    if (categoryMode.value === 'create-child') {
      return `新建下级：${selectedCategoryTitle.value}`;
    }
    return positionCategoryTitleOf(selectedCategory.value);
  });

  function handleCategoriesLoaded(records: PositionCategory[]) {
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
    syncSelectedPosition();
  }

  function handleSelectCategory(record: PositionCategory) {
    categoryEditor.select(record);
    if (positionMode.value !== 'view') {
      positionEditor.clearSelection();
      positionMode.value = 'view';
    }
    clearCategoryFeedback();
    clearPositionFeedback();
    syncSelectedPosition();
  }

  function startCreateRootCategory() {
    if (!canCreateCategory.value) {
      presentCategoryError('当前用户无权新增岗位分类');
      return;
    }
    categoryEditor.startCreate({ preserveSelection: true });
    clearCategoryFeedback();
  }

  function startCreateChildCategory() {
    if (!canCreateCategory.value) {
      presentCategoryError('当前用户无权新增岗位分类');
      return;
    }
    if (!selectedCategory.value?.id) {
      presentCategoryError('请先选择上级分类');
      return;
    }
    const parentId = selectedCategory.value.id;
    categoryEditor.startCreate({
      mode: 'create-child',
      preserveSelection: true,
      draft: () => emptyCategoryDraft(parentId),
    });
    clearCategoryFeedback();
  }

  function startEditCategory() {
    if (!selectedCategory.value) {
      return;
    }
    if (!canUpdateCategory.value) {
      presentCategoryError('当前用户无权编辑岗位分类');
      return;
    }
    categoryEditor.startEdit();
    clearCategoryFeedback();
  }

  function cancelCategoryEdit() {
    categoryEditor.cancel();
    categoryMode.value = 'view';
    clearCategoryFeedback();
  }

  async function saveCategory() {
    if (categorySaving.value || categoryMode.value === 'view') {
      return;
    }
    if (categoryMode.value.startsWith('create') ? !canCreateCategory.value : !canUpdateCategory.value) {
      presentCategoryError('当前用户无权保存岗位分类');
      return;
    }
    const validDraft = normalizeCategoryDraft(categoryDraft.value);
    if (!isValidCategory(validDraft)) {
      presentCategoryError('分类编码和分类名称不能为空');
      return;
    }
    clearCategoryFeedback();
    categorySaving.value = true;
    try {
      await categoryContext.runtime.ready;
      const crud = categoryContext.abilities.crud();
      const result =
        categoryMode.value === 'edit' && validDraft.id
          ? await crud.update(validDraft.id, validDraft)
          : await crud.insert(validDraft);
      const saved = result.record;
      categoryEditor.select(saved);
      await presentCategorySuccess(result, [
        platformActionResultReactions.closeEditor(),
        platformActionResultReactions.refreshList(),
      ]);
    } catch (cause) {
      presentCategoryCause(cause);
    } finally {
      categorySaving.value = false;
    }
  }

  async function toggleCategory() {
    if (!selectedCategory.value?.id || categorySaving.value) {
      return;
    }
    if (!canToggleCategory.value) {
      presentCategoryError('当前用户无权变更岗位分类启停状态');
      return;
    }
    clearCategoryFeedback();
    categorySaving.value = true;
    try {
      await categoryContext.runtime.ready;
      const enable = categoryContext.abilities.enable();
      const result =
        selectedCategory.value.enabled === false
          ? await enable.enable(selectedCategory.value.id, { version: selectedCategory.value.version! })
          : await enable.disable(selectedCategory.value.id, { version: selectedCategory.value.version! });
      categoryEditor.select(await categoryContext.abilities.crud().view(selectedCategory.value.id));
      await presentCategorySuccess(result, [platformActionResultReactions.refreshList()]);
    } catch (cause) {
      presentCategoryCause(cause);
    } finally {
      categorySaving.value = false;
    }
  }

  async function deleteCategory() {
    if (!selectedCategory.value?.id || categorySaving.value) {
      return;
    }
    if (!canDeleteCategory.value) {
      presentCategoryError('当前用户无权删除岗位分类');
      return;
    }
    const confirmed = await confirmAction({
      title: '删除岗位分类',
      content: `确认删除分类「${positionCategoryTitleOf(selectedCategory.value)}」？`,
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
      const result = await categoryContext.abilities
        .crud()
        .delete(selectedCategory.value.id, { version: selectedCategory.value.version! });
      await presentCategorySuccess(result, [
        platformActionResultReactions.clearSelection(),
        platformActionResultReactions.refreshList(),
      ]);
    } catch (cause) {
      presentCategoryCause(cause);
    } finally {
      categorySaving.value = false;
    }
  }

  async function loadPositions() {
    if (!selectedCategoryId.value) {
      positions.value = [];
      syncSelectedPosition();
      return;
    }
    if (!canQueryPosition.value) {
      positions.value = [];
      positionError.value = undefined;
      syncSelectedPosition();
      return;
    }
    positionLoading.value = true;
    positionError.value = undefined;
    try {
      const response = await positionClient.query({
        unpaged: true,
        conditions: [eqCondition('categoryId', selectedCategoryId.value)],
      });
      positions.value = response.records;
      syncSelectedPosition();
    } catch (cause) {
      presentPositionCause(cause);
    } finally {
      positionLoading.value = false;
    }
  }

  function syncSelectedPosition() {
    const rows = filteredPositions.value;
    const matched = selectedPosition.value?.id
      ? rows.find((item) => item.id === selectedPosition.value?.id)
      : undefined;
    const next = matched ?? rows[0];
    if (positionMode.value === 'view') {
      if (next) {
        positionEditor.select(next);
      } else {
        positionEditor.clearSelection();
      }
      return;
    }
    if (matched) {
      positionEditor.replaceSelected(matched);
    }
  }

  function selectPosition(record: Position) {
    positionEditor.select(record);
    clearPositionFeedback();
  }

  function startCreatePosition() {
    if (!selectedCategoryId.value) {
      presentPositionError('请先选择岗位分类');
      return;
    }
    if (!canCreatePosition.value) {
      presentPositionError('当前用户无权新增岗位');
      return;
    }
    positionEditor.startCreate({ preserveSelection: true });
    clearPositionFeedback();
  }

  function startEditPosition() {
    if (!selectedPosition.value) {
      return;
    }
    if (!canUpdatePosition.value) {
      presentPositionError('当前用户无权编辑岗位');
      return;
    }
    positionEditor.startEdit();
    clearPositionFeedback();
  }

  function cancelPositionEdit() {
    positionEditor.cancel();
    clearPositionFeedback();
  }

  async function savePosition() {
    if (positionSaving.value || positionMode.value === 'view') {
      return;
    }
    if (positionMode.value === 'create' ? !canCreatePosition.value : !canUpdatePosition.value) {
      presentPositionError('当前用户无权保存岗位');
      return;
    }
    const validDraft = normalizePositionDraft(positionDraft.value);
    if (!isValidPosition(validDraft)) {
      presentPositionError('所属分类、岗位编码和岗位名称不能为空');
      return;
    }
    clearPositionFeedback();
    positionSaving.value = true;
    try {
      const result =
        positionMode.value === 'edit' && validDraft.id
          ? await positionClient.update(validDraft.id, validDraft)
          : await positionClient.insert(validDraft);
      const saved = result.record;
      positionEditor.select(saved);
      await presentPositionSuccess(result, [
        platformActionResultReactions.closeEditor(),
        platformActionResultReactions.refreshList(),
      ]);
      if (saved.categoryId && saved.categoryId !== selectedCategoryId.value) {
        const savedCategory = categories.value.find((category) => category.id === saved.categoryId);
        if (savedCategory) {
          categoryEditor.select(savedCategory);
        } else {
          categoryEditor.clearSelection();
        }
        positions.value = [saved];
      }
    } catch (cause) {
      presentPositionCause(cause);
    } finally {
      positionSaving.value = false;
    }
  }

  async function togglePosition() {
    if (!selectedPosition.value?.id || positionSaving.value) {
      return;
    }
    if (!canTogglePosition.value) {
      presentPositionError('当前用户无权变更岗位启停状态');
      return;
    }
    clearPositionFeedback();
    positionSaving.value = true;
    try {
      const result =
        selectedPosition.value.enabled === false
          ? await positionClient.enable(selectedPosition.value.id, {
              version: selectedPosition.value.version!,
            })
          : await positionClient.disable(selectedPosition.value.id, {
              version: selectedPosition.value.version!,
            });
      positionEditor.select(await positionClient.view(selectedPosition.value.id));
      await presentPositionSuccess(result, [platformActionResultReactions.refreshList()]);
    } catch (cause) {
      presentPositionCause(cause);
    } finally {
      positionSaving.value = false;
    }
  }

  async function deletePosition() {
    if (!selectedPosition.value?.id || positionSaving.value) {
      return;
    }
    if (!canDeletePosition.value) {
      presentPositionError('当前用户无权删除岗位');
      return;
    }
    const confirmed = await confirmAction({
      title: '删除岗位',
      content: `确认删除岗位「${positionTitleOf(selectedPosition.value)}」？`,
      okText: '删除',
      danger: true,
    });
    if (!confirmed) {
      return;
    }
    clearPositionFeedback();
    positionSaving.value = true;
    try {
      const result = await positionClient.delete(selectedPosition.value.id, {
        version: selectedPosition.value.version!,
      });
      positionEditor.clearSelection();
      if (selectedCategoryId.value && canCreatePosition.value) {
        positionEditor.startCreate();
      } else {
        positionMode.value = 'view';
      }
      await presentPositionSuccess(result, [platformActionResultReactions.refreshList()]);
    } catch (cause) {
      presentPositionCause(cause);
    } finally {
      positionSaving.value = false;
    }
  }

  function clearCategoryFeedback() {
    categoryError.value = undefined;
  }

  function clearPositionFeedback() {
    positionError.value = undefined;
  }

  function presentCategoryCause(cause: unknown) {
    const error = normalizeError(cause);
    categoryError.value = error.message;
    presentPlatformError(error, { source: 'position-category-action', phase: 'action' });
  }

  function presentPositionCause(cause: unknown) {
    const error = normalizeError(cause);
    positionError.value = error.message;
    presentPlatformError(error, { source: 'position-action', phase: 'action' });
  }

  function presentCategoryError(message: string) {
    categoryError.value = message;
    presentPlatformMessage(message, { source: 'position-category-action', phase: 'action' });
  }

  function presentPositionError(message: string) {
    positionError.value = message;
    presentPlatformMessage(message, { source: 'position-action', phase: 'action' });
  }

  function presentCategorySuccess(result: unknown, defaultReactions: PlatformActionResultReaction[]) {
    return handlePlatformActionSuccess(withPlatformActionResultReactions(result, defaultReactions), {
      source: 'position-category-action',
      phase: 'action',
      reactionHandlers: categoryActionResultReactionHandlers,
    });
  }

  function presentPositionSuccess(result: unknown, defaultReactions: PlatformActionResultReaction[]) {
    return handlePlatformActionSuccess(withPlatformActionResultReactions(result, defaultReactions), {
      source: 'position-action',
      phase: 'action',
      reactionHandlers: positionActionResultReactionHandlers,
    });
  }

  function createCategoryActionReactionHandlers() {
    const defaultHandlers = createPlatformActionResultReactionHandlers({
      refreshList: () => {
        categoryReloadKey.value += 1;
      },
      closeEditor: () => {
        categoryMode.value = 'view';
      },
      clearSelection: () => {
        categoryEditor.clearSelection();
        categoryMode.value = 'view';
      },
    });
    return mergePlatformActionResultReactionHandlers(
      defaultHandlers,
      options.categoryActionResultReactionHandlers,
    );
  }

  function createPositionActionReactionHandlers() {
    const defaultHandlers = createPlatformActionResultReactionHandlers({
      refreshList: () => {
        positionReloadKey.value += 1;
      },
      closeEditor: () => {
        positionMode.value = 'view';
      },
    });
    return mergePlatformActionResultReactionHandlers(
      defaultHandlers,
      options.positionActionResultReactionHandlers,
    );
  }

  return {
    categoryReloadKey,
    positionReloadKey,
    categories,
    selectedCategory,
    categoryDraft,
    categoryMode,
    categorySaving,
    categoryError,
    positions,
    selectedPosition,
    positionDraft,
    positionMode,
    positionLoading,
    positionSaving,
    positionError,
    selectedCategoryId,
    selectedCategoryTitle,
    filteredPositions,
    canCreateCategory,
    canUpdateCategory,
    canDeleteCategory,
    canToggleCategory,
    canQueryPosition,
    canCreatePosition,
    canUpdatePosition,
    canDeletePosition,
    canTogglePosition,
    positionReadonly,
    categoryReadonly,
    positionCardTitle,
    categoryEditorTitle,
    handleCategoriesLoaded,
    handleSelectCategory,
    startCreateRootCategory,
    startCreateChildCategory,
    startEditCategory,
    cancelCategoryEdit,
    saveCategory,
    toggleCategory,
    deleteCategory,
    loadPositions,
    syncSelectedPosition,
    selectPosition,
    startCreatePosition,
    startEditPosition,
    cancelPositionEdit,
    savePosition,
    togglePosition,
    deletePosition,
  };
}

export function positionTitleOf(record: Position | undefined) {
  return record?.title ?? record?.code ?? record?.id ?? '岗位详情';
}

export function positionCategoryTitleOf(record: PositionCategory | undefined) {
  return record?.title ?? record?.code ?? record?.id ?? '岗位分类';
}

export function emptyPositionDraft(categoryId: string): Position {
  return {
    categoryId,
    code: '',
    title: '',
    description: '',
    enabled: true,
  };
}

export function copyPosition(record: Position): Position {
  return { ...record };
}

export function normalizePositionDraft(record: Position): Position {
  return {
    ...record,
    categoryId: record.categoryId?.trim(),
    code: record.code?.trim(),
    title: record.title?.trim(),
    description: normalizeBlank(record.description),
  };
}

export function isValidPosition(record: Position) {
  return Boolean(record.categoryId && record.code && record.title);
}

export function positionMatchesCategory(record: Position, categoryId: string | undefined) {
  return Boolean(categoryId) && record.categoryId === categoryId;
}

function positionToggleActionCode(record: Position) {
  return record.enabled === false ? 'position_enable' : 'position_disable';
}

function categoryToggleActionCode(record: PositionCategory) {
  return record.enabled === false ? 'enable' : 'disable';
}

export function emptyCategoryDraft(parentId?: string): PositionCategory {
  return {
    parentId,
    code: '',
    title: '',
    description: '',
    enabled: true,
  };
}

export function copyCategory(record: PositionCategory): PositionCategory {
  return { ...record };
}

export function normalizeCategoryDraft(record: PositionCategory): PositionCategory {
  return {
    ...record,
    parentId: normalizeBlank(record.parentId),
    code: record.code?.trim(),
    title: record.title?.trim(),
    description: normalizeBlank(record.description),
  };
}

export function isValidCategory(record: PositionCategory) {
  return Boolean(record.code && record.title);
}

function normalizeBlank(value: string | undefined) {
  const normalized = value?.trim();
  return normalized ? normalized : undefined;
}

function eqCondition(fieldName: string, value: unknown): WebQueryCondition {
  return {
    fieldName,
    operator: 'EQ',
    values: [value],
  };
}
