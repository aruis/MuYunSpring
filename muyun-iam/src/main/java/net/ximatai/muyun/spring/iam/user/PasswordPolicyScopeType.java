package net.ximatai.muyun.spring.iam.user;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;

public enum PasswordPolicyScopeType implements CodeTitleEnum {
    GLOBAL("global", "全局"),
    TENANT("tenant", "租户");

    private final String code;
    private final String title;

    PasswordPolicyScopeType(String code, String title) {
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
