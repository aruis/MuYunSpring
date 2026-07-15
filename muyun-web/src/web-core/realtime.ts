import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs';
import type { WebCommittedChangeSet, WebRealtimeEnvelope } from '@muyun/web-contracts';
import type { DataChangeDispatcher } from './dataChanges';

export type RealtimeConnectionState =
  | 'idle'
  | 'connecting'
  | 'connected'
  | 'reconnecting'
  | 'disconnected'
  | 'unauthorized'
  | 'failed';

export interface RealtimeChannel<TPayload> {
  destination: string;
  type?: string;
  readonly __payload?: TPayload;
}

export interface RealtimeCommand<TPayload> {
  destination: string;
  readonly __payload?: TPayload;
}

export type RealtimeHandler<TPayload> = (
  payload: TPayload,
  envelope: WebRealtimeEnvelope<TPayload> | undefined,
) => void | Promise<void>;

export interface RealtimeSubscription {
  unsubscribe(): void;
}

export interface RealtimeClient {
  connect(): Promise<void>;
  disconnect(): Promise<void>;
  subscribe<TPayload>(
    channel: RealtimeChannel<TPayload>,
    handler: RealtimeHandler<TPayload>,
  ): RealtimeSubscription;
  publish<TPayload>(command: RealtimeCommand<TPayload>, payload: TPayload): void;
  state(): RealtimeConnectionState;
}

export interface RealtimeClientOptions {
  baseUrl?: string;
  token?: string;
  brokerPath?: string;
  reconnectDelay?: number;
  heartbeatIncoming?: number;
  heartbeatOutgoing?: number;
  clientFactory?: StompClientFactory;
  onStateChange?: (state: RealtimeConnectionState) => void;
}

export interface StompClientFactory {
  (options: StompClientFactoryOptions): StompClientAdapter;
}

export interface StompClientFactoryOptions {
  brokerURL: string;
  connectHeaders: Record<string, string>;
  reconnectDelay: number;
  heartbeatIncoming: number;
  heartbeatOutgoing: number;
  onConnect: () => void;
  onDisconnect: () => void;
  onWebSocketClose: () => void;
  onStompError: () => void;
}

export interface StompClientAdapter {
  connected: boolean;
  activate(): void;
  deactivate(): Promise<void>;
  subscribe(destination: string, handler: (message: IMessage) => void): StompSubscriptionLike;
  publish(options: { destination: string; body: string }): void;
}

export interface StompSubscriptionLike {
  unsubscribe(): void;
}

export const realtimeMessageTypes = {
  dataChange: 'platform.data-change',
} as const;

export const dataChangeChannel: RealtimeChannel<WebCommittedChangeSet> = {
  destination: '/user/queue/platform/data-changes',
  type: realtimeMessageTypes.dataChange,
};

export const platformPingCommand: RealtimeCommand<{ timestamp: string }> = {
  destination: '/app/platform/ping',
};

export function createRealtimeClient(options: RealtimeClientOptions = {}): RealtimeClient {
  let connectionState: RealtimeConnectionState = 'idle';
  const subscriptions = new Set<TrackedSubscription<unknown>>();
  const client = (options.clientFactory ?? defaultStompClientFactory)({
    brokerURL: brokerUrlOf(options),
    connectHeaders: connectHeadersOf(options.token),
    reconnectDelay: options.reconnectDelay ?? 5000,
    heartbeatIncoming: options.heartbeatIncoming ?? 10000,
    heartbeatOutgoing: options.heartbeatOutgoing ?? 10000,
    onConnect: () => {
      setState('connected');
      restoreSubscriptions();
    },
    onDisconnect: () => setState('disconnected'),
    onWebSocketClose: () => setState(connectionState === 'connected' ? 'reconnecting' : 'disconnected'),
    onStompError: () => setState('failed'),
  });

  return {
    async connect() {
      if (connectionState === 'connected' || connectionState === 'connecting') {
        return;
      }
      setState('connecting');
      client.activate();
    },
    async disconnect() {
      for (const subscription of subscriptions) {
        unsubscribeActive(subscription);
      }
      await client.deactivate();
      setState('disconnected');
    },
    subscribe(channel, handler) {
      const subscription: TrackedSubscription<unknown> = {
        channel: channel as RealtimeChannel<unknown>,
        handler: handler as RealtimeHandler<unknown>,
      };
      subscriptions.add(subscription);
      if (client.connected) {
        activateSubscription(subscription);
      }
      return {
        unsubscribe() {
          subscriptions.delete(subscription);
          unsubscribeActive(subscription);
        },
      };
    },
    publish(command, payload) {
      if (!client.connected) {
        throw new Error('Realtime client is not connected');
      }
      client.publish({
        destination: command.destination,
        body: JSON.stringify(payload),
      });
    },
    state() {
      return connectionState;
    },
  };

  function setState(state: RealtimeConnectionState) {
    connectionState = state;
    options.onStateChange?.(state);
  }

  function restoreSubscriptions() {
    for (const subscription of subscriptions) {
      unsubscribeActive(subscription);
      activateSubscription(subscription);
    }
  }

  function activateSubscription(subscription: TrackedSubscription<unknown>) {
    subscription.active = client.subscribe(subscription.channel.destination, (message) => {
      const parsed = parseRealtimeMessage(message.body);
      if (subscription.channel.type && parsed.envelope?.type !== subscription.channel.type) {
        return;
      }
      void subscription.handler(parsed.payload, parsed.envelope);
    });
  }
}

export function connectRealtimeDataChanges(
  realtime: RealtimeClient,
  dispatcher: DataChangeDispatcher,
): RealtimeSubscription {
  return realtime.subscribe(dataChangeChannel, (changeSet) => {
    void dispatcher.dispatch(changeSet);
  });
}

function defaultStompClientFactory(options: StompClientFactoryOptions): StompClientAdapter {
  const client = new Client({
    brokerURL: options.brokerURL,
    connectHeaders: options.connectHeaders,
    reconnectDelay: options.reconnectDelay,
    heartbeatIncoming: options.heartbeatIncoming,
    heartbeatOutgoing: options.heartbeatOutgoing,
    onConnect: options.onConnect,
    onDisconnect: options.onDisconnect,
    onWebSocketClose: options.onWebSocketClose,
    onStompError: options.onStompError,
  });
  return client;
}

function brokerUrlOf(options: RealtimeClientOptions) {
  const brokerPath = options.brokerPath ?? '/ws/platform';
  const baseUrl =
    options.baseUrl ?? (typeof window === 'undefined' ? 'http://localhost' : window.location.origin);
  const url = new URL(brokerPath, baseUrl.endsWith('/') ? baseUrl : `${baseUrl}/`);
  if (url.protocol === 'https:') {
    url.protocol = 'wss:';
  } else {
    url.protocol = 'ws:';
  }
  return url.toString();
}

function connectHeadersOf(token: string | undefined) {
  const headers: Record<string, string> = {};
  if (token?.trim()) {
    headers.Authorization = `Bearer ${token.trim()}`;
  }
  return headers;
}

function parseRealtimeMessage(body: string): {
  envelope?: WebRealtimeEnvelope<unknown>;
  payload: unknown;
} {
  const value = JSON.parse(body) as unknown;
  if (isRealtimeEnvelope(value)) {
    return {
      envelope: value,
      payload: value.payload,
    };
  }
  return { payload: value };
}

function isRealtimeEnvelope(value: unknown): value is WebRealtimeEnvelope<unknown> {
  if (!value || typeof value !== 'object') {
    return false;
  }
  const envelope = value as { id?: unknown; type?: unknown; payload?: unknown };
  return typeof envelope.id === 'string' && typeof envelope.type === 'string' && 'payload' in envelope;
}

function unsubscribeActive(subscription: TrackedSubscription<unknown>) {
  subscription.active?.unsubscribe();
  subscription.active = undefined;
}

interface TrackedSubscription<TPayload> {
  channel: RealtimeChannel<TPayload>;
  handler: RealtimeHandler<TPayload>;
  active?: StompSubscription | StompSubscriptionLike;
}
