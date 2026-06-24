import { shallowRef } from 'vue';
import { normalizeError, type AppError } from '../errors';
import type { HttpClient } from '../http';
import type { ModuleAbilityCode } from './abilityCodes';

export interface ModuleRuntimeAction {
  actionCode: string;
  permissionActionCode?: string;
  title?: string;
  actionLevel?: 'DEFAULT' | 'LIST' | 'RECORD' | 'BATCH' | 'ANY';
  category?: string;
  accessMode?: 'AUTH_REQUIRED' | 'LOGIN_REQUIRED' | 'ANONYMOUS_ALLOWED';
  actionAuth?: boolean;
  dataAuth?: boolean;
  defaultGrantPolicy?: string;
  executorType?: string;
  executorKey?: string;
  authorized: boolean;
  authorizationDecision?: string;
}

export interface ModuleRuntimeContext {
  moduleAlias: string;
  title?: string;
  moduleKind?: 'STATIC' | 'DYNAMIC';
  entryType?: 'MODULE' | 'ROUTE' | 'LINK';
  entryRoute?: string;
  entryExternalUrl?: string;
  mainEntityAlias?: string;
  capabilities: string[];
  abilities?: string[];
  actions: ModuleRuntimeAction[];
}

export interface ModuleRuntimeContextState {
  ready: Promise<ModuleRuntimeContext>;
  load(): Promise<ModuleRuntimeContext>;
  snapshot(): ModuleRuntimeContext | undefined;
  error(): AppError | undefined;
  hasAbility(ability: ModuleAbilityCode | string): boolean | undefined;
  action(actionCode: string): ModuleRuntimeAction | undefined;
  can(actionCode: string): boolean | undefined;
}

export function createModuleRuntimeContextState(
  http: HttpClient,
  moduleAlias: string,
): ModuleRuntimeContextState {
  const current = shallowRef<ModuleRuntimeContext>();
  const currentError = shallowRef<AppError>();
  let loading: Promise<ModuleRuntimeContext> | undefined;
  const load = () => {
    loading ??= http
      .request<ModuleRuntimeContext>({
        path: `/platform.module/${encodeURIComponent(moduleAlias)}/context`,
      })
      .then((context) => {
        current.value = context;
        currentError.value = undefined;
        return context;
      })
      .catch((cause) => {
        currentError.value = normalizeError(cause);
        loading = undefined;
        throw cause;
      });
    return loading;
  };
  const ready = load();
  ready.catch(() => {
    // Keep background context loading from becoming an unhandled rejection.
  });
  return {
    ready,
    load,
    snapshot: () => current.value,
    error: () => currentError.value,
    hasAbility: (ability) => {
      const context = current.value;
      if (!context) {
        return undefined;
      }
      return runtimeAbilityCodes(context).includes(ability);
    },
    action: (actionCode) => current.value?.actions.find((action) => action.actionCode === actionCode),
    can: (actionCode) =>
      current.value?.actions.find((action) => action.actionCode === actionCode)?.authorized,
  };
}

export function isRuntimeAbilityAvailable(
  context: ModuleRuntimeContext | undefined,
  ability: ModuleAbilityCode,
) {
  if (!context) {
    return false;
  }
  return runtimeAbilityCodes(context).includes(ability);
}

function runtimeAbilityCodes(context: ModuleRuntimeContext): string[] {
  return context.abilities ?? context.capabilities.map(abilityCodeOfCapability);
}

function abilityCodeOfCapability(capability: string) {
  return capability.toLowerCase().replace(/_([a-z])/g, (_match, char: string) => char.toUpperCase());
}
