package net.ximatai.muyun.spring.module.metadata;

import java.util.List;

public record ModuleDefinition(
        String code,
        String name,
        List<EntityDefinition> entities
) {
    public ModuleDefinition {
        entities = entities == null ? List.of() : List.copyOf(entities);
    }
}
