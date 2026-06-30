import { computed, ref } from 'vue';
import type { Department, Organization } from '@muyun/web-contracts';
import type { ModuleContext } from '@muyun/web-core';
import type { UiConfirmOptions } from '@muyun/vue-ui-antdv';
import { executeStaticFormSave, executeStaticRecordAction } from '@muyun/platform-components';

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
  }

  function startCreateRoot() {
    if (!selectedOrganizationId.value || !canCreate.value) {
      return;
    }
    selectedDepartment.value = undefined;
    draft.value = emptyDepartmentDraft(selectedOrganizationId.value);
    mode.value = 'create-root';
  }

  function startCreateChild(parent?: Department) {
    const current = parent ?? selectedDepartment.value;
    if (!selectedOrganizationId.value || !current?.id || !canCreate.value) {
      return;
    }
    selectedDepartment.value = current;
    draft.value = {
      ...emptyDepartmentDraft(selectedOrganizationId.value),
      parentId: current.id,
    };
    mode.value = 'create-child';
  }

  function startEdit() {
    if (!selectedDepartment.value || !canUpdate.value) {
      return;
    }
    draft.value = copyDepartment(selectedDepartment.value);
    mode.value = 'edit';
  }

  function cancelEdit() {
    draft.value = selectedDepartment.value
      ? copyDepartment(selectedDepartment.value)
      : emptyDepartmentDraft(selectedOrganizationId.value);
    mode.value = 'view';
  }

  async function save() {
    await executeStaticFormSave<Department>({
      loading: saving,
      mode: mode.value === 'edit' ? 'edit' : 'create',
      source: 'department-management',
      validateContext: () => {
        if (mode.value === 'view') {
          return '请选择编辑或新建部门';
        }
        return selectedOrganizationId.value ? undefined : '请先选择机构';
      },
      canSave: () => (mode.value.startsWith('create') ? canCreate.value : canUpdate.value),
      deniedMessage: '当前用户无权保存部门',
      createRecord: () => normalizeDepartmentDraft(draft.value, selectedOrganizationId.value),
      validateRecord: (record) =>
        isValidDepartment(record) ? undefined : '所属机构、部门编码和部门名称不能为空',
      save: async (record, saveMode) => {
        await departmentContext.runtime.ready;
        const crud = departmentContext.abilities.crud();
        return saveMode === 'edit' && record.id ? crud.update(record.id, record) : crud.insert(record);
      },
      onSaved: ({ record }) => {
        selectedDepartment.value = record;
        draft.value = copyDepartment(record);
        mode.value = 'view';
        departmentReloadKey.value += 1;
      },
    });
  }

  async function toggleEnabled() {
    await executeStaticRecordAction({
      loading: saving,
      source: 'department-management',
      record: () => (selectedDepartment.value?.id ? selectedDepartment.value : undefined),
      canExecute: () => canToggle.value,
      deniedMessage: '当前用户无权变更部门启停状态',
      execute: async (department) => {
        await departmentContext.runtime.ready;
        const enable = departmentContext.abilities.enable();
        return department.enabled === false ? enable.enable(department.id!) : enable.disable(department.id!);
      },
      onExecuted: async (_, department) => {
        const refreshed = await departmentContext.abilities.crud().view(department.id!);
        selectedDepartment.value = refreshed;
        draft.value = copyDepartment(refreshed);
        departmentReloadKey.value += 1;
      },
    });
  }

  async function removeSelected() {
    await executeStaticRecordAction({
      loading: saving,
      source: 'department-management',
      record: () => (selectedDepartment.value?.id ? selectedDepartment.value : undefined),
      canExecute: () => canDelete.value,
      deniedMessage: '当前用户无权删除部门',
      confirm: (department) =>
        confirmAction({
          title: '删除部门',
          content: `确认删除部门「${departmentTitleOf(department)}」？`,
          okText: '删除',
          danger: true,
        }),
      execute: (department) => departmentContext.abilities.crud().delete(department.id!),
      onExecuted: () => {
        selectedDepartment.value = undefined;
        draft.value = emptyDepartmentDraft(selectedOrganizationId.value);
        mode.value = 'view';
        departmentReloadKey.value += 1;
      },
    });
  }

  function resetDepartmentsForOrganization() {
    departments.value = [];
    selectedDepartment.value = undefined;
    draft.value = emptyDepartmentDraft(selectedOrganizationId.value);
    mode.value = 'view';
    departmentReloadKey.value += 1;
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
