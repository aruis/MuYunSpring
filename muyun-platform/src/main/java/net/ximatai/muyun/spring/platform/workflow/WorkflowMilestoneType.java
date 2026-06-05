package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;

public enum WorkflowMilestoneType implements CodeTitleEnum {
    APPROVAL_COMPLETED("approval_completed", "审批完成");

    private final String code;
    private final String title;

    WorkflowMilestoneType(String code, String title) {
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
