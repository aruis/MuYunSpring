package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;

public enum WorkflowOvertimeStatus implements CodeTitleEnum {
    NORMAL("normal", "正常"),
    WARNED("warned", "已预警"),
    AUDITED("audited", "已审计");

    private final String code;
    private final String title;

    WorkflowOvertimeStatus(String code, String title) {
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
