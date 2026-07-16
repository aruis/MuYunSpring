export function normalizeRecordDraft<TRecord extends object>(
  draft: Partial<TRecord>,
  normalizedFields: Partial<TRecord>,
): TRecord {
  return {
    ...draft,
    ...normalizedFields,
  } as TRecord;
}
