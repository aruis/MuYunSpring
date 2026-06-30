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
