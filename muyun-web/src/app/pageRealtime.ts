import { onMounted, onUnmounted } from 'vue';
import type { WebCommittedChangeSet } from '@muyun/web-contracts';
import type { DataChangeSubscription } from '@muyun/web-core';
import { subscribeAppDataChanges, subscribeAppModuleDataChanges } from './realtime';

export function usePageDataChangeHandler(
  handler: (changeSet: WebCommittedChangeSet) => void | Promise<void>,
) {
  usePageSubscription(() => subscribeAppDataChanges(handler));
}

export function usePageModuleDataChanges(moduleAlias: string) {
  usePageSubscription(() => subscribeAppModuleDataChanges(moduleAlias));
}

function usePageSubscription(factory: () => DataChangeSubscription) {
  let subscription: DataChangeSubscription | undefined;
  onMounted(() => {
    subscription = factory();
  });
  onUnmounted(() => {
    subscription?.unsubscribe();
    subscription = undefined;
  });
}
