export interface ListRecordBase {
  id?: string;
  title?: string;
  alias?: string;
  code?: string;
  enabled?: boolean;
}

export function defaultListRecordTitle<TRecord extends ListRecordBase>(
  record: TRecord,
  fallback = '未命名记录',
) {
  return record.title ?? record.alias ?? record.code ?? record.id ?? fallback;
}

export function defaultListRecordSubtitle<TRecord extends ListRecordBase>(record: TRecord) {
  return record.alias ?? record.code ?? record.id;
}

export function defaultListRecordMatches<TRecord extends ListRecordBase>(
  record: TRecord,
  normalizedKeyword: string,
  titleOf: (record: TRecord) => string = defaultListRecordTitle,
  subtitleOf: (record: TRecord) => string | undefined = defaultListRecordSubtitle,
) {
  return [titleOf(record), subtitleOf(record), record.alias, record.code, record.id]
    .filter(Boolean)
    .some((item) => String(item).toLowerCase().includes(normalizedKeyword));
}
