/**
 * Public App-consumer surface. It deliberately exposes the workbench shell
 * and its runtime primitives, including first-party platform administration
 * pages.  It deliberately excludes pages owned by a consuming business App.
 *
 * Consumers must import `@ximatai/muyun-web-app/style.css` explicitly.
 */

import '../styles.css';

export * from '../web-contracts/index';
export * from '../web-core/index';
export * from '../platform-workbench/index';
export * from '../platform-admin-runtime/index';
// The standard module runner and its list/drawer/form components are shipped
// behind PlatformAdminOutlet.  Keeping them out of this entry preserves their
// freedom to evolve without making each implementation detail an App contract.
export { UiButton, UiInput, UiSwitch } from '../vue-ui-antdv/index';
export { default as PlatformAdminOutlet } from './PlatformAdminOutlet.vue';
