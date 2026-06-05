package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;

public enum WorkflowRejectResubmitMode implements CodeTitleEnum {
    RESTART("restart", "重新发起"),
    RETURN_TO_ME("return_to_me", "回到驳回人");

    private final String code;
    private final String title;

    WorkflowRejectResubmitMode(String code, String title) {
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
