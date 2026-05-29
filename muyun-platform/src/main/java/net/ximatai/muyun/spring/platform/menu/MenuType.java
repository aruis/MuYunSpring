package net.ximatai.muyun.spring.platform.menu;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;

public enum MenuType implements CodeTitleEnum {
    GROUP("group", "分组"),
    MODULE("module", "模块"),
    ROUTE("route", "路由"),
    LINK("link", "链接");

    private final String code;
    private final String title;

    MenuType(String code, String title) {
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
