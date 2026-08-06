/**
 * First-party management runtime shipped to management-oriented applications.
 *
 * It owns the platform administration route registry and the context APIs
 * required by those pages.  Application composition remains outside this
 * directory: an App supplies its startup state, navigation and HTTP factory.
 */
export { default as PlatformAdminRouteOutlet } from './PlatformAdminOutlet.vue';
export {
  isPlatformAdminRoutePage,
  platformAdminModuleRoutes,
  platformAdminRouteLayouts,
  platformAdminRoutePrefixes,
  platformAdminRoutes,
  resolvePlatformAdminRoute,
} from './platformAdminRoutes';
export type { PlatformAdminRoute } from './platformAdminRoutes';
export { provideCurrentUserContext, useCurrentUserContext } from './currentUserContext';
export { createBackendHttpClient } from './backendHttp';
