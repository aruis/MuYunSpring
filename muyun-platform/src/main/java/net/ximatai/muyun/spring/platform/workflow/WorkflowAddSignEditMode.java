package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;

public enum WorkflowAddSignEditMode implements CodeTitleEnum {
    CREATE("create", "创建加签段"),
    REPLACE("replace", "替换加签段");

    private final String code;
    private final String title;

    WorkflowAddSignEditMode(String code, String title) {
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
