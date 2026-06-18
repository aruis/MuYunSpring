import type { MenuNavigationTarget, MenuRecord, MenuTab, MenuTreeNode } from '@muyun/web-contracts';

export function getMenuNavigationTarget(menu: MenuRecord): MenuNavigationTarget | undefined {
  if (menu.menuType === 'MODULE' && menu.moduleAlias) {
    return {
      menuId: menu.id,
      menuType: 'MODULE',
      moduleAlias: menu.moduleAlias,
      pageMode: menu.pageMode,
      defaultUiConfigId: menu.defaultUiConfigId,
      defaultQueryTemplateId: menu.defaultQueryTemplateId,
      entryParamsJson: menu.entryParamsJson,
    };
  }

  if (menu.menuType === 'ROUTE' && menu.route) {
    return {
      menuId: menu.id,
      menuType: 'ROUTE',
      route: menu.route,
      entryParamsJson: menu.entryParamsJson,
    };
  }

  if (menu.menuType === 'LINK' && menu.externalUrl) {
    return {
      menuId: menu.id,
      menuType: 'LINK',
      externalUrl: menu.externalUrl,
      entryParamsJson: menu.entryParamsJson,
    };
  }

  return undefined;
}

export function createMenuTab(menu: MenuRecord, target: MenuNavigationTarget): MenuTab {
  return {
    key: tabKeyOf(target),
    title: menu.title,
    target,
    closable: true,
  };
}

export function findFirstNavigationMenu(nodes: MenuTreeNode[]): MenuRecord | undefined {
  for (const node of nodes) {
    if (node.record.enabled !== false && getMenuNavigationTarget(node.record)) {
      return node.record;
    }

    const childMenu: MenuRecord | undefined = findFirstNavigationMenu(node.children);
    if (childMenu) {
      return childMenu;
    }
  }

  return undefined;
}

export function tabKeyOf(target: MenuNavigationTarget) {
  return `${target.menuType}:${target.menuId}`;
}
