package net.ximatai.muyun.spring.iam.role;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;

public enum RoleSharePolicy implements CodeTitleEnum {
    PRIVATE("private", "私有"),
    OWNER_AND_CHILDREN("ownerAndChildren", "本级及下级"),
    TENANT("tenant", "租户公开"),
    PLATFORM("platform", "全局公开");

    private final String code;
    private final String title;

    RoleSharePolicy(String code, String title) {
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
