import type {
  AccountRoleGrant,
  ManagementScopeType,
  UserSelectorItem,
  WebActionResultEnvelope,
  WebPageResponse,
} from '@muyun/web-contracts';
import type { HttpClient } from '@muyun/web-core';

export interface AccountRoleGrantRequest {
  userId: string;
  managementScopeType?: ManagementScopeType;
  managementScopeId?: string;
}

export interface UserSelectorRequest {
  roleId?: string;
  keyword?: string;
  enabledOnly?: boolean;
  page?: {
    pageNum: number;
    pageSize: number;
  };
}

export function createRoleGrantClient(http: HttpClient) {
  return {
    accountRoleGrants(roleId: string) {
      return http.request<AccountRoleGrant[]>({
        path: `/iam.role/${encodeURIComponent(roleId)}/account-grants`,
      });
    },
    grantAccountRole(roleId: string, request: AccountRoleGrantRequest) {
      return http.request<WebActionResultEnvelope<string> | string>({
        method: 'POST',
        path: `/iam.role/${encodeURIComponent(roleId)}/account-grants`,
        body: request,
      });
    },
    deleteAccountRoleGrant(roleId: string, grantId: string) {
      return http.request<WebActionResultEnvelope<number> | number>({
        method: 'POST',
        path: `/iam.role/${encodeURIComponent(roleId)}/account-grants/${encodeURIComponent(grantId)}/delete`,
      });
    },
    userSelector(request: UserSelectorRequest) {
      return http.request<WebPageResponse<UserSelectorItem>>({
        method: 'POST',
        path: '/iam.user/selector/query',
        body: request,
      });
    },
  };
}
