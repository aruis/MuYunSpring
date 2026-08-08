<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import {
  ManagementExplorerColumn,
  ManagementWorkspace,
  CrudRecordListExplorer,
  ModuleActionButton,
  RecordDetailPanel,
  RecordDetailFields,
  RecordExplorerPanel,
  RecordFormFields,
  RecordMetaSection,
  RecordModeDrawer,
  RecordPanelButton,
  RecordPanelState,
  RecordQueryListPanel,
  RecordStatusSwitch,
  TreeRecordExplorer,
  confirmAction,
  parentRecordConstraints,
  providePageLayout,
  resolveRecordFormFields,
  type RecordFormFieldPickerConfig,
  type RecordActionItem,
  type QueryListRecord,
  type RecordFormRecord,
} from '@muyun/platform-components';
import type {
  DynamicModulePageDescriptor,
  MenuPageMode,
  ResolvedScopedListWorkspaceDescriptor,
  ResolvedViewDescriptor,
} from '@muyun/web-contracts';
import { createModuleContext, useModuleContext, type ModuleContext } from '@muyun/web-core';
import {
  canMutateDynamicModuleDetail,
  shouldCommitDynamicModuleDetailRequest,
} from './dynamicModuleDetailStateModel';

/**
 * Descriptor-driven CRUD runner shared by static and dynamic modules.
 *
 * The `DynamicModuleHost` name remains the compatibility counterpart of the
 * persisted `dynamic-module-host` page descriptor. It does not imply a
 * dynamic-only UI path.
 */
defineOptions({ name: 'DynamicModuleHost' });

const props = defineProps<{
  descriptor: DynamicModulePageDescriptor;
}>();

const context = useModuleContext<QueryListRecord>({
  moduleAlias: props.descriptor.target.moduleAlias,
});
const selectedRecord = ref<QueryListRecord>();
const editingRecord = ref<QueryListRecord>();
const editorMode = ref<'create' | 'edit' | 'view'>('view');
const detailOpen = ref(false);
const formFields = ref(resolveRecordFormFields(undefined));
const reloadKey = ref(0);
const treeReloadKey = ref(0);
const selectedTreeRecord = ref<QueryListRecord>();
const treeSearchKeyword = ref('');
const treeModule = ref(false);
const scopedListWorkspace = ref<ResolvedScopedListWorkspaceDescriptor>();
const selectedScopeRecord = ref<QueryListRecord>();
const scopeSearchKeyword = ref('');
const scopeReloadKey = ref(0);
const scopeTree = ref(false);
const saving = ref(false);
const togglingEnabled = ref(false);
const detailLoading = ref(false);
const detailLoadFailed = ref(false);
let detailLoadSequence = 0;

const title = computed(
  () => props.descriptor.title ?? context.runtime.snapshot()?.title ?? context.moduleAlias,
);
const detailTitle = computed(() => {
  if (editorMode.value === 'create') return `新建${title.value}`;
  return recordTitle(editingRecord.value ?? selectedRecord.value) ?? '记录详情';
});
const pageMode = computed<MenuPageMode>(() => props.descriptor.target.pageMode ?? 'LIST');
const isListPage = computed(() => pageMode.value === 'LIST');
const listUiConfigId = computed(() =>
  isListPage.value ? props.descriptor.target.defaultUiConfigId : undefined,
);
const unsupportedPageModeText = computed(() => `动态${pageMode.value}入口暂未接入运行器`);
// Tree modules are discovered from runtime metadata. Once discovered, their
// explorer/detail panes own the constrained work area instead of extending the
// workbench tab's document flow.
providePageLayout(
  computed(() => (treeModule.value || scopedListWorkspace.value ? 'workspace' : props.descriptor.layout)),
);
const scopeContext = computed<ModuleContext<QueryListRecord> | undefined>(() => {
  const workspace = scopedListWorkspace.value;
  return workspace
    ? createModuleContext({ http: context.http, moduleAlias: workspace.scopeModuleAlias })
    : undefined;
});
const scopeSelectionRequired = computed(() => scopedListWorkspace.value?.createPolicy === 'REQUIRE_SCOPE');
const canCreateRecord = computed(
  () => !scopeSelectionRequired.value || selectedScopeRecord.value?.id != null,
);
const scopedExternalQueryValues = computed<Record<string, unknown> | undefined>(() => {
  const workspace = scopedListWorkspace.value;
  const id = selectedScopeRecord.value?.id;
  return workspace && id != null ? { [workspace.queryCriteriaKey]: id } : undefined;
});
const scopedListActions = computed<RecordActionItem[]>(() => [
  {
    key: 'create',
    actionCode: 'create',
    title: '新建',
    primary: true,
    disabled: !canCreateRecord.value,
  },
]);
const canToggleEnabled = computed(() => {
  const record = selectedRecord.value;
  if (
    !record?.id ||
    editorMode.value !== 'view' ||
    detailLoading.value ||
    detailLoadFailed.value ||
    togglingEnabled.value
  ) {
    return false;
  }
  return context.can(record.enabled === false ? 'enable' : 'disable') === true;
});
const treeParentPickerConfigs = computed<Record<string, RecordFormFieldPickerConfig>>(() => {
  if (!treeModule.value || !formFields.value.has('parentId')) {
    return {} as Record<string, RecordFormFieldPickerConfig>;
  }
  return {
    parentId: {
      context,
      mode: 'tree',
      placeholder: '根标签留空',
      allowClear: true,
      constraints: parentRecordConstraints(
        editingRecord.value?.id == null ? undefined : String(editingRecord.value.id),
      ),
    },
  };
});
const referencePickerConfigs = computed<Record<string, RecordFormFieldPickerConfig>>(() => {
  const configs: Record<string, RecordFormFieldPickerConfig> = { ...treeParentPickerConfigs.value };
  for (const field of formFields.value.values()) {
    const reference = field.reference;
    if (!reference) {
      continue;
    }
    configs[field.fieldRef.fieldName] = {
      context: createModuleContext({ http: context.http, moduleAlias: reference.targetModuleAlias }),
      mode: 'tree',
      allowClear: !field.required?.constant,
    };
  }
  return configs;
});

onMounted(loadRuntimeForm);

async function loadRuntimeForm() {
  if (!isListPage.value) {
    return;
  }
  const runtimeContext = await context.runtime.ready;
  treeModule.value = context.abilities.hasTree() === true;
  scopedListWorkspace.value = scopedListWorkspaceFor(runtimeContext.uiDescriptor?.views ?? []);
  scopeTree.value = false;
  if (scopeContext.value) {
    await scopeContext.value.runtime.ready;
    scopeTree.value = scopeContext.value.abilities.hasTree() === true;
  }
  const view = defaultFormView(runtimeContext.uiDescriptor?.views ?? []);
  formFields.value = resolveRecordFormFields(runtimeContext.uiDescriptor, view?.viewCode);
}

function defaultFormView(views: ResolvedViewDescriptor[]) {
  return views.find((view) => view.viewKind === 'FORM');
}

function scopedListWorkspaceFor(
  views: ResolvedViewDescriptor[],
): ResolvedScopedListWorkspaceDescriptor | undefined {
  const listViews = views.filter((view) => view.viewKind === 'LIST');
  const configuredList =
    listUiConfigId.value == null
      ? undefined
      : listViews.find((view) => view.sourceUiConfigId === listUiConfigId.value);
  if (configuredList) {
    return configuredList.scopedListWorkspace;
  }
  return listUiConfigId.value == null && listViews.length === 1
    ? listViews[0].scopedListWorkspace
    : undefined;
}

function handleLoaded(records: QueryListRecord[]) {
  if (selectedRecord.value) {
    selectedRecord.value =
      records.find((record) => record.id === selectedRecord.value?.id) ?? selectedRecord.value;
    if (detailOpen.value && !detailLoading.value && editorMode.value === 'view') {
      editingRecord.value = selectedRecord.value;
    }
  }
}

function selectRecord(record: QueryListRecord) {
  selectedRecord.value = record;
}

function selectScopeRecord(record: { id?: string }) {
  if (selectedScopeRecord.value?.id === record.id) {
    selectedScopeRecord.value = undefined;
    return;
  }
  selectedScopeRecord.value = record as QueryListRecord;
}

function selectTreeRecord(record: unknown) {
  selectedTreeRecord.value = record as QueryListRecord;
  void openRecord(selectedTreeRecord.value, 'view');
}

function handleTreeLoaded(records: unknown[]) {
  if (selectedTreeRecord.value || editorMode.value !== 'view') return;
  const firstRecord = records.at(0);
  if (firstRecord) selectTreeRecord(firstRecord);
}

async function openRecord(record: QueryListRecord, mode: 'edit' | 'view') {
  const id = record.id == null ? undefined : String(record.id);
  if (!id) return;
  const requestSequence = ++detailLoadSequence;
  selectedRecord.value = record;
  editingRecord.value = undefined;
  editorMode.value = mode;
  detailOpen.value = true;
  detailLoading.value = true;
  detailLoadFailed.value = false;
  try {
    const detail = await context.crud.view(id);
    if (
      !shouldCommitDynamicModuleDetailRequest({ activeRequestSequence: detailLoadSequence, requestSequence })
    )
      return;
    editingRecord.value = detail;
    selectedRecord.value = detail;
  } catch {
    if (
      !shouldCommitDynamicModuleDetailRequest({ activeRequestSequence: detailLoadSequence, requestSequence })
    )
      return;
    editingRecord.value = undefined;
    detailLoadFailed.value = true;
  } finally {
    if (
      shouldCommitDynamicModuleDetailRequest({ activeRequestSequence: detailLoadSequence, requestSequence })
    ) {
      detailLoading.value = false;
    }
  }
}

function updateDraftField(
  fieldName: string,
  value: import('@muyun/platform-components').RecordFormFieldValue,
) {
  if (!editingRecord.value) {
    return;
  }
  editingRecord.value = {
    ...editingRecord.value,
    [fieldName]: value,
  };
}

function createRecord(parentId?: string) {
  if (scopeSelectionRequired.value && selectedScopeRecord.value?.id == null) return;
  detailLoadSequence += 1;
  detailLoading.value = false;
  detailLoadFailed.value = false;
  const workspace = scopedListWorkspace.value;
  editingRecord.value = parentId
    ? { parentId }
    : workspace && selectedScopeRecord.value?.id != null
      ? { [workspace.scopeField]: selectedScopeRecord.value.id }
      : {};
  editorMode.value = 'create';
  detailOpen.value = true;
}

function createRootRecord() {
  createRecord();
}

function createChildRecord() {
  const parentId = selectedRecord.value?.id == null ? undefined : String(selectedRecord.value.id);
  if (parentId) createRecord(parentId);
}

async function editRecord(record: QueryListRecord) {
  await openRecord(record, 'edit');
}

async function saveRecord() {
  const record = editingRecord.value;
  if (!record) return;
  if (
    !canMutateDynamicModuleDetail({
      hasRecord: true,
      saving: saving.value,
      loading: detailLoading.value,
      loadFailed: detailLoadFailed.value,
    })
  ) {
    return;
  }
  saving.value = true;
  try {
    const id = record.id == null ? undefined : String(record.id);
    const result =
      editorMode.value === 'edit' && id
        ? await context.crud.update(id, record)
        : await context.crud.insert(record);
    selectedRecord.value = result.record;
    if (treeModule.value) {
      selectedTreeRecord.value = result.record;
    }
    editingRecord.value = result.record;
    editorMode.value = 'view';
    reloadKey.value += 1;
    treeReloadKey.value += 1;
  } finally {
    saving.value = false;
  }
}

async function deleteRecord(record: QueryListRecord) {
  const id = record.id == null ? undefined : String(record.id);
  const version = typeof record.version === 'number' ? record.version : undefined;
  if (!id || version === undefined) return;
  if (
    !(await confirmAction({
      title: '删除记录',
      content: `确认删除「${recordTitle(record) ?? id}」？`,
      okText: '删除',
      danger: true,
    }))
  ) {
    return;
  }
  await context.crud.delete(id, { version });
  if (selectedRecord.value?.id === id) {
    selectedRecord.value = undefined;
    editingRecord.value = undefined;
    selectedTreeRecord.value = undefined;
  }
  reloadKey.value += 1;
  treeReloadKey.value += 1;
}

async function toggleEnabled() {
  const record = selectedRecord.value;
  const id = record?.id == null ? undefined : String(record.id);
  const version = typeof record?.version === 'number' ? record.version : undefined;
  if (!record || !id || version === undefined || !canToggleEnabled.value) return;

  togglingEnabled.value = true;
  try {
    if (record.enabled === false) {
      await context.crud.enable(id, { version });
    } else {
      await context.crud.disable(id, { version });
    }
    const refreshed = await context.crud.view(id);
    selectedRecord.value = refreshed;
    editingRecord.value = refreshed;
    reloadKey.value += 1;
    treeReloadKey.value += 1;
  } finally {
    togglingEnabled.value = false;
  }
}

function handleListAction(action: { key?: string }) {
  if (action.key === 'create') createRecord();
}

function handleRowAction(action: { key?: string }, record: QueryListRecord) {
  if (action.key === 'view') void openRecord(record, 'view');
  if (action.key === 'edit') void editRecord(record);
  if (action.key === 'delete') void deleteRecord(record);
}

function closeDetail() {
  if (saving.value) return;
  detailLoadSequence += 1;
  detailLoading.value = false;
  detailLoadFailed.value = false;
  detailOpen.value = false;
  editorMode.value = 'view';
  editingRecord.value = selectedRecord.value;
}

function closeTreeCardEditor() {
  if (saving.value) return;
  detailLoadSequence += 1;
  detailLoading.value = false;
  detailLoadFailed.value = false;
  detailOpen.value = false;
  editorMode.value = 'view';
  editingRecord.value = selectedRecord.value;
}

function retryLoadDetail() {
  const record = selectedRecord.value;
  if (!record || editorMode.value === 'create') return;
  void openRecord(record, editorMode.value);
}

function recordTitle(record: QueryListRecord | undefined) {
  const titleValue = record?.title ?? record?.name ?? record?.code ?? record?.id;
  return titleValue == null ? undefined : String(titleValue);
}
</script>

<template>
  <section
    v-if="isListPage"
    class="dynamic-module-workspace"
    :class="{ 'dynamic-module-workspace--tree': treeModule }"
  >
    <ManagementWorkspace v-if="scopedListWorkspace && scopeContext" class="dynamic-tree-workspace">
      <ManagementExplorerColumn>
        <RecordExplorerPanel
          :title="scopedListWorkspace.scopeTitle"
          :refresh-title="`刷新${scopedListWorkspace.scopeTitle}${scopeTree ? '树' : '列表'}`"
          :search-keyword="scopeSearchKeyword"
          :search-placeholder="scopedListWorkspace.scopeSearchPlaceholder"
          @update:search-keyword="scopeSearchKeyword = $event"
          @refresh="scopeReloadKey += 1"
        >
          <TreeRecordExplorer
            v-if="scopeTree"
            :context="scopeContext"
            :selected-id="selectedScopeRecord?.id == null ? undefined : String(selectedScopeRecord.id)"
            :reload-key="scopeReloadKey"
            :keyword="scopeSearchKeyword"
            search-mode="none"
            :empty-description="`暂无${scopedListWorkspace.scopeTitle}`"
            :secondary-of="scopedListWorkspace.showScopeItemSubtitle ? undefined : () => undefined"
            @select="selectScopeRecord"
          />
          <CrudRecordListExplorer
            v-else
            :context="scopeContext"
            :selected-id="selectedScopeRecord?.id == null ? undefined : String(selectedScopeRecord.id)"
            :reload-key="scopeReloadKey"
            :keyword="scopeSearchKeyword"
            :empty-description="`暂无${scopedListWorkspace.scopeTitle}`"
            :subtitle-of="scopedListWorkspace.showScopeItemSubtitle ? undefined : () => undefined"
            @select="selectScopeRecord"
          />
        </RecordExplorerPanel>
      </ManagementExplorerColumn>
      <RecordQueryListPanel
        class="dynamic-list"
        :context="context"
        :title="title"
        :selected-key="selectedRecord?.id"
        :reload-key="reloadKey"
        :actions="scopedListActions"
        :standard-crud-row-actions="true"
        :ui-config-id="listUiConfigId"
        :query-template-id="descriptor.target.defaultQueryTemplateId"
        :external-query-values="scopedExternalQueryValues"
        :required-external-criteria-keys="[scopedListWorkspace.queryCriteriaKey]"
        quick-search-placeholder="搜索动态记录"
        empty-description="暂无动态记录"
        @loaded="handleLoaded"
        @select="selectRecord"
        @row-dblclick="(record) => openRecord(record, 'view')"
        @action="handleListAction"
        @row-action="handleRowAction"
      />
    </ManagementWorkspace>

    <ManagementWorkspace v-else-if="treeModule" class="dynamic-tree-workspace">
      <ManagementExplorerColumn>
        <RecordExplorerPanel
          :title="`${title}树`"
          :refresh-title="`刷新${title}树`"
          :search-keyword="treeSearchKeyword"
          search-placeholder="搜索树节点"
          @update:search-keyword="treeSearchKeyword = $event"
          @refresh="treeReloadKey += 1"
        >
          <template #actions>
            <ModuleActionButton
              class="record-panel-create-button"
              :context="context"
              action-code="create"
              icon-only
              title="新建根节点"
              @click="createRootRecord"
            />
          </template>
          <TreeRecordExplorer
            :context="context"
            :selected-id="selectedTreeRecord?.id == null ? undefined : String(selectedTreeRecord.id)"
            :reload-key="treeReloadKey"
            :keyword="treeSearchKeyword"
            search-mode="none"
            search-trigger="external"
            empty-description="暂无记录"
            @select="selectTreeRecord"
            @loaded="handleTreeLoaded"
          />
        </RecordExplorerPanel>
      </ManagementExplorerColumn>

      <RecordDetailPanel class="dynamic-tree-card" :title="detailTitle">
        <template #actions>
          <template v-if="editorMode !== 'view'">
            <RecordPanelButton :disabled="saving" @click="closeTreeCardEditor">取消</RecordPanelButton>
            <RecordPanelButton
              type="primary"
              :loading="saving"
              :disabled="
                detailLoading ||
                detailLoadFailed ||
                context.can(editorMode === 'create' ? 'create' : 'update') !== true
              "
              @click="saveRecord"
            >
              {{ saving ? '保存中' : '保存' }}
            </RecordPanelButton>
          </template>
          <template v-else>
            <ModuleActionButton
              :context="context"
              action-code="create"
              :disabled="!selectedRecord"
              @click="createChildRecord"
            >
              新建子项
            </ModuleActionButton>
            <ModuleActionButton
              :context="context"
              action-code="update"
              :disabled="!selectedRecord"
              @click="selectedRecord && editRecord(selectedRecord)"
            >
              编辑
            </ModuleActionButton>
            <ModuleActionButton
              :context="context"
              action-code="delete"
              :loading="saving"
              danger
              :disabled="!selectedRecord"
              @click="selectedRecord && deleteRecord(selectedRecord)"
            >
              删除
            </ModuleActionButton>
          </template>
        </template>
        <template #status>
          <RecordStatusSwitch
            v-if="editorMode === 'view' && selectedRecord"
            :enabled="selectedRecord.enabled !== false"
            :disabled="!canToggleEnabled"
            :loading="togglingEnabled"
            :show-label="false"
            @change="toggleEnabled"
          />
        </template>

        <RecordPanelState
          v-if="!selectedRecord && editorMode === 'view'"
          description="请选择标签，或新建根标签"
        />
        <RecordPanelState v-else-if="detailLoading" loading loading-tip="加载记录详情" description="" />
        <RecordPanelState v-else-if="detailLoadFailed" description="详情加载失败，请重新选择标签" />
        <template v-else-if="editingRecord">
          <RecordDetailFields
            v-if="editorMode === 'view'"
            :record="editingRecord as RecordFormRecord"
            :fields="formFields"
            :exclude-field-names="['enabled']"
          />
          <RecordFormFields
            v-else
            class="dynamic-form"
            :record="editingRecord as RecordFormRecord"
            :fields="formFields"
            :option-context="context"
            :picker-configs="referencePickerConfigs"
            :exclude-field-names="['enabled']"
            @update:field="updateDraftField"
          />
          <RecordMetaSection v-if="editorMode !== 'create'" :record="editingRecord" show-sort-order />
        </template>
      </RecordDetailPanel>
    </ManagementWorkspace>

    <RecordQueryListPanel
      v-else
      class="dynamic-list"
      :context="context"
      :title="title"
      :selected-key="selectedRecord?.id"
      :reload-key="reloadKey"
      :standard-crud-actions="true"
      :standard-crud-row-actions="true"
      :ui-config-id="listUiConfigId"
      :query-template-id="descriptor.target.defaultQueryTemplateId"
      quick-search-placeholder="搜索动态记录"
      empty-description="暂无动态记录"
      @loaded="handleLoaded"
      @select="selectRecord"
      @row-dblclick="(record) => openRecord(record, 'view')"
      @action="handleListAction"
      @row-action="handleRowAction"
    />

    <RecordModeDrawer
      v-if="!treeModule"
      :open="detailOpen"
      :title="detailTitle"
      :mode="editorMode"
      :loading="detailLoading"
      :load-failed="detailLoadFailed"
      :edit-available="
        Boolean(selectedRecord) && !detailLoading && !detailLoadFailed && editorMode === 'view'
      "
      :save-available="!detailLoading && !detailLoadFailed && editorMode !== 'view'"
      :saving="saving"
      @close="closeDetail"
      @retry="retryLoadDetail"
      @edit="selectedRecord && editRecord(selectedRecord)"
      @save="saveRecord"
    >
      <template #status>
        <RecordStatusSwitch
          v-if="editorMode === 'view' && selectedRecord"
          :enabled="selectedRecord.enabled !== false"
          :disabled="!canToggleEnabled"
          :loading="togglingEnabled"
          :show-label="false"
          @change="toggleEnabled"
        />
      </template>
      <template #view>
        <RecordDetailFields
          v-if="editingRecord"
          :record="editingRecord as RecordFormRecord"
          :fields="formFields"
          :exclude-field-names="['enabled']"
        />
      </template>
      <template #form>
        <RecordFormFields
          v-if="editingRecord"
          class="dynamic-form"
          :record="editingRecord as RecordFormRecord"
          :fields="formFields"
          :option-context="context"
          :picker-configs="referencePickerConfigs"
          :exclude-field-names="['enabled']"
          @update:field="updateDraftField"
        />
      </template>
    </RecordModeDrawer>
  </section>
  <section v-else class="dynamic-module-unsupported">
    <h2>{{ title }}</h2>
    <p>{{ unsupportedPageModeText }}</p>
  </section>
</template>

<style scoped>
.dynamic-module-workspace {
  min-width: 0;
  min-height: calc(100vh - 116px);
}

/*
 * Tree metadata is loaded at runtime, so this boundary cannot be declared by
 * the menu descriptor. Keep the workbench tab fixed and let the explorer and
 * detail panels manage their own vertical scroll areas.
 */
.dynamic-module-workspace--tree {
  height: 100%;
  min-height: 0;
}

.dynamic-list {
  min-width: 0;
}

.dynamic-tree-workspace {
  height: 100%;
  min-height: 0;
}

.dynamic-tree-card {
  min-width: 0;
}

.dynamic-tree-workspace :deep(.record-panel-create-button) {
  width: 28px;
  height: 28px;
  padding: 0;
  border-radius: 999px;
}

.dynamic-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.dynamic-module-unsupported {
  display: grid;
  align-content: center;
  justify-items: center;
  min-height: calc(100vh - 116px);
  color: #64748b;
  text-align: center;
}

.dynamic-module-unsupported h2 {
  margin: 0 0 8px;
  color: #111827;
  font-size: 18px;
  font-weight: 600;
}

.dynamic-module-unsupported p {
  margin: 0;
  font-size: 13px;
}

@media (max-width: 720px) {
  .dynamic-module-workspace--tree {
    height: auto;
    min-height: calc(100vh - 116px);
  }

  .dynamic-tree-workspace {
    height: auto;
    min-height: 0;
  }

  .dynamic-form {
    grid-template-columns: 1fr;
  }
}
</style>
