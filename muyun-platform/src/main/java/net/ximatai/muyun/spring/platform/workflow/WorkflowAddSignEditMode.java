package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;

public enum WorkflowAddSignEditMode implements CodeTitleEnum {
    CREATE("create", "创建加签段"),
    UNSUPPORTED_REPLACE_FAIL_FAST("unsupported_replace_fail_fast", "重复加签替换暂未开放");

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
