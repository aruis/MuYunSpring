package net.ximatai.muyun.spring.iam.role;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;

public enum TenantScopePolicy implements CodeTitleEnum {
    CURRENT_TENANT("currentTenant", "当前租户"),
    ALL_TENANTS("allTenants", "全部租户");

    private final String code;
    private final String title;

    TenantScopePolicy(String code, String title) {
        this.code = code;
        this.title = title;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getTitle() {
        return title;
    }
}
