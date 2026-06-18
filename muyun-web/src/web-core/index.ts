import { QueryClient, VueQueryPlugin } from '@tanstack/vue-query';
import type { CurrentUser, MenuMineResponse, RouteQueryValue } from '@muyun/web-contracts';

export interface RequestContext {
  baseUrl?: string;
  token?: string;
  traceId?: string;
  headers?: Record<string, string>;
}

export interface HttpRequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  path: string;
  query?: Record<string, RouteQueryValue>;
  body?: unknown;
  headers?: Record<string, string>;
}

export class AppError extends Error {
  readonly code: string;
  readonly status?: number;
  readonly details?: unknown;

  constructor(message: string, options: { code?: string; status?: number; details?: unknown } = {}) {
    super(message);
    this.name = 'AppError';
    this.code = options.code ?? 'APP_ERROR';
    this.status = options.status;
    this.details = options.details;
  }
}

export interface HttpClient {
  request<T>(options: HttpRequestOptions): Promise<T>;
}

export function createHttpClient(context: RequestContext = {}): HttpClient {
  return {
    async request<T>(options: HttpRequestOptions): Promise<T> {
      let response: Response;
      try {
        response = await fetch(urlOf(context.baseUrl, options), {
          method: options.method ?? 'GET',
          headers: headersOf(context, options),
          body: options.body === undefined ? undefined : JSON.stringify(options.body),
        });
      } catch (error) {
        throw new AppError('Network request failed', { code: 'NETWORK_ERROR', details: error });
      }

      if (!response.ok) {
        throw await appErrorFromResponse(response);
      }

      return (await responseBody(response)) as T;
    },
  };
}

export function createQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 30_000,
        refetchOnWindowFocus: false,
      },
    },
  });
}

export const queryKeys = {
  session: {
    current: ['session', 'current'] as const,
  },
  menu: {
    mine: ['menu', 'mine'] as const,
  },
};

export interface SessionClient {
  current(): Promise<CurrentUser>;
}

export interface MenuClient {
  mine(): Promise<MenuMineResponse>;
}

export function createSessionClient(http: HttpClient): SessionClient {
  return {
    current: () => http.request<CurrentUser>({ path: '/iam.auth/context' }),
  };
}

export function createMenuClient(http: HttpClient): MenuClient {
  return {
    mine: () => http.request<MenuMineResponse>({ path: '/platform.menu/mine' }),
  };
}

function urlOf(baseUrl: string | undefined, options: HttpRequestOptions) {
  const base = baseUrl?.replace(/\/$/, '') ?? '';
  const path = options.path.startsWith('/') ? options.path : `/${options.path}`;
  const url = new URL(`${base}${path}`, window.location.origin);
  Object.entries(options.query ?? {}).forEach(([key, value]) => {
    const values = Array.isArray(value) ? value : [value];
    for (const item of values) {
      if (item !== null && item !== undefined) {
        url.searchParams.append(key, String(item));
      }
    }
  });
  return url;
}

function headersOf(context: RequestContext, options: HttpRequestOptions) {
  const headers: Record<string, string> = {
    Accept: 'application/json',
    ...context.headers,
  };
  if (options.body !== undefined) {
    headers['Content-Type'] = 'application/json';
  }
  if (context.token) {
    headers.Authorization = `Bearer ${context.token}`;
  }
  if (context.traceId) {
    headers['X-Trace-Id'] = context.traceId;
  }
  Object.assign(headers, options.headers);
  return headers;
}

async function appErrorFromResponse(response: Response) {
  const details = await responseBody(response);
  const message = messageOf(details) ?? `Request failed with status ${response.status}`;
  const code = codeOf(details) ?? 'HTTP_ERROR';
  return new AppError(message, { code, status: response.status, details });
}

async function responseBody(response: Response) {
  if (response.status === 204) {
    return undefined;
  }
  const text = await response.text();
  if (!text) {
    return undefined;
  }
  const contentType = response.headers.get('content-type') ?? '';
  if (contentType.includes('application/json')) {
    return JSON.parse(text) as unknown;
  }
  return text;
}

function messageOf(details: unknown) {
  return objectField(details, 'message');
}

function codeOf(details: unknown) {
  return objectField(details, 'code');
}

function objectField(value: unknown, key: string) {
  if (!value || typeof value !== 'object' || !(key in value)) {
    return undefined;
  }
  const field = (value as Record<string, unknown>)[key];
  return typeof field === 'string' ? field : undefined;
}

export { VueQueryPlugin };
