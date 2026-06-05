package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;

public enum WorkflowInstanceStatus implements CodeTitleEnum {
    RUNNING("running", "运行中"),
    COMPLETED("completed", "已完成"),
    REJECTED("rejected", "已驳回"),
    REVOKED("revoked", "已撤回"),
    TERMINATED("terminated", "已终止");

    private final String code;
    private final String title;

    WorkflowInstanceStatus(String code, String title) {
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
