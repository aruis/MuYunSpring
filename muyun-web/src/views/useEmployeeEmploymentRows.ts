import { ref } from 'vue';
import { presentPlatformError } from '@muyun/platform-components';
import type { Employee, EmploymentSelectorItem } from '@muyun/web-contracts';
import type { ModuleContext } from '@muyun/web-core';

export interface EmployeeEmploymentRowState {
  records: EmploymentSelectorItem[];
  loading: boolean;
  error?: string;
}

export function useEmployeeEmploymentRows(options: {
  context: ModuleContext<Employee>;
  source: string;
  pathOf?: (employeeId: string) => string;
}) {
  const expandedEmployeeKeys = ref<string[]>([]);
  const states = ref<Record<string, EmployeeEmploymentRowState>>({});

  async function loadEmployeeEmploymentRows(employeeId: string | undefined) {
    if (!employeeId || options.context.can('employeePositions', employeeId) === false) {
      return;
    }
    setState(employeeId, { ...employmentRowState(employeeId), loading: true, error: undefined });
    try {
      const response = await options.context.http.request<{ records: EmploymentSelectorItem[] }>({
        path:
          options.pathOf?.(employeeId) ?? `/iam.employee/${encodeURIComponent(employeeId)}/employment-view`,
      });
      setState(employeeId, { records: response.records, loading: false, error: undefined });
    } catch (cause) {
      const error = presentPlatformError(cause, {
        source: `${options.source}-employment-rows`,
        phase: 'load',
      });
      setState(employeeId, { ...employmentRowState(employeeId), loading: false, error: error.message });
    }
  }

  function handleEmployeeRowExpand(record: { id?: string }, expanded: boolean) {
    const employeeId = String(record.id ?? '');
    if (!employeeId) {
      return;
    }
    expandedEmployeeKeys.value = expanded
      ? Array.from(new Set([...expandedEmployeeKeys.value, employeeId]))
      : expandedEmployeeKeys.value.filter((key) => key !== employeeId);
    if (expanded && employmentRowState(employeeId).records.length === 0) {
      void loadEmployeeEmploymentRows(employeeId);
    }
  }

  function employmentRowState(employeeId: string | undefined): EmployeeEmploymentRowState {
    if (!employeeId) {
      return { records: [], loading: false };
    }
    return states.value[employeeId] ?? { records: [], loading: false };
  }

  function setState(employeeId: string, state: EmployeeEmploymentRowState) {
    states.value = { ...states.value, [employeeId]: state };
  }

  function resetEmployeeEmploymentRows() {
    expandedEmployeeKeys.value = [];
    states.value = {};
  }

  return {
    expandedEmployeeKeys,
    employmentRowState,
    handleEmployeeRowExpand,
    loadEmployeeEmploymentRows,
    resetEmployeeEmploymentRows,
  };
}
