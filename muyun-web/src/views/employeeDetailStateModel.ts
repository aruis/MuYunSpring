export type EmployeeDetailMode = 'view' | 'create' | 'edit';

export interface EmployeeFormDisabledState {
  mode: EmployeeDetailMode;
  loadingDetail: boolean;
  saving: boolean;
  selectedEmployeeId?: string;
}

export interface EmployeeDetailRequestState {
  activeRequestSeq: number;
  requestSeq: number;
  selectedEmployeeKey?: string;
  recordId: string;
}

export interface EmployeeDetailContentState {
  mode: EmployeeDetailMode;
  loadingDetail: boolean;
  loadFailed: boolean;
  selectedEmployeeId?: string;
}

export interface EmployeeDetailContextSwitchState {
  saving: boolean;
}

export interface EmployeeDetailCancelState {
  mode: EmployeeDetailMode;
  selectedEmployeeId?: string;
}

export interface EmployeeRequiredFormFieldState {
  fieldName: string;
  label: string;
  required: boolean;
  visible: boolean;
  value: unknown;
}

export function isEmployeeFormDisabled(state: EmployeeFormDisabledState) {
  if (state.mode === 'view' || state.loadingDetail || state.saving) {
    return true;
  }
  return state.mode === 'edit' && !state.selectedEmployeeId;
}

export function shouldCommitEmployeeDetailRequest(state: EmployeeDetailRequestState) {
  return state.activeRequestSeq === state.requestSeq && state.selectedEmployeeKey === state.recordId;
}

export function shouldShowEmployeeDetailContent(state: EmployeeDetailContentState) {
  if (state.mode === 'create') {
    return true;
  }
  return !state.loadingDetail && !state.loadFailed && Boolean(state.selectedEmployeeId);
}

export function canSwitchEmployeeDetailContext(state: EmployeeDetailContextSwitchState) {
  return !state.saving;
}

export function shouldCloseEmployeeDetailOnCancel(state: EmployeeDetailCancelState) {
  return state.mode !== 'edit' || !state.selectedEmployeeId;
}

export function validateEmployeeRequiredFormFields(fields: EmployeeRequiredFormFieldState[]) {
  const missingLabels = fields
    .filter((field) => field.visible && field.required && !hasEmployeeFormFieldValue(field.value))
    .map((field) => field.label);
  return missingLabels.length > 0 ? `请填写${missingLabels.join('、')}` : undefined;
}

function hasEmployeeFormFieldValue(value: unknown) {
  if (typeof value === 'string') {
    return value.trim().length > 0;
  }
  return value !== undefined && value !== null;
}
