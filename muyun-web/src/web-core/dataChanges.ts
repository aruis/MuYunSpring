import type { WebCommittedChangeSet, WebDataChange } from '@muyun/web-contracts';

export type DataChangeHandler = (changeSet: WebCommittedChangeSet) => void | Promise<void>;

export interface DataChangeDispatcher {
  dispatch(changeSet: WebCommittedChangeSet): Promise<boolean>;
  subscribe(handler: DataChangeHandler): DataChangeSubscription;
  markHandled(changeSetId: string | undefined): void;
}

export interface DataChangeDispatcherOptions {
  maxHandledChangeSetIds?: number;
}

export interface DataChangeSubscription {
  unsubscribe(): void;
}

export function createDataChangeDispatcher(options: DataChangeDispatcherOptions = {}): DataChangeDispatcher {
  const maxHandledChangeSetIds = Math.max(1, options.maxHandledChangeSetIds ?? 1000);
  const handledChangeSetIds = new Set<string>();
  const handlers = new Set<DataChangeHandler>();

  return {
    async dispatch(changeSet) {
      const normalized = normalizeChangeSet(changeSet);
      if (!normalized || handledChangeSetIds.has(normalized.changeSetId)) {
        return false;
      }
      rememberHandled(normalized.changeSetId);
      await Promise.allSettled(
        [...handlers].map((handler) => Promise.resolve().then(() => handler(normalized))),
      );
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
        rememberHandled(normalized);
      }
    },
  };

  function rememberHandled(changeSetId: string) {
    handledChangeSetIds.delete(changeSetId);
    handledChangeSetIds.add(changeSetId);
    while (handledChangeSetIds.size > maxHandledChangeSetIds) {
      const oldest = handledChangeSetIds.values().next().value;
      if (!oldest) {
        return;
      }
      handledChangeSetIds.delete(oldest);
    }
  }
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
