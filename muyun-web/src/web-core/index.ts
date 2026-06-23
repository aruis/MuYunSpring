import { computed, defineComponent, inject, provide, type InjectionKey } from 'vue';
import { QueryClient, VueQueryPlugin } from '@tanstack/vue-query';
import type {
  CurrentUser,
  LoginRequest,
  LoginResult,
  MenuMineResponse,
  RouteQueryValue,
  TreeSortRequest,
  WebCountResponse,
  WebListResponse,
  WebPageResponse,
  WebQueryRequest,
  WebTreeNode,
} from '@muyun/web-contracts';

export interface RequestContext {
  baseUrl?: string;
  token?: string;
  traceId?: string;
  credentials?: RequestCredentials;
  headers?: Record<string, string>;
}

export interface HttpRequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  path: string;
  query?: Record<string, RouteQueryValue>;
  body?: unknown;
  headers?: Record<string, string>;
}

export interface ErrorTarget {
  kind?: string;
  moduleAlias?: string;
  entityAlias?: string;
  relationAlias?: string;
  fieldName?: string;
  rowIndex?: number;
  recordId?: string;
  actionCode?: string;
  attachmentId?: string;
}

export interface ErrorUiContext {
  phase: 'page-load' | 'action' | 'background';
  surface: 'workbench' | 'form' | 'table' | 'dialog' | 'unknown';
}

export type GlobalErrorSlot = 'redirect-login' | 'page-error' | 'global-toast' | 'global-modal' | 'silent';

export interface GlobalErrorPresentation {
  slot: GlobalErrorSlot;
  message: string;
  traceId?: string;
}

export const platformErrorCodes = {
  appError: 'APP_ERROR',
  networkError: 'NETWORK_ERROR',
  httpError: 'HTTP_ERROR',
  authRequired: 'AUTH_REQUIRED',
  authExpired: 'AUTH_EXPIRED',
  loginBadCredentials: 'LOGIN_BAD_CREDENTIALS',
  accessDenied: 'ACCESS_DENIED',
  validationFailed: 'VALIDATION_FAILED',
  conflictVersion: 'CONFLICT_VERSION',
  resourceNotFound: 'RESOURCE_NOT_FOUND',
  configMissing: 'CONFIG_MISSING',
  internalError: 'INTERNAL_ERROR',
} as const;

export type PlatformErrorCode = (typeof platformErrorCodes)[keyof typeof platformErrorCodes] | (string & {});

export class AppError extends Error {
  readonly code: PlatformErrorCode;
  readonly status?: number;
  readonly traceId?: string;
  readonly scope?: Record<string, unknown>;
  readonly targets: ErrorTarget[];
  readonly details?: Record<string, unknown>;

  constructor(
    message: string,
    options: {
      code?: PlatformErrorCode;
      status?: number;
      traceId?: string;
      scope?: Record<string, unknown>;
      targets?: ErrorTarget[];
      details?: Record<string, unknown>;
    } = {},
  ) {
    super(message);
    this.name = 'AppError';
    this.code = options.code ?? platformErrorCodes.appError;
    this.status = options.status;
    this.traceId = options.traceId;
    this.scope = options.scope;
    this.targets = options.targets ?? [];
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
          credentials: context.credentials,
          headers: headersOf(context, options),
          body: options.body === undefined ? undefined : JSON.stringify(options.body),
        });
      } catch (error) {
        throw new AppError('Network request failed', {
          code: platformErrorCodes.networkError,
          details: { cause: error instanceof Error ? error.message : String(error) },
        });
      }

      if (!response.ok) {
        try {
          throw await appErrorFromResponse(response);
        } catch (error) {
          if (error instanceof AppError) {
            throw error;
          }
          throw new AppError(`Request failed with status ${response.status}`, {
            code: platformErrorCodes.httpError,
            status: response.status,
            traceId: response.headers.get('X-MuYun-Trace-Id') ?? undefined,
            details: { cause: error instanceof Error ? error.message : String(error) },
          });
        }
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

export function normalizeError(error: unknown): AppError {
  if (error instanceof AppError) {
    return error;
  }
  if (error instanceof Error) {
    return new AppError(error.message, { code: platformErrorCodes.appError });
  }
  return new AppError('Unknown error', { code: platformErrorCodes.appError, details: { cause: error } });
}

export function resolveGlobalErrorPresentation(
  error: AppError,
  context: ErrorUiContext = { phase: 'action', surface: 'unknown' },
): GlobalErrorPresentation {
  if (error.status === 401) {
    if (error.code === platformErrorCodes.loginBadCredentials) {
      return presentation('global-toast', error);
    }
    return presentation('redirect-login', error);
  }
  if (context.phase === 'background') {
    return presentation('silent', error);
  }
  if ((error.status === 403 || error.status === 404) && context.phase === 'page-load') {
    return presentation('page-error', error);
  }
  if (error.status !== undefined && error.status >= 500 && context.phase === 'page-load') {
    return presentation('page-error', error);
  }
  if (error.status === 409 && context.phase === 'action') {
    return presentation('global-modal', error);
  }
  return presentation('global-toast', error);
}

export const queryKeys = {
  session: {
    current: ['session', 'current'] as const,
  },
  menu: {
    mine: ['menu', 'mine'] as const,
  },
  staticModule: {
    query: (moduleAlias: string, request?: unknown) =>
      ['static-module', moduleAlias, 'query', request] as const,
    tree: (moduleAlias: string, options?: unknown) =>
      ['static-module', moduleAlias, 'tree', options] as const,
    view: (moduleAlias: string, id: string) => ['static-module', moduleAlias, 'view', id] as const,
  },
};

export interface SessionClient {
  current(): Promise<CurrentUser>;
}

export interface MenuClient {
  mine(): Promise<MenuMineResponse>;
}

export interface AuthClient {
  login(request: LoginRequest): Promise<LoginResult>;
  logout(token?: string): Promise<void>;
}

export interface StaticModuleCrudClient<TRecord> {
  query(request?: WebQueryRequest): Promise<WebPageResponse<TRecord>>;
  view(id: string): Promise<TRecord>;
  insert(record: TRecord): Promise<TRecord>;
  update(id: string, record: TRecord): Promise<TRecord>;
  delete(id: string): Promise<WebCountResponse>;
  enable(id: string): Promise<WebCountResponse>;
  disable(id: string): Promise<WebCountResponse>;
}

export interface StaticModuleTreeClient<TRecord> extends StaticModuleCrudClient<TRecord> {
  tree(): Promise<WebListResponse<WebTreeNode<TRecord>>>;
  treeFlat(options?: { rootId?: string; includeSelf?: boolean }): Promise<WebListResponse<TRecord>>;
  subtree(id: string, options?: { includeSelf?: boolean }): Promise<WebListResponse<WebTreeNode<TRecord>>>;
  sort(id: string, request: TreeSortRequest): Promise<WebCountResponse>;
}

export interface ModuleContext<TRecord> {
  moduleAlias: string;
  crud: StaticModuleCrudClient<TRecord>;
}

export interface ModuleTreeContext<TRecord> extends ModuleContext<TRecord> {
  tree: StaticModuleTreeClient<TRecord>;
}

export interface ModuleContextConfig {
  http?: HttpClient;
  httpFactory?: () => HttpClient;
}

export interface ModuleContextOptions extends ModuleContextConfig {
  moduleAlias: string;
}

const moduleContextConfigKey: InjectionKey<ModuleContextConfig> = Symbol('muyun.module-context-config');
const moduleAliasKey: InjectionKey<Readonly<{ value: string | undefined }>> = Symbol('muyun.module-alias');
let defaultModuleContextConfig: ModuleContextConfig | undefined;

export function configureModuleContext(config: ModuleContextConfig) {
  defaultModuleContextConfig = config;
}

export function provideModuleContextConfig(config: ModuleContextConfig) {
  provide(moduleContextConfigKey, config);
}

export function createModuleContext<TRecord>(options: ModuleContextOptions): ModuleContext<TRecord> {
  const http = resolveModuleHttpClient(options);
  return moduleContextOf<TRecord>(http, options.moduleAlias);
}

export function createModuleTreeContext<TRecord>(options: ModuleContextOptions): ModuleTreeContext<TRecord> {
  const http = resolveModuleHttpClient(options);
  return moduleTreeContextOf<TRecord>(http, options.moduleAlias);
}

export function useModuleContext<TRecord>(
  options: Partial<ModuleContextOptions> = {},
): ModuleContext<TRecord> {
  const config = inject(moduleContextConfigKey, undefined);
  const injectedModuleAlias = inject(moduleAliasKey, undefined);
  const moduleAlias = options.moduleAlias ?? injectedModuleAlias?.value;
  if (!moduleAlias) {
    throw new Error('Module context requires a moduleAlias');
  }
  const http = resolveModuleHttpClient(options, config);
  return moduleContextOf<TRecord>(http, moduleAlias);
}

export function useModuleTreeContext<TRecord>(
  options: Partial<ModuleContextOptions> = {},
): ModuleTreeContext<TRecord> {
  const config = inject(moduleContextConfigKey, undefined);
  const injectedModuleAlias = inject(moduleAliasKey, undefined);
  const moduleAlias = options.moduleAlias ?? injectedModuleAlias?.value;
  if (!moduleAlias) {
    throw new Error('Module tree context requires a moduleAlias');
  }
  const http = resolveModuleHttpClient(options, config);
  return moduleTreeContextOf<TRecord>(http, moduleAlias);
}

export const ModuleContextProvider = defineComponent({
  name: 'ModuleContextProvider',
  props: {
    moduleAlias: {
      type: String,
      required: false,
      default: undefined,
    },
  },
  setup(props, { slots }) {
    provide(
      moduleAliasKey,
      computed(() => props.moduleAlias),
    );
    return () => slots.default?.();
  },
});

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

export function createAuthClient(http: HttpClient): AuthClient {
  return {
    login: (request) => http.request<LoginResult>({ method: 'POST', path: '/iam.auth/login', body: request }),
    logout: (token) =>
      http.request<void>({
        method: 'POST',
        path: '/iam.auth/logout',
        headers: token ? { Authorization: `Bearer ${token}` } : undefined,
      }),
  };
}

export function createStaticModuleCrudClient<TRecord>(
  http: HttpClient,
  options: { moduleAlias: string },
): StaticModuleCrudClient<TRecord> {
  const modulePath = modulePathOf(options.moduleAlias);
  return {
    query: (request) =>
      http.request<WebPageResponse<TRecord>>({
        method: 'POST',
        path: `${modulePath}/query`,
        body: request,
      }),
    view: (id) => http.request<TRecord>({ path: `${modulePath}/view/${encodeURIComponent(id)}` }),
    insert: (record) =>
      http.request<TRecord>({
        method: 'POST',
        path: `${modulePath}/insert`,
        body: record,
      }),
    update: (id, record) =>
      http.request<TRecord>({
        method: 'POST',
        path: `${modulePath}/update/${encodeURIComponent(id)}`,
        body: record,
      }),
    delete: (id) =>
      http.request<WebCountResponse>({
        method: 'POST',
        path: `${modulePath}/delete/${encodeURIComponent(id)}`,
      }),
    enable: (id) =>
      http.request<WebCountResponse>({
        method: 'POST',
        path: `${modulePath}/enable/${encodeURIComponent(id)}`,
      }),
    disable: (id) =>
      http.request<WebCountResponse>({
        method: 'POST',
        path: `${modulePath}/disable/${encodeURIComponent(id)}`,
      }),
  };
}

export function createStaticModuleTreeClient<TRecord>(
  http: HttpClient,
  options: { moduleAlias: string },
): StaticModuleTreeClient<TRecord> {
  const modulePath = modulePathOf(options.moduleAlias);
  const crud = createStaticModuleCrudClient<TRecord>(http, { moduleAlias: options.moduleAlias });
  return {
    ...crud,
    tree: () =>
      http.request<WebListResponse<WebTreeNode<TRecord>>>({
        path: `${modulePath}/tree`,
      }),
    treeFlat: (options) => {
      const rootId = options?.rootId;
      const path = rootId ? `${modulePath}/tree/${encodeURIComponent(rootId)}` : `${modulePath}/tree`;
      return http.request<WebListResponse<TRecord>>({
        path,
        query: {
          flat: true,
          includeSelf: options?.includeSelf,
        },
      });
    },
    subtree: (id, query) =>
      http.request<WebListResponse<WebTreeNode<TRecord>>>({
        path: `${modulePath}/tree/${encodeURIComponent(id)}`,
        query,
      }),
    sort: (id, request) =>
      http.request<WebCountResponse>({
        method: 'POST',
        path: `${modulePath}/sort/${encodeURIComponent(id)}`,
        body: request,
      }),
  };
}

function moduleContextOf<TRecord>(http: HttpClient, moduleAlias: string): ModuleContext<TRecord> {
  return {
    moduleAlias,
    crud: createStaticModuleCrudClient<TRecord>(http, { moduleAlias }),
  };
}

function moduleTreeContextOf<TRecord>(http: HttpClient, moduleAlias: string): ModuleTreeContext<TRecord> {
  return {
    ...moduleContextOf<TRecord>(http, moduleAlias),
    tree: createStaticModuleTreeClient<TRecord>(http, { moduleAlias }),
  };
}

function resolveModuleHttpClient(
  options: ModuleContextConfig,
  injectedConfig?: ModuleContextConfig,
): HttpClient {
  const config =
    options.http || options.httpFactory ? options : (injectedConfig ?? defaultModuleContextConfig);
  const http = config?.http ?? config?.httpFactory?.();
  if (!http) {
    throw new Error('Module context requires an HttpClient or httpFactory');
  }
  return http;
}

function urlOf(baseUrl: string | undefined, options: HttpRequestOptions) {
  const base = baseUrl?.replace(/\/$/, '') ?? '';
  const path = options.path.startsWith('/') ? options.path : `/${options.path}`;
  const origin = typeof window === 'undefined' ? 'http://localhost' : window.location.origin;
  const url = new URL(`${base}${path}`, origin);
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

function modulePathOf(moduleAlias: string) {
  const normalized = moduleAlias.trim();
  return normalized.startsWith('/') ? normalized : `/${normalized}`;
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
    headers['X-MuYun-Trace-Id'] = context.traceId;
  }
  Object.assign(headers, options.headers);
  return headers;
}

async function appErrorFromResponse(response: Response) {
  const details = await responseBody(response);
  const message = messageOf(details) ?? `Request failed with status ${response.status}`;
  const code = codeOf(details) ?? platformErrorCodes.httpError;
  return new AppError(message, {
    code,
    status: response.status,
    traceId: traceIdOf(details) ?? response.headers.get('X-MuYun-Trace-Id') ?? undefined,
    scope: recordField(details, 'scope'),
    targets: targetsOf(details),
    details: recordField(details, 'details') ?? (isRecord(details) ? details : undefined),
  });
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

function traceIdOf(details: unknown) {
  return objectField(details, 'traceId');
}

function targetsOf(details: unknown): ErrorTarget[] {
  if (!isRecord(details) || !Array.isArray(details.targets)) {
    return [];
  }
  return details.targets.filter(isRecord) as ErrorTarget[];
}

function recordField(value: unknown, key: string) {
  if (!isRecord(value) || !isRecord(value[key])) {
    return undefined;
  }
  return value[key] as Record<string, unknown>;
}

function objectField(value: unknown, key: string) {
  if (!isRecord(value) || !(key in value)) {
    return undefined;
  }
  const field = value[key];
  return typeof field === 'string' ? field : undefined;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return !!value && typeof value === 'object' && !Array.isArray(value);
}

function presentation(slot: GlobalErrorSlot, error: AppError): GlobalErrorPresentation {
  return {
    slot,
    message: error.message,
    traceId: error.traceId,
  };
}

export { VueQueryPlugin };
