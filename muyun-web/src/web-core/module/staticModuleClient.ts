import type {
  TreeSortRequest,
  WebActionResultFacts,
  WebActionResultEnvelope,
  QuerySchema,
  WebCountResponse,
  WebListResponse,
  WebPageResponse,
  WebQueryRequest,
  WebTreeNode,
} from '@muyun/web-contracts';
import type { HttpClient } from '../http';

export interface StaticRecordMutationResult<TRecord> extends WebActionResultFacts {
  record: TRecord;
}

export interface QuerySchemaRequestOptions {
  uiConfigId?: string;
}

export interface StaticModuleCrudClient<TRecord> {
  querySchema(options?: QuerySchemaRequestOptions): Promise<QuerySchema>;
  query(request?: WebQueryRequest): Promise<WebPageResponse<TRecord>>;
  view(id: string): Promise<TRecord>;
  insert(record: TRecord): Promise<StaticRecordMutationResult<TRecord>>;
  update(id: string, record: TRecord): Promise<StaticRecordMutationResult<TRecord>>;
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

export interface ModuleEnableClient {
  enable(id: string): Promise<WebCountResponse>;
  disable(id: string): Promise<WebCountResponse>;
}

export function createStaticModuleCrudClient<TRecord>(
  http: HttpClient,
  options: { moduleAlias: string },
): StaticModuleCrudClient<TRecord> {
  return createStaticResourceCrudClient(http, modulePathOf(options.moduleAlias));
}

export function createStaticResourceCrudClient<TRecord>(
  http: HttpClient,
  resourcePath: string,
): StaticModuleCrudClient<TRecord> {
  const modulePath = modulePathOf(resourcePath);
  return {
    querySchema: (options) =>
      http.request<QuerySchema>({
        path: `${modulePath}/query/schema`,
        query: {
          uiConfigId: options?.uiConfigId,
        },
      }),
    query: (request) =>
      http.request<WebPageResponse<TRecord>>({
        method: 'POST',
        path: `${modulePath}/query`,
        body: request,
      }),
    view: (id) => http.request<TRecord>({ path: `${modulePath}/view/${encodeURIComponent(id)}` }),
    insert: async (record) =>
      normalizeRecordMutationResponse(
        await http.request<TRecord | WebActionResultEnvelope<TRecord>>({
          method: 'POST',
          path: `${modulePath}/insert`,
          body: record,
        }),
      ),
    update: async (id, record) =>
      normalizeRecordMutationResponse(
        await http.request<TRecord | WebActionResultEnvelope<TRecord>>({
          method: 'POST',
          path: `${modulePath}/update/${encodeURIComponent(id)}`,
          body: record,
        }),
      ),
    delete: async (id) =>
      normalizeCountMutationResponse(
        await http.request<WebCountResponse | WebActionResultEnvelope<WebCountResponse>>({
          method: 'POST',
          path: `${modulePath}/delete/${encodeURIComponent(id)}`,
        }),
      ),
    enable: async (id) =>
      normalizeCountMutationResponse(
        await http.request<WebCountResponse | WebActionResultEnvelope<WebCountResponse>>({
          method: 'POST',
          path: `${modulePath}/enable/${encodeURIComponent(id)}`,
        }),
      ),
    disable: async (id) =>
      normalizeCountMutationResponse(
        await http.request<WebCountResponse | WebActionResultEnvelope<WebCountResponse>>({
          method: 'POST',
          path: `${modulePath}/disable/${encodeURIComponent(id)}`,
        }),
      ),
  };
}

export function createStaticModuleTreeClient<TRecord>(
  http: HttpClient,
  options: { moduleAlias: string },
): StaticModuleTreeClient<TRecord> {
  return createStaticResourceTreeClient(http, modulePathOf(options.moduleAlias));
}

export function createStaticResourceTreeClient<TRecord>(
  http: HttpClient,
  resourcePath: string,
): StaticModuleTreeClient<TRecord> {
  const modulePath = modulePathOf(resourcePath);
  const crud = createStaticResourceCrudClient<TRecord>(http, modulePath);
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
    sort: async (id, request) =>
      normalizeCountMutationResponse(
        await http.request<WebCountResponse | WebActionResultEnvelope<WebCountResponse>>({
          method: 'POST',
          path: `${modulePath}/sort/${encodeURIComponent(id)}`,
          body: request,
        }),
      ),
  };
}

function modulePathOf(moduleAlias: string) {
  const normalized = moduleAlias.trim();
  return normalized.startsWith('/') ? normalized : `/${normalized}`;
}

function normalizeRecordMutationResponse<TRecord>(
  response: TRecord | WebActionResultEnvelope<TRecord>,
): StaticRecordMutationResult<TRecord> {
  if (isWebActionResultEnvelope<TRecord>(response)) {
    return {
      record: response.data,
      message: response.message,
      resultType: response.resultType,
      changes: response.changes,
      changeSetId: response.changeSetId,
    };
  }
  return { record: response };
}

function normalizeCountMutationResponse(
  response: WebCountResponse | WebActionResultEnvelope<WebCountResponse>,
): WebCountResponse {
  if (isWebActionResultEnvelope(response)) {
    return {
      count: webCount(response.data),
      message: response.message,
      resultType: response.resultType,
      changes: response.changes,
      changeSetId: response.changeSetId,
    };
  }
  return response;
}

function webCount(data: unknown) {
  if (!data || typeof data !== 'object' || !('count' in data)) {
    return 0;
  }
  const count = (data as { count?: unknown }).count;
  return typeof count === 'number' && Number.isFinite(count) ? count : 0;
}

function isWebActionResultEnvelope<TData>(response: unknown): response is WebActionResultEnvelope<TData> {
  return typeof response === 'object' && response !== null && 'data' in response && 'changeSetId' in response;
}
