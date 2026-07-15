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
const moduleDataChangeSubscriptions = new Map<string, DataChangeTopicSubscription>();
let activeRealtime: RealtimeClient | undefined;

export interface AppRealtimeOptions {
  onUnauthorized?: () => void;
  onUserNotification?: (notification: WebUserNotification) => void;
}

interface DataChangeTopicSubscription {
  moduleAlias: string;
  references: number;
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
  const normalized = moduleAlias.trim();
  if (!normalized) {
    throw new Error('Module data change subscription requires a moduleAlias');
  }
  const existing = moduleDataChangeSubscriptions.get(normalized);
  const topic = existing ?? { moduleAlias: normalized, references: 0 };
  topic.references += 1;
  moduleDataChangeSubscriptions.set(normalized, topic);
  if (!topic.active && activeRealtime) {
    topic.active = bindModuleDataChangeTopic(activeRealtime, normalized);
  }
  return {
    unsubscribe() {
      topic.references -= 1;
      if (topic.references > 0) {
        return;
      }
      topic.active?.unsubscribe();
      topic.active = undefined;
      moduleDataChangeSubscriptions.delete(normalized);
    },
  };
}

export function subscribeAppDataChanges(handler: (changeSet: WebCommittedChangeSet) => void | Promise<void>) {
  return appDataChangeDispatcher.subscribe(handler);
}

function bindPageRealtimeSubscriptions(realtime: RealtimeClient) {
  for (const subscription of moduleDataChangeSubscriptions.values()) {
    subscription.active?.unsubscribe();
    subscription.active = bindModuleDataChangeTopic(realtime, subscription.moduleAlias);
  }
}

function unbindPageRealtimeSubscriptions() {
  for (const subscription of moduleDataChangeSubscriptions.values()) {
    subscription.active?.unsubscribe();
    subscription.active = undefined;
  }
}

function bindModuleDataChangeTopic(realtime: RealtimeClient, moduleAlias: string) {
  return realtime.subscribe(moduleDataChangeChannel(moduleAlias), (changeSet) => {
    void appDataChangeDispatcher.dispatch(changeSet);
  });
}
