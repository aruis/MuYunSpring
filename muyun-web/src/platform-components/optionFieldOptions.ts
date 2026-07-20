import type { Option, OptionItemDescriptor } from '@muyun/web-contracts';

export interface UiTreeSelectNode {
  value: string;
  title: string;
  disabled?: boolean;
  children?: UiTreeSelectNode[];
}

export function optionItemsToOptions(items: OptionItemDescriptor[]): Option[] {
  return items.map((item) => ({ label: item.title, value: item.code, disabled: !item.enabled }));
}

export function hasOptionHierarchy(items: OptionItemDescriptor[]): boolean {
  return items.some((item) => Boolean(item.parentCode));
}

export function optionItemsToTree(items: OptionItemDescriptor[]): UiTreeSelectNode[] {
  const nodes = new Map<string, UiTreeSelectNode>();
  const roots: UiTreeSelectNode[] = [];
  items.forEach((item) =>
    nodes.set(item.code, {
      value: item.code,
      title: item.title,
      disabled: !item.enabled,
      children: [],
    }),
  );
  items.forEach((item) => {
    const node = nodes.get(item.code)!;
    const parent = item.parentCode ? nodes.get(item.parentCode) : undefined;
    if (parent) {
      parent.children!.push(node);
    } else {
      roots.push(node);
    }
  });
  return roots;
}
