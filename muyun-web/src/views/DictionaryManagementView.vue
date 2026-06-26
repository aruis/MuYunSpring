<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import type { Application, DictionaryCategory, DictionaryItem } from '@muyun/web-contracts';
import {
  createStaticResourceTreeClient,
  useModuleContext,
  type ModuleContext,
  type StaticModuleTreeClient,
} from '@muyun/web-core';
import {
  ApplicationScopeSwitcher,
  EnabledSelect,
  ModuleActionButton,
  RecordActionBar,
  RecordDetailPanel,
  RecordExplorerPanel,
  RecordMetaSection,
  RecordPicker,
  RecordStatusSwitch,
  TreeRecordExplorer,
  parentRecordConstraints,
  presentPlatformError,
  type RecordActionItem,
  type RecordPickerRecord,
  type TreeRecordBase,
} from '@muyun/platform-components';
import { UiEmpty, UiInput, UiSelect, confirmAction, type UiRecordInlineAction } from '@muyun/vue-ui-antdv';
import {
  createDictionaryManagementState,
  dictionaryCategoryTitleOf,
  dictionaryItemTitleOf,
  isFolderCategory,
} from './dictionaryManagementState';

defineOptions({ name: 'DictionaryManagementView' });

const categoryContext = useModuleContext<DictionaryCategory>({ moduleAlias: 'platform.dictionary_category' });
const applicationContext = useModuleContext<Application>({ moduleAlias: 'platform.application' });
const categorySearchKeyword = ref('');
const itemSearchKeyword = ref('');
const applications = ref<Application[]>([]);
const applicationsLoading = ref(false);
const selectedApplicationAlias = ref<string>();

const itemClients = new Map<string, StaticModuleTreeClient<DictionaryItem>>();
const categoryClients = new Map<string, StaticModuleTreeClient<DictionaryCategory>>();
const {
  categoryReloadKey,
  itemReloadKey,
  selectedCategory,
  categoryDraft,
  categoryMode,
  categorySaving,
  selectedItem,
  itemDraft,
  itemMode,
  itemSaving,
  selectedCategoryIsDictionary,
  canCreateCategory,
  canDeleteCategory,
  canToggleCategory,
  canTreeItem,
  canCreateItem,
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
  selectItem,
  startCreateItem,
  startCreateChildItem,
  startEditItem,
  cancelItemEdit,
  saveItem,
  toggleItem,
  deleteItem,
  resetForApplication,
} = createDictionaryManagementState(
  categoryContext,
  categoryClientOf,
  itemClientOf,
  () => selectedApplicationAlias.value,
  confirmAction,
);

const categoryEditorVisible = computed(() => categoryMode.value !== 'view');
const applicationOptions = computed(() =>
  applications.value.map((application) => ({
    label: application.title ?? application.alias ?? application.id ?? '未命名应用',
    value: application.alias ?? application.id ?? '',
    disabled: application.enabled === false,
  })),
);
const categoryExplorerContext = computed(() => scopedCategoryContext(selectedApplicationAlias.value));
const itemExplorerContext = computed(() => scopedItemContext(selectedCategory.value?.id));
const itemListEmptyDescription = computed(() =>
  itemSearchKeyword.value.trim() ? '没有匹配的字典项' : '当前类目暂无字典项',
);
const categoryKindOptions = [
  { label: '字典', value: 'DICTIONARY' },
  { label: '目录', value: 'FOLDER' },
];
const categoryActions = computed<RecordActionItem[]>(() => {
  if (categoryMode.value !== 'view') {
    return [
      { key: 'category-cancel', title: '取消', disabled: categorySaving.value },
      {
        key: 'category-save',
        actionCode: categoryMode.value.startsWith('create') ? 'create' : 'update',
        title: categorySaving.value ? '保存中' : '保存',
        primary: true,
        loading: categorySaving.value,
      },
    ];
  }
  return [
    { key: 'category-edit', actionCode: 'update', title: '编辑', disabled: !selectedCategory.value },
    {
      key: 'category-delete',
      actionCode: 'delete',
      title: '删除',
      danger: true,
      disabled: !selectedCategory.value || !canDeleteCategory.value,
      loading: categorySaving.value,
    },
  ];
});

const itemActions = computed<RecordActionItem[]>(() => {
  if (itemMode.value !== 'view') {
    return [
      { key: 'item-cancel', title: '取消', disabled: itemSaving.value },
      {
        key: 'item-save',
        actionCode: itemMode.value === 'create' ? 'item_create' : 'item_update',
        title: itemSaving.value ? '保存中' : '保存',
        primary: true,
        loading: itemSaving.value,
      },
    ];
  }
  return [
    { key: 'item-edit', actionCode: 'item_update', title: '编辑', disabled: !selectedItem.value },
    {
      key: 'item-delete',
      actionCode: 'item_delete',
      title: '删除',
      danger: true,
      disabled: !selectedItem.value || !canDeleteItem.value,
      loading: itemSaving.value,
    },
  ];
});

watch(selectedApplicationAlias, () => {
  categorySearchKeyword.value = '';
  itemSearchKeyword.value = '';
  resetForApplication();
  categoryReloadKey.value += 1;
});

onMounted(loadApplications);

async function loadApplications() {
  applicationsLoading.value = true;
  try {
    await applicationContext.runtime.ready;
    const response = await applicationContext.abilities.crud().query({ page: { pageNum: 1, pageSize: 200 } });
    applications.value = response.records;
    if (!selectedApplicationAlias.value) {
      const first =
        response.records.find((application) => application.enabled !== false) ?? response.records[0];
      selectedApplicationAlias.value = first?.alias ?? first?.id;
    }
  } catch (cause) {
    presentPlatformError(cause, { source: 'dictionary-application-load', phase: 'load' });
  } finally {
    applicationsLoading.value = false;
  }
}

function categoryClientOf() {
  const applicationAlias = selectedApplicationAlias.value;
  if (!applicationAlias) {
    throw new Error('Dictionary category client requires selected application');
  }
  const cached = categoryClients.get(applicationAlias);
  if (cached) {
    return cached;
  }
  const client = createStaticResourceTreeClient<DictionaryCategory>(
    categoryContext.http,
    `/platform.application/${encodeURIComponent(applicationAlias)}/dictionary-categories`,
  );
  categoryClients.set(applicationAlias, client);
  return client;
}

function scopedCategoryContext(applicationAlias: string | undefined): ModuleContext<DictionaryCategory> {
  const scopedClient = applicationAlias ? categoryClientOf() : fallbackCategoryClient();
  return {
    ...categoryContext,
    crud: scopedClient,
    abilities: {
      crud: () => scopedClient,
      tree: () => scopedClient,
      enable: () => scopedClient,
      tryCrud: () => scopedClient,
      tryTree: () => scopedClient,
      tryEnable: () => scopedClient,
      has: categoryContext.abilities.has,
      hasCrud: categoryContext.abilities.hasCrud,
      hasTree: categoryContext.abilities.hasTree,
      hasEnable: categoryContext.abilities.hasEnable,
    },
  };
}

function scopedItemContext(categoryId: string | undefined): ModuleContext<DictionaryItem> {
  const scopedClient = categoryId ? itemClientOf(categoryId) : fallbackItemClient();
  return {
    ...categoryContext,
    crud: scopedClient,
    abilities: {
      crud: () => scopedClient,
      tree: () => scopedClient,
      enable: () => scopedClient,
      tryCrud: () => scopedClient,
      tryTree: () => scopedClient,
      tryEnable: () => scopedClient,
      has: categoryContext.abilities.has,
      hasCrud: categoryContext.abilities.hasCrud,
      hasTree: categoryContext.abilities.hasTree,
      hasEnable: categoryContext.abilities.hasEnable,
    },
  };
}

function fallbackCategoryClient(): StaticModuleTreeClient<DictionaryCategory> {
  return {
    query: async () => emptyPage(),
    view: async () => ({}),
    insert: async (record) => record,
    update: async (_id, record) => record,
    delete: async () => ({ count: 0 }),
    enable: async () => ({ count: 0 }),
    disable: async () => ({ count: 0 }),
    tree: async () => ({ records: [] }),
    treeFlat: async () => ({ records: [] }),
    subtree: async () => ({ records: [] }),
    sort: async () => ({ count: 0 }),
  };
}

function fallbackItemClient(): StaticModuleTreeClient<DictionaryItem> {
  return {
    query: async () => emptyPage(),
    view: async () => ({}),
    insert: async (record) => record,
    update: async (_id, record) => record,
    delete: async () => ({ count: 0 }),
    enable: async () => ({ count: 0 }),
    disable: async () => ({ count: 0 }),
    tree: async () => ({ records: [] }),
    treeFlat: async () => ({ records: [] }),
    subtree: async () => ({ records: [] }),
    sort: async () => ({ count: 0 }),
  };
}

function emptyPage() {
  return {
    records: [],
    total: 0,
    pageNum: 1,
    pageSize: 20,
    pages: 0,
    totalKnown: true,
  };
}

function itemClientOf(categoryId: string) {
  const cached = itemClients.get(categoryId);
  if (cached) {
    return cached;
  }
  const client = createStaticResourceTreeClient<DictionaryItem>(
    categoryContext.http,
    `/platform.dictionary_category/categories/${encodeURIComponent(categoryId)}/items`,
  );
  itemClients.set(categoryId, client);
  return client;
}

function categoryTreeActionsOf(record: TreeRecordBase): UiRecordInlineAction[] {
  return [
    {
      key: 'create-child',
      title: '新增下级类目',
      iconName: 'plus',
      disabled: !canCreateCategory.value || categorySaving.value,
    },
    {
      key: 'edit',
      title: '编辑类目',
      iconName: 'edit',
      disabled: categorySaving.value,
    },
    {
      key: 'delete',
      title: '删除类目',
      iconName: 'delete',
      danger: true,
      disabled: !record.id || !canDeleteCategory.value || categorySaving.value,
    },
  ];
}

function categoryTagOf(record: DictionaryCategory) {
  if (record.enabled === false) {
    return '停用';
  }
  return isFolderCategory(record) ? '目录' : undefined;
}

function categoryMatchesKeyword(record: DictionaryCategory, normalized: string) {
  return [dictionaryCategoryTitleOf(record), record.alias, record.applicationAlias, record.id].some((value) =>
    value?.toLowerCase().includes(normalized),
  );
}

function itemMatchesKeyword(record: DictionaryItem, normalized: string) {
  return [dictionaryItemTitleOf(record), record.code, record.id].some((value) =>
    value?.toLowerCase().includes(normalized),
  );
}

function itemPickerTitle(record: RecordPickerRecord) {
  return dictionaryItemTitleOf(record as DictionaryItem);
}

function itemTagOf(record: DictionaryItem) {
  return record.enabled === false ? '停用' : undefined;
}

function itemTreeActionsOf(record: TreeRecordBase): UiRecordInlineAction[] {
  return [
    {
      key: 'create-child',
      title: '新增下级字典项',
      iconName: 'plus',
      disabled: !selectedCategoryIsDictionary.value || !canCreateItem.value || itemSaving.value,
    },
    {
      key: 'edit',
      title: '编辑字典项',
      iconName: 'edit',
      disabled: itemSaving.value,
    },
    {
      key: 'delete',
      title: '删除字典项',
      iconName: 'delete',
      danger: true,
      disabled: !record.id || !canDeleteItem.value || itemSaving.value,
    },
  ];
}

function handleCategoryTreeAction(action: { key: string }, record: DictionaryCategory) {
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

function handleCategoryAction(action: RecordActionItem) {
  if (action.key === 'category-edit') {
    startEditCategory();
    return;
  }
  if (action.key === 'category-delete') {
    void deleteCategory();
    return;
  }
  if (action.key === 'category-cancel') {
    cancelCategoryEdit();
    return;
  }
  if (action.key === 'category-save') {
    void saveCategory();
  }
}

function handleItemAction(action: RecordActionItem) {
  if (action.key === 'item-edit') {
    startEditItem();
    return;
  }
  if (action.key === 'item-delete') {
    void deleteItem();
    return;
  }
  if (action.key === 'item-cancel') {
    cancelItemEdit();
    return;
  }
  if (action.key === 'item-save') {
    void saveItem();
  }
}

function handleItemTreeAction(action: { key: string }, record: DictionaryItem) {
  selectItem(record);
  if (action.key === 'create-child') {
    startCreateChildItem(record);
    return;
  }
  if (action.key === 'edit') {
    startEditItem();
    return;
  }
  if (action.key === 'delete') {
    void deleteItem();
  }
}
</script>

<template>
  <section class="dictionary-workspace">
    <RecordExplorerPanel
      v-model:search-keyword="categorySearchKeyword"
      class="dictionary-column category-column"
      title="字典类目"
      search-placeholder="搜索类目名称、alias 或 ID"
      @refresh="categoryReloadKey += 1"
    >
      <template #title-extra>
        <ApplicationScopeSwitcher
          v-model:value="selectedApplicationAlias"
          :options="applicationOptions"
          :disabled="applicationsLoading"
          placeholder="选择应用"
        />
      </template>
      <template #actions>
        <ModuleActionButton
          class="record-panel-create-button"
          :context="categoryContext"
          action-code="create"
          title="新增字典类目"
          icon-only
          :disabled="!selectedApplicationAlias || categorySaving"
          @click="startCreateRootCategory"
        />
      </template>
      <UiEmpty v-if="!applicationsLoading && applications.length === 0" description="暂无可用应用" />
      <TreeRecordExplorer
        v-else
        :context="categoryExplorerContext"
        :selected-id="selectedCategory?.id"
        :reload-key="categoryReloadKey"
        :keyword="categorySearchKeyword"
        search-mode="none"
        search-placeholder="搜索类目名称、alias 或 ID"
        empty-description="暂无字典类目"
        loading-tip="加载字典类目"
        fallback-title="未命名字典类目"
        :title-of="dictionaryCategoryTitleOf"
        :actions-of="categoryTreeActionsOf"
        :filter-option="categoryMatchesKeyword"
        :tag-of="categoryTagOf"
        @loaded="handleCategoriesLoaded"
        @select="handleSelectCategory"
        @action="handleCategoryTreeAction"
      />
      <template #editor>
        <Transition name="category-editor-drawer">
          <section v-if="categoryEditorVisible" class="category-editor-panel">
            <header class="category-editor-header">
              <h3>{{ categoryEditorTitle }}</h3>
              <RecordActionBar
                :context="categoryContext"
                :actions="categoryActions"
                size="compact"
                @action="handleCategoryAction"
              />
            </header>
            <form class="category-form" @submit.prevent="saveCategory">
              <label>
                <span>类目 alias</span>
                <UiInput v-model:value="categoryDraft.alias" :disabled="categoryReadonly" />
              </label>
              <label>
                <span>类目名称</span>
                <UiInput v-model:value="categoryDraft.title" :disabled="categoryReadonly" />
              </label>
              <label>
                <span>类目类型</span>
                <UiSelect
                  v-model:value="categoryDraft.categoryKind"
                  :options="categoryKindOptions"
                  :disabled="categoryReadonly"
                  :allow-clear="false"
                />
              </label>
              <label>
                <span>启用状态</span>
                <EnabledSelect v-model:value="categoryDraft.enabled" :disabled="categoryReadonly" />
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
      v-model:search-keyword="itemSearchKeyword"
      class="dictionary-column list-column"
      title="字典项"
      search-placeholder="搜索字典项名称、编码或 ID"
      @refresh="itemReloadKey += 1"
    >
      <template #actions>
        <ModuleActionButton
          class="record-panel-create-button"
          :context="categoryContext"
          action-code="item_create"
          title="新增字典项"
          icon-only
          :disabled="!selectedCategoryIsDictionary || itemSaving || !canCreateItem"
          @click="startCreateItem"
        />
      </template>
      <UiEmpty v-if="!selectedCategory" description="请选择字典类目" />
      <UiEmpty v-else-if="!selectedCategoryIsDictionary" description="目录类目不维护字典项" />
      <UiEmpty v-else-if="!canTreeItem" description="当前用户无权查看字典项" />
      <TreeRecordExplorer
        v-else
        :context="itemExplorerContext"
        :selected-id="selectedItem?.id"
        :reload-key="itemReloadKey"
        :keyword="itemSearchKeyword"
        search-mode="none"
        search-placeholder="搜索字典项名称、编码或 ID"
        :empty-description="itemListEmptyDescription"
        loading-tip="加载字典项"
        fallback-title="未命名字典项"
        :title-of="dictionaryItemTitleOf"
        :actions-of="itemTreeActionsOf"
        :filter-option="itemMatchesKeyword"
        :tag-of="itemTagOf"
        :muted-of="(record) => record.enabled === false"
        @loaded="handleItemsLoaded"
        @select="selectItem"
        @action="handleItemTreeAction"
      />
    </RecordExplorerPanel>

    <RecordDetailPanel class="dictionary-column" :title="itemCardTitle">
      <template #status>
        <RecordStatusSwitch
          v-if="itemMode === 'create'"
          :enabled="itemDraft.enabled"
          :show-label="false"
          @change="itemDraft.enabled = $event"
        />
        <RecordStatusSwitch
          v-else-if="selectedItem"
          :enabled="selectedItem.enabled"
          :disabled="itemSaving || !canToggleItem"
          :loading="itemSaving"
          :show-label="false"
          @change="toggleItem"
        />
      </template>
      <template #actions>
        <RecordActionBar :context="categoryContext" :actions="itemActions" @action="handleItemAction" />
      </template>
      <UiEmpty v-if="!selectedItem && itemMode === 'view'" description="请选择或新建字典项" />
      <form v-else class="item-form" @submit.prevent="saveItem">
        <label>
          <span>所属类目</span>
          <UiInput :value="selectedCategory ? dictionaryCategoryTitleOf(selectedCategory) : ''" disabled />
        </label>
        <label>
          <span>字典项编码</span>
          <UiInput v-model:value="itemDraft.code" :disabled="itemReadonly" placeholder="请输入字典项编码" />
        </label>
        <label>
          <span>字典项名称</span>
          <UiInput v-model:value="itemDraft.title" :disabled="itemReadonly" placeholder="请输入字典项名称" />
        </label>
        <label>
          <span>上级字典项</span>
          <RecordPicker
            v-if="canTreeItem"
            v-model:value="itemDraft.parentId"
            :context="itemExplorerContext"
            :reload-key="itemReloadKey"
            :disabled="itemReadonly"
            :constraints="parentRecordConstraints(itemDraft.id)"
            :title-of="itemPickerTitle"
            placeholder="根字典项留空"
          />
          <UiInput v-else :value="itemDraft.parentId ?? ''" disabled placeholder="无权查看上级字典项" />
        </label>
      </form>
      <RecordMetaSection v-if="selectedItem || itemMode !== 'view'" :record="itemDraft" show-sort-order />
    </RecordDetailPanel>
  </section>
</template>

<style scoped>
.dictionary-workspace {
  display: grid;
  grid-template-columns: minmax(260px, 320px) minmax(280px, 360px) minmax(420px, 1fr);
  gap: 12px;
  min-height: calc(100vh - 116px);
}

.dictionary-column {
  display: grid;
  align-content: start;
  min-width: 0;
  min-height: 0;
  border: 1px solid var(--muyun-border);
  border-radius: 8px;
  background: var(--muyun-surface);
}

.category-column,
.list-column {
  min-height: 0;
}

.category-editor-header {
  display: flex;
  align-items: center;
}

.category-editor-header {
  justify-content: space-between;
  gap: 12px;
}

.record-panel-create-button {
  width: 28px;
  height: 28px;
  padding: 0;
  border-radius: 999px;
}

h2,
h3 {
  margin: 0;
  color: var(--muyun-text);
}

h2 {
  overflow: hidden;
  font-size: 16px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

h3 {
  overflow: hidden;
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
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
  max-height: min(480px, 68%);
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

.category-editor-header :deep(.record-action-bar) {
  flex: 0 0 auto;
}

.category-form,
.item-form {
  display: grid;
  gap: 12px;
}

.item-form {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.category-form label,
.item-form label {
  display: grid;
  gap: 6px;
  color: var(--muyun-text-body);
  font-size: 13px;
}

.category-status-panel {
  padding-top: 10px;
  border-top: 1px solid var(--muyun-border-subtle);
}

@media (max-width: 1180px) {
  .dictionary-workspace {
    grid-template-columns: 1fr;
  }

  .item-form {
    grid-template-columns: 1fr;
  }
}
</style>
