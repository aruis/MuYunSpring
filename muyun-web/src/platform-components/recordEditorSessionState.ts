import { computed, ref } from 'vue';

export interface RecordEditorSessionOptions<TRecord, TMode extends string> {
  viewMode: TMode;
  createMode: TMode;
  editMode: TMode;
  emptyDraft: () => TRecord;
  copyRecord?: (record: TRecord) => TRecord;
}

export interface RecordEditorSessionCreateOptions<TRecord, TMode extends string> {
  mode?: TMode;
  draft?: TRecord | (() => TRecord);
  selectedRecord?: TRecord;
  preserveSelection?: boolean;
}

export function createRecordEditorSessionState<TRecord, TMode extends string>(
  options: RecordEditorSessionOptions<TRecord, TMode>,
) {
  const selected = ref<TRecord>();
  const draft = ref<TRecord>(options.emptyDraft());
  const mode = ref<TMode>(options.viewMode);
  const copyRecord = options.copyRecord ?? ((record: TRecord) => ({ ...record }) as TRecord);
  const readonly = computed(() => mode.value === options.viewMode);

  function select(record: TRecord) {
    selected.value = record;
    draft.value = copyRecord(record);
    mode.value = options.viewMode;
  }

  function replaceSelected(record: TRecord | undefined) {
    selected.value = record;
  }

  function clearSelection() {
    selected.value = undefined;
    draft.value = options.emptyDraft();
  }

  function createDraft(draftOption: RecordEditorSessionCreateOptions<TRecord, TMode>['draft']) {
    return typeof draftOption === 'function' ? (draftOption as () => TRecord)() : draftOption;
  }

  function startCreate(createOptions: RecordEditorSessionCreateOptions<TRecord, TMode> = {}) {
    const nextMode = createOptions.mode ?? options.createMode;
    if (nextMode === options.viewMode || nextMode === options.editMode) {
      throw new Error('Record editor create mode cannot be view or edit mode');
    }
    if (createOptions.selectedRecord !== undefined) {
      selected.value = createOptions.selectedRecord;
    } else if (!createOptions.preserveSelection) {
      selected.value = undefined;
    }
    draft.value = createDraft(createOptions.draft) ?? options.emptyDraft();
    mode.value = nextMode;
  }

  function startEdit() {
    if (!selected.value) {
      return false;
    }
    draft.value = copyRecord(selected.value);
    mode.value = options.editMode;
    return true;
  }

  function cancel() {
    draft.value = selected.value ? copyRecord(selected.value) : options.emptyDraft();
    mode.value = options.viewMode;
  }

  return {
    selected,
    draft,
    mode,
    readonly,
    select,
    replaceSelected,
    clearSelection,
    startCreate,
    startEdit,
    cancel,
  };
}
