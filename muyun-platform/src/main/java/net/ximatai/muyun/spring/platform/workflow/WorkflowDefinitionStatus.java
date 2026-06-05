package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;

public enum WorkflowDefinitionStatus implements CodeTitleEnum {
    DRAFT("draft", "草稿"),
    PUBLISHED("published", "已发布"),
    DISABLED("disabled", "已停用"),
    ARCHIVED("archived", "已归档");

    private final String code;
    private final String title;

    WorkflowDefinitionStatus(String code, String title) {
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
