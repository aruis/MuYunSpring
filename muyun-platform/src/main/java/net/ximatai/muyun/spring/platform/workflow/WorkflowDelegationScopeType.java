package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;

public enum WorkflowDelegationScopeType implements CodeTitleEnum {
    ALL("all", "全部"),
    INCLUDE("include", "指定");

    private final String code;
    private final String title;

    WorkflowDelegationScopeType(String code, String title) {
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
