package net.ximatai.muyun.spring.iam.role;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;

public enum ManagementScopeType implements CodeTitleEnum {
    PLATFORM("platform", "平台"),
    TENANT("tenant", "租户"),
    ORGANIZATION("organization", "机构");

    private final String code;
    private final String title;

    ManagementScopeType(String code, String title) {
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
