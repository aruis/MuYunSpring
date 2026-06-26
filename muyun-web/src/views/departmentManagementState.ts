import { computed, ref } from 'vue';
import type { Department, Organization } from '@muyun/web-contracts';
import { normalizeError, type ModuleContext } from '@muyun/web-core';
import type { UiConfirmOptions } from '@muyun/vue-ui-antdv';
import { presentPlatformError, presentPlatformMessage } from '@muyun/platform-components';

export type DepartmentMode = 'view' | 'edit' | 'create-root' | 'create-child';
type ConfirmAction = (options: UiConfirmOptions) => Promise<boolean>;

export function createDepartmentManagementState(
  departmentContext: ModuleContext<Department>,
  confirmAction: ConfirmAction,
) {
  const organizationReloadKey = ref(0);
  const departmentReloadKey = ref(0);
  const organizations = ref<Organization[]>([]);
  const selectedOrganization = ref<Organization>();
  const departments = ref<Department[]>([]);
  const selectedDepartment = ref<Department>();
  const draft = ref<Department>(emptyDepartmentDraft());
  const mode = ref<DepartmentMode>('view');
  const saving = ref(false);
  const actionError = ref<string>();

  const selectedOrganizationId = computed(() => selectedOrganization.value?.id);
  const selectedOrganizationTitle = computed(() => organizationTitleOf(selectedOrganization.value));
  const selectedDepartmentTitle = computed(() => departmentTitleOf(selectedDepartment.value));
  const readonly = computed(() => mode.value === 'view');
  const canCreate = computed(() => departmentContext.can('create') === true);
  const canUpdate = computed(() => departmentContext.can('update') === true);
  const canDelete = computed(() => departmentContext.can('delete') === true);
  const canToggle = computed(() => {
    if (!selectedDepartment.value?.id) {
      return false;
    }
    return departmentContext.can(departmentToggleActionCode(selectedDepartment.value)) === true;
  });
  const cardTitle = computed(() => {
    if (mode.value === 'create-root') {
      return '新建部门';
    }
    if (mode.value === 'create-child') {
      return `新建下级：${selectedDepartmentTitle.value}`;
    }
    return selectedDepartmentTitle.value;
  });

  function handleOrganizationsLoaded(records: Organization[]) {
    organizations.value = records;
    if (
      !selectedOrganization.value?.id ||
      !records.some((item) => item.id === selectedOrganization.value?.id)
    ) {
      selectedOrganization.value = records[0];
      resetDepartmentsForOrganization();
      return;
    }
    selectedOrganization.value = records.find((item) => item.id === selectedOrganization.value?.id);
  }

  function selectOrganization(record: Organization) {
    if (selectedOrganization.value?.id === record.id) {
      return;
    }
    selectedOrganization.value = record;
    resetDepartmentsForOrganization();
    clearFeedback();
  }

  function handleDepartmentsLoaded(records: Department[]) {
    departments.value = records;
    if (!selectedDepartment.value?.id || !records.some((item) => item.id === selectedDepartment.value?.id)) {
      selectedDepartment.value = records[0];
    } else {
      selectedDepartment.value = records.find((item) => item.id === selectedDepartment.value?.id);
    }
    draft.value = selectedDepartment.value
      ? copyDepartment(selectedDepartment.value)
      : emptyDepartmentDraft(selectedOrganizationId.value);
    mode.value = 'view';
  }

  function selectDepartment(record: Department) {
    selectedDepartment.value = record;
    draft.value = copyDepartment(record);
    mode.value = 'view';
    clearFeedback();
  }

  function startCreateRoot() {
    if (!selectedOrganizationId.value) {
      presentActionMessage('请先选择机构');
      return;
    }
    if (!canCreate.value) {
      presentActionMessage('当前用户无权新增部门');
      return;
    }
    selectedDepartment.value = undefined;
    draft.value = emptyDepartmentDraft(selectedOrganizationId.value);
    mode.value = 'create-root';
    clearFeedback();
  }

  function startCreateChild(parent?: Department) {
    const current = parent ?? selectedDepartment.value;
    if (!selectedOrganizationId.value) {
      presentActionMessage('请先选择机构');
      return;
    }
    if (!current?.id) {
      presentActionMessage('请先选择上级部门');
      return;
    }
    if (!canCreate.value) {
      presentActionMessage('当前用户无权新增部门');
      return;
    }
    selectedDepartment.value = current;
    draft.value = {
      ...emptyDepartmentDraft(selectedOrganizationId.value),
      parentId: current.id,
    };
    mode.value = 'create-child';
    clearFeedback();
  }

  function startEdit() {
    if (!selectedDepartment.value) {
      return;
    }
    if (!canUpdate.value) {
      presentActionMessage('当前用户无权编辑部门');
      return;
    }
    draft.value = copyDepartment(selectedDepartment.value);
    mode.value = 'edit';
    clearFeedback();
  }

  function cancelEdit() {
    draft.value = selectedDepartment.value
      ? copyDepartment(selectedDepartment.value)
      : emptyDepartmentDraft(selectedOrganizationId.value);
    mode.value = 'view';
    clearFeedback();
  }

  async function save() {
    if (saving.value || mode.value === 'view') {
      return;
    }
    if (mode.value.startsWith('create') ? !canCreate.value : !canUpdate.value) {
      presentActionMessage('当前用户无权保存部门');
      return;
    }
    const validDraft = normalizeDepartmentDraft(draft.value, selectedOrganizationId.value);
    if (!isValidDepartment(validDraft)) {
      presentActionMessage('所属机构、部门编码和部门名称不能为空');
      return;
    }
    clearFeedback();
    saving.value = true;
    try {
      await departmentContext.runtime.ready;
      const crud = departmentContext.abilities.crud();
      const saved =
        mode.value === 'edit' && validDraft.id
          ? await crud.update(validDraft.id, validDraft)
          : await crud.insert(validDraft);
      selectedDepartment.value = saved;
      draft.value = copyDepartment(saved);
      mode.value = 'view';
      presentActionSuccess('已保存');
      departmentReloadKey.value += 1;
    } catch (cause) {
      presentActionCause(cause);
    } finally {
      saving.value = false;
    }
  }

  async function toggleEnabled() {
    if (!selectedDepartment.value?.id || saving.value) {
      return;
    }
    if (!canToggle.value) {
      presentActionMessage('当前用户无权变更部门启停状态');
      return;
    }
    clearFeedback();
    saving.value = true;
    try {
      await departmentContext.runtime.ready;
      const enable = departmentContext.abilities.enable();
      if (selectedDepartment.value.enabled === false) {
        await enable.enable(selectedDepartment.value.id);
      } else {
        await enable.disable(selectedDepartment.value.id);
      }
      const refreshed = await departmentContext.abilities.crud().view(selectedDepartment.value.id);
      selectedDepartment.value = refreshed;
      draft.value = copyDepartment(refreshed);
      departmentReloadKey.value += 1;
    } catch (cause) {
      presentActionCause(cause);
    } finally {
      saving.value = false;
    }
  }

  async function removeSelected() {
    if (!selectedDepartment.value?.id || saving.value) {
      return;
    }
    if (!canDelete.value) {
      presentActionMessage('当前用户无权删除部门');
      return;
    }
    const confirmed = await confirmAction({
      title: '删除部门',
      content: `确认删除部门「${departmentTitleOf(selectedDepartment.value)}」？`,
      okText: '删除',
      danger: true,
    });
    if (!confirmed) {
      return;
    }
    clearFeedback();
    saving.value = true;
    try {
      await departmentContext.abilities.crud().delete(selectedDepartment.value.id);
      selectedDepartment.value = undefined;
      draft.value = emptyDepartmentDraft(selectedOrganizationId.value);
      mode.value = 'view';
      presentActionSuccess('已删除');
      departmentReloadKey.value += 1;
    } catch (cause) {
      presentActionCause(cause);
    } finally {
      saving.value = false;
    }
  }

  function resetDepartmentsForOrganization() {
    departments.value = [];
    selectedDepartment.value = undefined;
    draft.value = emptyDepartmentDraft(selectedOrganizationId.value);
    mode.value = 'view';
    departmentReloadKey.value += 1;
  }

  function clearFeedback() {
    actionError.value = undefined;
  }

  function presentActionCause(cause: unknown) {
    const error = normalizeError(cause);
    actionError.value = error.message;
    presentPlatformError(error, { source: 'department-management-action', phase: 'action' });
  }

  function presentActionMessage(message: string) {
    actionError.value = message;
    presentPlatformMessage(message, { source: 'department-management-action', phase: 'action' });
  }

  function presentActionSuccess(message: string) {
    presentPlatformMessage(message, {
      source: 'department-management-action',
      phase: 'action',
      tone: 'success',
    });
  }

  return {
    organizationReloadKey,
    departmentReloadKey,
    organizations,
    selectedOrganization,
    departments,
    selectedDepartment,
    draft,
    mode,
    saving,
    actionError,
    selectedOrganizationId,
    selectedOrganizationTitle,
    selectedDepartmentTitle,
    readonly,
    canCreate,
    canUpdate,
    canDelete,
    canToggle,
    cardTitle,
    handleOrganizationsLoaded,
    selectOrganization,
    handleDepartmentsLoaded,
    selectDepartment,
    startCreateRoot,
    startCreateChild,
    startEdit,
    cancelEdit,
    save,
    toggleEnabled,
    removeSelected,
  };
}

export function organizationTitleOf(record: Organization | undefined) {
  return record?.title ?? record?.code ?? record?.id ?? '机构';
}

export function departmentTitleOf(record: Department | undefined) {
  return record?.title ?? record?.code ?? record?.id ?? '部门详情';
}

export function emptyDepartmentDraft(organizationId?: string): Department {
  return {
    organizationId,
    parentId: undefined,
    code: '',
    title: '',
    enabled: true,
  };
}

export function copyDepartment(record: Department): Department {
  return { ...record };
}

export function normalizeDepartmentDraft(record: Department, organizationId: string | undefined): Department {
  return {
    ...record,
    organizationId: record.organizationId?.trim() || organizationId,
    parentId: normalizeBlank(record.parentId),
    code: record.code?.trim(),
    title: record.title?.trim(),
  };
}

export function isValidDepartment(record: Department) {
  return Boolean(record.organizationId && record.code && record.title);
}

function departmentToggleActionCode(record: Department) {
  return record.enabled === false ? 'enable' : 'disable';
}

function normalizeBlank(value: string | undefined) {
  const normalized = value?.trim();
  return normalized ? normalized : undefined;
}
