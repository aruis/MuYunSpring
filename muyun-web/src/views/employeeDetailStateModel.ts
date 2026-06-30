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

export function isEmployeeFormDisabled(state: EmployeeFormDisabledState) {
  if (state.mode === 'view' || state.loadingDetail || state.saving) {
    return true;
  }
  return state.mode === 'edit' && !state.selectedEmployeeId;
}

export function shouldCommitEmployeeDetailRequest(state: EmployeeDetailRequestState) {
  return state.activeRequestSeq === state.requestSeq && state.selectedEmployeeKey === state.recordId;
}
