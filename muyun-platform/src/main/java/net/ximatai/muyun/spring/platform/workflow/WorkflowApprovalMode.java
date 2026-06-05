package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;

public enum WorkflowApprovalMode implements CodeTitleEnum {
    ANY("any", "或签"),
    ALL("all", "会签"),
    RATIO("ratio", "比例通过"),
    NOTICE("notice", "知会");

    private final String code;
    private final String title;

    WorkflowApprovalMode(String code, String title) {
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
