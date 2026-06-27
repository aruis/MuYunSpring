<script setup lang="ts">
import { computed, ref } from 'vue';
import {
  RecordActionBar,
  RecordDetailDrawer,
  RecordExplorerPanel,
  RecordMetaSection,
  RecordPicker,
  RecordQueryListPanel,
  RecordStatusSwitch,
  TreeRecordExplorer,
  type QueryListRecord,
  type RecordActionItem,
  type RecordQueryListColumn,
  type ResolvedRecordActionItem,
  presentPlatformError,
  presentPlatformMessage,
} from '@muyun/platform-components';
import { UiInput, confirmAction } from '@muyun/vue-ui-antdv';
import type {
  Department,
  Employee,
  Organization,
  WebListResponse,
  WebQueryRequest,
  WebTreeNode,
} from '@muyun/web-contracts';
import {
  useModuleContext,
  type ModuleAbilities,
  type ModuleContext,
  type StaticModuleTreeClient,
} from '@muyun/web-core';

defineOptions({ name: 'EmployeeManagementView' });

type EmployeeDetailMode = 'view' | 'create' | 'edit';

const organizationContext = useModuleContext<Organization>({ moduleAlias: 'iam.organization' });
const departmentContext = useModuleContext<Department>({ moduleAlias: 'iam.department' });
const employeeContext = useModuleContext<Employee>({ moduleAlias: 'iam.employee' });
const organizationSearchKeyword = ref('');
const organizationReloadKey = ref(0);
const employeeReloadKey = ref(0);
const selectedOrganization = ref<Organization>();
const selectedEmployeeKey = ref<string>();
const selectedEmployee = ref<Employee>();
const employeeDetailOpen = ref(false);
const employeeDetailMode = ref<EmployeeDetailMode>('view');
const savingEmployee = ref(false);
const employeeDraft = ref<Partial<Employee>>(createEmployeeDraft(undefined));

const employeeListContext = computed(() => employeeContext as unknown as ModuleContext<QueryListRecord>);
const selectedOrganizationId = computed(() => selectedOrganization.value?.id);
const scopedDepartmentContext = computed(() =>
  createOrganizationScopedDepartmentContext(departmentContext, selectedOrganizationId.value),
);
const employeeExternalQueryValues = computed<Record<string, unknown> | undefined>(() => {
  const organizationId = selectedOrganizationId.value;
  if (!organizationId) {
    return undefined;
  }
  return {
    departmentScope: {
      organizationId,
      includeChildren: true,
    },
  };
});

const employeeColumns: RecordQueryListColumn[] = [
  { key: 'employeeNo', title: '职员编号', width: '150px' },
  { key: 'title', title: '职员姓名', width: '150px' },
  { key: 'mobile', title: '手机号', width: '150px' },
  { key: 'email', title: '邮箱' },
  {
    key: 'enabled',
    title: '状态',
    type: 'enabledStatus',
    width: '90px',
    align: 'center',
  },
];
const employeeListActions = computed<RecordActionItem[]>(() => [
  {
    key: 'create',
    actionCode: 'create',
    title: '新建职员',
    primary: true,
    disabled: !selectedOrganizationId.value,
    iconName: 'plus',
  },
]);
const employeeDetailTitle = computed(() => {
  if (employeeDetailMode.value === 'create') {
    return '新建职员';
  }
  return employeeTitle(selectedEmployee.value ?? employeeDraft.value);
});
const employeeReadonly = computed(() => employeeDetailMode.value === 'view');
const employeeDetailActions = computed<RecordActionItem[]>(() => {
  if (employeeDetailMode.value === 'view') {
    if (!selectedEmployee.value?.id) {
      return [];
    }
    return [
      { key: 'edit', actionCode: 'update', title: '编辑', iconName: 'edit' },
      { key: 'delete', actionCode: 'delete', title: '删除', iconName: 'delete', danger: true },
    ];
  }
  return [
    { key: 'cancel', title: '取消', iconName: 'close', disabled: savingEmployee.value },
    {
      key: 'save',
      actionCode: employeeDetailMode.value === 'create' ? 'create' : 'update',
      title: '保存',
      iconName: 'save',
      primary: true,
      loading: savingEmployee.value,
    },
  ];
});

function handleOrganizationsLoaded(records: Organization[]) {
  if (!selectedOrganization.value && records.length > 0) {
    selectedOrganization.value = records[0];
  }
}

function selectOrganization(record: Organization) {
  selectedOrganization.value = record;
  selectedEmployeeKey.value = undefined;
  selectedEmployee.value = undefined;
  closeEmployeeDetail();
}

function refreshOrganizations() {
  organizationReloadKey.value += 1;
}

function selectEmployee(record: QueryListRecord) {
  selectedEmployeeKey.value = String(record.id ?? '');
}

function handleEmployeeListAction(action: RecordActionItem) {
  if (action.key === 'create') {
    startCreateEmployee();
  }
}

function employeeRowActionsOf(): RecordActionItem[] {
  return [
    { key: 'view', title: '查看' },
    { key: 'edit', actionCode: 'update', title: '修改', iconName: 'edit' },
    { key: 'delete', actionCode: 'delete', title: '删除', iconName: 'delete', danger: true },
  ];
}

function handleEmployeeRowAction(action: ResolvedRecordActionItem, record: QueryListRecord) {
  if (action.key === 'view') {
    void openEmployeeDetail(record, 'view');
    return;
  }
  if (action.key === 'edit') {
    void openEmployeeDetail(record, 'edit');
    return;
  }
  if (action.key === 'delete') {
    void removeEmployee(record);
  }
}

function handleEmployeeRowDblclick(record: QueryListRecord) {
  void openEmployeeDetail(record, 'view');
}

function startCreateEmployee() {
  if (!selectedOrganizationId.value) {
    presentPlatformMessage('请先选择机构', { phase: 'validation' });
    return;
  }
  employeeDraft.value = createEmployeeDraft(selectedOrganizationId.value);
  selectedEmployee.value = undefined;
  selectedEmployeeKey.value = undefined;
  employeeDetailMode.value = 'create';
  employeeDetailOpen.value = true;
}

function closeEmployeeDetail() {
  if (savingEmployee.value) {
    return;
  }
  employeeDetailOpen.value = false;
  employeeDetailMode.value = 'view';
  employeeDraft.value = selectedEmployee.value
    ? copyEmployee(selectedEmployee.value)
    : createEmployeeDraft(selectedOrganizationId.value);
}

async function openEmployeeDetail(record: QueryListRecord, mode: EmployeeDetailMode) {
  const id = String(record.id ?? '');
  if (!id) {
    return;
  }
  selectedEmployeeKey.value = id;
  employeeDetailOpen.value = true;
  employeeDetailMode.value = mode;
  selectedEmployee.value = record as Employee;
  employeeDraft.value = copyEmployee(record as Employee);
  try {
    const fullRecord = await employeeContext.crud.view(id);
    selectedEmployee.value = fullRecord;
    employeeDraft.value = copyEmployee(fullRecord);
  } catch (cause) {
    presentPlatformError(cause, { source: 'employee-management', phase: 'load' });
  }
}

function handleEmployeeDetailAction(action: RecordActionItem) {
  if (action.key === 'cancel') {
    closeEmployeeDetail();
    return;
  }
  if (action.key === 'save') {
    void saveEmployee();
    return;
  }
  if (action.key === 'edit') {
    if (selectedEmployee.value) {
      employeeDraft.value = copyEmployee(selectedEmployee.value);
    }
    employeeDetailMode.value = 'edit';
    return;
  }
  if (action.key === 'delete') {
    void removeEmployee(selectedEmployee.value);
  }
}

async function saveEmployee() {
  const organizationId = selectedOrganizationId.value;
  if (!organizationId) {
    presentPlatformMessage('请先选择机构', { phase: 'validation' });
    return;
  }
  const draft = normalizedEmployeeDraft(employeeDraft.value, organizationId);
  if (!draft.departmentId || !draft.employeeNo || !draft.title) {
    presentPlatformMessage('请填写部门、职员编号和职员姓名', { phase: 'validation' });
    return;
  }
  savingEmployee.value = true;
  try {
    const result =
      employeeDetailMode.value === 'edit' && selectedEmployee.value?.id
        ? await employeeContext.crud.update(selectedEmployee.value.id, draft)
        : await employeeContext.crud.insert(draft);
    const saved = result.record;
    selectedEmployee.value = saved;
    employeeDraft.value = copyEmployee(saved);
    selectedEmployeeKey.value = saved.id;
    employeeDetailMode.value = 'view';
    employeeDetailOpen.value = true;
    employeeReloadKey.value += 1;
    presentPlatformMessage(result.message ?? '操作成功', { tone: 'success' });
  } catch (cause) {
    presentPlatformError(cause, { source: 'employee-management', phase: 'action' });
  } finally {
    savingEmployee.value = false;
  }
}

async function toggleEmployeeEnabled() {
  const employee = selectedEmployee.value;
  if (!employee?.id || savingEmployee.value) {
    return;
  }
  savingEmployee.value = true;
  try {
    const result =
      employee.enabled === false
        ? await employeeContext.crud.enable(employee.id)
        : await employeeContext.crud.disable(employee.id);
    const refreshed = await employeeContext.crud.view(employee.id);
    selectedEmployee.value = refreshed;
    employeeDraft.value = copyEmployee(refreshed);
    employeeReloadKey.value += 1;
    presentPlatformMessage(result.message ?? '操作成功', {
      tone: 'success',
    });
  } catch (cause) {
    presentPlatformError(cause, { source: 'employee-management', phase: 'action' });
  } finally {
    savingEmployee.value = false;
  }
}

async function removeEmployee(record: Partial<Employee> | QueryListRecord | undefined) {
  const id = String(record?.id ?? '');
  if (!id || savingEmployee.value) {
    return;
  }
  const confirmed = await confirmAction({
    title: '删除职员',
    content: `确认删除职员「${employeeTitle(record)}」？`,
    okText: '删除',
    danger: true,
  });
  if (!confirmed) {
    return;
  }
  savingEmployee.value = true;
  try {
    const result = await employeeContext.crud.delete(id);
    if (selectedEmployeeKey.value === id) {
      selectedEmployeeKey.value = undefined;
      selectedEmployee.value = undefined;
      employeeDraft.value = createEmployeeDraft(selectedOrganizationId.value);
      employeeDetailOpen.value = false;
      employeeDetailMode.value = 'view';
    }
    employeeReloadKey.value += 1;
    presentPlatformMessage(result.message ?? '操作成功', { tone: 'success' });
  } catch (cause) {
    presentPlatformError(cause, { source: 'employee-management', phase: 'action' });
  } finally {
    savingEmployee.value = false;
  }
}

function createEmployeeDraft(organizationId: string | undefined): Partial<Employee> {
  return {
    organizationId,
    enabled: true,
    sortOrder: 100,
  };
}

function copyEmployee(record: Partial<Employee>): Partial<Employee> {
  return { ...record };
}

function normalizedEmployeeDraft(draft: Partial<Employee>, organizationId: string): Employee {
  return {
    ...draft,
    organizationId,
    departmentId: draft.departmentId?.trim(),
    employeeNo: draft.employeeNo?.trim(),
    title: draft.title?.trim(),
    mobile: draft.mobile?.trim() || undefined,
    email: draft.email?.trim() || undefined,
    enabled: draft.enabled !== false,
  } as Employee;
}

function employeeTitle(record: Partial<Employee> | QueryListRecord | undefined) {
  return String(record?.title ?? record?.employeeNo ?? record?.id ?? '职员档案');
}

function departmentTitle(record: Department) {
  return record.title ?? record.code ?? record.id ?? '未命名部门';
}

function createOrganizationScopedDepartmentContext(
  context: ModuleContext<Department>,
  organizationId: string | undefined,
): ModuleContext<Department> {
  const treeClient = createOrganizationScopedDepartmentTreeClient(context, organizationId);
  const abilities: ModuleAbilities<Department> = {
    ...context.abilities,
    tree: () => treeClient,
    tryTree: () => (context.abilities.hasTree() ? treeClient : undefined),
  };
  return {
    ...context,
    abilities,
  };
}

function createOrganizationScopedDepartmentTreeClient(
  context: ModuleContext<Department>,
  organizationId: string | undefined,
): StaticModuleTreeClient<Department> {
  return {
    ...context.crud,
    query: (request) => context.crud.query(scopedDepartmentQuery(request, organizationId)),
    tree: () => {
      if (!organizationId) {
        return emptyTreeResponse<Department>();
      }
      return context.http.request<WebListResponse<WebTreeNode<Department>>>({
        path: '/iam.department/tree',
        query: { organizationId },
      });
    },
    treeFlat: (options) => {
      if (!organizationId) {
        return emptyListResponse<Department>();
      }
      const rootId = options?.rootId;
      const path = rootId ? `/iam.department/tree/${encodeURIComponent(rootId)}` : '/iam.department/tree';
      return context.http.request<WebListResponse<Department>>({
        path,
        query: {
          organizationId,
          flat: true,
          includeSelf: options?.includeSelf,
        },
      });
    },
    subtree: (id, options) =>
      context.http.request<WebListResponse<WebTreeNode<Department>>>({
        path: `/iam.department/tree/${encodeURIComponent(id)}`,
        query: options,
      }),
    sort: (id, request) =>
      context.http.request({
        method: 'POST',
        path: `/iam.department/sort/${encodeURIComponent(id)}`,
        body: request,
      }),
  };
}

function scopedDepartmentQuery(request: WebQueryRequest | undefined, organizationId: string | undefined) {
  if (!organizationId) {
    return request;
  }
  return {
    ...request,
    conditions: [
      ...(request?.conditions ?? []),
      { fieldName: 'organizationId', operator: 'EQ', values: [organizationId] },
    ],
  };
}

async function emptyTreeResponse<TRecord>(): Promise<WebListResponse<WebTreeNode<TRecord>>> {
  return { records: [] };
}

async function emptyListResponse<TRecord>(): Promise<WebListResponse<TRecord>> {
  return { records: [] };
}
</script>

<template>
  <section class="employee-management-page">
    <RecordExplorerPanel
      class="employee-scope-panel"
      title="机构树"
      refresh-title="刷新机构树"
      :search-keyword="organizationSearchKeyword"
      search-placeholder="搜索机构名称、编码或 ID"
      @refresh="refreshOrganizations"
      @update:search-keyword="organizationSearchKeyword = $event"
    >
      <TreeRecordExplorer
        :context="organizationContext"
        :selected-id="selectedOrganization?.id"
        :reload-key="organizationReloadKey"
        :keyword="organizationSearchKeyword"
        search-mode="none"
        search-trigger="external"
        empty-description="暂无机构"
        loading-tip="加载机构树"
        fallback-title="未命名机构"
        @loaded="handleOrganizationsLoaded"
        @select="selectOrganization"
      />
    </RecordExplorerPanel>

    <RecordQueryListPanel
      class="employee-list-panel"
      :context="employeeListContext"
      title="职员列表"
      :columns="employeeColumns"
      :actions="employeeListActions"
      :row-actions-of="employeeRowActionsOf"
      :selected-key="selectedEmployeeKey"
      :reload-key="employeeReloadKey"
      :ready="Boolean(selectedOrganization?.id)"
      :external-query-values="employeeExternalQueryValues"
      quick-search-placeholder="搜索编号、姓名、手机号或邮箱"
      empty-description="当前机构暂无职员"
      waiting-description="请选择机构"
      @action="handleEmployeeListAction"
      @row-action="handleEmployeeRowAction"
      @row-dblclick="handleEmployeeRowDblclick"
      @select="selectEmployee"
    />

    <RecordDetailDrawer
      :open="employeeDetailOpen"
      :title="employeeDetailTitle"
      :close-on-outside="employeeDetailMode === 'view'"
      @close="closeEmployeeDetail"
    >
      <template #status>
        <RecordStatusSwitch
          v-if="employeeDetailMode !== 'view'"
          :enabled="employeeDraft.enabled !== false"
          :show-label="false"
          @change="employeeDraft.enabled = $event"
        />
        <RecordStatusSwitch
          v-else-if="selectedEmployee"
          :enabled="selectedEmployee.enabled !== false"
          :disabled="savingEmployee"
          :loading="savingEmployee"
          :show-label="false"
          @change="toggleEmployeeEnabled"
        />
      </template>
      <template #actions>
        <RecordActionBar
          :context="employeeListContext"
          :actions="employeeDetailActions"
          @action="handleEmployeeDetailAction"
        />
      </template>

      <form class="employee-form" @submit.prevent="saveEmployee">
        <label>
          <span>所属机构</span>
          <UiInput :value="selectedOrganization?.title ?? selectedOrganization?.id ?? '-'" disabled />
        </label>
        <label>
          <span>所属部门</span>
          <RecordPicker
            v-model:value="employeeDraft.departmentId"
            :context="scopedDepartmentContext"
            :title-of="departmentTitle"
            :disabled="employeeReadonly"
            placeholder="请选择部门"
          />
        </label>
        <label>
          <span>职员编号</span>
          <UiInput
            v-model:value="employeeDraft.employeeNo"
            :disabled="employeeReadonly"
            placeholder="请输入职员编号"
          />
        </label>
        <label>
          <span>职员姓名</span>
          <UiInput
            v-model:value="employeeDraft.title"
            :disabled="employeeReadonly"
            placeholder="请输入职员姓名"
          />
        </label>
        <label>
          <span>手机号</span>
          <UiInput
            v-model:value="employeeDraft.mobile"
            :disabled="employeeReadonly"
            placeholder="请输入手机号"
          />
        </label>
        <label>
          <span>邮箱</span>
          <UiInput
            v-model:value="employeeDraft.email"
            :disabled="employeeReadonly"
            placeholder="请输入邮箱"
          />
        </label>
      </form>
      <RecordMetaSection v-if="employeeDetailMode !== 'create'" :record="employeeDraft" show-sort-order />
    </RecordDetailDrawer>
  </section>
</template>

<style scoped>
.employee-management-page {
  position: relative;
  display: grid;
  grid-template-columns: minmax(260px, 320px) minmax(0, 1fr);
  gap: 12px;
  height: calc(100vh - 116px);
  overflow: hidden;
}

.employee-scope-panel,
.employee-list-panel {
  min-width: 0;
  min-height: 0;
}

.employee-form {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
}

.employee-form label {
  display: grid;
  gap: 6px;
  color: var(--muyun-text-muted);
  font-size: 13px;
}

@media (max-width: 900px) {
  .employee-management-page {
    grid-template-columns: 1fr;
  }
}
</style>
