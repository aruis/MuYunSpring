import type { ResolvedModuleUiDescriptor } from '@muyun/web-contracts';

export function actionConfirmationRequiredText(
  descriptor: ResolvedModuleUiDescriptor | undefined,
  actionCode: string,
  record: Record<string, unknown>,
): string | undefined {
  const confirmation = descriptor?.actions?.find((action) => action.actionCode === actionCode)?.confirmation;
  if (confirmation?.mode !== 'typedText') {
    return undefined;
  }
  const value = record[confirmation.requiredField];
  return typeof value === 'string' && value.trim() ? value.trim() : undefined;
}

export function recordLabelOf(
  descriptor: ResolvedModuleUiDescriptor | undefined,
  record: Record<string, unknown>,
): string | undefined {
  const field = descriptor?.recordLabelField;
  const value = field ? record[field] : (record.title ?? record.alias ?? record.id);
  return typeof value === 'string' && value.trim() ? value.trim() : undefined;
}
