import { onMounted, onUnmounted } from 'vue';
import type { WebBusinessRealtimeEvent, WebCommittedChangeSet } from '@muyun/web-contracts';
import type { DataChangeSubscription } from '@muyun/web-core';
import {
  subscribeAppBusinessEvents,
  subscribeAppDataChanges,
  subscribeAppModuleDataChanges,
} from './realtime';

export function usePageDataChangeHandler(
  handler: (changeSet: WebCommittedChangeSet) => void | Promise<void>,
) {
  usePageSubscription(() => subscribeAppDataChanges(handler));
}

export function usePageModuleDataChanges(moduleAlias: string) {
  usePageSubscription(() => subscribeAppModuleDataChanges(moduleAlias));
}

export function usePageBusinessEventHandler(
  handler: (event: WebBusinessRealtimeEvent) => void | Promise<void>,
) {
  usePageSubscription(() => subscribeAppBusinessEvents(handler));
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
