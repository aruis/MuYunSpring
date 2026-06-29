package net.ximatai.muyun.spring.dynamic.metadata;

import net.ximatai.muyun.database.core.builder.ColumnType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class DynamicFieldColumnMetadata {
    private DynamicFieldColumnMetadata() {
    }

    public static ColumnType columnType(FieldDefinition field) {
        if (isJsonSetField(field)) {
            return ColumnType.JSON_SET;
        }
        return field.type().toColumnType();
    }

    public static Class<?> fieldJavaType(FieldDefinition field) {
        if (isJsonSetField(field)) {
            return List.class;
        }
        return switch (field.type()) {
            case STRING, TEXT -> String.class;
            case INTEGER -> Integer.class;
            case LONG -> Long.class;
            case BOOLEAN -> Boolean.class;
            case TIMESTAMP, ZONED_TIMESTAMP -> Instant.class;
            case DATE -> LocalDate.class;
            case DECIMAL -> BigDecimal.class;
            case JSON -> Object.class;
        };
    }

    public static boolean isJsonSetField(FieldDefinition field) {
        return field.valueShape() == FieldValueShape.JSON_SET;
    }
}
