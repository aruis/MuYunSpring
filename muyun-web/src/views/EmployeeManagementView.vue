<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import {
  RecordActionBar,
  RecordDetailDrawer,
  RecordExplorerPanel,
  RecordFormFields,
  RecordMetaSection,
  RecordQueryListPanel,
  RecordStatusSwitch,
  TreeRecordExplorer,
  createScopedTreeModuleContext,
  type QueryListRecord,
  type RecordActionItem,
  type RecordFormFieldFallback,
  type RecordFormFieldPickerConfig,
  type RecordFormRecord,
  type RecordPickerRecord,
  type ResolvedRecordActionItem,
  executeStaticFormSave,
  executeStaticRecordAction,
  presentPlatformError,
  presentPlatformMessage,
  resolveRecordFormFieldState,
} from '@muyun/platform-components';
import { UiInput, confirmAction } from '@muyun/vue-ui-antdv';
import type {
  Department,
  Employee,
  Organization,
  ResolvedViewFieldDescriptor,
  ViewFieldDefinition,
} from '@muyun/web-contracts';
import { useModuleContext, type ModuleContext } from '@muyun/web-core';

defineOptions({ name: 'EmployeeManagementView' });

type EmployeeDetailMode = 'view' | 'create' | 'edit';
type EmployeeFormFieldName =
  | 'organizationId'
  | 'departmentId'
  | 'employeeNo'
  | 'title'
  | 'gender'
  | 'mobile'
  | 'email'
  | 'enabled';
type EmployeeFormPickerFieldName = 'departmentId';

const organizationContext = useModuleContext<Organization>({ moduleAlias: 'iam.organization' });
const departmentContext = useModuleContext<Department>({ moduleAlias: 'iam.department' });
const employeeContext = useModuleContext<Employee>({ moduleAlias: 'iam.employee' });
const employeeFormFieldDefinitions = ref<Map<string, ViewFieldDefinition | ResolvedViewFieldDescriptor>>(
  new Map(),
);
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
  createScopedTreeModuleContext(departmentContext, {
    scopeFieldName: 'organizationId',
    scopeValue: selectedOrganizationId.value,
    treePath: '/iam.department/tree',
    sortPath: '/iam.department/sort',
  }),
);
const employeeFormPickerConfigs = computed<Record<EmployeeFormPickerFieldName, RecordFormFieldPickerConfig>>(
  () => ({
    departmentId: {
      context: scopedDepartmentContext.value as unknown as ModuleContext<RecordPickerRecord>,
      reloadKey: organizationReloadKey.value,
      placeholder: '请选择部门',
      titleOf: (record) => departmentTitle(record as Department),
    },
  }),
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
const employeeDetailTitle = computed(() => {
  if (employeeDetailMode.value === 'create') {
    return '新建职员';
  }
  return employeeTitle(selectedEmployee.value ?? employeeDraft.value);
});
const employeeReadonly = computed(() => employeeDetailMode.value === 'view');
const employeeFormDisabled = computed(() => employeeReadonly.value || savingEmployee.value);
const canSaveEmployee = computed(() => {
  if (employeeDetailMode.value === 'create') {
    return Boolean(selectedOrganizationId.value) && employeeContext.can('create') === true;
  }
  return Boolean(selectedEmployee.value?.id) && employeeContext.can('update') === true;
});
const canToggleEmployee = computed(() => {
  if (!selectedEmployee.value?.id) {
    return false;
  }
  return employeeContext.can(employeeToggleActionCode(selectedEmployee.value)) === true;
});
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
      disabled: !canSaveEmployee.value,
      loading: savingEmployee.value,
    },
  ];
});

onMounted(loadEmployeeFormDefinition);

async function loadEmployeeFormDefinition() {
  try {
    const runtimeContext = await employeeContext.runtime.ready;
    const formView = runtimeContext.uiDescriptor?.views?.find(
      (view) => view.viewKind === 'FORM' && view.viewCode === 'default_form',
    );
    employeeFormFieldDefinitions.value = new Map(
      formView?.fields.map((field) => [field.fieldRef.fieldName, field]) ?? [],
    );
  } catch (cause) {
    presentPlatformError(cause, { source: 'employee-management', phase: 'load' });
  }
}

function employeeFormField(fieldName: EmployeeFormFieldName) {
  return resolveRecordFormFieldState(fieldName, {
    fields: employeeFormFieldDefinitions.value,
    fallback: employeeFormFieldFallback,
  });
}

function employeeFormLabel(fieldName: EmployeeFormFieldName) {
  return employeeFormField(fieldName).label;
}

function employeeFormRequired(fieldName: EmployeeFormFieldName) {
  return employeeFormField(fieldName).required;
}

function employeeFormVisible(fieldName: EmployeeFormFieldName) {
  return employeeFormField(fieldName).visible;
}

function updateEmployeeDraftField(fieldName: string, value: string | boolean | undefined) {
  employeeDraft.value = {
    ...employeeDraft.value,
    [fieldName]: value,
  };
}

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
  await executeStaticFormSave<Employee>({
    loading: savingEmployee,
    mode: employeeDetailMode.value === 'edit' ? 'edit' : 'create',
    source: 'employee-management',
    validateContext: () => (selectedOrganizationId.value ? undefined : '请先选择机构'),
    canSave: () => canSaveEmployee.value,
    deniedMessage: '当前用户无权保存职员',
    createRecord: () => normalizedEmployeeDraft(employeeDraft.value, selectedOrganizationId.value ?? ''),
    validateRecord: (draft) =>
      draft.departmentId && draft.employeeNo && draft.title ? undefined : '请填写部门、职员编号和职员姓名',
    save: (draft, mode) =>
      mode === 'edit' && selectedEmployee.value?.id
        ? employeeContext.crud.update(selectedEmployee.value.id, draft)
        : employeeContext.crud.insert(draft),
    onSaved: ({ record }) => {
      selectedEmployee.value = record;
      employeeDraft.value = copyEmployee(record);
      selectedEmployeeKey.value = record.id;
      employeeDetailMode.value = 'view';
      employeeDetailOpen.value = true;
      employeeReloadKey.value += 1;
    },
  });
}

async function toggleEmployeeEnabled() {
  await executeStaticRecordAction({
    loading: savingEmployee,
    source: 'employee-management',
    record: () => (selectedEmployee.value && selectedEmployee.value.id ? selectedEmployee.value : undefined),
    canExecute: () => canToggleEmployee.value,
    deniedMessage: '当前用户无权变更职员启停状态',
    execute: (employee) =>
      employee.enabled === false
        ? employeeContext.crud.enable(employee.id!)
        : employeeContext.crud.disable(employee.id!),
    onExecuted: async (_, employee) => {
      const refreshed = await employeeContext.crud.view(employee.id!);
      selectedEmployee.value = refreshed;
      employeeDraft.value = copyEmployee(refreshed);
      employeeReloadKey.value += 1;
    },
  });
}

async function removeEmployee(record: Partial<Employee> | QueryListRecord | undefined) {
  await executeStaticRecordAction({
    loading: savingEmployee,
    source: 'employee-management',
    record: () => (record?.id ? record : undefined),
    canExecute: () => employeeContext.can('delete') === true,
    deniedMessage: '当前用户无权删除职员',
    confirm: (target) =>
      confirmAction({
        title: '删除职员',
        content: `确认删除职员「${employeeTitle(target)}」？`,
        okText: '删除',
        danger: true,
      }),
    execute: (target) => employeeContext.crud.delete(String(target.id)),
    onExecuted: (_, target) => {
      const id = String(target.id);
      if (selectedEmployeeKey.value === id) {
        selectedEmployeeKey.value = undefined;
        selectedEmployee.value = undefined;
        employeeDraft.value = createEmployeeDraft(selectedOrganizationId.value);
        employeeDetailOpen.value = false;
        employeeDetailMode.value = 'view';
      }
      employeeReloadKey.value += 1;
    },
  });
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
    gender: draft.gender?.trim() || undefined,
    mobile: draft.mobile?.trim() || undefined,
    email: draft.email?.trim() || undefined,
    enabled: draft.enabled !== false,
  } as Employee;
}

function employeeTitle(record: Partial<Employee> | QueryListRecord | undefined) {
  return String(record?.title ?? record?.employeeNo ?? record?.id ?? '职员档案');
}

function employeeToggleActionCode(record: Partial<Employee>) {
  return record.enabled === false ? 'enable' : 'disable';
}

function departmentTitle(record: Department) {
  return record.title ?? record.code ?? record.id ?? '未命名部门';
}

const employeeFormFieldFallback: Record<EmployeeFormFieldName, RecordFormFieldFallback> = {
  organizationId: { label: '所属机构', required: true, readOnly: true, visible: true },
  departmentId: {
    label: '所属部门',
    required: true,
    readOnly: false,
    visible: true,
    controlType: 'recordPicker',
  },
  employeeNo: {
    label: '职员编号',
    required: true,
    readOnly: false,
    visible: true,
    placeholder: '请输入职员编号',
  },
  title: {
    label: '职员姓名',
    required: true,
    readOnly: false,
    visible: true,
    placeholder: '请输入职员姓名',
  },
  gender: { label: '性别', required: false, readOnly: false, visible: true, placeholder: '请输入性别' },
  mobile: {
    label: '手机号',
    required: false,
    readOnly: false,
    visible: true,
    placeholder: '请输入手机号',
  },
  email: { label: '邮箱', required: false, readOnly: false, visible: true, placeholder: '请输入邮箱' },
  enabled: {
    label: '启用状态',
    required: false,
    readOnly: false,
    visible: true,
    controlType: 'enabledStatus',
  },
};
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
      standard-crud-actions
      create-title="新建职员"
      standard-crud-row-actions
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
          v-if="employeeDetailMode === 'view' && selectedEmployee"
          :enabled="selectedEmployee.enabled !== false"
          :disabled="savingEmployee || !canToggleEmployee"
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
        <label v-if="employeeFormVisible('organizationId')">
          <span class="employee-form-label">
            {{ employeeFormLabel('organizationId') }}
            <strong v-if="employeeFormRequired('organizationId')" aria-hidden="true">*</strong>
          </span>
          <UiInput :value="selectedOrganization?.title ?? selectedOrganization?.id ?? '-'" disabled />
        </label>
        <RecordFormFields
          :record="employeeDraft as RecordFormRecord"
          :fields="employeeFormFieldDefinitions"
          :exclude-field-names="['organizationId']"
          :fallback="employeeFormFieldFallback"
          :picker-configs="employeeFormPickerConfigs"
          :disabled="employeeFormDisabled"
          @update:field="updateEmployeeDraftField"
        />
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

.employee-form > label {
  display: grid;
  gap: 6px;
  color: var(--muyun-text-muted);
  font-size: 13px;
}

.employee-form-label {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.employee-form-label strong {
  color: #d92d20;
  font-weight: 600;
}

@media (max-width: 900px) {
  .employee-management-page {
    grid-template-columns: 1fr;
  }
}
</style>
