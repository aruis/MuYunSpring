import {
  createDataChangeDispatcher,
  createRealtimeClient,
  connectRealtimeDataChanges,
} from '@muyun/web-core';
import { effectiveAuthToken } from './authSession';

export const appDataChangeDispatcher = createDataChangeDispatcher();

export interface AppRealtimeOptions {
  onUnauthorized?: () => void;
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
  void realtime.connect();
  return {
    realtime,
    async disconnect() {
      dataChangeSubscription.unsubscribe();
      await realtime.disconnect();
    },
  };
}
