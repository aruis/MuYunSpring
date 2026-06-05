package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;

public enum WorkflowPublishStatus implements CodeTitleEnum {
    DRAFT("draft", "草稿"),
    PUBLISHED("published", "已发布"),
    RETIRED("retired", "已退役");

    private final String code;
    private final String title;

    WorkflowPublishStatus(String code, String title) {
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
