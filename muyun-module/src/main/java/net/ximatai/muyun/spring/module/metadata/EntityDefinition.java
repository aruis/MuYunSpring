package net.ximatai.muyun.spring.module.metadata;

import java.util.EnumSet;
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
        capabilities = normalizeCapabilities(capabilities);
    }

    public EntityDefinition withCapabilities(EntityCapability... values) {
        return new EntityDefinition(code, tableName, name, fields, Set.of(values));
    }

    public boolean supports(EntityCapability capability) {
        return capabilities.contains(capability);
    }

    private static Set<EntityCapability> normalizeCapabilities(Set<EntityCapability> capabilities) {
        EnumSet<EntityCapability> normalized = capabilities == null || capabilities.isEmpty()
                ? EnumSet.of(EntityCapability.CRUD)
                : EnumSet.copyOf(capabilities);
        normalized.add(EntityCapability.CRUD);
        if (normalized.contains(EntityCapability.TREE)) {
            normalized.add(EntityCapability.SORT);
        }
        return Set.copyOf(normalized);
    }
}
