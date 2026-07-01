import { computed, ref } from 'vue';

export interface RecordEditorSessionOptions<TRecord, TMode extends string> {
  viewMode: TMode;
  createMode: TMode;
  editMode: TMode;
  emptyDraft: () => TRecord;
  copyRecord?: (record: TRecord) => TRecord;
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

  function clearSelection() {
    selected.value = undefined;
    draft.value = options.emptyDraft();
  }

  function startCreate() {
    clearSelection();
    mode.value = options.createMode;
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
    clearSelection,
    startCreate,
    startEdit,
    cancel,
  };
}
