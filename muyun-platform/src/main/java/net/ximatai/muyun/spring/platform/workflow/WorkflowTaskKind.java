package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;

public enum WorkflowTaskKind implements CodeTitleEnum {
    APPROVAL("approval", "审批任务"),
    BUSINESS("business", "业务任务"),
    NOTICE("notice", "知会任务");

    private final String code;
    private final String title;

    WorkflowTaskKind(String code, String title) {
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
