<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import {
  RecordActionBar,
  RecordDetailDrawer,
  RecordDetailFields,
  RecordExternalChangeNotice,
  RecordExplorerPanel,
  RecordFormFields,
  RecordMetaSection,
  RecordQueryListPanel,
  RecordStatusSwitch,
  TreeRecordExplorer,
  createScopedTreeModuleContext,
  type QueryListRecord,
  type RecordActionItem,
  type RecordExplorerItemDescriptor,
  type RecordFormFieldFallback,
  type RecordFormFieldPickerConfig,
  type RecordFormRecord,
  type RecordPickerRecord,
  type ResolvedRecordActionItem,
  executeStaticFormSave,
  executeStaticRecordAction,
  handlePlatformActionSuccess,
  normalizeRecordDraft,
  presentPlatformError,
  presentPlatformMessage,
  resolveRecordFormFields,
  resolveRecordFormFieldState,
} from '@muyun/platform-components';
import { UiButton, UiError, UiInput, UiSpin, confirmAction } from '@muyun/vue-ui-antdv';
import type {
  Department,
  Employee,
  EmployeeAccount,
  EmployeeAccountProvisionResponse,
  Organization,
  UserAccount,
  WebActionResultEnvelope,
} from '@muyun/web-contracts';
import { actionResultData, platformErrorCodes, useModuleContext, type ModuleContext } from '@muyun/web-core';
import { usePageDataChange, usePageRecordExternalChange, useRealtimeRefreshQueue } from '../app/pageRealtime';
import {
  canSwitchEmployeeDetailContext,
  isEmployeeFormDisabled,
  shouldCommitEmployeeDetailRequest,
  shouldCloseEmployeeDetailOnCancel,
  shouldShowEmployeeDetailContent,
  validateEmployeeRequiredFormFields,
  type EmployeeDetailMode,
} from './employeeDetailStateModel';

defineOptions({ name: 'EmployeeManagementView' });

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

const employeeRequiredFormFieldNames = [
  'departmentId',
  'employeeNo',
  'title',
] as const satisfies readonly EmployeeFormFieldName[];

const organizationContext = useModuleContext<Organization>({ moduleAlias: 'iam.organization' });
const departmentContext = useModuleContext<Department>({ moduleAlias: 'iam.department' });
const employeeContext = useModuleContext<Employee>({ moduleAlias: 'iam.employee' });
const userContext = useModuleContext<UserAccount>({ moduleAlias: 'iam.user' });
const employeeFormFieldDefinitions = ref(resolveRecordFormFields(undefined));
const organizationSearchKeyword = ref('');
const organizationReloadKey = ref(0);
const employeeReloadKey = ref(0);
const selectedOrganization = ref<Organization>();
const selectedEmployeeKey = ref<string>();
const selectedEmployee = ref<Employee>();
const employeeDetailOpen = ref(false);
const employeeDetailMode = ref<EmployeeDetailMode>('view');
const loadingEmployeeDetail = ref(false);
const employeeDetailLoadFailed = ref(false);
const savingEmployee = ref(false);
const employeeDetailRequestSeq = ref(0);
const employeeDraft = ref<Partial<Employee>>(createEmployeeDraft(undefined));
const employeeDetailDepartment = ref<Department>();
const employeeAccount = ref<EmployeeAccount>();
const employeeAccountUser = ref<UserAccount>();
const loadingEmployeeAccounts = ref(false);
const savingEmployeeAccount = ref(false);
const employeeAccountsLoadFailed = ref(false);
const showAccountProvisionForm = ref(false);
const accountProvisionDraft = ref<Partial<UserAccount>>(createAccountProvisionDraft(undefined));
const employeeExternalChange = usePageRecordExternalChange({
  moduleAlias: 'iam.employee',
  recordId: () => selectedEmployee.value?.id,
  editing: () => employeeDetailMode.value === 'edit',
  saving: () => savingEmployee.value,
});
const employeeRealtimeRefreshQueue = useRealtimeRefreshQueue<string>({
  delay: 80,
  load: async (run) => {
    employeeReloadKey.value += 1;
    const currentDetailId = selectedEmployee.value?.id;
    if (
      employeeDetailOpen.value &&
      employeeDetailMode.value === 'view' &&
      currentDetailId &&
      run.keys.includes(currentDetailId)
    ) {
      await openEmployeeDetail({ ...employeeDraft.value, id: currentDetailId } as QueryListRecord, 'view');
    }
  },
});
usePageDataChange({
  moduleAlias: 'iam.employee',
  handler: (_changeSet, changes) => {
    const changedRecordIds = changes
      .map((change) => change.recordId)
      .filter((recordId): recordId is string => Boolean(recordId));
    employeeRealtimeRefreshQueue.enqueue(changedRecordIds.length > 0 ? changedRecordIds : '__collection__');
  },
});

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
const employeeFormDisabled = computed(() =>
  isEmployeeFormDisabled({
    mode: employeeDetailMode.value,
    loadingDetail: loadingEmployeeDetail.value,
    saving: savingEmployee.value,
    selectedEmployeeId: selectedEmployee.value?.id,
  }),
);
const showEmployeeDetailContent = computed(() =>
  shouldShowEmployeeDetailContent({
    mode: employeeDetailMode.value,
    loadingDetail: loadingEmployeeDetail.value,
    loadFailed: employeeDetailLoadFailed.value,
    selectedEmployeeId: selectedEmployee.value?.id,
  }),
);
const canSaveEmployee = computed(() => {
  if (loadingEmployeeDetail.value) {
    return false;
  }
  if (employeeDetailMode.value === 'create') {
    return Boolean(selectedOrganizationId.value) && employeeContext.can('create') === true;
  }
  return Boolean(selectedEmployee.value?.id) && employeeContext.can('update') === true;
});
const canToggleEmployee = computed(() => {
  if (loadingEmployeeDetail.value || !selectedEmployee.value?.id) {
    return false;
  }
  return employeeContext.can(employeeToggleActionCode(selectedEmployee.value)) === true;
});
const canManageEmployeeAccounts = computed(() => {
  if (!selectedEmployee.value?.id || loadingEmployeeDetail.value || savingEmployee.value) {
    return false;
  }
  return employeeContext.can('employeeAccounts', selectedEmployee.value.id) !== false;
});
const employeeDetailActions = computed<RecordActionItem[]>(() => {
  if (employeeDetailMode.value === 'view') {
    if (!selectedEmployee.value?.id) {
      return [];
    }
    return [
      { key: 'edit', actionCode: 'update', title: '编辑', iconName: 'edit', disabled: savingEmployee.value },
      {
        key: 'delete',
        actionCode: 'delete',
        title: '删除',
        iconName: 'delete',
        danger: true,
        disabled: savingEmployee.value,
      },
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
    employeeFormFieldDefinitions.value = resolveRecordFormFields(runtimeContext.uiDescriptor);
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

function canLeaveEmployeeDetailContext() {
  return canSwitchEmployeeDetailContext({ saving: savingEmployee.value });
}

function updateEmployeeDraftField(fieldName: string, value: string | number | boolean | undefined) {
  employeeDraft.value = {
    ...employeeDraft.value,
    [fieldName]: value,
  };
}

function employeeDetailDisplayValue(
  fieldName: string,
  value: unknown,
): string | number | boolean | undefined | null {
  if (fieldName === 'organizationId') {
    return selectedOrganization.value?.title ?? selectedOrganization.value?.id ?? String(value ?? '');
  }
  if (fieldName === 'departmentId') {
    const department = employeeDetailDepartment.value;
    if (department && department.id === value) {
      return departmentTitle(department);
    }
    return undefined;
  }
  return undefined;
}

function handleOrganizationsLoaded(records: Organization[]) {
  if (!selectedOrganization.value && records.length > 0) {
    selectedOrganization.value = records[0];
  }
}

function selectOrganization(record: Organization) {
  if (!canLeaveEmployeeDetailContext()) {
    return;
  }
  selectedOrganization.value = record;
  selectedEmployeeKey.value = undefined;
  selectedEmployee.value = undefined;
  employeeExternalChange.clearExternalChanged();
  loadingEmployeeDetail.value = false;
  employeeDetailLoadFailed.value = false;
  employeeDetailDepartment.value = undefined;
  resetEmployeeAccountState();
  closeEmployeeDetail();
}

function refreshOrganizations() {
  organizationReloadKey.value += 1;
}

function selectEmployee(record: QueryListRecord) {
  if (!canLeaveEmployeeDetailContext()) {
    return;
  }
  const nextKey = String(record.id ?? '');
  const currentDetailId = String(selectedEmployee.value?.id ?? employeeDraft.value.id ?? '');
  selectedEmployeeKey.value = nextKey;
  if (employeeDetailOpen.value && currentDetailId !== nextKey) {
    employeeDetailRequestSeq.value += 1;
    loadingEmployeeDetail.value = false;
    employeeDetailLoadFailed.value = false;
    employeeDetailDepartment.value = undefined;
    resetEmployeeAccountState();
    selectedEmployee.value = undefined;
    employeeDraft.value = createEmployeeDraft(selectedOrganizationId.value);
    employeeExternalChange.clearExternalChanged();
    employeeDetailOpen.value = false;
    employeeDetailMode.value = 'view';
  }
}

function handleEmployeeListAction(action: RecordActionItem) {
  if (!canLeaveEmployeeDetailContext()) {
    return;
  }
  if (action.key === 'create') {
    startCreateEmployee();
  }
}

function handleEmployeeRowAction(action: ResolvedRecordActionItem, record: QueryListRecord) {
  if (!canLeaveEmployeeDetailContext()) {
    return;
  }
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
  if (!canLeaveEmployeeDetailContext()) {
    return;
  }
  void openEmployeeDetail(record, 'view');
}

function startCreateEmployee() {
  if (!canLeaveEmployeeDetailContext()) {
    return;
  }
  if (!selectedOrganizationId.value) {
    presentPlatformMessage('请先选择机构', { phase: 'validation' });
    return;
  }
  employeeDraft.value = createEmployeeDraft(selectedOrganizationId.value);
  selectedEmployee.value = undefined;
  selectedEmployeeKey.value = undefined;
  employeeExternalChange.clearExternalChanged();
  employeeDetailMode.value = 'create';
  loadingEmployeeDetail.value = false;
  employeeDetailLoadFailed.value = false;
  employeeDetailRequestSeq.value += 1;
  employeeDetailDepartment.value = undefined;
  resetEmployeeAccountState();
  employeeDetailOpen.value = true;
}

function closeEmployeeDetail() {
  if (savingEmployee.value) {
    return;
  }
  employeeDetailRequestSeq.value += 1;
  loadingEmployeeDetail.value = false;
  employeeDetailLoadFailed.value = false;
  employeeDetailOpen.value = false;
  employeeDetailMode.value = 'view';
  employeeDetailDepartment.value = undefined;
  employeeExternalChange.clearExternalChanged();
  resetEmployeeAccountState();
  employeeDraft.value = selectedEmployee.value
    ? copyEmployee(selectedEmployee.value)
    : createEmployeeDraft(selectedOrganizationId.value);
}

function cancelEmployeeDetail() {
  if (savingEmployee.value) {
    return;
  }
  if (
    shouldCloseEmployeeDetailOnCancel({
      mode: employeeDetailMode.value,
      selectedEmployeeId: selectedEmployee.value?.id,
    })
  ) {
    closeEmployeeDetail();
    return;
  }
  employeeDraft.value = copyEmployee(selectedEmployee.value!);
  employeeExternalChange.clearExternalChanged();
  employeeDetailMode.value = 'view';
  loadingEmployeeDetail.value = false;
  employeeDetailLoadFailed.value = false;
}

async function openEmployeeDetail(record: QueryListRecord, mode: EmployeeDetailMode) {
  if (!canLeaveEmployeeDetailContext()) {
    return;
  }
  const id = String(record.id ?? '');
  if (!id) {
    return;
  }
  selectedEmployeeKey.value = id;
  employeeExternalChange.clearExternalChanged();
  employeeDetailOpen.value = true;
  employeeDetailMode.value = mode;
  selectedEmployee.value = undefined;
  employeeDraft.value = copyEmployee(record as Employee);
  employeeDetailDepartment.value = undefined;
  resetEmployeeAccountState();
  loadingEmployeeDetail.value = true;
  employeeDetailLoadFailed.value = false;
  const requestSeq = employeeDetailRequestSeq.value + 1;
  employeeDetailRequestSeq.value = requestSeq;
  const canCommitRequest = () =>
    shouldCommitEmployeeDetailRequest({
      activeRequestSeq: employeeDetailRequestSeq.value,
      requestSeq,
      selectedEmployeeKey: selectedEmployeeKey.value,
      recordId: id,
    });
  try {
    const fullRecord = await employeeContext.crud.view(id);
    if (!canCommitRequest()) {
      return;
    }
    selectedEmployee.value = fullRecord;
    employeeDraft.value = copyEmployee(fullRecord);
    employeeDetailLoadFailed.value = false;
    await loadEmployeeDetailDepartment(fullRecord, requestSeq);
    void loadEmployeeAccounts(fullRecord, requestSeq);
  } catch (cause) {
    if (canCommitRequest()) {
      employeeDetailLoadFailed.value = true;
      presentPlatformError(cause, { source: 'employee-management', phase: 'load' });
    }
  } finally {
    if (canCommitRequest()) {
      loadingEmployeeDetail.value = false;
    }
  }
}

function handleEmployeeDetailAction(action: RecordActionItem) {
  if (action.key === 'cancel') {
    cancelEmployeeDetail();
    return;
  }
  if (action.key === 'save') {
    void saveEmployee();
    return;
  }
  if (!canLeaveEmployeeDetailContext()) {
    return;
  }
  if (action.key === 'edit') {
    if (!selectedEmployee.value || loadingEmployeeDetail.value) {
      return;
    }
    employeeDraft.value = copyEmployee(selectedEmployee.value);
    employeeExternalChange.clearExternalChanged();
    employeeDetailMode.value = 'edit';
    return;
  }
  if (action.key === 'delete') {
    void removeEmployee(selectedEmployee.value);
  }
}

function retryEmployeeDetail() {
  if (!canLeaveEmployeeDetailContext()) {
    return;
  }
  const id = String(employeeDraft.value.id ?? selectedEmployeeKey.value ?? '');
  if (!id) {
    return;
  }
  const mode = employeeDetailMode.value === 'create' ? 'view' : employeeDetailMode.value;
  void openEmployeeDetail({ ...employeeDraft.value, id } as QueryListRecord, mode);
}

function reloadExternalEmployeeChange() {
  const id = String(
    employeeExternalChange.externalChangedRecordId.value ??
      employeeDraft.value.id ??
      selectedEmployeeKey.value ??
      '',
  );
  if (!id) {
    return;
  }
  employeeExternalChange.clearExternalChanged();
  void openEmployeeDetail({ ...employeeDraft.value, id } as QueryListRecord, 'edit');
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
    validateRecord: validateEmployeeDraft,
    save: (draft, mode) =>
      mode === 'edit' && selectedEmployee.value?.id
        ? employeeContext.crud.update(selectedEmployee.value.id, draft)
        : employeeContext.crud.insert(draft),
    actionErrorHandlers: [
      {
        code: platformErrorCodes.conflictVersion,
        handle: (_error, { mode, record }) =>
          mode === 'edit' && employeeExternalChange.markExternalRecordChanged(record.id),
      },
    ],
    onSaved: ({ record }) => {
      const requestSeq = commitEmployeeDetailRecord(record);
      employeeReloadKey.value += 1;
      void loadEmployeeDetailDepartment(record, requestSeq);
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
      const requestSeq = commitEmployeeDetailRecord(refreshed);
      await loadEmployeeDetailDepartment(refreshed, requestSeq);
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
        employeeExternalChange.clearExternalChanged();
        loadingEmployeeDetail.value = false;
        employeeDetailLoadFailed.value = false;
        employeeDetailRequestSeq.value += 1;
        employeeDetailDepartment.value = undefined;
        resetEmployeeAccountState();
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

function createAccountProvisionDraft(employee: Partial<Employee> | undefined): Partial<UserAccount> {
  return {
    username: defaultAccountUsername(employee),
    password: '',
    enabled: true,
  };
}

function copyEmployee(record: Partial<Employee>): Partial<Employee> {
  return { ...record };
}

function normalizedEmployeeDraft(draft: Partial<Employee>, organizationId: string): Employee {
  return normalizeRecordDraft<Employee>(draft, {
    organizationId,
    departmentId: draft.departmentId?.trim(),
    employeeNo: draft.employeeNo?.trim(),
    title: draft.title?.trim(),
    gender: draft.gender?.trim() || undefined,
    mobile: draft.mobile?.trim() || undefined,
    email: draft.email?.trim() || undefined,
    enabled: draft.enabled !== false,
  });
}

function validateEmployeeDraft(draft: Employee) {
  return validateEmployeeRequiredFormFields(
    employeeRequiredFormFieldNames.map((fieldName) => {
      const field = employeeFormField(fieldName);
      return {
        fieldName,
        label: field.label,
        required: field.required,
        visible: field.visible,
        value: draft[fieldName],
      };
    }),
  );
}

function commitEmployeeDetailRecord(record: Employee) {
  selectedEmployee.value = record;
  employeeDraft.value = copyEmployee(record);
  selectedEmployeeKey.value = record.id;
  employeeDetailMode.value = 'view';
  employeeExternalChange.clearExternalChanged();
  employeeDetailOpen.value = true;
  loadingEmployeeDetail.value = false;
  employeeDetailLoadFailed.value = false;
  const requestSeq = employeeDetailRequestSeq.value + 1;
  employeeDetailRequestSeq.value = requestSeq;
  return requestSeq;
}

function canCommitEmployeeDetailSideEffect(recordId: string | undefined, requestSeq: number) {
  return (
    Boolean(recordId) &&
    shouldCommitEmployeeDetailRequest({
      activeRequestSeq: employeeDetailRequestSeq.value,
      requestSeq,
      selectedEmployeeKey: selectedEmployeeKey.value,
      recordId: recordId ?? '',
    })
  );
}

async function loadEmployeeDetailDepartment(
  record: Partial<Employee>,
  requestSeq = employeeDetailRequestSeq.value,
) {
  employeeDetailDepartment.value = undefined;
  const employeeId = record.id;
  const departmentId = record.departmentId;
  if (!departmentId) {
    return;
  }
  try {
    const department = await departmentContext.crud.view(departmentId);
    if (canCommitEmployeeDetailSideEffect(employeeId, requestSeq)) {
      employeeDetailDepartment.value = department;
    }
  } catch (cause) {
    if (canCommitEmployeeDetailSideEffect(employeeId, requestSeq)) {
      presentPlatformError(cause, { source: 'employee-management', phase: 'load' });
    }
  }
}

async function loadEmployeeAccounts(
  record: Partial<Employee> = selectedEmployee.value ?? employeeDraft.value,
  requestSeq = employeeDetailRequestSeq.value,
) {
  const employeeId = record.id;
  if (!employeeId) {
    resetEmployeeAccountState();
    return;
  }
  loadingEmployeeAccounts.value = true;
  employeeAccountsLoadFailed.value = false;
  try {
    const binding = await employeeContext.http.request<EmployeeAccount | undefined>({
      path: `/iam.employee/${encodeURIComponent(employeeId)}/account`,
    });
    if (!canCommitEmployeeDetailSideEffect(employeeId, requestSeq)) {
      return;
    }
    employeeAccount.value = binding;
    await loadEmployeeAccountUser(binding, employeeId, requestSeq);
  } catch (cause) {
    if (canCommitEmployeeDetailSideEffect(employeeId, requestSeq)) {
      employeeAccountsLoadFailed.value = true;
      presentPlatformError(cause, { source: 'employee-management', phase: 'load' });
    }
  } finally {
    if (canCommitEmployeeDetailSideEffect(employeeId, requestSeq)) {
      loadingEmployeeAccounts.value = false;
    }
  }
}

async function loadEmployeeAccountUser(
  binding: EmployeeAccount | undefined,
  employeeId: string,
  requestSeq: number,
) {
  const userId = binding?.userId;
  if (!userId) {
    if (canCommitEmployeeDetailSideEffect(employeeId, requestSeq)) {
      employeeAccountUser.value = undefined;
    }
    return;
  }
  let user: UserAccount;
  try {
    user = await userContext.crud.view(userId);
  } catch (cause) {
    presentPlatformError(cause, { source: 'employee-management', phase: 'load' });
    user = { id: userId, username: userId } as UserAccount;
  }
  if (canCommitEmployeeDetailSideEffect(employeeId, requestSeq)) {
    employeeAccountUser.value = user;
  }
}

async function provisionEmployeeAccount() {
  const employee = selectedEmployee.value;
  const draft = normalizedAccountProvisionDraft(accountProvisionDraft.value);
  if (!employee?.id || !canManageEmployeeAccounts.value) {
    return;
  }
  const validationError = validateAccountProvisionDraft(draft);
  if (validationError) {
    presentPlatformMessage(validationError, { source: 'employee-management', phase: 'validation' });
    return;
  }
  savingEmployeeAccount.value = true;
  try {
    const result = await employeeContext.http.request<
      WebActionResultEnvelope<EmployeeAccountProvisionResponse>
    >({
      method: 'POST',
      path: `/iam.employee/${encodeURIComponent(employee.id)}/account/provision`,
      body: draft,
    });
    const response = actionResultData(result);
    employeeAccount.value = response.binding;
    employeeAccountUser.value = response.user;
    showAccountProvisionForm.value = false;
    accountProvisionDraft.value = createAccountProvisionDraft(employee);
    await handlePlatformActionSuccess(result, {
      source: 'employee-management',
      phase: 'action',
    });
  } catch (cause) {
    presentPlatformError(cause, { source: 'employee-management', phase: 'action' });
  } finally {
    savingEmployeeAccount.value = false;
  }
}

async function removeEmployeeAccount() {
  const employee = selectedEmployee.value;
  if (!employee?.id || !employeeAccount.value?.id || !canManageEmployeeAccounts.value) {
    return;
  }
  const confirmed = await confirmAction({
    title: '移除账户',
    content: `确认移除账户「${employeeAccountUserTitle()}」？该用户账号会同步删除。`,
    okText: '移除',
    danger: true,
  });
  if (!confirmed) {
    return;
  }
  savingEmployeeAccount.value = true;
  try {
    const result = await employeeContext.http.request<WebActionResultEnvelope<number>>({
      method: 'POST',
      path: `/iam.employee/${encodeURIComponent(employee.id)}/account/delete`,
    });
    await loadEmployeeAccounts(employee, employeeDetailRequestSeq.value);
    await handlePlatformActionSuccess(result, {
      source: 'employee-management',
      phase: 'action',
    });
  } catch (cause) {
    presentPlatformError(cause, { source: 'employee-management', phase: 'action' });
  } finally {
    savingEmployeeAccount.value = false;
  }
}

function resetEmployeeAccountState() {
  employeeAccount.value = undefined;
  employeeAccountUser.value = undefined;
  loadingEmployeeAccounts.value = false;
  savingEmployeeAccount.value = false;
  employeeAccountsLoadFailed.value = false;
  showAccountProvisionForm.value = false;
  accountProvisionDraft.value = createAccountProvisionDraft(selectedEmployee.value ?? employeeDraft.value);
}

function employeeTitle(record: Partial<Employee> | QueryListRecord | undefined) {
  return String(record?.title ?? record?.employeeNo ?? record?.id ?? '职员档案');
}

function employeeToggleActionCode(record: Partial<Employee>) {
  return record.enabled === false ? 'enable' : 'disable';
}

function employeeAccountUserTitle() {
  const binding = employeeAccount.value;
  const user = employeeAccountUser.value;
  return String(user?.username ?? binding?.userId ?? '未设置账号');
}

function employeeAccountUserDescription() {
  const user = employeeAccountUser.value;
  return user?.id ? `账号ID ${user.id}` : '-';
}

function employeeAccountStatusTitle() {
  return employeeAccountUser.value?.enabled === false ? '停用' : '启用';
}

function defaultAccountUsername(employee: Partial<Employee> | undefined) {
  return String(employee?.employeeNo ?? employee?.mobile ?? '')
    .trim()
    .toLowerCase();
}

function startAccountProvision() {
  accountProvisionDraft.value = createAccountProvisionDraft(selectedEmployee.value ?? employeeDraft.value);
  showAccountProvisionForm.value = true;
}

function cancelAccountProvision() {
  showAccountProvisionForm.value = false;
  accountProvisionDraft.value = createAccountProvisionDraft(selectedEmployee.value ?? employeeDraft.value);
}

function normalizedAccountProvisionDraft(draft: Partial<UserAccount>): UserAccount {
  return {
    ...draft,
    username: draft.username?.trim(),
    password: draft.password?.trim(),
    enabled: true,
  } as UserAccount;
}

function validateAccountProvisionDraft(draft: Partial<UserAccount>) {
  if (!draft.username) {
    return '请输入账号';
  }
  if (!draft.password) {
    return '请输入初始密码';
  }
  return undefined;
}

function departmentTitle(record: Department) {
  return record.title ?? record.code ?? record.id ?? '未命名部门';
}

function organizationItemOf(record: Organization): RecordExplorerItemDescriptor {
  return {
    title: record.title ?? record.code ?? record.id ?? '未命名机构',
    secondary: record.code ?? record.id,
    muted: record.enabled === false,
  };
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
        :item-of="(record) => organizationItemOf(record as Organization)"
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

      <UiSpin v-if="loadingEmployeeDetail" class="employee-detail-state" tip="加载职员详情" />
      <div v-else-if="employeeDetailLoadFailed" class="employee-detail-state">
        <UiError title="详情加载失败" message="无法加载职员详情，请重试" />
        <UiButton type="primary" icon-name="reload" @click="retryEmployeeDetail">重试</UiButton>
      </div>

      <template v-else-if="showEmployeeDetailContent">
        <template v-if="employeeDetailMode === 'view'">
          <RecordDetailFields
            :record="employeeDraft as RecordFormRecord"
            :fields="employeeFormFieldDefinitions"
            :fallback="employeeFormFieldFallback"
            :picker-configs="employeeFormPickerConfigs"
            :display-of="employeeDetailDisplayValue"
          />

          <section class="employee-account-section">
            <div class="employee-account-header">
              <strong>登录账号</strong>
              <UiButton
                v-if="!employeeAccount && !showAccountProvisionForm"
                type="primary"
                icon-name="plus"
                :disabled="!canManageEmployeeAccounts"
                @click="startAccountProvision"
              >
                设置账号
              </UiButton>
            </div>
            <UiSpin v-if="loadingEmployeeAccounts" class="employee-account-state" tip="加载账号绑定" />
            <div v-else-if="employeeAccountsLoadFailed" class="employee-account-state">
              <UiError title="账号绑定加载失败" message="无法加载职员账号绑定，请重试" />
              <UiButton icon-name="reload" @click="loadEmployeeAccounts()">重试</UiButton>
            </div>
            <form
              v-else-if="showAccountProvisionForm"
              class="employee-account-form"
              @submit.prevent="provisionEmployeeAccount"
            >
              <label>
                <span>账号</span>
                <UiInput
                  v-model:value="accountProvisionDraft.username"
                  placeholder="请输入登录账号"
                  :disabled="savingEmployeeAccount"
                />
              </label>
              <label>
                <span>初始密码</span>
                <UiInput
                  v-model:value="accountProvisionDraft.password"
                  type="password"
                  placeholder="请输入初始密码"
                  :disabled="savingEmployeeAccount"
                />
              </label>
              <div class="employee-account-form-actions">
                <UiButton :disabled="savingEmployeeAccount" @click="cancelAccountProvision">取消</UiButton>
                <UiButton
                  type="primary"
                  html-type="submit"
                  icon-name="plus"
                  :loading="savingEmployeeAccount"
                  :disabled="!canManageEmployeeAccounts"
                >
                  创建账号并绑定
                </UiButton>
              </div>
            </form>
            <div v-else-if="!employeeAccount" class="employee-account-empty">
              <span>未设置登录账号</span>
              <small>可从职员档案生成账号并自动完成一对一绑定。</small>
            </div>
            <div v-else class="employee-account-card">
              <div>
                <strong>{{ employeeAccountUserTitle() }}</strong>
                <span>{{ employeeAccountUserDescription() }}</span>
              </div>
              <span class="employee-account-status">{{ employeeAccountStatusTitle() }}</span>
              <UiButton
                danger
                icon-name="delete"
                :disabled="savingEmployeeAccount || !canManageEmployeeAccounts"
                @click="removeEmployeeAccount"
              >
                移除账户
              </UiButton>
            </div>
          </section>

          <RecordMetaSection :record="employeeDraft" show-sort-order />
        </template>

        <template v-else>
          <RecordExternalChangeNotice
            v-if="employeeExternalChange.externallyChanged.value"
            @reload="reloadExternalEmployeeChange"
            @dismiss="employeeExternalChange.clearExternalChanged"
          />
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
        </template>
      </template>
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

.employee-detail-state {
  display: grid;
  place-items: center;
  gap: 12px;
  min-height: 180px;
}

.employee-account-section {
  display: grid;
  gap: 12px;
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid var(--muyun-border);
}

.employee-account-header {
  display: inline-flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.employee-account-state,
.employee-account-empty {
  display: grid;
  place-items: center;
  gap: 10px;
  min-height: 140px;
  color: var(--muyun-text-muted);
}

.employee-account-empty small {
  font-size: 12px;
}

.employee-account-form {
  display: grid;
  gap: 12px;
  padding: 12px;
  border: 1px solid var(--muyun-border);
  border-radius: 8px;
}

.employee-account-form label {
  display: grid;
  gap: 6px;
  color: var(--muyun-text-muted);
  font-size: 13px;
}

.employee-account-form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.employee-account-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border: 1px solid var(--muyun-border);
  border-radius: 8px;
}

.employee-account-card div {
  display: grid;
  min-width: 0;
  gap: 3px;
}

.employee-account-card strong,
.employee-account-card span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.employee-account-card span {
  color: var(--muyun-text-muted);
  font-size: 13px;
}

.employee-account-status {
  padding: 2px 8px;
  border-radius: 999px;
  background: var(--muyun-hover-subtle);
}

@media (max-width: 900px) {
  .employee-management-page {
    grid-template-columns: 1fr;
  }

  .employee-account-card {
    grid-template-columns: 1fr;
    align-items: start;
  }

  .employee-account-form-actions,
  .employee-account-header {
    justify-content: flex-start;
    flex-wrap: wrap;
  }
}
</style>
