package net.ximatai.muyun.spring.platform.menu;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;

public enum MenuScopeType implements CodeTitleEnum {
    SYSTEM("system", "系统"),
    TENANT("tenant", "租户"),
    ORGANIZATION("organization", "机构");

    private final String code;
    private final String title;

    MenuScopeType(String code, String title) {
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
