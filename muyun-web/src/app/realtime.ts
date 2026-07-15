import {
  createDataChangeDispatcher,
  createRealtimeClient,
  connectRealtimeDataChanges,
} from '@muyun/web-core';
import { effectiveAuthToken } from './authSession';

export const appDataChangeDispatcher = createDataChangeDispatcher();

export function createAppRealtimeClient() {
  return createRealtimeClient({
    baseUrl: import.meta.env.VITE_MUYUN_API_BASE_URL,
    token: effectiveAuthToken(import.meta.env.VITE_MUYUN_AUTH_TOKEN),
  });
}

export function connectAppRealtime() {
  const realtime = createAppRealtimeClient();
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
