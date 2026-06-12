package net.ximatai.muyun.spring.iam.role;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;

public enum RoleKind implements CodeTitleEnum {
    STANDARD("standard", "标准角色"),
    POSITION_TEMPLATE("positionTemplate", "岗位模板角色"),
    SYSTEM("system", "系统角色"),
    GROUP("group", "角色组"),
    WILDCARD_DATA_SCOPE("wildcardDataScope", "数据权限通配角色");

    private final String code;
    private final String title;

    RoleKind(String code, String title) {
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
