import type { WebCommittedChangeSet, WebDataChange } from '@muyun/web-contracts';

export type DataChangeHandler = (changeSet: WebCommittedChangeSet) => void | Promise<void>;

export interface DataChangeDispatcher {
  dispatch(changeSet: WebCommittedChangeSet): Promise<boolean>;
  subscribe(handler: DataChangeHandler): DataChangeSubscription;
  markHandled(changeSetId: string | undefined): void;
}

export interface DataChangeSubscription {
  unsubscribe(): void;
}

export function createDataChangeDispatcher(): DataChangeDispatcher {
  const handledChangeSetIds = new Set<string>();
  const handlers = new Set<DataChangeHandler>();

  return {
    async dispatch(changeSet) {
      const normalized = normalizeChangeSet(changeSet);
      if (!normalized || handledChangeSetIds.has(normalized.changeSetId)) {
        return false;
      }
      handledChangeSetIds.add(normalized.changeSetId);
      await Promise.all([...handlers].map((handler) => handler(normalized)));
      return true;
    },
    subscribe(handler) {
      handlers.add(handler);
      return {
        unsubscribe() {
          handlers.delete(handler);
        },
      };
    },
    markHandled(changeSetId) {
      const normalized = normalizeChangeSetId(changeSetId);
      if (normalized) {
        handledChangeSetIds.add(normalized);
      }
    },
  };
}

export function normalizeChangeSet(
  changeSet: WebCommittedChangeSet | undefined,
): WebCommittedChangeSet | undefined {
  const changeSetId = normalizeChangeSetId(changeSet?.changeSetId);
  if (!changeSetId || !Array.isArray(changeSet?.changes)) {
    return undefined;
  }
  return {
    changeSetId,
    changes: changeSet.changes.filter(isWebDataChange),
  };
}

function normalizeChangeSetId(changeSetId: string | undefined) {
  const normalized = changeSetId?.trim();
  return normalized ? normalized : undefined;
}

function isWebDataChange(change: unknown): change is WebDataChange {
  if (!change || typeof change !== 'object') {
    return false;
  }
  const type = (change as { type?: unknown }).type;
  const moduleAlias = (change as { moduleAlias?: unknown }).moduleAlias;
  return (
    typeof type === 'string' &&
    type.trim() !== '' &&
    typeof moduleAlias === 'string' &&
    moduleAlias.trim() !== ''
  );
}
