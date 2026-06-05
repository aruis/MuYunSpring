package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;

public enum WorkflowTaskStatus implements CodeTitleEnum {
    TODO("todo", "待办"),
    DONE("done", "已办"),
    TRANSFERRED("transferred", "已转办"),
    REJECTED("rejected", "已驳回"),
    ROLLED_BACK("rolled_back", "已回退"),
    NOTICED("noticed", "已知会"),
    INVALIDATED("invalidated", "已失效"),
    SKIPPED("skipped", "已跳过"),
    CANCELED("canceled", "已取消");

    private final String code;
    private final String title;

    WorkflowTaskStatus(String code, String title) {
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
