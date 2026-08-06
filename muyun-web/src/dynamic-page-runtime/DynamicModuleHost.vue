<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import {
  RecordDetailPanel,
  RecordFormFields,
  RecordQueryListPanel,
  confirmAction,
  resolveRecordFormFields,
  type QueryListRecord,
  type RecordFormRecord,
} from '@muyun/platform-components';
import type { DynamicModulePageDescriptor, MenuPageMode, ResolvedViewDescriptor } from '@muyun/web-contracts';
import { useModuleContext } from '@muyun/web-core';

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
    if (editorMode.value === 'view') {
      editingRecord.value = selectedRecord.value;
    }
    return;
  }
  selectedRecord.value = records[0];
  editingRecord.value = records[0];
}

function selectRecord(record: QueryListRecord) {
  selectedRecord.value = record;
  editingRecord.value = record;
  editorMode.value = 'view';
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
  editingRecord.value = { enabled: true };
  editorMode.value = 'create';
}

async function editRecord(record: QueryListRecord) {
  const id = record.id == null ? undefined : String(record.id);
  if (!id) return;
  editingRecord.value = await context.crud.view(id);
  selectedRecord.value = editingRecord.value;
  editorMode.value = 'edit';
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
  if (action.key === 'edit') void editRecord(record);
  if (action.key === 'delete') void deleteRecord(record);
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
      @row-dblclick="selectRecord"
      @action="handleListAction"
      @row-action="handleRowAction"
    />

    <RecordDetailPanel class="dynamic-detail" :title="detailTitle">
      <template #actions>
        <span v-if="formViewCode" class="view-code">{{ formViewCode }}</span>
        <button
          v-if="selectedRecord && editorMode === 'view'"
          type="button"
          @click="editRecord(selectedRecord)"
        >
          编辑
        </button>
        <button v-if="editorMode !== 'view'" type="button" :disabled="saving" @click="saveRecord">
          {{ saving ? '保存中' : '保存' }}
        </button>
      </template>
      <RecordFormFields
        v-if="editingRecord"
        class="dynamic-form"
        :record="editingRecord as RecordFormRecord"
        :fields="formFields"
        :option-context="context"
        :disabled="editorMode === 'view'"
        @update:field="updateDraftField"
      />
      <p v-else class="empty-detail">请选择一条动态记录</p>
    </RecordDetailPanel>
  </section>
  <section v-else class="dynamic-module-unsupported">
    <h2>{{ title }}</h2>
    <p>{{ unsupportedPageModeText }}</p>
  </section>
</template>

<style scoped>
.dynamic-module-workspace {
  display: grid;
  grid-template-columns: minmax(420px, 1.25fr) minmax(320px, 0.75fr);
  gap: 12px;
  min-height: calc(100vh - 116px);
}

.dynamic-list,
.dynamic-detail {
  min-width: 0;
}

.dynamic-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.view-code {
  color: #64748b;
  font-size: 12px;
}

.empty-detail {
  margin: 0;
  color: #64748b;
  font-size: 13px;
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

@media (max-width: 1100px) {
  .dynamic-module-workspace {
    grid-template-columns: 1fr;
  }

  .dynamic-form {
    grid-template-columns: 1fr;
  }
}
</style>
