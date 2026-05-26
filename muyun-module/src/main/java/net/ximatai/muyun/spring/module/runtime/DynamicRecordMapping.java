package net.ximatai.muyun.spring.module.runtime;

import net.ximatai.muyun.spring.common.schema.StandardEntitySchema;
import net.ximatai.muyun.spring.module.metadata.EntityDefinition;
import net.ximatai.muyun.spring.module.metadata.FieldDefinition;

import java.util.HashMap;
import java.util.Map;

final class DynamicRecordMapping {
    private final Map<String, String> columns = new HashMap<>();

    DynamicRecordMapping(EntityDefinition entity) {
        columns.put(StandardEntitySchema.ID_FIELD, StandardEntitySchema.ID_COLUMN);
        columns.put(StandardEntitySchema.ID_COLUMN, StandardEntitySchema.ID_COLUMN);
        columns.put(StandardEntitySchema.VERSION_FIELD, StandardEntitySchema.VERSION_COLUMN);
        columns.put(StandardEntitySchema.VERSION_COLUMN, StandardEntitySchema.VERSION_COLUMN);
        columns.put(StandardEntitySchema.DELETED_FIELD, StandardEntitySchema.DELETED_COLUMN);
        columns.put(StandardEntitySchema.DELETED_COLUMN, StandardEntitySchema.DELETED_COLUMN);
        columns.put(StandardEntitySchema.CREATED_BY_FIELD, StandardEntitySchema.CREATED_BY_COLUMN);
        columns.put(StandardEntitySchema.CREATED_BY_COLUMN, StandardEntitySchema.CREATED_BY_COLUMN);
        columns.put(StandardEntitySchema.CREATED_AT_FIELD, StandardEntitySchema.CREATED_AT_COLUMN);
        columns.put(StandardEntitySchema.CREATED_AT_COLUMN, StandardEntitySchema.CREATED_AT_COLUMN);
        columns.put(StandardEntitySchema.UPDATED_BY_FIELD, StandardEntitySchema.UPDATED_BY_COLUMN);
        columns.put(StandardEntitySchema.UPDATED_BY_COLUMN, StandardEntitySchema.UPDATED_BY_COLUMN);
        columns.put(StandardEntitySchema.UPDATED_AT_FIELD, StandardEntitySchema.UPDATED_AT_COLUMN);
        columns.put(StandardEntitySchema.UPDATED_AT_COLUMN, StandardEntitySchema.UPDATED_AT_COLUMN);
        for (FieldDefinition field : entity.fields()) {
            columns.put(field.code(), field.columnName());
            columns.put(field.columnName(), field.columnName());
        }
    }

    String resolveColumn(String fieldOrColumn) {
        String column = columns.get(fieldOrColumn);
        if (column == null) {
            throw new IllegalArgumentException("unknown dynamic field or column: " + fieldOrColumn);
        }
        return column;
    }
}
