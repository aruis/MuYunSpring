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

export interface WebPageResponse<T> {
  records: T[];
  total: number;
  pageNum: number;
  pageSize: number;
  pages: number;
  totalKnown: boolean;
  navigation?: unknown;
}

export interface WebCountResponse {
  count: number;
  message?: string;
}

export interface WebRecordResponse<T> {
  record: T;
  message?: string;
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

export interface LoginRequest {
  tenantId: string;
  username: string;
  password: string;
}

export interface LoginResult {
  token: string;
  tokenType: 'Bearer' | string;
  issuedAt: string;
  currentUser: CurrentUser;
}

// Matches current Spring/Jackson code output from /platform.menu/mine.
export type MenuType = 'group' | 'module' | 'route' | 'link';

// Matches current Spring/Jackson code output from menu page mode fields.
export type MenuPageMode = 'LIST' | 'FORM' | 'DETAIL';

export type MenuOpenMode = 'tab' | 'window';

export interface MenuRecord {
  id: string;
  tenantId?: string;
  schemeId: string;
  parentId?: string;
  title: string;
  menuType: MenuType;
  openMode?: MenuOpenMode;
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
  menuType: 'module';
  openMode: MenuOpenMode;
  moduleAlias: string;
  pageMode?: MenuPageMode;
  defaultUiConfigId?: string;
  defaultQueryTemplateId?: string;
  entryParamsJson?: string;
  query?: Record<string, RouteQueryValue>;
}

export interface RouteMenuTarget {
  menuId: string;
  menuType: 'route';
  openMode: MenuOpenMode;
  route: string;
  moduleAlias?: string;
  entryParamsJson?: string;
  query?: Record<string, RouteQueryValue>;
}

export interface ExternalLinkMenuTarget {
  menuId: string;
  menuType: 'link';
  openMode: MenuOpenMode;
  externalUrl: string;
  moduleAlias?: string;
  entryParamsJson?: string;
}

export type MenuNavigationTarget = ModuleMenuTarget | RouteMenuTarget | ExternalLinkMenuTarget;

export type MenuNavigationType = MenuNavigationTarget['menuType'];

export type PageType =
  | 'platform-route'
  | 'business-route'
  | 'dynamic-module'
  | 'remote-url'
  | 'external-link';

export type OpenMode = 'workbench-route' | 'dynamic-runner' | 'iframe' | 'new-window';

export type PageHostType =
  | 'platform-route-host'
  | 'business-route-host'
  | 'dynamic-module-host'
  | 'external-page-host';

export type TabIdentityStrategy = 'by-menu' | 'by-target' | 'by-params';

export interface TabPolicy {
  identity: TabIdentityStrategy;
  closable?: boolean;
  cacheable?: boolean;
}

export interface TabRestoreState {
  url?: string;
  snapshot?: unknown;
}

export interface PageDescriptorBase<
  TPageType extends PageType,
  TOpenMode extends OpenMode,
  THostType extends PageHostType,
  TTarget,
> {
  pageType: TPageType;
  openMode: TOpenMode;
  hostType: THostType;
  title?: string;
  menuId?: string;
  target: TTarget;
  params?: Record<string, RouteQueryValue>;
  entryParamsJson?: string;
  tabPolicy: TabPolicy;
  restoreState?: TabRestoreState;
}

export interface RoutePageTarget {
  route?: string;
  routeName?: string;
  pageKey?: string;
  moduleAlias?: string;
  query?: Record<string, RouteQueryValue>;
}

export type PlatformRoutePageDescriptor = PageDescriptorBase<
  'platform-route',
  'workbench-route',
  'platform-route-host',
  RoutePageTarget
>;

export type BusinessRoutePageDescriptor = PageDescriptorBase<
  'business-route',
  'workbench-route',
  'business-route-host',
  RoutePageTarget
>;

export interface DynamicModulePageTarget {
  moduleAlias: string;
  pageMode?: MenuPageMode;
  defaultUiConfigId?: string;
  defaultQueryTemplateId?: string;
}

export type DynamicModulePageDescriptor = PageDescriptorBase<
  'dynamic-module',
  'dynamic-runner',
  'dynamic-module-host',
  DynamicModulePageTarget
>;

export interface UrlPageTarget {
  url: string;
  moduleAlias?: string;
}

export type RemoteUrlPageDescriptor = PageDescriptorBase<
  'remote-url',
  'iframe' | 'new-window',
  'external-page-host',
  UrlPageTarget
>;

export type ExternalLinkPageDescriptor = PageDescriptorBase<
  'external-link',
  'new-window',
  'external-page-host',
  UrlPageTarget
>;

export type PageDescriptor =
  | PlatformRoutePageDescriptor
  | BusinessRoutePageDescriptor
  | DynamicModulePageDescriptor
  | RemoteUrlPageDescriptor
  | ExternalLinkPageDescriptor;

export interface MenuTab {
  key: string;
  title: string;
  target?: MenuNavigationTarget;
  pageDescriptor?: PageDescriptor;
  restoreState?: TabRestoreState;
  closable?: boolean;
}

export interface WorkbenchStartupState {
  session: SessionContext;
  menus: MenuTreeNode[];
  tabs?: MenuTab[];
  activeTabKey?: string;
}

export interface WebPageRequest {
  pageNum: number;
  pageSize: number;
}

export interface WebQueryCondition {
  fieldName: string;
  operator?: string;
  values?: unknown[];
  timeZone?: string;
}

export interface WebSort {
  field: string;
  desc?: boolean;
}

export interface WebQueryRequest {
  page?: WebPageRequest;
  unpaged?: boolean;
  conditions?: WebQueryCondition[];
  criteria?: unknown;
  queryForm?: Record<string, unknown>;
  sorts?: WebSort[];
  quickSearch?: string;
  quickSearchFields?: string[];
  externalQueryValues?: Record<string, unknown>;
}

export type QueryValueType =
  | 'STRING'
  | 'TEXT'
  | 'BOOLEAN'
  | 'INTEGER'
  | 'LONG'
  | 'DECIMAL'
  | 'INSTANT'
  | 'DATE'
  | 'JSON';

export type QueryOperator =
  | 'EQ'
  | 'NOT_EQUAL'
  | 'LIKE'
  | 'IN'
  | 'NOT_IN'
  | 'GT'
  | 'GTE'
  | 'LT'
  | 'LTE'
  | 'BETWEEN'
  | 'NULL'
  | 'NOT_NULL';

export interface QuerySchemaField {
  name: string;
  title?: string;
  valueType: QueryValueType;
  operators: QueryOperator[];
  defaultOperator?: QueryOperator;
  quickSearch?: boolean;
  sortable?: boolean;
}

export interface QuerySchemaQuickSearch {
  enabled: boolean;
  fields: string[];
  fieldSchemas: QuerySchemaField[];
}

export interface QuerySchemaExternalCriteria {
  key: string;
  valueType?: string;
  providedBy?: string;
}

export interface QuerySchemaDefaultSort {
  field: string;
  desc?: boolean;
}

export interface QuerySchema {
  scopeName: string;
  entityAlias?: string;
  quickSearch: QuerySchemaQuickSearch;
  fields: QuerySchemaField[];
  externalCriteria: QuerySchemaExternalCriteria[];
  defaultSorts: QuerySchemaDefaultSort[];
}

export type ModuleViewKind = 'LIST' | 'FORM' | 'DETAIL';

export type ModuleUiClientType = 'WEB';

export interface UiRule<T> {
  constant?: T;
}

export interface ViewFieldRef {
  relationCode?: string;
  fieldName: string;
  fieldId?: string;
}

export interface ViewFieldDefinition {
  fieldRef: ViewFieldRef;
  label?: string;
  visible?: UiRule<boolean>;
  required?: UiRule<boolean>;
  readOnly?: UiRule<boolean>;
  uiType?: string;
  width?: string;
  align?: 'left' | 'center' | 'right' | string;
  fixed?: boolean;
}

export interface ResolvedViewFieldDescriptor {
  fieldRef: ViewFieldRef;
  label?: string;
  visible?: UiRule<boolean>;
  required?: UiRule<boolean>;
  readOnly?: UiRule<boolean>;
  uiType?: string;
  width?: string;
  align?: 'left' | 'center' | 'right' | string;
  fixed?: boolean;
}

export interface ViewDefinition {
  viewCode: string;
  viewKind: ModuleViewKind;
  clientType?: ModuleUiClientType;
  title?: string;
  fields: ViewFieldDefinition[];
}

export interface ResolvedViewDescriptor {
  viewCode: string;
  viewKind: ModuleViewKind;
  clientType?: ModuleUiClientType;
  title?: string;
  fields: ResolvedViewFieldDescriptor[];
}

export interface ModuleUiDefinition {
  moduleAlias: string;
  views: ViewDefinition[];
}

export interface ResolvedModuleUiDescriptor {
  moduleAlias: string;
  views: ResolvedViewDescriptor[];
}

export interface StandardEntity {
  id?: string;
  tenantId?: string;
  version?: number;
  deleted?: boolean;
  deletedAt?: string;
  createdBy?: string;
  createdAt?: string;
  updatedBy?: string;
  updatedAt?: string;
}

export interface StandardTitledEntity extends StandardEntity {
  title?: string;
}

export interface StandardSortableEntity extends StandardTitledEntity {
  sortOrder?: number;
}

export interface StandardEnabledSortableEntity extends StandardSortableEntity {
  enabled?: boolean;
}

export interface StandardTreeEntity extends StandardSortableEntity {
  parentId?: string;
}

export interface StandardEnabledTreeEntity extends StandardTreeEntity {
  enabled?: boolean;
}

export interface Organization extends StandardEnabledTreeEntity {
  code?: string;
}

export interface Department extends StandardEnabledTreeEntity {
  organizationId?: string;
  code?: string;
}

export interface Employee extends StandardEnabledSortableEntity {
  organizationId?: string;
  departmentId?: string;
  employeeNo?: string;
  mobile?: string;
  email?: string;
}

export interface Application extends StandardEnabledSortableEntity {
  alias?: string;
}

export interface Tenant extends StandardEnabledSortableEntity {
  alias?: string;
}

export interface PositionCategory extends StandardEnabledTreeEntity {
  code?: string;
  description?: string;
}

export interface Position extends StandardEnabledSortableEntity {
  categoryId?: string;
  code?: string;
  description?: string;
}

export type DictionaryCategoryKind = 'FOLDER' | 'DICTIONARY' | 'folder' | 'dictionary';

export interface DictionaryCategory extends StandardEnabledTreeEntity {
  applicationAlias?: string;
  alias?: string;
  categoryKind?: DictionaryCategoryKind;
}

export interface DictionaryItem extends StandardEnabledTreeEntity {
  categoryId?: string;
  categoryAlias?: string;
  code?: string;
}

export interface TreeSortRequest {
  previousId?: string | null;
  nextId?: string | null;
  parentId?: string | null;
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
