export type Primitive = string | number | boolean | null | undefined;

export type OptionValue = string | number;

export interface Option {
  label: string;
  value: OptionValue;
  disabled?: boolean;
}

export type RouteQueryPrimitive = string | number | boolean | null | undefined;

export type RouteQueryValue = RouteQueryPrimitive | RouteQueryPrimitive[];

export interface WebListResponse<T> {
  records: T[];
}

export interface WebTreeNode<T> {
  record: T;
  children: WebTreeNode<T>[];
}

export interface CurrentUser {
  userId: string;
  username?: string;
  tenantId?: string;
  organizationId?: string;
  system: boolean;
}

export interface SessionContext {
  currentUser: CurrentUser;
}

// Matches current Spring/Jackson enum-name output from /platform.menu/mine.
export type MenuType = 'GROUP' | 'MODULE' | 'ROUTE' | 'LINK';

// Matches current Spring/Jackson enum-name output from menu page mode fields.
export type MenuPageMode = 'LIST' | 'FORM' | 'DETAIL';

export interface MenuRecord {
  id: string;
  tenantId?: string;
  schemeId: string;
  parentId?: string;
  title: string;
  menuType: MenuType;
  moduleAlias?: string;
  route?: string;
  externalUrl?: string;
  pageMode?: MenuPageMode;
  defaultUiConfigId?: string;
  defaultQueryTemplateId?: string;
  entryParamsJson?: string;
  enabled?: boolean;
  sortOrder?: number;
}

export type MenuTreeNode = WebTreeNode<MenuRecord>;

export type MenuMineResponse = WebListResponse<MenuTreeNode>;

export interface ModuleMenuTarget {
  menuId: string;
  menuType: 'MODULE';
  moduleAlias: string;
  pageMode?: MenuPageMode;
  defaultUiConfigId?: string;
  defaultQueryTemplateId?: string;
  entryParamsJson?: string;
  query?: Record<string, RouteQueryValue>;
}

export interface RouteMenuTarget {
  menuId: string;
  menuType: 'ROUTE';
  route: string;
  entryParamsJson?: string;
  query?: Record<string, RouteQueryValue>;
}

export interface ExternalLinkMenuTarget {
  menuId: string;
  menuType: 'LINK';
  externalUrl: string;
  entryParamsJson?: string;
}

export type MenuNavigationTarget = ModuleMenuTarget | RouteMenuTarget | ExternalLinkMenuTarget;

export type MenuNavigationType = MenuNavigationTarget['menuType'];

export interface MenuTab {
  key: string;
  title: string;
  target: MenuNavigationTarget;
  closable?: boolean;
}

export interface ShellStartupState {
  session: SessionContext;
  menus: MenuTreeNode[];
  tabs?: MenuTab[];
  activeTabKey?: string;
}

export type FieldKind = 'input' | 'select' | 'dictionary-select' | 'reference-select';

export interface FieldCondition {
  field: string;
  equals?: Primitive;
  notEquals?: Primitive;
}

export interface ReferenceContract {
  targetModuleAlias: string;
  keyField: string;
  labelField: string;
  fillBack?: Record<string, string>;
}

export interface FieldContract {
  name: string;
  label: string;
  kind: FieldKind;
  placeholder?: string;
  required?: boolean;
  disabled?: boolean;
  options?: Option[];
  dictionaryAlias?: string;
  reference?: ReferenceContract;
  visibleWhen?: FieldCondition;
  readonlyWhen?: FieldCondition;
  requiredWhen?: FieldCondition;
}

export interface FormContract {
  title?: string;
  fields: FieldContract[];
}

export interface TableColumn {
  key: string;
  title: string;
  width?: number;
  dictionaryAlias?: string;
}

export interface TableContract {
  rowKey?: string;
  columns: TableColumn[];
}

export interface ActionContract {
  actionCode: string;
  title: string;
  level?: 'primary' | 'default' | 'danger';
  disabled?: boolean;
  disabledReason?: string;
  refresh?: 'none' | 'record' | 'list' | 'all';
}

export type RecordData = Record<string, Primitive>;

export interface DynamicPageDescriptor {
  moduleAlias: string;
  title: string;
  form: FormContract;
  list: TableContract;
  actions: ActionContract[];
  initialRecord: RecordData;
  records: RecordData[];
}
