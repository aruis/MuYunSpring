package net.ximatai.muyun.spring.platform.menu;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;

public enum MenuNodeType implements CodeTitleEnum {
    GROUP("group", "分组"),
    ENTRY("entry", "入口");

    private final String code;
    private final String title;

    MenuNodeType(String code, String title) {
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
