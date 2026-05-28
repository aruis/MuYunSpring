package net.ximatai.muyun.spring.platform.metadata;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;

public enum RelationRole implements CodeTitleEnum {
    MAIN("main", "主元数据"),
    CHILD("child", "子元数据");

    private final String code;
    private final String title;

    RelationRole(String code, String title) {
        this.code = code;
        this.title = title;
    }

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }
}
