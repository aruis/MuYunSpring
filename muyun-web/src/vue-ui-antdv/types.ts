import type { UiIconName } from './components/UiIcon.vue';

export type UiDataTableKey = string | number;
export type UiDataTableRecord = Record<string, unknown>;

export interface UiDataTableColumn {
  key: string;
  title: string;
  dataIndex?: string;
  width?: string | number;
  align?: 'left' | 'center' | 'right';
  fixed?: 'left' | 'right' | boolean;
}

export interface UiDataTablePagination {
  pageSize: number;
  showSizeChanger?: boolean;
}

export interface UiDataTableSelection {
  selectedRowKeys: UiDataTableKey[];
  preserveSelectedRowKeys?: boolean;
  disabledOf?: (record: UiDataTableRecord) => boolean;
  onChange?: (keys: UiDataTableKey[]) => void;
}

export interface UiMenuItem {
  key: string;
  title: string;
  disabled?: boolean;
  children?: UiMenuItem[];
}

export interface UiTabItem {
  key: string;
  title: string;
  closable?: boolean;
}

export interface UiDropdownItem {
  key: string;
  title: string;
  disabled?: boolean;
  danger?: boolean;
}

export interface UiRecordInlineAction {
  key: string;
  title: string;
  iconName?: UiIconName;
  showLabel?: boolean;
  disabled?: boolean;
  danger?: boolean;
}

export type UiTreeNodeAction = UiRecordInlineAction;

export interface UiTreeNode {
  key: string;
  title: string;
  disabled?: boolean;
  secondary?: string;
  tag?: string;
  muted?: boolean;
  actions?: UiRecordInlineAction[];
  children?: UiTreeNode[];
}

export interface UiConfirmOptions {
  title: string;
  content?: string;
  okText?: string;
  cancelText?: string;
  danger?: boolean;
  requiredText?: string;
}
