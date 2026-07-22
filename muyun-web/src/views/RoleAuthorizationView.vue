<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import {
  RecordExplorerPanel,
  RecordTreeSelector,
  handlePlatformActionSuccess,
  presentPlatformError,
} from '@muyun/platform-components';
import { UiButton, UiDataTable, UiEmpty, UiError, UiSelect, UiSpin, UiSwitch } from '@muyun/vue-ui-antdv';
import type { UiDataTableColumn, UiDataTableRecord } from '@muyun/vue-ui-antdv';
import type {
  DataScopePolicy,
  Role,
  RoleAuthorizationModule,
  RoleDataGrantActionMatrix,
  RoleDataScopePolicyCatalog,
  RolePermissionAction,
} from '@muyun/web-contracts';
import { useModuleContext } from '@muyun/web-core';
import { createRoleGrantClient } from './roleGrantClient';

defineOptions({ name: 'RoleAuthorizationView' });

const roleContext = useModuleContext<Role>({ moduleAlias: 'iam.role' });
const client = createRoleGrantClient(roleContext.http);
const roleId = new URLSearchParams(window.location.search).get('roleId') ?? '';
const role = ref<Role>();
const modules = ref<RoleAuthorizationModule[]>([]);
const selectedModuleAlias = ref<string>();
const actions = ref<RolePermissionAction[]>([]);
const dataGrantMatrix = ref<RoleDataGrantActionMatrix>();
const dataScopeCatalog = ref<RoleDataScopePolicyCatalog>();
const loading = ref(false);
const loadingActions = ref(false);
const saving = ref(false);
const error = ref<string>();

const isGroup = computed(() => role.value?.roleKind === 'group');
const isDataGrant = computed(() => role.value?.roleKind === 'dataGrant');
const isEmploymentRole = computed(() => role.value?.assignmentType === 'employment');
const roleTitle = computed(() => role.value?.title ?? roleId ?? '角色');
const actionPanelTitle = computed(() =>
  selectedModule.value ? `动作授权 - ${selectedModule.value.title}` : `动作授权 - ${roleTitle.value}`,
);
const selectedModule = computed(() =>
  modules.value.find((item) => item.moduleAlias === selectedModuleAlias.value),
);
const moduleTreeRecords = computed(() =>
  modules.value.map((module) => ({
    id: module.moduleAlias,
    parentId: module.parentId,
    title: module.title,
    secondary: module.applicationAlias,
  })),
);
const scopeOptions = computed(() =>
  (dataScopeCatalog.value?.options ?? []).map((option) => ({ value: option.code, label: option.title })),
);
const referenceDependencyOptions = computed(() =>
  (dataScopeCatalog.value?.referenceDependencies ?? []).map((dependency) => ({
    value: dependency.referenceFieldId,
    label: `${dependency.title} → ${dependency.targetModuleTitle}`,
  })),
);
const referenceDependencyByField = computed(
  () =>
    new Map(
      (dataScopeCatalog.value?.referenceDependencies ?? []).map((item) => [item.referenceFieldId, item]),
    ),
);
const actionColumns: UiDataTableColumn[] = [
  { key: 'title', title: '动作', width: 280 },
  { key: 'granted', title: '授权', width: 100, align: 'center' },
  { key: 'dataScopePolicy', title: '数据范围', width: 260 },
];
const dataGrantColumns: UiDataTableColumn[] = [
  { key: 'title', title: '标准动作', width: 280 },
  { key: 'configured', title: '启用模板', width: 120, align: 'center' },
  { key: 'dataScopePolicy', title: '数据范围', width: 260 },
];
const actionRows = computed(() => actions.value as unknown as UiDataTableRecord[]);
const dataGrantRows = computed(
  () => (dataGrantMatrix.value?.actions ?? []) as unknown as UiDataTableRecord[],
);

onMounted(() => void load());
watch(selectedModuleAlias, () => void loadActions());

async function load() {
  if (!roleId) {
    error.value = '缺少角色标识，无法打开授权页。';
    return;
  }
  loading.value = true;
  error.value = undefined;
  try {
    role.value = await roleContext.crud.view(roleId);
    if (role.value.roleKind === 'group') return;
    if (role.value.roleKind === 'dataGrant') {
      const [matrix, catalog] = await Promise.all([
        client.dataGrantActionMatrix(roleId),
        client.dataScopePolicyCatalog(roleId),
      ]);
      dataGrantMatrix.value = matrix;
      dataScopeCatalog.value = catalog;
      return;
    }
    modules.value = (await client.authorizationModules(roleId)).records;
    selectedModuleAlias.value = modules.value[0]?.moduleAlias;
  } catch (cause) {
    error.value = '授权信息加载失败，请重试。';
    presentPlatformError(cause, { source: 'role-authorization', phase: 'load' });
  } finally {
    loading.value = false;
  }
}

async function loadActions() {
  const moduleAlias = selectedModuleAlias.value;
  if (!moduleAlias || isDataGrant.value || isGroup.value) return;
  loadingActions.value = true;
  try {
    const [matrix, catalog] = await Promise.all([
      client.permissionMatrix(roleId, [moduleAlias]),
      client.dataScopePolicyCatalog(roleId, moduleAlias),
    ]);
    actions.value = matrix.modules[0]?.actions ?? [];
    dataScopeCatalog.value = catalog;
  } catch (cause) {
    presentPlatformError(cause, { source: 'role-authorization', phase: 'load' });
  } finally {
    loadingActions.value = false;
  }
}

async function updateAction(action: RolePermissionAction, granted: boolean) {
  if (!selectedModuleAlias.value) return;
  if (granted && action.dataAuth && isEmploymentRole.value && !action.dataScopePolicy) {
    action.dataScopePolicy = 'inheritDataGrant';
  }
  if (granted && action.dataScopePolicy === 'referenceDependency' && !action.referenceFieldId) return;
  saving.value = true;
  try {
    const result = granted
      ? await client.grantAction(roleId, {
          moduleAlias: selectedModuleAlias.value,
          actionCode: action.actionCode,
          dataScopePolicy:
            action.dataAuth && isEmploymentRole.value
              ? (action.dataScopePolicy ?? 'inheritDataGrant')
              : undefined,
          referenceFieldId:
            action.dataScopePolicy === 'referenceDependency' ? action.referenceFieldId : undefined,
          referenceActionCode:
            action.dataScopePolicy === 'referenceDependency' ? action.referenceActionCode : undefined,
        })
      : await client.revokeAction(roleId, selectedModuleAlias.value, action.actionCode);
    action.granted = granted;
    await handlePlatformActionSuccess(result, {
      source: 'role-authorization',
      phase: 'action',
      fallbackMessage: '授权已保存',
    });
  } catch (cause) {
    presentPlatformError(cause, { source: 'role-authorization', phase: 'action' });
    await loadActions();
  } finally {
    saving.value = false;
  }
}

async function updateActionScope(action: RolePermissionAction, value: unknown) {
  action.dataScopePolicy = String(value || 'none') as DataScopePolicy;
  if (action.dataScopePolicy === 'referenceDependency') {
    action.referenceFieldId = undefined;
    action.referenceActionCode = undefined;
    return;
  }
  action.referenceFieldId = undefined;
  action.referenceActionCode = undefined;
  if (action.granted) await updateAction(action, true);
}

async function updateReferenceDependency(action: RolePermissionAction, referenceFieldId: unknown) {
  const dependency = referenceDependencyByField.value.get(String(referenceFieldId || ''));
  action.referenceFieldId = dependency?.referenceFieldId;
  action.referenceActionCode = dependency?.referenceActionCode;
  if (action.granted && dependency) await updateAction(action, true);
}

function referenceDependencyOf(referenceFieldId: unknown) {
  return referenceDependencyByField.value.get(String(referenceFieldId || ''));
}

function selectValue(value: unknown) {
  return typeof value === 'string' || typeof value === 'number' ? value : undefined;
}

function displayedDataScopePolicy(action: RolePermissionAction): DataScopePolicy {
  return action.dataScopePolicy ?? 'inheritDataGrant';
}

async function saveDataGrantMatrix() {
  if (!dataGrantMatrix.value) return;
  saving.value = true;
  try {
    const result = await client.replaceDataGrantActions(
      roleId,
      dataGrantMatrix.value.actions.map((action) => ({
        actionCode: action.actionCode,
        dataScopePolicy: action.dataScopePolicy,
        enabled: action.configured,
      })),
    );
    await handlePlatformActionSuccess(result, {
      source: 'role-authorization',
      phase: 'action',
      fallbackMessage: '数据权限模板已保存',
    });
  } catch (cause) {
    presentPlatformError(cause, { source: 'role-authorization', phase: 'action' });
  } finally {
    saving.value = false;
  }
}
</script>

<template>
  <section class="role-authorization-page">
    <UiSpin v-if="loading" tip="加载授权信息" />
    <UiError v-else-if="error" title="授权页加载失败" :message="error" />
    <UiEmpty
      v-else-if="isGroup"
      title="角色组不独立授权"
      description="角色组只组合成员角色；请到成员角色分别配置动作和数据权限。"
    />
    <RecordExplorerPanel
      v-else-if="isDataGrant && dataGrantMatrix"
      class="data-grant-content"
      :title="`数据权限模板 - ${roleTitle}`"
      :searchable="false"
      @refresh="load"
    >
      <header class="panel-title">
        <div>
          <h3>标准动作的数据范围模板</h3>
          <p>模板只会在具体任职的普通角色动作选择“继承数据授权角色”时生效。</p>
        </div>
        <UiButton type="primary" :loading="saving" @click="saveDataGrantMatrix">保存</UiButton>
      </header>
      <UiDataTable :columns="dataGrantColumns" :rows="dataGrantRows" row-key="actionCode" horizontal-scroll>
        <template #cell="{ column, record }">
          <template v-if="column.key === 'title'">
            <strong>{{ record.title || record.actionCode }}</strong>
            <small>{{ record.actionCode }}</small>
          </template>
          <UiSwitch
            v-else-if="column.key === 'configured'"
            :checked="Boolean(record.configured)"
            :disabled="saving"
            @change="record.configured = $event"
          />
          <UiSelect
            v-else
            :value="record.dataScopePolicy as DataScopePolicy"
            :options="scopeOptions"
            :disabled="!record.configured || saving"
            placeholder="请选择数据范围"
            :allow-clear="false"
            @update:value="record.dataScopePolicy = String($event || '')"
          />
        </template>
      </UiDataTable>
    </RecordExplorerPanel>
    <section v-else class="authorization-layout">
      <RecordExplorerPanel
        class="module-panel"
        title="模块树"
        refresh-title="刷新模块目录"
        :searchable="false"
        @refresh="load"
      >
        <RecordTreeSelector
          :records="moduleTreeRecords"
          :selected-id="selectedModuleAlias"
          @select="selectedModuleAlias = $event.id"
        />
      </RecordExplorerPanel>
      <RecordExplorerPanel class="action-panel" :title="actionPanelTitle" :searchable="false">
        <UiSpin v-if="loadingActions" tip="加载模块动作" />
        <UiEmpty v-else-if="!selectedModuleAlias" title="请选择模块" />
        <UiDataTable
          v-else
          :columns="actionColumns"
          :rows="actionRows"
          row-key="actionCode"
          fill-height
          horizontal-scroll
        >
          <template #cell="{ column, record }">
            <template v-if="column.key === 'title'">
              <strong>{{ record.title || record.actionCode }}</strong>
              <small>{{ record.permissionActionCode || record.actionCode }}</small>
            </template>
            <UiSwitch
              v-else-if="column.key === 'granted'"
              :checked="Boolean(record.granted)"
              :disabled="saving"
              @change="updateAction(record as unknown as RolePermissionAction, $event)"
            />
            <div v-else-if="record.dataAuth && isEmploymentRole" class="data-scope-editor">
              <UiSelect
                :value="displayedDataScopePolicy(record as unknown as RolePermissionAction)"
                :options="scopeOptions"
                :disabled="saving"
                :allow-clear="false"
                @update:value="updateActionScope(record as unknown as RolePermissionAction, $event)"
              />
              <template v-if="record.dataScopePolicy === 'referenceDependency'">
                <UiSelect
                  :value="selectValue(record.referenceFieldId)"
                  :options="referenceDependencyOptions"
                  :disabled="saving"
                  placeholder="请选择引用字段"
                  :allow-clear="false"
                  @update:value="updateReferenceDependency(record as unknown as RolePermissionAction, $event)"
                />
                <small v-if="record.referenceFieldId">
                  依赖目标：{{ referenceDependencyOf(record.referenceFieldId)?.targetModuleTitle }} ·
                  {{ referenceDependencyOf(record.referenceFieldId)?.referenceActionTitle }}
                </small>
                <small v-else>请先选择引用字段，再开启该动作授权。</small>
              </template>
            </div>
            <span v-else class="not-applicable">不适用</span>
          </template>
        </UiDataTable>
      </RecordExplorerPanel>
    </section>
  </section>
</template>

<style scoped>
.role-authorization-page {
  display: grid;
  gap: 16px;
  height: 100%;
  min-height: 0;
  min-width: 960px;
  overflow: hidden;
}
.panel-title {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}
h3,
p {
  margin: 0;
}
h3 {
  font-size: 16px;
}
p,
small {
  color: var(--muyun-text-muted);
}
small {
  display: block;
  margin-top: 3px;
}
.authorization-layout {
  display: grid;
  grid-template-columns: minmax(230px, 280px) minmax(640px, 1fr);
  gap: 16px;
  min-height: 0;
  overflow: hidden;
}
.module-panel,
.action-panel,
.data-grant-content {
  min-width: 0;
}
.module-panel {
  min-height: 0;
}
.module-panel :deep(.record-explorer-panel-content) {
  display: flex;
  flex-direction: column;
  min-height: 0;
}
.action-panel {
  min-height: 0;
}
.action-panel :deep(.record-explorer-panel-content) {
  min-height: 0;
}
.action-panel :deep(.record-explorer-panel-content),
.data-grant-content {
  display: grid;
  gap: 16px;
}
.authorization-table {
  width: 100%;
  border-collapse: collapse;
}
.authorization-table th,
.authorization-table td {
  padding: 11px 12px;
  border-bottom: 1px solid var(--muyun-border-color);
  text-align: left;
  vertical-align: middle;
}
.authorization-table th {
  color: var(--muyun-text-muted);
  font-weight: 500;
  background: var(--muyun-surface-muted);
}
.authorization-table th:nth-child(2),
.authorization-table td:nth-child(2) {
  width: 110px;
}
.authorization-table th:nth-child(3),
.authorization-table td:nth-child(3) {
  width: 260px;
}
.not-applicable {
  color: var(--muyun-text-muted);
}
.data-scope-editor {
  display: grid;
  gap: 6px;
}
</style>
