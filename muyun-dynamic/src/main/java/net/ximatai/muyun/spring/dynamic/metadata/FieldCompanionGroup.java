package net.ximatai.muyun.spring.dynamic.metadata;

import java.util.List;

public record FieldCompanionGroup(
        String ownerField,
        FieldCompanionKind kind,
        List<FieldCompanionDefinition> companions
) {
    public FieldCompanionGroup {
        companions = companions == null ? List.of() : List.copyOf(companions);
    }
}
