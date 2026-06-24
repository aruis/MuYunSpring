export type RecordPickerMode = 'list' | 'tree';

export function resolveRecordPickerMode(mode: RecordPickerMode, treeAvailable: boolean): RecordPickerMode {
  return mode === 'tree' && treeAvailable ? 'tree' : 'list';
}
