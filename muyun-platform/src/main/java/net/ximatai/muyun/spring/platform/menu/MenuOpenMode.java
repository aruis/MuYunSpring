package net.ximatai.muyun.spring.platform.menu;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;

public enum MenuOpenMode implements CodeTitleEnum {
    TAB("tab", "页签"),
    WINDOW("window", "窗口");

    private final String code;
    private final String title;

    MenuOpenMode(String code, String title) {
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
