import type {
  ChangeOwnPasswordRequest,
  CurrentUser,
  LoginRequest,
  LoginResult,
  MenuMineResponse,
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
    mine: () => http.request<MenuMineResponse>({ path: '/platform.menu/mine' }),
  };
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
