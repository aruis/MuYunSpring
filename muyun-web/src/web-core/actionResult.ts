import { webDataChangeTypes, type WebDataChange, type WebDataChangeType } from '@muyun/web-contracts';

export interface ResolvedWebActionResult {
  message: string;
  resultType?: string;
  changes: WebDataChange[];
  raw: unknown;
}

export { webDataChangeTypes };
export type { WebDataChange, WebDataChangeType };

export const webDataChanges = {
  recordCreated(moduleAlias: string, recordId: string, facts: WebDataChangeFacts = {}): WebDataChange {
    return dataChangeOf(webDataChangeTypes.recordCreated, moduleAlias, { ...facts, recordId });
  },
  recordUpdated(moduleAlias: string, recordId: string, facts: WebDataChangeFacts = {}): WebDataChange {
    return dataChangeOf(webDataChangeTypes.recordUpdated, moduleAlias, { ...facts, recordId });
  },
  recordDeleted(moduleAlias: string, recordId: string, facts: WebDataChangeFacts = {}): WebDataChange {
    return dataChangeOf(webDataChangeTypes.recordDeleted, moduleAlias, { ...facts, recordId });
  },
  collectionChanged(moduleAlias: string, facts: WebDataChangeFacts = {}): WebDataChange {
    return dataChangeOf(webDataChangeTypes.collectionChanged, moduleAlias, facts);
  },
};

export type WebDataChangeFacts = Omit<WebDataChange, 'type' | 'moduleAlias'>;

export function withWebActionResultChanges<T>(
  result: T,
  changes: WebDataChange[],
): T & { changes: WebDataChange[] } {
  const existingChanges = actionResultChanges(result);
  const existingKeys = new Set(existingChanges.map(webDataChangeKey));
  const missingChanges = changes.filter((change) => !existingKeys.has(webDataChangeKey(change)));
  if (result && typeof result === 'object') {
    return {
      ...result,
      changes: [...existingChanges, ...missingChanges],
    };
  }
  return {
    message: undefined,
    raw: result,
    changes: missingChanges,
  } as unknown as T & { changes: WebDataChange[] };
}

export function resolveWebActionResultMessage(result: unknown, fallbackMessage = '操作成功') {
  return resolveWebActionResult(result, { fallbackMessage }).message;
}

export function resolveWebActionResult(
  result: unknown,
  options: { fallbackMessage?: string } = {},
): ResolvedWebActionResult {
  return {
    message: actionResultMessage(result) ?? options.fallbackMessage ?? '操作成功',
    resultType: actionResultType(result),
    changes: actionResultChanges(result),
    raw: result,
  };
}

export function webDataChangeKey(change: WebDataChange) {
  return [
    change.type,
    change.moduleAlias,
    stringFact(change.recordId),
    stringFact(change.resourceKey),
    stringFact(change.scope),
  ].join('|');
}

function actionResultMessage(result: unknown): string | undefined {
  if (!result || typeof result !== 'object' || !('message' in result)) {
    return undefined;
  }
  const message = (result as { message?: unknown }).message;
  return typeof message === 'string' && message.trim() ? message : undefined;
}

function actionResultType(result: unknown): string | undefined {
  if (!result || typeof result !== 'object' || !('resultType' in result)) {
    return undefined;
  }
  const resultType = (result as { resultType?: unknown }).resultType;
  return typeof resultType === 'string' && resultType.trim() ? resultType : undefined;
}

function actionResultChanges(result: unknown): WebDataChange[] {
  if (!result || typeof result !== 'object' || !('changes' in result)) {
    return [];
  }
  const changes = (result as { changes?: unknown }).changes;
  if (!Array.isArray(changes)) {
    return [];
  }
  return changes.filter(isWebDataChange);
}

function isWebDataChange(change: unknown): change is WebDataChange {
  if (!change || typeof change !== 'object') {
    return false;
  }
  const type = (change as { type?: unknown }).type;
  const moduleAlias = (change as { moduleAlias?: unknown }).moduleAlias;
  return Boolean(
    typeof type === 'string' && type.trim() && typeof moduleAlias === 'string' && moduleAlias.trim(),
  );
}

function dataChangeOf(
  type: WebDataChangeType,
  moduleAlias: string,
  facts: WebDataChangeFacts,
): WebDataChange {
  return {
    type,
    moduleAlias,
    ...facts,
  };
}

function stringFact(value: unknown) {
  return typeof value === 'string' && value.trim() ? value : '';
}
