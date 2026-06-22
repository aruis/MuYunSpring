import type { MenuClient, SessionClient } from '@muyun/web-core';
import type {
  MenuNavigationTarget,
  MenuRecord,
  MenuTab,
  PageDescriptor,
  WorkbenchStartupState,
} from '@muyun/web-contracts';
import {
  createMenuTab,
  findFirstNavigationMenu,
  getMenuNavigationTarget,
  pageDescriptorToUrl,
  resolvePageDescriptor,
  tabKeyOf,
  tryPageDescriptorFromUrl,
} from '@muyun/platform-workbench';

export interface WorkbenchStartupClients {
  sessionClient: SessionClient;
  menuClient: MenuClient;
}

export async function loadWorkbenchStartupState(
  clients: WorkbenchStartupClients,
): Promise<WorkbenchStartupState> {
  const [currentUser, menuResponse] = await Promise.all([
    clients.sessionClient.current(),
    clients.menuClient.mine(),
  ]);
  const initialTab = initialTabOf(menuResponse.records);

  return {
    session: { currentUser },
    menus: menuResponse.records,
    tabs: initialTab ? [initialTab] : [],
    activeTabKey: initialTab?.key,
  };
}

export function openMenuTab(
  tabs: MenuTab[],
  menu: MenuRecord,
  target: MenuNavigationTarget,
): { tabs: MenuTab[]; activeTabKey: string } {
  const tab = createMenuTab(menu, target);
  if (tabs.some((item) => item.key === tab.key)) {
    return { tabs, activeTabKey: tab.key };
  }

  return { tabs: [...tabs, tab], activeTabKey: tab.key };
}

export function activeTabUrlOf(state: WorkbenchStartupState): string | undefined {
  const activeTab = (state.tabs ?? []).find((tab) => tab.key === state.activeTabKey);
  const descriptor =
    activeTab?.pageDescriptor ??
    (activeTab?.target ? resolvePageDescriptor(activeTab.target, { title: activeTab.title }) : undefined);
  return descriptor ? pageDescriptorToUrl(descriptor) : undefined;
}

export function restoreWorkbenchStartupStateFromUrl(
  state: WorkbenchStartupState,
  url: string,
): WorkbenchStartupState {
  if (url === '/' || url === '') {
    return state;
  }

  const descriptor = tryPageDescriptorFromUrl(url);
  if (!descriptor) {
    return state;
  }

  const explicitMenu = descriptor.menuId ? findMenuById(state.menus, descriptor.menuId) : undefined;
  const menu =
    explicitMenu && menuMatchesDescriptor(explicitMenu, descriptor)
      ? explicitMenu
      : findMenuByDescriptor(state.menus, descriptor);
  const target = menu ? getMenuNavigationTarget(menu) : undefined;
  const tab = menu && target ? createRestoredMenuTab(menu, target, descriptor) : createDirectTab(descriptor);
  const existingTabs = state.tabs ?? [];
  const tabs = upsertTab(existingTabs, tab);

  return {
    ...state,
    tabs,
    activeTabKey: tab.key,
  };
}

export function closeMenuTab(
  tabs: MenuTab[],
  activeTabKey: string | undefined,
  closedTabKey: string,
): { tabs: MenuTab[]; activeTabKey: string | undefined } {
  const closedIndex = tabs.findIndex((tab) => tab.key === closedTabKey);
  if (closedIndex < 0) {
    return { tabs, activeTabKey };
  }

  const nextTabs = tabs.filter((tab) => tab.key !== closedTabKey);
  if (activeTabKey !== closedTabKey) {
    return { tabs: nextTabs, activeTabKey };
  }

  return {
    tabs: nextTabs,
    activeTabKey: nextTabs[closedIndex]?.key ?? nextTabs[closedIndex - 1]?.key,
  };
}

function initialTabOf(menus: WorkbenchStartupState['menus']) {
  const menu = findFirstNavigationMenu(menus);
  const target = menu ? getMenuNavigationTarget(menu) : undefined;
  return menu && target ? createMenuTab(menu, target) : undefined;
}

function createDirectTab(descriptor: PageDescriptor): MenuTab {
  return {
    key: tabKeyOf(descriptor),
    title: descriptor.title ?? directTabTitleOf(descriptor),
    pageDescriptor: descriptor,
    restoreState: { url: pageDescriptorToUrl(descriptor) },
    closable: true,
  };
}

function upsertTab(tabs: MenuTab[], tab: MenuTab): MenuTab[] {
  const index = tabs.findIndex((item) => item.key === tab.key);
  if (index < 0) {
    return [...tabs, tab];
  }

  return tabs.map((item, itemIndex) => (itemIndex === index ? tab : item));
}

function createRestoredMenuTab(
  menu: MenuRecord,
  target: MenuNavigationTarget,
  descriptor: PageDescriptor,
): MenuTab {
  const tab = createMenuTab(menu, target);
  const pageDescriptor = {
    ...descriptor,
    title: descriptor.title ?? menu.title,
    menuId: menu.id,
    tabPolicy: tab.pageDescriptor?.tabPolicy ?? descriptor.tabPolicy,
  };

  return {
    ...tab,
    pageDescriptor,
    restoreState: { url: pageDescriptorToUrl(pageDescriptor) },
  };
}

function directTabTitleOf(descriptor: PageDescriptor): string {
  if (descriptor.pageType === 'dynamic-module') {
    return descriptor.target.moduleAlias;
  }

  if (descriptor.pageType === 'platform-route' || descriptor.pageType === 'business-route') {
    return descriptor.target.route ?? descriptor.target.routeName ?? descriptor.target.pageKey ?? 'workspace';
  }

  return descriptor.target.url;
}

function findMenuByDescriptor(
  nodes: WorkbenchStartupState['menus'],
  descriptor: PageDescriptor,
): MenuRecord | undefined {
  for (const node of nodes) {
    if (menuMatchesDescriptor(node.record, descriptor)) {
      return node.record;
    }

    const childMenu = findMenuByDescriptor(node.children, descriptor);
    if (childMenu) {
      return childMenu;
    }
  }

  return undefined;
}

function menuMatchesDescriptor(menu: MenuRecord, descriptor: PageDescriptor): boolean {
  const target = getMenuNavigationTarget(menu);
  const menuDescriptor = target ? resolvePageDescriptor(target, { title: menu.title }) : undefined;
  return menuDescriptor ? matchesPageDescriptor(menuDescriptor, descriptor) : false;
}

function matchesPageDescriptor(left: PageDescriptor, right: PageDescriptor): boolean {
  if (left.pageType !== right.pageType || left.hostType !== right.hostType) {
    return false;
  }

  if (left.pageType === 'dynamic-module' && right.pageType === 'dynamic-module') {
    return (
      left.target.moduleAlias === right.target.moduleAlias &&
      left.target.pageMode === right.target.pageMode &&
      left.target.defaultUiConfigId === right.target.defaultUiConfigId &&
      left.target.defaultQueryTemplateId === right.target.defaultQueryTemplateId
    );
  }

  if (
    (left.pageType === 'platform-route' || left.pageType === 'business-route') &&
    (right.pageType === 'platform-route' || right.pageType === 'business-route')
  ) {
    return (
      left.target.route === right.target.route &&
      left.target.routeName === right.target.routeName &&
      left.target.pageKey === right.target.pageKey
    );
  }

  if (
    (left.pageType === 'remote-url' || left.pageType === 'external-link') &&
    (right.pageType === 'remote-url' || right.pageType === 'external-link')
  ) {
    return left.target.url === right.target.url;
  }

  return false;
}

function findMenuById(nodes: WorkbenchStartupState['menus'], menuId: string): MenuRecord | undefined {
  for (const node of nodes) {
    if (node.record.id === menuId) {
      return node.record;
    }

    const childMenu = findMenuById(node.children, menuId);
    if (childMenu) {
      return childMenu;
    }
  }

  return undefined;
}
