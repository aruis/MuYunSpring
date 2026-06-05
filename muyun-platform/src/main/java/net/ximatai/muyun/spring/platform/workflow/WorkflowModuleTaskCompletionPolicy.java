package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;

public enum WorkflowModuleTaskCompletionPolicy implements CodeTitleEnum {
    CHECK_PASSED("check_passed", "判定通过"),
    MANUAL_CONFIRM("manual_confirm", "人工确认"),
    AFTER_ACTION_SUCCESS("after_action_success", "动作成功后完成");

    private final String code;
    private final String title;

    WorkflowModuleTaskCompletionPolicy(String code, String title) {
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
