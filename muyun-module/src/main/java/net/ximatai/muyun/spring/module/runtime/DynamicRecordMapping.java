package net.ximatai.muyun.spring.module.runtime;

import net.ximatai.muyun.spring.module.metadata.EntityDefinition;
import net.ximatai.muyun.spring.module.metadata.FieldDefinition;

import java.util.HashMap;
import java.util.Map;

final class DynamicRecordMapping {
    private final Map<String, String> columns = new HashMap<>();

    DynamicRecordMapping(EntityDefinition entity) {
        columns.put("id", "id");
        columns.put("version", "version");
        columns.put("deleted", "deleted");
        columns.put("createdBy", "created_by");
        columns.put("created_by", "created_by");
        columns.put("createdAt", "created_at");
        columns.put("created_at", "created_at");
        columns.put("updatedBy", "updated_by");
        columns.put("updated_by", "updated_by");
        columns.put("updatedAt", "updated_at");
        columns.put("updated_at", "updated_at");
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
