import type { CountResult, WebListResponse, WebQueryRequest, WebTreeNode } from '@muyun/web-contracts';
import type { ModuleContext, StaticModuleTreeClient } from '@muyun/web-core';

export interface ScopedTreeModuleContextOptions {
  scopeFieldName: string;
  scopeValue: string | undefined;
  treePath: string;
  sortPath?: string;
  treeQueryParam?: string;
}

export function createScopedTreeModuleContext<TRecord>(
  context: ModuleContext<TRecord>,
  options: ScopedTreeModuleContextOptions,
): ModuleContext<TRecord> {
  const treeClient = createScopedTreeClient(context, options);
  return {
    ...context,
    abilities: {
      ...context.abilities,
      tree: () => treeClient,
      tryTree: () => (context.abilities.hasTree() ? treeClient : undefined),
    },
  };
}

export function createScopedTreeClient<TRecord>(
  context: ModuleContext<TRecord>,
  options: ScopedTreeModuleContextOptions,
): StaticModuleTreeClient<TRecord> {
  return {
    ...context.crud,
    query: (request) => context.crud.query(scopedQuery(request, options)),
    tree: () => {
      if (!options.scopeValue) {
        return emptyTreeResponse<TRecord>();
      }
      return context.http.request<WebListResponse<WebTreeNode<TRecord>>>({
        path: options.treePath,
        query: scopeQueryParams(options),
      });
    },
    treeFlat: (treeOptions) => {
      if (!options.scopeValue) {
        return emptyListResponse<TRecord>();
      }
      return context.http.request<WebListResponse<TRecord>>({
        path: treePathOf(options.treePath, treeOptions?.rootId),
        query: {
          ...scopeQueryParams(options),
          flat: true,
          includeSelf: treeOptions?.includeSelf,
        },
      });
    },
    subtree: (id, subtreeOptions) =>
      context.http.request<WebListResponse<WebTreeNode<TRecord>>>({
        path: treePathOf(options.treePath, id),
        query: {
          ...scopeQueryParams(options),
          ...subtreeOptions,
        },
      }),
    sort: (id, request) =>
      context.http.request<CountResult>({
        method: 'POST',
        path: `${(options.sortPath ?? `${options.treePath.replace(/\/tree\/?$/, '')}/sort`).replace(
          /\/$/,
          '',
        )}/${encodeURIComponent(id)}`,
        query: scopeQueryParams(options),
        body: request,
      }),
  };
}

function scopedQuery(request: WebQueryRequest | undefined, options: ScopedTreeModuleContextOptions) {
  if (!options.scopeValue) {
    return request;
  }
  return {
    ...request,
    conditions: [
      ...(request?.conditions ?? []),
      { fieldName: options.scopeFieldName, operator: 'EQ', values: [options.scopeValue] },
    ],
  };
}

function scopeQueryParams(options: ScopedTreeModuleContextOptions) {
  return { [options.treeQueryParam ?? options.scopeFieldName]: options.scopeValue };
}

function treePathOf(treePath: string, rootId: string | undefined) {
  const normalized = treePath.replace(/\/$/, '');
  return rootId ? `${normalized}/${encodeURIComponent(rootId)}` : normalized;
}

async function emptyTreeResponse<TRecord>(): Promise<WebListResponse<WebTreeNode<TRecord>>> {
  return { records: [] };
}

async function emptyListResponse<TRecord>(): Promise<WebListResponse<TRecord>> {
  return { records: [] };
}
