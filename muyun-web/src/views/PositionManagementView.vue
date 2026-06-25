<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import {
  ModuleActionButton,
  RecordActionBar,
  RecordExplorerPanel,
  RecordListExplorer,
  RecordMetaSection,
  RecordStatusSwitch,
  TreeRecordExplorer,
  type RecordActionItem,
} from '@muyun/platform-components';
import type { Option, Position, PositionCategory } from '@muyun/web-contracts';
import { useModuleContext } from '@muyun/web-core';
import {
  confirmAction,
  showErrorMessage,
  UiEmpty,
  UiError,
  UiInput,
  UiSelect,
  UiSpin,
  type UiRecordInlineAction,
} from '@muyun/vue-ui-antdv';
import {
  createPositionManagementState,
  positionCategoryTitleOf,
  positionTitleOf,
} from './positionManagementState';

defineOptions({ name: 'PositionManagementView' });

const categoryContext = useModuleContext<PositionCategory>({ moduleAlias: 'iam.position_category' });
const positionContext = useModuleContext<Position>({ moduleAlias: 'iam.position' });
const categorySearchKeyword = ref('');
const positionSearchKeyword = ref('');
const {
  categoryReloadKey,
  positionReloadKey,
  categories,
  selectedCategory,
  categoryDraft,
  categoryMode,
  categorySaving,
  categoryError,
  categoryMessage,
  selectedPosition,
  positionDraft,
  positionMode,
  positionLoading,
  positionSaving,
  positionError,
  positionMessage,
  selectedCategoryId,
  filteredPositions,
  canToggleCategory,
  canCreatePosition,
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
  selectPosition,
  startCreatePosition,
  startEditPosition,
  cancelPositionEdit,
  savePosition,
  togglePosition,
  deletePosition,
} = createPositionManagementState(categoryContext, positionContext.crud, confirmAction, showErrorMessage);

const categoryOptions = computed<Option[]>(() =>
  categories.value
    .filter((category) => category.id && category.enabled !== false)
    .map((category) => ({
      label: positionCategoryTitleOf(category),
      value: category.id ?? '',
    })),
);

const categoryActions = computed<RecordActionItem[]>(() => {
  return [
    { key: 'category-cancel', title: '取消', disabled: categorySaving.value },
    {
      key: 'category-save',
      actionCode: categoryMode.value.startsWith('create') ? 'create' : 'update',
      iconName: 'save',
      title: categorySaving.value ? '保存中' : '保存',
      loading: categorySaving.value,
      primary: true,
    },
  ];
});

const categoryEditorVisible = computed(() => categoryMode.value !== 'view');
const positionListEmptyDescription = computed(() =>
  positionSearchKeyword.value.trim() ? '没有匹配的岗位' : '当前分类暂无岗位',
);

const positionActions = computed<RecordActionItem[]>(() => {
  if (positionMode.value !== 'view') {
    return [
      { key: 'position-cancel', title: '取消', disabled: positionSaving.value },
      {
        key: 'position-save',
        actionCode: positionMode.value === 'create' ? 'position_create' : 'position_update',
        iconName: 'save',
        title: positionSaving.value ? '保存中' : '保存',
        loading: positionSaving.value,
        primary: true,
      },
    ];
  }
  return [
    { key: 'position-edit', actionCode: 'position_update', title: '编辑', disabled: !selectedPosition.value },
    {
      key: 'position-delete',
      actionCode: 'position_delete',
      title: '删除',
      disabled: !selectedPosition.value,
      loading: positionSaving.value,
      danger: true,
    },
  ];
});

onMounted(loadPositions);

watch(positionReloadKey, () => {
  void loadPositions();
});

watch(selectedCategoryId, () => {
  void loadPositions();
});

function handleCategoryAction(action: RecordActionItem) {
  if (action.key === 'category-cancel') {
    cancelCategoryEdit();
    return;
  }
  if (action.key === 'category-save') {
    void saveCategory();
  }
}

function categoryTreeActionsOf(record: PositionCategory): UiRecordInlineAction[] {
  const actions: UiRecordInlineAction[] = [];
  if (record.id && categoryContext.can('create') === true) {
    actions.push({ key: 'create-child', title: '新增下级', iconName: 'plus' });
  }
  if (record.id && categoryContext.can('update') === true) {
    actions.push({ key: 'edit', title: '编辑分类', iconName: 'edit' });
  }
  if (record.id && categoryContext.can('delete') === true) {
    actions.push({ key: 'delete', title: '删除分类', iconName: 'delete', danger: true });
  }
  return actions;
}

function handleCategoryTreeAction(action: UiRecordInlineAction, record: PositionCategory) {
  handleSelectCategory(record);
  if (action.key === 'create-child') {
    startCreateChildCategory();
    return;
  }
  if (action.key === 'edit') {
    startEditCategory();
    return;
  }
  if (action.key === 'delete') {
    void deleteCategory();
  }
}

function handlePositionAction(action: RecordActionItem) {
  if (action.key === 'position-edit') {
    startEditPosition();
    return;
  }
  if (action.key === 'position-delete') {
    void deletePosition();
    return;
  }
  if (action.key === 'position-cancel') {
    cancelPositionEdit();
    return;
  }
  if (action.key === 'position-save') {
    void savePosition();
  }
}
</script>

<template>
  <section class="position-workspace">
    <RecordExplorerPanel
      v-model:search-keyword="categorySearchKeyword"
      class="category-column"
      title="岗位分类"
      search-placeholder="搜索分类名称、编码或 ID"
      @refresh="categoryReloadKey += 1"
    >
      <template #actions>
        <ModuleActionButton
          class="record-panel-create-button"
          :context="categoryContext"
          action-code="create"
          title="新增分类"
          icon-only
          :disabled="categorySaving"
          @click="startCreateRootCategory"
        />
      </template>
      <TreeRecordExplorer
        :context="categoryContext"
        :selected-id="selectedCategory?.id"
        :reload-key="categoryReloadKey"
        :keyword="categorySearchKeyword"
        search-mode="none"
        search-placeholder="搜索分类名称、编码或 ID"
        empty-description="暂无岗位分类"
        loading-tip="加载岗位分类"
        fallback-title="未命名分类"
        :title-of="positionCategoryTitleOf"
        :actions-of="categoryTreeActionsOf"
        @loaded="handleCategoriesLoaded"
        @select="handleSelectCategory"
        @action="handleCategoryTreeAction"
      />
      <template #editor>
        <Transition name="category-editor-drawer">
          <section v-if="categoryEditorVisible" class="category-editor-panel">
            <header class="category-editor-header">
              <div>
                <h3>{{ categoryEditorTitle }}</h3>
              </div>
              <RecordActionBar
                :context="categoryContext"
                :actions="categoryActions"
                size="compact"
                @action="handleCategoryAction"
              />
            </header>
            <div v-if="categoryError" class="message error">{{ categoryError }}</div>
            <div v-else-if="categoryMessage" class="message success">{{ categoryMessage }}</div>
            <form class="category-form" @submit.prevent="saveCategory">
              <label>
                <span>分类编码</span>
                <UiInput v-model:value="categoryDraft.code" :disabled="categoryReadonly" />
              </label>
              <label>
                <span>分类名称</span>
                <UiInput v-model:value="categoryDraft.title" :disabled="categoryReadonly" />
              </label>
              <label>
                <span>说明</span>
                <UiInput v-model:value="categoryDraft.description" :disabled="categoryReadonly" />
              </label>
            </form>
            <section v-if="categoryMode === 'edit' && selectedCategory?.id" class="category-status-panel">
              <RecordStatusSwitch
                :enabled="selectedCategory.enabled"
                :disabled="categorySaving || !canToggleCategory"
                :loading="categorySaving"
                @change="toggleCategory"
              />
            </section>
          </section>
        </Transition>
      </template>
    </RecordExplorerPanel>

    <RecordExplorerPanel
      v-model:search-keyword="positionSearchKeyword"
      class="list-column"
      title="岗位"
      search-placeholder="搜索岗位名称、编码或 ID"
      @refresh="positionReloadKey += 1"
    >
      <template #actions>
        <ModuleActionButton
          class="record-panel-create-button"
          :context="categoryContext"
          action-code="position_create"
          title="新增岗位"
          icon-only
          :disabled="!selectedCategory || positionSaving || !canCreatePosition"
          @click="startCreatePosition"
        />
      </template>
      <UiSpin v-if="positionLoading" tip="加载岗位列表" />
      <UiError v-else-if="positionError" :message="positionError" />
      <UiEmpty v-else-if="!selectedCategory" description="请选择岗位分类" />
      <RecordListExplorer
        v-else
        :records="filteredPositions"
        :selected-id="selectedPosition?.id"
        :keyword="positionSearchKeyword"
        :empty-description="positionListEmptyDescription"
        :title-of="positionTitleOf"
        @select="selectPosition"
      />
    </RecordExplorerPanel>

    <main class="position-column detail-column">
      <header class="column-header">
        <div class="detail-title-group">
          <h2>{{ positionCardTitle }}</h2>
          <RecordStatusSwitch
            v-if="positionMode === 'create'"
            :enabled="positionDraft.enabled"
            :show-label="false"
            @change="positionDraft.enabled = $event"
          />
          <RecordStatusSwitch
            v-else-if="selectedPosition"
            :enabled="selectedPosition.enabled"
            :disabled="positionSaving || !canTogglePosition"
            :loading="positionSaving"
            :show-label="false"
            @change="togglePosition"
          />
        </div>
        <div class="detail-header-actions">
          <RecordActionBar
            :context="categoryContext"
            :actions="positionActions"
            @action="handlePositionAction"
          />
        </div>
      </header>
      <div v-if="positionError" class="message error">{{ positionError }}</div>
      <div v-else-if="positionMessage" class="message success">{{ positionMessage }}</div>
      <UiEmpty v-if="!selectedPosition && positionMode === 'view'" description="请选择或新建岗位" />
      <form v-else class="position-form" @submit.prevent="savePosition">
        <label>
          <span>所属分类</span>
          <UiSelect
            v-model:value="positionDraft.categoryId"
            :options="categoryOptions"
            :disabled="positionReadonly"
            :allow-clear="false"
            placeholder="选择岗位分类"
          />
        </label>
        <label>
          <span>岗位编码</span>
          <UiInput
            v-model:value="positionDraft.code"
            :disabled="positionReadonly"
            placeholder="请输入岗位编码"
          />
        </label>
        <label>
          <span>岗位名称</span>
          <UiInput
            v-model:value="positionDraft.title"
            :disabled="positionReadonly"
            placeholder="请输入岗位名称"
          />
        </label>
        <label class="full-row">
          <span>说明</span>
          <UiInput v-model:value="positionDraft.description" :disabled="positionReadonly" />
        </label>
      </form>
      <RecordMetaSection
        v-if="selectedPosition || positionMode !== 'view'"
        :record="positionDraft"
        show-sort-order
      />
    </main>
  </section>
</template>

<style scoped>
.position-workspace {
  display: grid;
  grid-template-columns: minmax(260px, 320px) minmax(280px, 360px) minmax(420px, 1fr);
  gap: 12px;
  min-height: calc(100vh - 116px);
}

.position-column {
  display: grid;
  align-content: start;
  min-width: 0;
  min-height: 0;
  border: 1px solid var(--muyun-border);
  border-radius: 8px;
  background: var(--muyun-surface);
}

.detail-column {
  gap: 12px;
  padding: 14px;
}

.category-column,
.list-column {
  min-height: 0;
}

.column-header {
  display: flex;
  align-items: center;
}

.column-header {
  justify-content: space-between;
  gap: 12px;
}

.column-header > div:first-child {
  min-width: 0;
}

.column-header h2 {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-header-actions {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 10px;
}

.detail-title-group {
  display: inline-flex;
  flex: 1 1 auto;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.record-panel-create-button {
  width: 28px;
  height: 28px;
  padding: 0;
  border-radius: 999px;
}

.column-header p,
h2,
h3 {
  margin: 0;
}

.column-header p {
  color: var(--muyun-text-muted);
  font-size: 12px;
  font-weight: 700;
}

h2 {
  color: var(--muyun-text);
  font-size: 16px;
}

h3 {
  color: var(--muyun-text);
  font-size: 14px;
}

.category-editor-panel {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 3;
  display: grid;
  align-content: start;
  gap: 10px;
  max-height: min(420px, 62%);
  min-height: 0;
  padding: 12px;
  border: 1px solid var(--muyun-border);
  border-top: 1px solid var(--muyun-border-subtle);
  border-radius: 8px 8px 0 0;
  background: var(--muyun-surface);
  box-shadow:
    0 -1px 0 rgb(15 23 42 / 4%),
    0 -12px 28px rgb(15 23 42 / 12%);
  overflow: auto;
}

.category-editor-drawer-enter-active,
.category-editor-drawer-leave-active {
  transition:
    transform 0.18s ease,
    opacity 0.18s ease;
}

.category-editor-drawer-enter-from,
.category-editor-drawer-leave-to {
  opacity: 0;
  transform: translateY(100%);
}

.category-editor-drawer-enter-to,
.category-editor-drawer-leave-from {
  opacity: 1;
  transform: translateY(0);
}

.category-editor-header h3 {
  margin: 0;
}

.category-editor-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-width: 0;
}

.category-editor-header h3 {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.category-editor-header :deep(.record-action-bar) {
  flex: 0 0 auto;
}

.category-form {
  display: grid;
  gap: 12px;
}

.category-form label {
  display: grid;
  gap: 6px;
  color: var(--muyun-text-body);
  font-size: 13px;
}

.position-form {
  display: grid;
  gap: 12px;
}

.category-status-panel {
  padding-top: 10px;
  border-top: 1px solid var(--muyun-border-subtle);
}

.position-form label {
  display: grid;
  gap: 6px;
  color: var(--muyun-text-body);
  font-size: 13px;
}

.position-form {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.full-row {
  grid-column: 1 / -1;
}

.message {
  padding: 9px 10px;
  border-radius: 6px;
  font-size: 13px;
}

.message.error {
  border: 1px solid var(--muyun-danger-border);
  background: var(--muyun-danger-bg);
  color: var(--muyun-danger-text);
}

.message.success {
  border: 1px solid var(--muyun-success-border);
  background: var(--muyun-success-bg);
  color: var(--muyun-success-text);
}

@media (max-width: 1180px) {
  .position-workspace {
    grid-template-columns: 1fr;
  }

  .position-form {
    grid-template-columns: 1fr;
  }
}
</style>
