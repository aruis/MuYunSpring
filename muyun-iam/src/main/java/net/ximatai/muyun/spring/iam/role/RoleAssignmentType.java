package net.ximatai.muyun.spring.iam.role;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;

public enum RoleAssignmentType implements CodeTitleEnum {
    ACCOUNT("account", "账号角色"),
    EMPLOYMENT("employment", "任职角色");

    private final String code;
    private final String title;

    RoleAssignmentType(String code, String title) {
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
