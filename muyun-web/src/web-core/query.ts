import { QueryClient, VueQueryPlugin } from '@tanstack/vue-query';

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
  staticModule: {
    query: (moduleAlias: string, request?: unknown) =>
      ['static-module', moduleAlias, 'query', request] as const,
    tree: (moduleAlias: string, options?: unknown) =>
      ['static-module', moduleAlias, 'tree', options] as const,
    view: (moduleAlias: string, id: string) => ['static-module', moduleAlias, 'view', id] as const,
  },
};

export { VueQueryPlugin };
