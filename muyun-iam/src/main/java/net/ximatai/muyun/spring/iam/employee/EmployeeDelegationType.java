package net.ximatai.muyun.spring.iam.employee;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;

public enum EmployeeDelegationType implements CodeTitleEnum {
    BUSINESS("business", "业务代办");

    private final String code;
    private final String title;

    EmployeeDelegationType(String code, String title) {
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
