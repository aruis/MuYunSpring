package net.ximatai.muyun.spring.common.schema;

import net.ximatai.muyun.database.core.builder.ColumnType;

public final class PlatformAbilityFields {
    public static final String TREE_PARENT_FIELD = "parentId";
    public static final String TREE_PARENT_COLUMN = "parent_id";
    public static final ColumnType TREE_PARENT_COLUMN_TYPE = ColumnType.VARCHAR;
    public static final int TREE_PARENT_LENGTH = 32;

    public static final String SORT_FIELD = "sortOrder";
    public static final String SORT_COLUMN = "sort_order";
    public static final ColumnType SORT_COLUMN_TYPE = ColumnType.INT;

    public static final String TITLE_FIELD = "title";
    public static final String TITLE_COLUMN = "title";
    public static final ColumnType TITLE_COLUMN_TYPE = ColumnType.VARCHAR;
    public static final int TITLE_LENGTH = 128;

    public static final String ENABLED_FIELD = "enabled";
    public static final String ENABLED_COLUMN = "enabled";
    public static final ColumnType ENABLED_COLUMN_TYPE = ColumnType.BOOLEAN;

    public static final String AUTH_USER_FIELD = "authUserId";
    public static final String AUTH_USER_COLUMN = "auth_user_id";
    public static final ColumnType AUTH_USER_COLUMN_TYPE = ColumnType.VARCHAR;
    public static final int AUTH_USER_LENGTH = 64;

    public static final String AUTH_ASSIGNEE_FIELD = "authAssigneeIds";
    public static final String AUTH_ASSIGNEE_COLUMN = "auth_assignee_ids";
    public static final ColumnType AUTH_ASSIGNEE_COLUMN_TYPE = ColumnType.TEXT;

    public static final String AUTH_MEMBER_FIELD = "authMemberIds";
    public static final String AUTH_MEMBER_COLUMN = "auth_member_ids";
    public static final ColumnType AUTH_MEMBER_COLUMN_TYPE = ColumnType.TEXT;

    public static final String AUTH_ORGANIZATION_FIELD = "authOrganizationId";
    public static final String AUTH_ORGANIZATION_COLUMN = "auth_organization_id";
    public static final ColumnType AUTH_ORGANIZATION_COLUMN_TYPE = ColumnType.VARCHAR;
    public static final int AUTH_ORGANIZATION_LENGTH = 64;

    public static final String AUTH_MODULE_FIELD = "authModuleAlias";
    public static final String AUTH_MODULE_COLUMN = "auth_module_alias";
    public static final ColumnType AUTH_MODULE_COLUMN_TYPE = ColumnType.VARCHAR;
    public static final int AUTH_MODULE_LENGTH = 128;

    private PlatformAbilityFields() {
    }
}
