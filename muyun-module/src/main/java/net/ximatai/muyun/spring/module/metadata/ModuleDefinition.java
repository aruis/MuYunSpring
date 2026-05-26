package net.ximatai.muyun.spring.module.metadata;

import java.util.List;

public record ModuleDefinition(
        String moduleAlias,
        String name,
        List<EntityDefinition> entities
) {
    public ModuleDefinition {
        entities = entities == null ? List.of() : List.copyOf(entities);
    }

    public String code() {
        return moduleAlias;
    }
}
