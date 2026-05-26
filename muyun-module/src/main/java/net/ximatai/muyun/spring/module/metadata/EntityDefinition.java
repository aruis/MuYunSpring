package net.ximatai.muyun.spring.module.metadata;

import java.util.List;

public record EntityDefinition(
        String code,
        String tableName,
        String name,
        List<FieldDefinition> fields
) {
    public EntityDefinition {
        fields = fields == null ? List.of() : List.copyOf(fields);
    }
}
