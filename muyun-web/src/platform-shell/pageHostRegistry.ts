import type { PageHostType } from '@muyun/web-contracts';

export type PageHostComponentName = 'PlatformRouteHost' | 'DynamicModuleHost' | 'ExternalPageHost';

export function resolvePageHostComponentName(hostType: PageHostType): PageHostComponentName {
  switch (hostType) {
    case 'platform-route-host':
    case 'business-route-host':
      return 'PlatformRouteHost';
    case 'dynamic-module-host':
      return 'DynamicModuleHost';
    case 'external-page-host':
      return 'ExternalPageHost';
    default:
      return exhaustiveHostType(hostType);
  }
}

function exhaustiveHostType(hostType: never): never {
  throw new Error(`Unsupported page host type: ${hostType}`);
}
