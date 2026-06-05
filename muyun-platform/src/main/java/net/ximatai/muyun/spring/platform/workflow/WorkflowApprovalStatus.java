package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;

public enum WorkflowApprovalStatus implements CodeTitleEnum {
    NONE("none", "无审批"),
    DRAFT("draft", "草稿"),
    PROCESSING("processing", "审批中"),
    APPROVED("approved", "已通过"),
    REJECTED("rejected", "已驳回"),
    REVOKED("revoked", "已撤回"),
    TERMINATED("terminated", "已终止");

    private final String code;
    private final String title;

    WorkflowApprovalStatus(String code, String title) {
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
