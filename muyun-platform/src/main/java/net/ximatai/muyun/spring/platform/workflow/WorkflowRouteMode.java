package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;

public enum WorkflowRouteMode implements CodeTitleEnum {
    AUTO("auto", "自动分支"),
    MANUAL("manual", "手工分支");

    private final String code;
    private final String title;

    WorkflowRouteMode(String code, String title) {
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
