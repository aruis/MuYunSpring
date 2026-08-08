package net.ximatai.muyun.spring.platform.web;

import net.ximatai.muyun.spring.ability.reference.ReferenceCardinality;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;

import java.util.List;

/**
 * Client-safe fact for a structured reference summary read projection.
 *
 * <p>Every item always includes {@code id}; {@link #fields()} contains the
 * additional target attributes supplied by the shared reference projection.</p>
 */
public record ResolvedReferenceSummaryFieldDescriptor(String sourceField,
                                                      String targetModuleAlias,
                                                      ReferenceCardinality cardinality,
                                                      List<String> fields) {
    public ResolvedReferenceSummaryFieldDescriptor {
        if (sourceField == null || sourceField.isBlank()) {
            throw new IllegalArgumentException("reference summary source field must not be blank");
        }
        sourceField = sourceField.trim();
        targetModuleAlias = PlatformNameRules.requireModuleAlias(targetModuleAlias);
        cardinality = cardinality == null ? ReferenceCardinality.ONE : cardinality;
        fields = fields == null ? List.of() : List.copyOf(fields);
    }

    public boolean includes(String fieldName) {
        return fields.contains(fieldName);
    }
}
