<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import {
  RecordDetailFields,
  RecordFormFields,
  RecordModeDrawer,
  RecordQueryListPanel,
  confirmAction,
  resolveRecordFormFields,
  type QueryListRecord,
  type RecordFormRecord,
} from '@muyun/platform-components';
import { UiButton } from '@muyun/vue-ui-antdv';
import type { DynamicModulePageDescriptor, MenuPageMode, ResolvedViewDescriptor } from '@muyun/web-contracts';
import { useModuleContext } from '@muyun/web-core';

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
const formViewCode = ref<string>();
const formFields = ref(resolveRecordFormFields(undefined));
const reloadKey = ref(0);
const saving = ref(false);

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

onMounted(loadRuntimeForm);

async function loadRuntimeForm() {
  if (!isListPage.value) {
    return;
  }
  const runtimeContext = await context.runtime.ready;
  const view = defaultFormView(runtimeContext.uiDescriptor?.views ?? []);
  formViewCode.value = view?.viewCode;
  formFields.value = resolveRecordFormFields(runtimeContext.uiDescriptor, view?.viewCode);
}

function defaultFormView(views: ResolvedViewDescriptor[]) {
  return views.find((view) => view.viewKind === 'FORM');
}

function handleLoaded(records: QueryListRecord[]) {
  if (selectedRecord.value) {
    selectedRecord.value =
      records.find((record) => record.id === selectedRecord.value?.id) ?? selectedRecord.value;
    if (detailOpen.value && editorMode.value === 'view') {
      editingRecord.value = selectedRecord.value;
    }
  }
}

function selectRecord(record: QueryListRecord) {
  selectedRecord.value = record;
}

async function openRecord(record: QueryListRecord, mode: 'edit' | 'view') {
  const id = record.id == null ? undefined : String(record.id);
  if (!id) return;
  selectedRecord.value = record;
  editingRecord.value = record;
  editorMode.value = mode;
  detailOpen.value = true;
  editingRecord.value = await context.crud.view(id);
  selectedRecord.value = editingRecord.value;
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

function createRecord() {
  editingRecord.value = {};
  editorMode.value = 'create';
  detailOpen.value = true;
}

async function editRecord(record: QueryListRecord) {
  await openRecord(record, 'edit');
}

async function saveRecord() {
  const record = editingRecord.value;
  if (!record || saving.value) return;
  saving.value = true;
  try {
    const id = record.id == null ? undefined : String(record.id);
    const result =
      editorMode.value === 'edit' && id
        ? await context.crud.update(id, record)
        : await context.crud.insert(record);
    selectedRecord.value = result.record;
    editingRecord.value = result.record;
    editorMode.value = 'view';
    reloadKey.value += 1;
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
  }
  reloadKey.value += 1;
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
  detailOpen.value = false;
  editorMode.value = 'view';
  editingRecord.value = selectedRecord.value;
}

function recordTitle(record: QueryListRecord | undefined) {
  const titleValue = record?.title ?? record?.name ?? record?.code ?? record?.id;
  return titleValue == null ? undefined : String(titleValue);
}
</script>

<template>
  <section v-if="isListPage" class="dynamic-module-workspace">
    <RecordQueryListPanel
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
      :open="detailOpen"
      :title="detailTitle"
      :subtitle="formViewCode"
      :mode="editorMode"
      @close="closeDetail"
    >
      <template #view>
        <RecordDetailFields
          v-if="editingRecord"
          :record="editingRecord as RecordFormRecord"
          :fields="formFields"
        />
      </template>
      <template #form>
        <RecordFormFields
          v-if="editingRecord"
          class="dynamic-form"
          :record="editingRecord as RecordFormRecord"
          :fields="formFields"
          :option-context="context"
          @update:field="updateDraftField"
        />
      </template>
      <template #operation>
        <UiButton
          v-if="selectedRecord && editorMode === 'view'"
          icon-name="edit"
          @click="editRecord(selectedRecord)"
        >
          编辑
        </UiButton>
        <UiButton v-if="editorMode !== 'view'" type="primary" :loading="saving" @click="saveRecord">
          保存
        </UiButton>
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

.dynamic-list {
  min-width: 0;
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
  .dynamic-form {
    grid-template-columns: 1fr;
  }
}
</style>
