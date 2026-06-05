package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;

public enum WorkflowAssignmentKind implements CodeTitleEnum {
    NORMAL("normal", "正常分派"),
    DELEGATED("delegated", "委托分派"),
    TRANSFERRED("transferred", "转办分派"),
    ADD_SIGN("add_sign", "加签分派"),
    AGENT("agent", "代办分派");

    private final String code;
    private final String title;

    WorkflowAssignmentKind(String code, String title) {
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
