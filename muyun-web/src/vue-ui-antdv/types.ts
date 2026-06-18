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
