package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;

public enum WorkflowAddSignMode implements CodeTitleEnum {
    BEFORE("before", "前加签"),
    AFTER("after", "后加签"),
    PARALLEL("parallel", "并行加签");

    private final String code;
    private final String title;

    WorkflowAddSignMode(String code, String title) {
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
