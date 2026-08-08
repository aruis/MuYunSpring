package net.ximatai.muyun.spring.ability.reference;

import java.util.List;

/** Compiled structured projection of a static reference field. */
public record ReferenceSummaryPlan(String sourceField,
                                   ReferenceTarget target,
                                   ReferenceCardinality cardinality,
                                   List<String> fields,
                                   String outputField) {
    public ReferenceSummaryPlan {
        fields = fields == null ? List.of() : List.copyOf(fields);
    }
}
