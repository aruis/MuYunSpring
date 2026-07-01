import { createStaticResourceTreeClient } from '@muyun/web-core';
import type { ModuleContext } from '@muyun/web-core';
import {
  createEmptyStaticTreeClient,
  createStaticTreeResourceModuleContext,
} from './staticTreeResourceModuleContext';

export interface ScopedResourceTreeModuleContextOptions {
  resourcePath?: string;
  emptyQueryScopeName?: string;
}

export function createScopedResourceTreeModuleContext<TRecord, TContextRecord = unknown>(
  context: ModuleContext<TContextRecord>,
  options: ScopedResourceTreeModuleContextOptions,
): ModuleContext<TRecord> {
  if (!options.resourcePath) {
    return createStaticTreeResourceModuleContext<TRecord, TContextRecord>(context, {
      client: createEmptyStaticTreeClient(options.emptyQueryScopeName),
      emptyQueryScopeName: options.emptyQueryScopeName,
    });
  }
  return createStaticTreeResourceModuleContext<TRecord, TContextRecord>(context, {
    client: createStaticResourceTreeClient<TRecord>(context.http, options.resourcePath),
    emptyQueryScopeName: options.emptyQueryScopeName,
  });
}
