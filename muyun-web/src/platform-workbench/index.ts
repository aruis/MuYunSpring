export { default as Workbench } from './Workbench.vue';
export { default as WorkbenchMenu } from './WorkbenchMenu.vue';
export { default as WorkbenchOutlet } from './WorkbenchOutlet.vue';
export {
  createMenuTab,
  findFirstNavigationMenu,
  getMenuNavigationTarget,
  pageDescriptorFromUrl,
  pageDescriptorToUrl,
  resolvePageDescriptor,
  tabKeyOf,
  tryPageDescriptorFromUrl,
} from './menuNavigation';
export {
  buildWorkbenchMegaMenuModel,
  createWorkbenchMenuNodes,
  filterWorkbenchMenuNodes,
  findWorkbenchMenuNodeById,
  findWorkbenchMenuPath,
  firstDeepRootIdOf,
} from './menuTreeModel';
export type { WorkbenchMegaMenuModel, WorkbenchMenuNode } from './menuTreeModel';
