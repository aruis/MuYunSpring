import {
  createDataChangeDispatcher,
  createRealtimeClient,
  connectRealtimeDataChanges,
  connectRealtimeUserNotifications,
} from '@muyun/web-core';
import type { WebUserNotification } from '@muyun/web-contracts';
import { effectiveAuthToken } from './authSession';

export const appDataChangeDispatcher = createDataChangeDispatcher();

export interface AppRealtimeOptions {
  onUnauthorized?: () => void;
  onUserNotification?: (notification: WebUserNotification) => void;
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
  const dataChangeSubscription = connectRealtimeDataChanges(realtime, appDataChangeDispatcher);
  const userNotificationSubscription = connectRealtimeUserNotifications(realtime, (notification) => {
    options.onUserNotification?.(notification);
  });
  void realtime.connect();
  return {
    realtime,
    async disconnect() {
      dataChangeSubscription.unsubscribe();
      userNotificationSubscription.unsubscribe();
      await realtime.disconnect();
    },
  };
}
