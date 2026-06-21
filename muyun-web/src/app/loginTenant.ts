export interface LoginTenantDefaults {
  tenantId: string;
  tenantLocked: boolean;
}

export function resolveLoginTenantDefaults(
  envTenantId?: string | null,
  search = currentSearch(),
  pathname = currentPathname(),
): LoginTenantDefaults {
  const urlTenantId = tenantIdFromSearch(search, pathname === '/');
  if (urlTenantId) {
    return {
      tenantId: urlTenantId,
      tenantLocked: true,
    };
  }
  return {
    tenantId: normalizeInitialValue(envTenantId),
    tenantLocked: false,
  };
}

export function normalizeInitialValue(value: string | null | undefined) {
  return value?.trim() ?? '';
}

function tenantIdFromSearch(search: string, allowGenericTenantParameter: boolean) {
  const params = new URLSearchParams(search.startsWith('?') ? search : `?${search}`);
  const shellTenantId = normalizeInitialValue(params.get('_muyunTenantId'));
  if (shellTenantId || !allowGenericTenantParameter) {
    return shellTenantId;
  }
  return normalizeInitialValue(params.get('tenantId')) || normalizeInitialValue(params.get('tenant'));
}

function currentSearch() {
  return typeof window === 'undefined' ? '' : window.location.search;
}

function currentPathname() {
  return typeof window === 'undefined' ? '/' : window.location.pathname;
}
