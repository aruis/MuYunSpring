package net.ximatai.muyun.spring.dynamic.metadata;

import net.ximatai.muyun.spring.common.platform.EntityCapability;

import java.util.EnumSet;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public record EntityDefinition(
        String alias,
        String schemaName,
        String tableName,
        String name,
        List<FieldDefinition> fields,
        Set<EntityCapability> capabilities,
        List<EntityFormulaRuleDefinition> formulaRules
) {
    public static final String DEFAULT_SCHEMA_NAME = "public";

    public EntityDefinition(String alias, String tableName, String name, List<FieldDefinition> fields) {
        this(alias, DEFAULT_SCHEMA_NAME, tableName, name, fields, Set.of(EntityCapability.CRUD), List.of());
    }

    public EntityDefinition(String alias,
                            String tableName,
                            String name,
                            List<FieldDefinition> fields,
                            Set<EntityCapability> capabilities) {
        this(alias, DEFAULT_SCHEMA_NAME, tableName, name, fields, capabilities, List.of());
    }

    public EntityDefinition(String alias,
                            String schemaName,
                            String tableName,
                            String name,
                            List<FieldDefinition> fields,
                            Set<EntityCapability> capabilities) {
        this(alias, schemaName, tableName, name, fields, capabilities, List.of());
    }

    public EntityDefinition {
        schemaName = schemaName == null || schemaName.isBlank() ? DEFAULT_SCHEMA_NAME : schemaName;
        fields = fields == null ? List.of() : List.copyOf(fields);
        capabilities = normalizeCapabilities(capabilities);
        formulaRules = formulaRules == null ? List.of() : List.copyOf(formulaRules);
    }

    public EntityDefinition withCapabilities(EntityCapability... values) {
        return new EntityDefinition(alias, schemaName, tableName, name, fields, Set.of(values), formulaRules);
    }

    public EntityDefinition withFormulaRules(EntityFormulaRuleDefinition... values) {
        return new EntityDefinition(alias, schemaName, tableName, name, fields, capabilities,
                values == null ? List.of() : List.of(values));
    }

    public List<EntityFormulaRuleDefinition> orderedFormulaRules() {
        return formulaRules.stream()
                .sorted(Comparator.<EntityFormulaRuleDefinition>comparingInt(EntityFormulaRuleDefinition::sortOrder)
                        .thenComparing(EntityFormulaRuleDefinition::code))
                .toList();
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
        if (normalized.contains(EntityCapability.APPROVAL)) {
            normalized.add(EntityCapability.WORKFLOW);
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
