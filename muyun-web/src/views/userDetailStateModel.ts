export type UserDetailMode = 'view' | 'create' | 'edit' | 'resetPassword';

export interface UserDetailRequestState {
  activeRequestSeq: number;
  requestSeq: number;
  selectedUserKey?: string;
  recordId: string;
}

export function canSwitchUserDetailContext(saving: boolean) {
  return !saving;
}

export function shouldCommitUserDetailRequest(state: UserDetailRequestState) {
  return state.activeRequestSeq === state.requestSeq && state.selectedUserKey === state.recordId;
}

export function persistedUserDetailMode(mode: UserDetailMode): 'view' | 'edit' {
  return mode === 'edit' ? 'edit' : 'view';
}

export function hasUserDetailUnsavedChanges(draft: object, selected: object) {
  const draftValues = draft as Record<string, unknown>;
  const selectedValues = selected as Record<string, unknown>;
  const keys = new Set([...Object.keys(draftValues), ...Object.keys(selectedValues)]);
  for (const key of keys) {
    if (key === 'password' || (draftValues[key] === undefined && selectedValues[key] === undefined)) {
      continue;
    }
    if (draftValues[key] !== selectedValues[key]) {
      return true;
    }
  }
  return false;
}
