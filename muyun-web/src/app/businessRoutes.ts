import type { Component } from 'vue';
import type { BusinessRoutePageDescriptor, PageDescriptor, RoutePageTarget } from '@muyun/web-contracts';
import ApplicationManagementView from '../views/ApplicationManagementView.vue';
import OrganizationManagementView from '../views/OrganizationManagementView.vue';
import TenantManagementView from '../views/TenantManagementView.vue';

export interface StaticBusinessRoute {
  route: string;
  moduleAlias: string;
  component: Component;
}

export const staticBusinessRoutes: StaticBusinessRoute[] = [
  {
    route: '/config/applications',
    moduleAlias: 'platform.application',
    component: ApplicationManagementView,
  },
  {
    route: '/iam/tenants',
    moduleAlias: 'iam.tenant',
    component: TenantManagementView,
  },
  {
    route: '/iam/organizations',
    moduleAlias: 'iam.organization',
    component: OrganizationManagementView,
  },
];

export const businessRoutePrefixes = staticBusinessRoutes.map((route) => route.route);
export const businessModuleRoutes = Object.fromEntries(
  staticBusinessRoutes.map((route) => [route.moduleAlias, route.route]),
);

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
  return target.route ? target.route === route.route : target.moduleAlias === route.moduleAlias;
}
