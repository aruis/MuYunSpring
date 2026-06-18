export type MuyunPrimitive = string | number | boolean | null | undefined;

export type MuyunOptionValue = string | number;

export interface MuyunOption {
  label: string;
  value: MuyunOptionValue;
  disabled?: boolean;
}

export type MuyunFieldKind = 'input' | 'select' | 'dictionary-select' | 'reference-select';

export interface MuyunFieldCondition {
  field: string;
  equals?: MuyunPrimitive;
  notEquals?: MuyunPrimitive;
}

export interface MuyunReferenceContract {
  targetModuleAlias: string;
  keyField: string;
  labelField: string;
  fillBack?: Record<string, string>;
}

export interface MuyunFieldContract {
  name: string;
  label: string;
  kind: MuyunFieldKind;
  placeholder?: string;
  required?: boolean;
  disabled?: boolean;
  options?: MuyunOption[];
  dictionaryAlias?: string;
  reference?: MuyunReferenceContract;
  visibleWhen?: MuyunFieldCondition;
  readonlyWhen?: MuyunFieldCondition;
  requiredWhen?: MuyunFieldCondition;
}

export interface MuyunFormContract {
  title?: string;
  fields: MuyunFieldContract[];
}

export interface MuyunTableColumn {
  key: string;
  title: string;
  width?: number;
  dictionaryAlias?: string;
}

export interface MuyunTableContract {
  rowKey?: string;
  columns: MuyunTableColumn[];
}

export interface MuyunActionContract {
  actionCode: string;
  title: string;
  level?: 'primary' | 'default' | 'danger';
  disabled?: boolean;
  disabledReason?: string;
  refresh?: 'none' | 'record' | 'list' | 'all';
}

export type MuyunRecord = Record<string, MuyunPrimitive>;

export interface MuyunDynamicPageDescriptor {
  moduleAlias: string;
  title: string;
  form: MuyunFormContract;
  list: MuyunTableContract;
  actions: MuyunActionContract[];
  initialRecord: MuyunRecord;
  records: MuyunRecord[];
}
