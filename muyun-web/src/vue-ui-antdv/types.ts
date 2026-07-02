import type { UiIconName } from './components/UiIcon.vue';

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
}
