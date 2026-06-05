package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;

public enum WorkflowRuntimeTerminateMode implements CodeTitleEnum {
    NORMAL("normal", "普通终止"),
    FORCE("force", "强制终止");

    private final String code;
    private final String title;

    WorkflowRuntimeTerminateMode(String code, String title) {
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
