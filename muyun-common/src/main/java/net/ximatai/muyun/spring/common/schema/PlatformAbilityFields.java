package net.ximatai.muyun.spring.common.schema;

import net.ximatai.muyun.database.core.builder.ColumnType;

public final class PlatformAbilityFields {
    public static final String SORT_FIELD = "sortOrder";
    public static final String SORT_COLUMN = "sort_order";
    public static final ColumnType SORT_COLUMN_TYPE = ColumnType.INT;

    public static final String TITLE_FIELD = "title";
    public static final String TITLE_COLUMN = "title";
    public static final ColumnType TITLE_COLUMN_TYPE = ColumnType.VARCHAR;
    public static final int TITLE_LENGTH = 128;

    private PlatformAbilityFields() {
    }
}
