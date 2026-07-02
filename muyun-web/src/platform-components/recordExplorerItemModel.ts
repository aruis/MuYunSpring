import type { UiRecordInlineAction } from '@muyun/vue-ui-antdv';

export interface RecordExplorerItemDescriptor {
  title: string;
  secondary?: string;
  tag?: string;
  muted?: boolean;
  actions?: UiRecordInlineAction[];
}
