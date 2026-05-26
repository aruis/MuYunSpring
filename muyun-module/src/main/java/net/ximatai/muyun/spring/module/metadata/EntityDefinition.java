package net.ximatai.muyun.spring.module.metadata;

import java.util.List;
import java.util.Set;

public record EntityDefinition(
        String code,
        String tableName,
        String name,
        List<FieldDefinition> fields,
        Set<EntityCapability> capabilities
) {
    public EntityDefinition(String code, String tableName, String name, List<FieldDefinition> fields) {
        this(code, tableName, name, fields, Set.of(EntityCapability.CRUD));
    }

    public EntityDefinition {
        fields = fields == null ? List.of() : List.copyOf(fields);
        capabilities = capabilities == null ? Set.of(EntityCapability.CRUD) : Set.copyOf(capabilities);
    }

    public EntityDefinition withCapabilities(EntityCapability... values) {
        return new EntityDefinition(code, tableName, name, fields, Set.of(values));
    }

    public boolean supports(EntityCapability capability) {
        return capabilities.contains(capability);
    }
}
