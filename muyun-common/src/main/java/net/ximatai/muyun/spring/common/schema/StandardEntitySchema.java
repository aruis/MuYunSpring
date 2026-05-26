package net.ximatai.muyun.spring.common.schema;

import net.ximatai.muyun.database.core.annotation.Id;
import net.ximatai.muyun.database.core.builder.Column;
import net.ximatai.muyun.spring.common.model.StandardEntity;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

public final class StandardEntitySchema {
    private StandardEntitySchema() {
    }

    public static Column idColumn() {
        return columnFrom(field("id"));
    }

    public static List<Column> auditColumns() {
        return Arrays.stream(StandardEntity.class.getDeclaredFields())
                .filter(field -> field.getAnnotation(Id.class) == null)
                .map(StandardEntitySchema::columnFrom)
                .toList();
    }

    public static List<String> columnNames() {
        return Arrays.stream(StandardEntity.class.getDeclaredFields())
                .map(StandardEntitySchema::columnName)
                .toList();
    }

    private static Field field(String name) {
        try {
            return StandardEntity.class.getDeclaredField(name);
        } catch (NoSuchFieldException exception) {
            throw new IllegalStateException("standard entity field missing: " + name, exception);
        }
    }

    private static Column columnFrom(Field field) {
        net.ximatai.muyun.database.core.annotation.Column annotation =
                field.getAnnotation(net.ximatai.muyun.database.core.annotation.Column.class);
        if (annotation == null) {
            throw new IllegalStateException("standard entity field has no @Column: " + field.getName());
        }
        Column column = Column.of(columnName(field))
                .setType(annotation.type())
                .setNullable(annotation.nullable());
        if (annotation.length() > 0) {
            column.setLength(annotation.length());
        }
        if (annotation.precision() > 0) {
            column.setPrecision(annotation.precision());
        }
        if (annotation.scale() > 0) {
            column.setScale(annotation.scale());
        }
        if (!annotation.comment().isEmpty()) {
            column.setComment(annotation.comment());
        }
        if (field.getAnnotation(Id.class) != null) {
            column.setPrimaryKey();
        }
        return column;
    }

    private static String columnName(Field field) {
        net.ximatai.muyun.database.core.annotation.Column annotation =
                field.getAnnotation(net.ximatai.muyun.database.core.annotation.Column.class);
        return annotation.name();
    }
}
