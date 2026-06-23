import type { Component } from 'vue';
import type { BusinessRoutePageDescriptor, PageDescriptor, RoutePageTarget } from '@muyun/web-contracts';
import OrganizationManagementView from '../views/OrganizationManagementView.vue';

export interface StaticBusinessRoute {
  route: string;
  moduleAlias: string;
  component: Component;
}

export const staticBusinessRoutes: StaticBusinessRoute[] = [
  {
    route: '/iam/organizations',
    moduleAlias: 'iam.organization',
    component: OrganizationManagementView,
  },
];

export const businessRoutePrefixes = staticBusinessRoutes.map((route) => route.route);

export function resolveStaticBusinessRoute(
  descriptor?: BusinessRoutePageDescriptor,
): StaticBusinessRoute | undefined {
  if (!descriptor) {
    return undefined;
  }
  return staticBusinessRoutes.find((route) => routeMatchesTarget(route, descriptor.target));
}

export function isStaticBusinessRoutePage(
  descriptor?: PageDescriptor,
): descriptor is BusinessRoutePageDescriptor {
  return descriptor?.pageType === 'business-route' && Boolean(resolveStaticBusinessRoute(descriptor));
}

function routeMatchesTarget(route: StaticBusinessRoute, target: RoutePageTarget) {
  return target.route === route.route;
}
