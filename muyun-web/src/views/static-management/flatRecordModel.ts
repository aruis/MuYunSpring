export interface FlatRecordBase {
  id?: string;
  title?: string;
  alias?: string;
  code?: string;
  enabled?: boolean;
}

export function defaultFlatRecordTitle<TRecord extends FlatRecordBase>(
  record: TRecord,
  fallback = '未命名记录',
) {
  return record.title ?? record.alias ?? record.code ?? record.id ?? fallback;
}

export function defaultFlatRecordSubtitle<TRecord extends FlatRecordBase>(record: TRecord) {
  return record.alias ?? record.code ?? record.id;
}

export function defaultFlatRecordMatches<TRecord extends FlatRecordBase>(
  record: TRecord,
  normalizedKeyword: string,
  titleOf: (record: TRecord) => string = defaultFlatRecordTitle,
  subtitleOf: (record: TRecord) => string | undefined = defaultFlatRecordSubtitle,
) {
  return [titleOf(record), subtitleOf(record), record.alias, record.code, record.id]
    .filter(Boolean)
    .some((item) => String(item).toLowerCase().includes(normalizedKeyword));
}
