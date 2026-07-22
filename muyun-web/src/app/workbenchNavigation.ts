import { inject, provide, type InjectionKey } from 'vue';
import type { PageDescriptor } from '@muyun/web-contracts';

export interface WorkbenchNavigation {
  openPage(descriptor: PageDescriptor): void;
}

const workbenchNavigationKey: InjectionKey<WorkbenchNavigation> = Symbol('workbench-navigation');

export function provideWorkbenchNavigation(navigation: WorkbenchNavigation) {
  provide(workbenchNavigationKey, navigation);
}

export function useWorkbenchNavigation() {
  return inject(workbenchNavigationKey);
}
