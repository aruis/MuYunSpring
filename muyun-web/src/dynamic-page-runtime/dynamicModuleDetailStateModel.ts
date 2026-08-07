export interface DynamicModuleDetailRequestState {
  activeRequestSequence: number;
  requestSequence: number;
}

export interface DynamicModuleDetailMutationState {
  hasRecord: boolean;
  saving: boolean;
  loading: boolean;
  loadFailed: boolean;
}

export function shouldCommitDynamicModuleDetailRequest(state: DynamicModuleDetailRequestState) {
  return state.activeRequestSequence === state.requestSequence;
}

export function canMutateDynamicModuleDetail(state: DynamicModuleDetailMutationState) {
  return state.hasRecord && !state.saving && !state.loading && !state.loadFailed;
}
