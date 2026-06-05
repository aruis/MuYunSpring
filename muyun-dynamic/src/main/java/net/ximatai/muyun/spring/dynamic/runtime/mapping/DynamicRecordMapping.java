package net.ximatai.muyun.spring.dynamic.runtime.mapping;

import net.ximatai.muyun.spring.common.platform.EntityCapability;
import net.ximatai.muyun.spring.common.schema.PlatformDataScopeSchema;
import net.ximatai.muyun.spring.common.schema.StandardEntitySchema;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldCompanionRules;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class DynamicRecordMapping {
    private final Map<String, String> columns = new HashMap<>();
    private final Set<String> protectedStorageFields = new HashSet<>();

    public DynamicRecordMapping(EntityDefinition entity) {
        columns.put(StandardEntitySchema.ID_FIELD, StandardEntitySchema.ID_COLUMN);
        columns.put(StandardEntitySchema.ID_COLUMN, StandardEntitySchema.ID_COLUMN);
        columns.put(StandardEntitySchema.TENANT_ID_FIELD, StandardEntitySchema.TENANT_ID_COLUMN);
        columns.put(StandardEntitySchema.TENANT_ID_COLUMN, StandardEntitySchema.TENANT_ID_COLUMN);
        columns.put(StandardEntitySchema.VERSION_FIELD, StandardEntitySchema.VERSION_COLUMN);
        columns.put(StandardEntitySchema.VERSION_COLUMN, StandardEntitySchema.VERSION_COLUMN);
        columns.put(StandardEntitySchema.DELETED_FIELD, StandardEntitySchema.DELETED_COLUMN);
        columns.put(StandardEntitySchema.DELETED_COLUMN, StandardEntitySchema.DELETED_COLUMN);
        columns.put(StandardEntitySchema.DELETED_AT_FIELD, StandardEntitySchema.DELETED_AT_COLUMN);
        columns.put(StandardEntitySchema.DELETED_AT_COLUMN, StandardEntitySchema.DELETED_AT_COLUMN);
        columns.put(StandardEntitySchema.CREATED_BY_FIELD, StandardEntitySchema.CREATED_BY_COLUMN);
        columns.put(StandardEntitySchema.CREATED_BY_COLUMN, StandardEntitySchema.CREATED_BY_COLUMN);
        columns.put(StandardEntitySchema.CREATED_AT_FIELD, StandardEntitySchema.CREATED_AT_COLUMN);
        columns.put(StandardEntitySchema.CREATED_AT_COLUMN, StandardEntitySchema.CREATED_AT_COLUMN);
        columns.put(StandardEntitySchema.UPDATED_BY_FIELD, StandardEntitySchema.UPDATED_BY_COLUMN);
        columns.put(StandardEntitySchema.UPDATED_BY_COLUMN, StandardEntitySchema.UPDATED_BY_COLUMN);
        columns.put(StandardEntitySchema.UPDATED_AT_FIELD, StandardEntitySchema.UPDATED_AT_COLUMN);
        columns.put(StandardEntitySchema.UPDATED_AT_COLUMN, StandardEntitySchema.UPDATED_AT_COLUMN);
        if (entity.supports(EntityCapability.DATA_SCOPE)) {
            PlatformDataScopeSchema.fieldToColumn().forEach((field, column) -> {
                columns.put(field, column);
                columns.put(column, column);
            });
        }
        for (FieldDefinition field : entity.fields()) {
            columns.put(field.code(), field.columnName());
            columns.put(field.columnName(), field.columnName());
            if (field.protection().hasStorageProtection()) {
                protectedStorageFields.add(field.code());
                protectedStorageFields.add(field.columnName());
                if (field.protection().signatureMode().enabled()) {
                    protectedStorageFields.add(FieldCompanionRules.signatureFieldName(field.fieldName()));
                    protectedStorageFields.add(FieldCompanionRules.signatureColumnName(field.columnName()));
                }
            }
        }
    }

    public String resolveColumn(String fieldOrColumn) {
        String column = columns.get(fieldOrColumn);
        if (column == null) {
            throw new IllegalArgumentException("unknown dynamic field or column: " + fieldOrColumn);
        }
        return column;
    }

    public String resolveQueryableColumn(String fieldOrColumn) {
        if (protectedStorageFields.contains(fieldOrColumn)) {
            throw new IllegalArgumentException("protected storage field cannot be used in dynamic query or sort: "
                    + fieldOrColumn);
        }
        return resolveColumn(fieldOrColumn);
    }
}
