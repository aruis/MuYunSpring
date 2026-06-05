package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;

public enum WorkflowRuntimePluginDispatchTiming implements CodeTitleEnum {
    SYNCHRONOUS("synchronous", "同步执行"),
    AFTER_COMMIT("after_commit", "提交后执行");

    private final String code;
    private final String title;

    WorkflowRuntimePluginDispatchTiming(String code, String title) {
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
