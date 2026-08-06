import { inject, provide, type InjectionKey, type Ref } from 'vue';
import type { CurrentUser } from '@muyun/web-contracts';

const currentUserKey: InjectionKey<Readonly<Ref<CurrentUser | undefined>>> = Symbol('muyun.current-user');

export function provideCurrentUserContext(currentUser: Readonly<Ref<CurrentUser | undefined>>) {
  provide(currentUserKey, currentUser);
}

export function useCurrentUserContext() {
  return inject(currentUserKey, undefined);
}
