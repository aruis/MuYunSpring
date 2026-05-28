package net.ximatai.muyun.spring.dynamic.metadata;

import net.ximatai.muyun.database.core.builder.ColumnType;

public enum FieldType {
    STRING(ColumnType.VARCHAR),
    TEXT(ColumnType.TEXT),
    INTEGER(ColumnType.INT),
    LONG(ColumnType.BIGINT),
    BOOLEAN(ColumnType.BOOLEAN),
    TIMESTAMP(ColumnType.TIMESTAMP),
    DATE(ColumnType.DATE),
    DECIMAL(ColumnType.NUMERIC),
    JSON(ColumnType.JSON);

    private final ColumnType columnType;

    FieldType(ColumnType columnType) {
        this.columnType = columnType;
    }

    public ColumnType toColumnType() {
        return columnType;
    }
}
