package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;

public enum WorkflowTaskCheckStatus implements CodeTitleEnum {
    NOT_CHECKED("not_checked", "未判定"),
    PASSED("passed", "已通过"),
    FAILED("failed", "未通过"),
    NO_CHECK("no_check", "无需判定");

    private final String code;
    private final String title;

    WorkflowTaskCheckStatus(String code, String title) {
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
