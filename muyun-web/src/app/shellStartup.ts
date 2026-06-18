import type { MenuClient, SessionClient } from '@muyun/web-core';
import type { MenuNavigationTarget, MenuRecord, MenuTab, ShellStartupState } from '@muyun/web-contracts';
import { createMenuTab, findFirstNavigationMenu, getMenuNavigationTarget } from '@muyun/platform-shell';

export interface ShellStartupClients {
  sessionClient: SessionClient;
  menuClient: MenuClient;
}

export async function loadShellStartupState(clients: ShellStartupClients): Promise<ShellStartupState> {
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

export function initialOpenMenuKeys(state: ShellStartupState) {
  const activeMenuId = (state.tabs ?? []).find((tab) => tab.key === state.activeTabKey)?.target.menuId;
  return activeMenuId ? ancestorMenuIds(state.menus, activeMenuId) : [];
}

function initialTabOf(menus: ShellStartupState['menus']) {
  const menu = findFirstNavigationMenu(menus);
  const target = menu ? getMenuNavigationTarget(menu) : undefined;
  return menu && target ? createMenuTab(menu, target) : undefined;
}

function ancestorMenuIds(
  nodes: ShellStartupState['menus'],
  menuId: string,
  ancestors: string[] = [],
): string[] {
  for (const node of nodes) {
    if (node.record.id === menuId) {
      return ancestors;
    }

    const childAncestors = ancestorMenuIds(node.children, menuId, [...ancestors, node.record.id]);
    if (childAncestors.length > 0) {
      return childAncestors;
    }
  }

  return [];
}
