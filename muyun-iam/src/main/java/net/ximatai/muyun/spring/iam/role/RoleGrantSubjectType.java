package net.ximatai.muyun.spring.iam.role;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;
import net.ximatai.muyun.spring.common.exception.PlatformException;

import java.util.Arrays;

public enum RoleGrantSubjectType implements CodeTitleEnum {
    USER_ACCOUNT("userAccount", "用户账号"),
    EMPLOYEE("employee", "职员"),
    EMPLOYEE_POSITION("employeePosition", "职员任岗");

    private final String code;
    private final String title;

    RoleGrantSubjectType(String code, String title) {
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

    public static RoleGrantSubjectType fromCode(String code) {
        return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new PlatformException("unsupported role grant subject type: " + code));
    }
}
