package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;

public enum WorkflowRouteStatus implements CodeTitleEnum {
    CANDIDATE("candidate", "候选"),
    EFFECTIVE("effective", "有效"),
    INEFFECTIVE("ineffective", "无效"),
    DROPPED("dropped", "已裁剪"),
    CLOSED("closed", "已关闭"),
    CANCELED("canceled", "已取消");

    private final String code;
    private final String title;

    WorkflowRouteStatus(String code, String title) {
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
