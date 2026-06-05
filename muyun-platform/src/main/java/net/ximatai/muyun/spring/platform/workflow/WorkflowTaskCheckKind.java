package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;

public enum WorkflowTaskCheckKind implements CodeTitleEnum {
    MANUAL_CONFIRM("manual_confirm", "人工确认"),
    FORMULA("formula", "公式判定"),
    QUERY_EXISTS("query_exists", "查询存在"),
    RELATED_QUERY_EXISTS("related_query_exists", "关联查询存在"),
    GENERATED_QUERY_EXISTS("generated_query_exists", "生成查询存在");

    private final String code;
    private final String title;

    WorkflowTaskCheckKind(String code, String title) {
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
