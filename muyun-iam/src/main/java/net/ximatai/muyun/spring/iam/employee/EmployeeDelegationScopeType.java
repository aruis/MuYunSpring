package net.ximatai.muyun.spring.iam.employee;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;

public enum EmployeeDelegationScopeType implements CodeTitleEnum {
    ALL("all", "全部"),
    INCLUDE("include", "指定");

    private final String code;
    private final String title;

    EmployeeDelegationScopeType(String code, String title) {
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
