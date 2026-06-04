package net.ximatai.muyun.spring.iam.role;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;

public enum DataScopePolicy implements CodeTitleEnum {
    NONE("none", "无数据权限"),
    WILDCARD("wildcard", "通配数据权限"),
    ALL("all", "全部"),
    OWNER("owner", "本人负责"),
    ASSIGNEE("assignee", "负责人"),
    MEMBER("member", "相关人"),
    ORGANIZATION("organization", "本机构"),
    ORGANIZATION_AND_CHILDREN("organizationAndChildren", "本机构及下级"),
    CUSTOM("custom", "自定义条件"),
    REFERENCE_DEPENDENCY("referenceDependency", "引用依赖");

    private final String code;
    private final String title;

    DataScopePolicy(String code, String title) {
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
