import { inject, provide, type InjectionKey, type Ref } from 'vue';

const platformTimeZoneKey: InjectionKey<Readonly<Ref<string | undefined>>> =
  Symbol('muyun.platform-time-zone');

export function providePlatformTimeZoneContext(timeZone: Readonly<Ref<string | undefined>>) {
  provide(platformTimeZoneKey, timeZone);
}

export function usePlatformTimeZoneContext() {
  return inject(platformTimeZoneKey, undefined);
}
