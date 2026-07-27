<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import {
  CrudRecordListExplorer,
  type CrudRecordListBase,
  ModuleActionButton,
  RecordDetailDrawer,
  RecordActionBar,
  RecordMetaSection,
  presentPlatformError,
  RecycleBinPanel,
  RecordStatusSwitch,
  StaticManagementLayout,
  createSoftDeletedConflictErrorHandler,
  type RecordActionItem,
  type RecordExplorerItemDescriptor,
} from '@muyun/platform-components';
import type { Application, Tenant, TenantApplication, WebPageResponse } from '@muyun/web-contracts';
import { useModuleContext } from '@muyun/web-core';
import { confirmAction, UiButton, UiDataTable, UiInput } from '@muyun/vue-ui-antdv';
import type { UiDataTableColumn, UiDataTableRecord } from '@muyun/vue-ui-antdv';
import { createTenantManagementState } from './tenantManagementState';

defineOptions({ name: 'TenantManagementView' });

type TenantViewMode = 'list' | 'recycleBin';

const tenantContext = useModuleContext<Tenant>();
const applicationContext = useModuleContext<Application>({ moduleAlias: 'platform.application' });
const viewMode = ref<TenantViewMode>('list');
const explorerSearchKeyword = ref('');
const applications = ref<Application[]>([]);
const applicationsLoading = ref(false);
const tenantApplications = ref<TenantApplication[]>([]);
const tenantApplicationsLoading = ref(false);
const applicationConfigurationOpen = ref(false);
const applicationConfigurationSaving = ref(false);
const configuredApplicationAliases = ref<Set<string>>(new Set());
let tenantApplicationsLoadVersion = 0;
const {
  selected,
  draft,
  mode,
  reloadKey,
  saving,
  cardTitle,
  readonly,
  aliasReadonly,
  canDelete,
  canEnable,
  handleListLoaded,
  handleSelect,
  startCreate,
  startEdit,
  cancelEdit,
  save,
  toggleEnabled,
  removeSelected,
} = createTenantManagementState(tenantContext, confirmAction, {
  actionErrorHandlers: [
    createSoftDeletedConflictErrorHandler({
      resourceLabel: '租户',
      onNavigateToRecycleBin: () => switchToRecycleBin(),
    }),
  ],
});

const enabledReadonly = computed(() => false);
const tenantApplicationColumns: UiDataTableColumn[] = [{ key: 'applicationAlias', title: '已开通应用' }];
const tenantApplicationRows = computed(() => tenantApplications.value as unknown as UiDataTableRecord[]);
const applicationRows = computed(() => applications.value as unknown as UiDataTableRecord[]);
const applicationConfigurationSelection = computed(() => ({
  selectedRowKeys: [...configuredApplicationAliases.value],
  preserveSelectedRowKeys: true,
  disabledOf: (record: UiDataTableRecord) => applicationConfigurationSaving.value || record.alias === 'iam',
  onChange: (keys: (string | number)[]) => {
    configuredApplicationAliases.value = new Set(keys.map((key) => String(key)));
  },
}));
const applicationConfigurationColumns: UiDataTableColumn[] = [
  { key: 'title', title: '应用名称', width: 260 },
  { key: 'alias', title: '应用 alias', width: 220 },
];

watch(
  () => selected.value?.id,
  (tenantId) => {
    applicationConfigurationOpen.value = false;
    configuredApplicationAliases.value = new Set();
    void loadTenantApplications(tenantId);
  },
  { immediate: true },
);
onMounted(() => void loadApplications());

const cardActions = computed<RecordActionItem[]>(() => {
  if (mode.value !== 'view') {
    return [
      { key: 'cancel', title: '取消', disabled: saving.value },
      {
        key: 'save',
        actionCode: mode.value === 'create' ? 'create' : 'update',
        title: saving.value ? '保存中' : '保存',
        loading: saving.value,
        primary: true,
      },
    ];
  }
  return [
    { key: 'edit', actionCode: 'update', title: '编辑', disabled: !selected.value },
    {
      key: 'delete',
      actionCode: 'delete',
      title: '删除',
      disabled: !selected.value || !canDelete.value,
      loading: saving.value,
      danger: true,
    },
  ];
});

function tenantItemOf(record: CrudRecordListBase): RecordExplorerItemDescriptor {
  return {
    title: record.title ?? record.alias ?? record.id ?? '未命名租户',
    secondary: record.alias ?? record.id,
    muted: record.enabled === false,
  };
}

function handleLoaded(records: CrudRecordListBase[]) {
  handleListLoaded(records as Tenant[]);
}

function handleTenantSelect(record: CrudRecordListBase) {
  handleSelect(record as Tenant);
}

async function handleCardAction(action: RecordActionItem) {
  if (action.key === 'edit') return startEdit();
  if (action.key === 'delete') return removeSelected();
  if (action.key === 'cancel') return cancelEdit();
  if (action.key === 'save') await save();
}

async function loadApplications() {
  applicationsLoading.value = true;
  try {
    await applicationContext.runtime.ready;
    const response = await applicationContext.abilities.crud().query({ page: { pageNum: 1, pageSize: 200 } });
    applications.value = response.records.filter((application) => application.enabled !== false);
  } catch (cause) {
    presentPlatformError(cause, { source: 'tenant-management-applications', phase: 'load' });
  } finally {
    applicationsLoading.value = false;
  }
}

async function loadTenantApplications(tenantId?: string) {
  const loadVersion = ++tenantApplicationsLoadVersion;
  if (!tenantId) {
    tenantApplications.value = [];
    return;
  }
  tenantApplicationsLoading.value = true;
  try {
    const response = await tenantContext.http.request<WebPageResponse<TenantApplication>>({
      method: 'POST',
      path: `${tenantApplicationsPath(tenantId)}/query`,
      body: { page: { pageNum: 1, pageSize: 200 } },
    });
    if (loadVersion === tenantApplicationsLoadVersion && selected.value?.id === tenantId) {
      tenantApplications.value = response.records;
    }
  } catch (cause) {
    presentPlatformError(cause, { source: 'tenant-management-applications', phase: 'load' });
  } finally {
    if (loadVersion === tenantApplicationsLoadVersion) tenantApplicationsLoading.value = false;
  }
}

async function openApplicationConfiguration() {
  const tenantId = selected.value?.id;
  if (!tenantId) return;
  await Promise.all([loadApplications(), loadTenantApplications(tenantId)]);
  const activeApplicationAliases = new Set<string>();
  for (const application of applications.value) {
    if (application.alias) activeApplicationAliases.add(application.alias);
  }
  configuredApplicationAliases.value = new Set([
    'iam',
    ...tenantApplications.value
      .map((application) => application.applicationAlias)
      .filter((applicationAlias): applicationAlias is string => {
        return typeof applicationAlias === 'string' && activeApplicationAliases.has(applicationAlias);
      }),
  ]);
  applicationConfigurationOpen.value = true;
}

async function saveApplicationConfiguration() {
  const tenantId = selected.value?.id;
  if (!tenantId) return;
  applicationConfigurationSaving.value = true;
  try {
    await tenantContext.http.request<{ records: string[] }>({
      method: 'POST',
      path: `${tenantApplicationsPath(tenantId)}/configure`,
      body: { applicationAliases: [...configuredApplicationAliases.value] },
    });
    await loadTenantApplications(tenantId);
    applicationConfigurationOpen.value = false;
  } catch (cause) {
    presentPlatformError(cause, { source: 'tenant-management-applications', phase: 'action' });
  } finally {
    applicationConfigurationSaving.value = false;
  }
}

function closeApplicationConfiguration() {
  if (!applicationConfigurationSaving.value) applicationConfigurationOpen.value = false;
}

function tenantApplicationsPath(tenantId: string) {
  return `/iam.tenant/${encodeURIComponent(tenantId)}/applications`;
}

function tenantRecordTitle(record: unknown): string {
  const tenant = record as Tenant;
  return tenant.title ?? tenant.alias ?? tenant.id ?? '未命名租户';
}

function switchToRecycleBin() {
  viewMode.value = 'recycleBin';
}

function switchToList() {
  viewMode.value = 'list';
  reloadKey.value += 1;
}
</script>

<template>
  <StaticManagementLayout
    v-model:explorer-search-keyword="explorerSearchKeyword"
    :explorer-title="viewMode === 'list' ? '租户列表' : '回收站'"
    :refresh-title="viewMode === 'list' ? '刷新租户列表' : '刷新回收站'"
    explorer-search-placeholder="搜索租户名称、alias 或 ID"
    :explorer-searchable="viewMode === 'list'"
    :mode="mode"
    :detail-title="viewMode === 'list' ? cardTitle : '回收站'"
    @refresh="viewMode === 'list' ? (reloadKey += 1) : undefined"
  >
    <template #explorer-actions>
      <UiButton
        v-if="viewMode === 'list'"
        icon-name="delete"
        type="text"
        title="回收站"
        @click="switchToRecycleBin"
      />
      <UiButton v-else type="text" title="返回租户列表" @click="switchToList"> 返回列表 </UiButton>
      <ModuleActionButton
        v-if="viewMode === 'list'"
        class="record-panel-create-button"
        :context="tenantContext"
        action-code="create"
        title="新建租户"
        icon-only
        @click="startCreate"
      />
    </template>
    <template #explorer>
      <CrudRecordListExplorer
        v-if="viewMode === 'list'"
        :context="tenantContext"
        :selected-id="selected?.id"
        :reload-key="reloadKey"
        :keyword="explorerSearchKeyword"
        empty-description="暂无租户"
        loading-tip="加载租户列表"
        fallback-title="未命名租户"
        :item-of="tenantItemOf"
        @select="handleTenantSelect"
        @loaded="handleLoaded"
      />
      <RecycleBinPanel
        v-else
        :context="tenantContext"
        :record-title="tenantRecordTitle"
        title="已删除租户"
        empty-description="回收站为空"
        @restored="switchToList"
        @purged="undefined"
      />
    </template>
    <template #detail-actions>
      <RecordActionBar
        v-if="viewMode === 'list'"
        :context="tenantContext"
        :actions="cardActions"
        @action="handleCardAction"
      />
    </template>
    <template #detail-status>
      <template v-if="viewMode === 'list'">
        <RecordStatusSwitch
          v-if="mode !== 'view'"
          :enabled="draft.enabled"
          :disabled="enabledReadonly"
          :show-label="false"
          @change="draft.enabled = $event"
        />
        <RecordStatusSwitch
          v-else-if="selected"
          :enabled="selected.enabled"
          :disabled="saving || !canEnable"
          :loading="saving"
          :show-label="false"
          @change="toggleEnabled"
        />
      </template>
    </template>

    <template v-if="viewMode === 'recycleBin'">
      <div class="recycle-bin-detail-hint">
        <p>回收站中的租户可以恢复或彻底删除。</p>
        <p>恢复后租户及其关联资源将回到正常状态；彻底删除后数据不可恢复。</p>
      </div>
    </template>
    <template v-else>
      <form class="static-record-form" @submit.prevent="save">
        <label
          ><span>租户 alias</span><UiInput v-model:value="draft.alias" :disabled="aliasReadonly"
        /></label>
        <label><span>租户名称</span><UiInput v-model:value="draft.title" :disabled="readonly" /></label>
      </form>

      <section v-if="selected && mode === 'view'" class="tenant-applications">
        <div class="tenant-applications-header">
          <div>
            <h3>已开通应用</h3>
            <p>应用是否可用以“是否开通”为准，不再维护租户侧启停状态。</p>
          </div>
          <UiButton type="primary" :loading="applicationsLoading" @click="openApplicationConfiguration">
            配置应用
          </UiButton>
        </div>
        <UiDataTable
          :columns="tenantApplicationColumns"
          :rows="tenantApplicationRows"
          :loading="tenantApplicationsLoading"
          :pagination="false"
          empty-description="暂未开通应用"
        />
      </section>
      <RecordMetaSection :record="draft" show-sort-order />
    </template>
  </StaticManagementLayout>

  <RecordDetailDrawer
    :open="applicationConfigurationOpen"
    title="配置应用"
    close-title="取消"
    @close="closeApplicationConfiguration"
  >
    <template #operation>
      <UiButton :disabled="applicationConfigurationSaving" @click="closeApplicationConfiguration">
        取消
      </UiButton>
      <UiButton
        type="primary"
        :loading="applicationConfigurationSaving"
        @click="saveApplicationConfiguration"
      >
        确认
      </UiButton>
    </template>
    <section class="tenant-application-configuration">
      <p>勾选表示向当前租户开通应用；取消勾选将移除该租户的应用开通记录。</p>
      <UiDataTable
        :columns="applicationConfigurationColumns"
        :rows="applicationRows"
        :loading="applicationsLoading"
        :pagination="false"
        :selection="applicationConfigurationSelection"
        :row-key="(record) => String(record.alias ?? record.id ?? '')"
        empty-description="暂无可配置应用"
      />
    </section>
  </RecordDetailDrawer>
</template>

<style scoped>
.recycle-bin-detail-hint {
  display: grid;
  gap: 8px;
  padding: 16px;
  border: 1px solid var(--muyun-border);
  border-radius: 6px;
  background: var(--muyun-hover-subtle);
}
.recycle-bin-detail-hint p {
  margin: 0;
  color: var(--muyun-text-muted);
  font-size: 13px;
  line-height: 1.6;
}
.tenant-applications {
  display: grid;
  gap: 12px;
  margin-top: 20px;
}
.tenant-applications-header {
  display: flex;
  align-items: center;
  gap: 10px;
}
.tenant-applications-header {
  justify-content: space-between;
}
.tenant-applications h3,
.tenant-applications p {
  margin: 0;
}
.tenant-applications p {
  margin-top: 4px;
  color: var(--ui-text-muted, #8c8c8c);
  font-size: 13px;
}
.tenant-application-configuration {
  display: grid;
  gap: 12px;
}
.tenant-application-configuration p {
  margin: 0;
  color: var(--ui-text-muted, #8c8c8c);
}
</style>
