package net.ximatai.muyun.spring.dynamic.metadata;

import java.util.List;

public record EntityViewDefinition(
        String entityAlias,
        EntityViewType viewType,
        String title,
        List<EntityViewFieldDefinition> fields
) {
    public EntityViewDefinition {
        fields = fields == null ? List.of() : List.copyOf(fields);
    }
}
