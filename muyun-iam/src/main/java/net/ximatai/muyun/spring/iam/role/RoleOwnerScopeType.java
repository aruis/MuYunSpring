package net.ximatai.muyun.spring.iam.role;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;

public enum RoleOwnerScopeType implements CodeTitleEnum {
    PLATFORM("platform", "平台"),
    TENANT("tenant", "租户"),
    ORGANIZATION("organization", "机构");

    private final String code;
    private final String title;

    RoleOwnerScopeType(String code, String title) {
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
