import type {
  ChangeOwnPasswordRequest,
  CurrentUser,
  LoginRequest,
  LoginResult,
  MenuMineResponse,
  MenuOpenMode,
  MenuTreeNode,
} from '@muyun/web-contracts';
import type { HttpClient } from './http';

export interface SessionClient {
  current(): Promise<CurrentUser>;
}

export interface MenuClient {
  mine(): Promise<MenuMineResponse>;
}

export interface AuthClient {
  login(request: LoginRequest): Promise<LoginResult>;
  changeOwnPassword(request: ChangeOwnPasswordRequest, token: string): Promise<void>;
  logout(token?: string): Promise<void>;
}

export function createSessionClient(http: HttpClient): SessionClient {
  return {
    current: () => http.request<CurrentUser>({ path: '/iam.auth/context' }),
  };
}

export function createMenuClient(http: HttpClient): MenuClient {
  return {
    mine: async () => normalizeMenuMineResponse(await http.request<unknown>({ path: '/platform.menu/mine' })),
  };
}

/**
 * The backend serializes Java enum values as upper-case identifiers while the
 * web contract deliberately uses lower-case literals.  Normalize once at the
 * HTTP projection boundary so every workbench consumer sees the same menu
 * contract instead of requiring application-level compatibility code.
 */
export function normalizeMenuMineResponse(response: unknown): MenuMineResponse {
  if (!isRecord(response) || !Array.isArray(response.records)) {
    return response as MenuMineResponse;
  }

  return {
    ...response,
    records: response.records.map(normalizeMenuTreeNode),
  } as MenuMineResponse;
}

function normalizeMenuTreeNode(node: unknown): MenuTreeNode {
  if (!isRecord(node)) {
    return node as MenuTreeNode;
  }

  const record = node.record;
  return {
    ...node,
    record: isRecord(record) ? { ...record, openMode: normalizeMenuOpenMode(record.openMode) } : record,
    children: Array.isArray(node.children) ? node.children.map(normalizeMenuTreeNode) : [],
  } as MenuTreeNode;
}

function normalizeMenuOpenMode(value: unknown): MenuOpenMode | undefined {
  if (typeof value !== 'string') {
    return undefined;
  }

  const normalized = value.toLowerCase();
  return normalized === 'tab' || normalized === 'window' ? normalized : undefined;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null;
}

export function createAuthClient(http: HttpClient): AuthClient {
  return {
    login: (request) => http.request<LoginResult>({ method: 'POST', path: '/iam.auth/login', body: request }),
    changeOwnPassword: (request, token) =>
      http.request<void>({
        method: 'POST',
        path: '/iam.auth/changeOwnPassword',
        body: request,
        headers: { Authorization: `Bearer ${token}` },
      }),
    logout: (token) =>
      http.request<void>({
        method: 'POST',
        path: '/iam.auth/logout',
        headers: token ? { Authorization: `Bearer ${token}` } : undefined,
      }),
  };
}
