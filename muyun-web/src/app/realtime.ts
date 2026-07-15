import {
  createDataChangeDispatcher,
  createRealtimeClient,
  connectRealtimeDataChanges,
  connectRealtimeUserNotifications,
  moduleDataChangeChannel,
  type RealtimeClient,
  type RealtimeSubscription,
} from '@muyun/web-core';
import type { WebCommittedChangeSet, WebUserNotification } from '@muyun/web-contracts';
import { effectiveAuthToken } from './authSession';

export const appDataChangeDispatcher = createDataChangeDispatcher();
const pageRealtimeSubscriptions = new Map<number, PageRealtimeSubscription>();
let activeRealtime: RealtimeClient | undefined;
let nextPageRealtimeSubscriptionId = 1;

export interface AppRealtimeOptions {
  onUnauthorized?: () => void;
  onUserNotification?: (notification: WebUserNotification) => void;
}

interface PageRealtimeSubscription {
  bind(realtime: RealtimeClient): RealtimeSubscription;
  active?: RealtimeSubscription;
}

export function createAppRealtimeClient(options: AppRealtimeOptions = {}) {
  return createRealtimeClient({
    baseUrl: import.meta.env.VITE_MUYUN_API_BASE_URL,
    token: effectiveAuthToken(import.meta.env.VITE_MUYUN_AUTH_TOKEN),
    onStateChange: (state) => {
      if (state === 'unauthorized') {
        options.onUnauthorized?.();
      }
    },
  });
}

export function connectAppRealtime(options: AppRealtimeOptions = {}) {
  const realtime = createAppRealtimeClient(options);
  activeRealtime = realtime;
  const dataChangeSubscription = connectRealtimeDataChanges(realtime, appDataChangeDispatcher);
  const userNotificationSubscription = connectRealtimeUserNotifications(realtime, (notification) => {
    options.onUserNotification?.(notification);
  });
  bindPageRealtimeSubscriptions(realtime);
  void realtime.connect();
  return {
    realtime,
    async disconnect() {
      dataChangeSubscription.unsubscribe();
      userNotificationSubscription.unsubscribe();
      unbindPageRealtimeSubscriptions();
      if (activeRealtime === realtime) {
        activeRealtime = undefined;
      }
      await realtime.disconnect();
    },
  };
}

export function subscribeAppModuleDataChanges(moduleAlias: string) {
  return registerPageRealtimeSubscription((realtime) =>
    realtime.subscribe(moduleDataChangeChannel(moduleAlias), (changeSet) => {
      void appDataChangeDispatcher.dispatch(changeSet);
    }),
  );
}

export function subscribeAppDataChanges(handler: (changeSet: WebCommittedChangeSet) => void | Promise<void>) {
  return appDataChangeDispatcher.subscribe(handler);
}

function registerPageRealtimeSubscription(bind: PageRealtimeSubscription['bind']) {
  const id = nextPageRealtimeSubscriptionId;
  nextPageRealtimeSubscriptionId += 1;
  const subscription: PageRealtimeSubscription = { bind };
  pageRealtimeSubscriptions.set(id, subscription);
  if (activeRealtime) {
    subscription.active = subscription.bind(activeRealtime);
  }
  return {
    unsubscribe() {
      subscription.active?.unsubscribe();
      subscription.active = undefined;
      pageRealtimeSubscriptions.delete(id);
    },
  };
}

function bindPageRealtimeSubscriptions(realtime: RealtimeClient) {
  for (const subscription of pageRealtimeSubscriptions.values()) {
    subscription.active?.unsubscribe();
    subscription.active = subscription.bind(realtime);
  }
}

function unbindPageRealtimeSubscriptions() {
  for (const subscription of pageRealtimeSubscriptions.values()) {
    subscription.active?.unsubscribe();
    subscription.active = undefined;
  }
}
