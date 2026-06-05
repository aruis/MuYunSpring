package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;

public enum WorkflowTaskGuideKind implements CodeTitleEnum {
    OPEN_FORM("open_form", "打开表单"),
    OPEN_LIST("open_list", "打开列表"),
    FOCUS_FIELD("focus_field", "聚焦字段"),
    EXECUTE_ACTION("execute_action", "执行业务动作"),
    READ_INSTRUCTION("read_instruction", "阅读说明");

    private final String code;
    private final String title;

    WorkflowTaskGuideKind(String code, String title) {
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
