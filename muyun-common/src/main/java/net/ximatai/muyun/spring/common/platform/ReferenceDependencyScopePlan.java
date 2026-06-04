package net.ximatai.muyun.spring.common.platform;

import net.ximatai.muyun.database.core.metadata.DBInfo;
import net.ximatai.muyun.spring.common.schema.StandardEntitySchema;
import net.ximatai.muyun.spring.common.util.Preconditions;

import java.util.Map;

public record ReferenceDependencyScopePlan(
        String sourceField,
        String targetModuleAlias,
        String targetEntityAlias,
        String targetSchemaName,
        String targetTableName,
        Map<String, String> targetFieldToColumn,
        DBInfo.Type databaseType
) {
    public ReferenceDependencyScopePlan {
        sourceField = Preconditions.requireText(sourceField, "sourceField");
        targetModuleAlias = Preconditions.requireText(targetModuleAlias, "targetModuleAlias");
        targetEntityAlias = Preconditions.requireText(targetEntityAlias, "targetEntityAlias");
        targetSchemaName = Preconditions.requireText(targetSchemaName, "targetSchemaName");
        targetTableName = Preconditions.requireText(targetTableName, "targetTableName");
        targetFieldToColumn = targetFieldToColumn == null ? Map.of() : Map.copyOf(targetFieldToColumn);
        databaseType = databaseType == null ? DBInfo.Type.POSTGRESQL : databaseType;
        if (!targetFieldToColumn.containsKey(StandardEntitySchema.ID_FIELD)) {
            throw new IllegalArgumentException("targetFieldToColumn must contain id mapping");
        }
    }

    public String resolveTargetColumn(String fieldOrColumn) {
        String column = targetFieldToColumn.get(fieldOrColumn);
        if (column == null) {
            throw new IllegalArgumentException("unknown reference target field or column: " + fieldOrColumn);
        }
        return column;
    }
}
