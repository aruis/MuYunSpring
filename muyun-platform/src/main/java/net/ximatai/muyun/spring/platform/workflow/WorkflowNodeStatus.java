package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;

public enum WorkflowNodeStatus implements CodeTitleEnum {
    WAITING("waiting", "等待中"),
    ACTIVE("active", "处理中"),
    COMPLETED("completed", "已完成"),
    ROLLED_BACK("rolled_back", "已回退"),
    REJECTED("rejected", "已驳回"),
    SKIPPED("skipped", "已跳过"),
    CANCELED("canceled", "已取消");

    private final String code;
    private final String title;

    WorkflowNodeStatus(String code, String title) {
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
