package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;

public enum WorkflowConvergeMode implements CodeTitleEnum {
    ANY("any", "任一汇聚"),
    ALL("all", "全部汇聚"),
    RATIO("ratio", "比例汇聚");

    private final String code;
    private final String title;

    WorkflowConvergeMode(String code, String title) {
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
