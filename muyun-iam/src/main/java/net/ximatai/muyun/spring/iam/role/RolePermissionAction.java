package net.ximatai.muyun.spring.iam.role;

public record RolePermissionAction(
        String moduleAlias,
        String actionCode,
        String permissionActionCode,
        String title,
        String titleKey,
        boolean actionAuth,
        boolean dataAuth,
        boolean granted,
        DataScopePolicy dataScopePolicy,
        TenantScopePolicy tenantScopePolicy,
        String scopeCondition,
        String referenceFieldId,
        String referenceActionCode
) {
    public RolePermissionAction(String moduleAlias,
                                String actionCode,
                                String permissionActionCode,
                                String title,
                                boolean actionAuth,
                                boolean dataAuth,
                                boolean granted,
                                DataScopePolicy dataScopePolicy,
                                TenantScopePolicy tenantScopePolicy,
                                String scopeCondition,
                                String referenceFieldId,
                                String referenceActionCode) {
        this(moduleAlias, actionCode, permissionActionCode, title, null, actionAuth, dataAuth, granted,
                dataScopePolicy, tenantScopePolicy, scopeCondition, referenceFieldId, referenceActionCode);
    }

    public static RolePermissionAction of(GrantableAction action, RoleAction grant) {
        boolean granted = grant != null && Boolean.TRUE.equals(grant.getEnabled());
        return new RolePermissionAction(
                action.moduleAlias(),
                action.actionCode(),
                action.permissionActionCode(),
                action.title(),
                action.titleKey(),
                action.actionAuth(),
                action.dataAuth(),
                granted,
                grant == null || grant.getDataScopePolicy() == null ? DataScopePolicy.NONE : grant.getDataScopePolicy(),
                grant == null || grant.getTenantScopePolicy() == null
                        ? TenantScopePolicy.CURRENT_TENANT
                        : grant.getTenantScopePolicy(),
                grant == null ? null : grant.getScopeCondition(),
                grant == null ? null : grant.getReferenceFieldId(),
                grant == null ? null : grant.getReferenceActionCode()
        );
    }
}
