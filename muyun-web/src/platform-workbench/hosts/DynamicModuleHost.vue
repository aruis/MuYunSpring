<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import {
  RecordDetailPanel,
  RecordFormFields,
  RecordQueryListPanel,
  resolveRecordFormFields,
  type QueryListRecord,
  type RecordFormRecord,
} from '@muyun/platform-components';
import type { DynamicModulePageDescriptor, ResolvedViewDescriptor } from '@muyun/web-contracts';
import { useModuleContext } from '@muyun/web-core';

defineOptions({ name: 'DynamicModuleHost' });

const props = defineProps<{
  descriptor: DynamicModulePageDescriptor;
}>();

const context = useModuleContext<QueryListRecord>({
  moduleAlias: props.descriptor.target.moduleAlias,
});
const selectedRecord = ref<QueryListRecord>();
const formViewCode = ref<string>();
const formFields = ref(resolveRecordFormFields(undefined));

const title = computed(
  () => props.descriptor.title ?? context.runtime.snapshot()?.title ?? context.moduleAlias,
);
const detailTitle = computed(() => recordTitle(selectedRecord.value) ?? '记录详情');

onMounted(loadRuntimeForm);

async function loadRuntimeForm() {
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
    return;
  }
  selectedRecord.value = records[0];
}

function selectRecord(record: QueryListRecord) {
  selectedRecord.value = record;
}

function updateDraftField(fieldName: string, value: string | number | boolean | undefined) {
  if (!selectedRecord.value) {
    return;
  }
  selectedRecord.value = {
    ...selectedRecord.value,
    [fieldName]: value,
  };
}

function recordTitle(record: QueryListRecord | undefined) {
  const titleValue = record?.title ?? record?.name ?? record?.code ?? record?.id;
  return titleValue == null ? undefined : String(titleValue);
}
</script>

<template>
  <section class="dynamic-module-workspace">
    <RecordQueryListPanel
      class="dynamic-list"
      :context="context"
      :title="title"
      :selected-key="selectedRecord?.id"
      :standard-crud-actions="false"
      :standard-crud-row-actions="false"
      :ui-config-id="descriptor.target.defaultUiConfigId"
      :query-template-id="descriptor.target.defaultQueryTemplateId"
      quick-search-placeholder="搜索动态记录"
      empty-description="暂无动态记录"
      @loaded="handleLoaded"
      @select="selectRecord"
      @row-dblclick="selectRecord"
    />

    <RecordDetailPanel class="dynamic-detail" :title="detailTitle">
      <template #actions>
        <span v-if="formViewCode" class="view-code">{{ formViewCode }}</span>
      </template>
      <RecordFormFields
        v-if="selectedRecord"
        class="dynamic-form"
        :record="selectedRecord as RecordFormRecord"
        :fields="formFields"
        :disabled="true"
        @update:field="updateDraftField"
      />
      <p v-else class="empty-detail">请选择一条动态记录</p>
    </RecordDetailPanel>
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

@media (max-width: 1100px) {
  .dynamic-module-workspace {
    grid-template-columns: 1fr;
  }

  .dynamic-form {
    grid-template-columns: 1fr;
  }
}
</style>
