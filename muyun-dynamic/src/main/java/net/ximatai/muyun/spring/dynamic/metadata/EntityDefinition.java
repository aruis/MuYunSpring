package net.ximatai.muyun.spring.dynamic.metadata;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public record EntityDefinition(
        String code,
        String schemaName,
        String tableName,
        String name,
        List<FieldDefinition> fields,
        Set<EntityCapability> capabilities
) {
    public static final String DEFAULT_SCHEMA_NAME = "public";

    public EntityDefinition(String code, String tableName, String name, List<FieldDefinition> fields) {
        this(code, DEFAULT_SCHEMA_NAME, tableName, name, fields, Set.of(EntityCapability.CRUD));
    }

    public EntityDefinition(String code,
                            String tableName,
                            String name,
                            List<FieldDefinition> fields,
                            Set<EntityCapability> capabilities) {
        this(code, DEFAULT_SCHEMA_NAME, tableName, name, fields, capabilities);
    }

    public EntityDefinition {
        schemaName = schemaName == null || schemaName.isBlank() ? DEFAULT_SCHEMA_NAME : schemaName;
        fields = fields == null ? List.of() : List.copyOf(fields);
        capabilities = normalizeCapabilities(capabilities);
    }

    public EntityDefinition withCapabilities(EntityCapability... values) {
        return new EntityDefinition(code, schemaName, tableName, name, fields, Set.of(values));
    }

    public boolean supports(EntityCapability capability) {
        return capabilities.contains(capability);
    }

    private static Set<EntityCapability> normalizeCapabilities(Set<EntityCapability> capabilities) {
        EnumSet<EntityCapability> normalized = capabilities == null || capabilities.isEmpty()
                ? baselineCapabilities()
                : EnumSet.copyOf(capabilities);
        normalized.addAll(baselineCapabilities());
        if (normalized.contains(EntityCapability.TREE)) {
            normalized.add(EntityCapability.SORT);
        }
        return Set.copyOf(normalized);
    }

    private static EnumSet<EntityCapability> baselineCapabilities() {
        EnumSet<EntityCapability> values = EnumSet.noneOf(EntityCapability.class);
        for (EntityCapability capability : EntityCapability.values()) {
            if (capability.isBaseline()) {
                values.add(capability);
            }
        }
        return values;
    }
}
