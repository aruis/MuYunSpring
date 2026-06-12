package net.ximatai.muyun.spring.common.schema;

import net.ximatai.muyun.database.core.builder.Column;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PlatformDataScopeSchema {
    private PlatformDataScopeSchema() {
    }

    public static List<Column> columns() {
        return List.of(
                Column.of(PlatformAbilityFields.AUTH_USER_COLUMN)
                        .setType(PlatformAbilityFields.AUTH_USER_COLUMN_TYPE)
                        .setLength(PlatformAbilityFields.AUTH_USER_LENGTH)
                        .setComment("Data scope owner user id"),
                Column.of(PlatformAbilityFields.AUTH_ASSIGNEE_COLUMN)
                        .setType(PlatformAbilityFields.AUTH_ASSIGNEE_COLUMN_TYPE)
                        .setComment("Data scope assignee ids"),
                Column.of(PlatformAbilityFields.AUTH_MEMBER_COLUMN)
                        .setType(PlatformAbilityFields.AUTH_MEMBER_COLUMN_TYPE)
                        .setComment("Data scope member ids"),
                Column.of(PlatformAbilityFields.AUTH_ORGANIZATION_COLUMN)
                        .setType(PlatformAbilityFields.AUTH_ORGANIZATION_COLUMN_TYPE)
                        .setLength(PlatformAbilityFields.AUTH_ORGANIZATION_LENGTH)
                        .setComment("Data scope organization id"),
                Column.of(PlatformAbilityFields.AUTH_DEPARTMENT_COLUMN)
                        .setType(PlatformAbilityFields.AUTH_DEPARTMENT_COLUMN_TYPE)
                        .setLength(PlatformAbilityFields.AUTH_DEPARTMENT_LENGTH)
                        .setComment("Data scope department id"),
                Column.of(PlatformAbilityFields.AUTH_MODULE_COLUMN)
                        .setType(PlatformAbilityFields.AUTH_MODULE_COLUMN_TYPE)
                        .setLength(PlatformAbilityFields.AUTH_MODULE_LENGTH)
                        .setComment("Data scope module alias")
        );
    }

    public static List<String> fieldNames() {
        return List.of(
                PlatformAbilityFields.AUTH_USER_FIELD,
                PlatformAbilityFields.AUTH_ASSIGNEE_FIELD,
                PlatformAbilityFields.AUTH_MEMBER_FIELD,
                PlatformAbilityFields.AUTH_ORGANIZATION_FIELD,
                PlatformAbilityFields.AUTH_DEPARTMENT_FIELD,
                PlatformAbilityFields.AUTH_MODULE_FIELD
        );
    }

    public static List<String> columnNames() {
        return columns().stream().map(Column::getName).toList();
    }

    public static Map<String, String> fieldToColumn() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put(PlatformAbilityFields.AUTH_USER_FIELD, PlatformAbilityFields.AUTH_USER_COLUMN);
        values.put(PlatformAbilityFields.AUTH_ASSIGNEE_FIELD, PlatformAbilityFields.AUTH_ASSIGNEE_COLUMN);
        values.put(PlatformAbilityFields.AUTH_MEMBER_FIELD, PlatformAbilityFields.AUTH_MEMBER_COLUMN);
        values.put(PlatformAbilityFields.AUTH_ORGANIZATION_FIELD, PlatformAbilityFields.AUTH_ORGANIZATION_COLUMN);
        values.put(PlatformAbilityFields.AUTH_DEPARTMENT_FIELD, PlatformAbilityFields.AUTH_DEPARTMENT_COLUMN);
        values.put(PlatformAbilityFields.AUTH_MODULE_FIELD, PlatformAbilityFields.AUTH_MODULE_COLUMN);
        return Map.copyOf(values);
    }
}
