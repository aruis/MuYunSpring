package net.ximatai.muyun.spring.common.schema;

import net.ximatai.muyun.database.core.annotation.AnnotationProcessor;
import net.ximatai.muyun.database.core.annotation.Indexed;
import net.ximatai.muyun.database.core.builder.TableWrapper;
import net.ximatai.muyun.spring.common.model.StandardEntity;

import java.lang.reflect.Field;
import java.util.Objects;

public class StaticEntityTableMapper {
    private final PlatformTableValidator tableValidator;

    public StaticEntityTableMapper() {
        this(new PlatformTableValidator());
    }

    public StaticEntityTableMapper(PlatformTableValidator tableValidator) {
        this.tableValidator = Objects.requireNonNull(tableValidator, "tableValidator must not be null");
    }

    public TableWrapper toTable(Class<?> modelClass) {
        requirePlatformEntity(modelClass);
        TableWrapper table = AnnotationProcessor.fromEntityClass(modelClass);
        PlatformUniqueIndexes.normalizeTenantUniqueIndexes(table);
        addTenantUniqueIndexesFromFields(table, modelClass);
        tableValidator.requireStandardEntityTable(table, modelClass.getName());
        return table;
    }

    private void requirePlatformEntity(Class<?> modelClass) {
        Objects.requireNonNull(modelClass, "modelClass must not be null");
        if (!StandardEntity.class.isAssignableFrom(modelClass)) {
            throw new IllegalArgumentException("static entity must extend StandardEntity: " + modelClass.getName());
        }
    }

    private void addTenantUniqueIndexesFromFields(TableWrapper table, Class<?> modelClass) {
        Class<?> current = modelClass;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                net.ximatai.muyun.database.core.annotation.Column column =
                        field.getAnnotation(net.ximatai.muyun.database.core.annotation.Column.class);
                Indexed indexed = field.getAnnotation(Indexed.class);
                if ((column != null && column.unique()) || (indexed != null && indexed.unique())) {
                    PlatformUniqueIndexes.addTenantUniqueIndex(table, columnName(field, column));
                }
            }
            current = current.getSuperclass();
        }
    }

    private String columnName(Field field, net.ximatai.muyun.database.core.annotation.Column column) {
        if (column != null && !column.name().isBlank()) {
            return column.name();
        }
        return field.getName();
    }
}
