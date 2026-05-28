package net.ximatai.muyun.spring.common.schema;

import net.ximatai.muyun.database.core.builder.ColumnType;

public final class PlatformAbilityFields {
    public static final String TREE_PARENT_FIELD = "parentId";
    public static final String TREE_PARENT_COLUMN = "parent_id";
    public static final ColumnType TREE_PARENT_COLUMN_TYPE = ColumnType.VARCHAR;
    public static final int TREE_PARENT_LENGTH = 128;

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

    private PlatformAbilityFields() {
    }
}
