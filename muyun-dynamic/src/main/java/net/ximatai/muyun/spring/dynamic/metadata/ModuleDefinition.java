package net.ximatai.muyun.spring.dynamic.metadata;

import java.util.List;

public record ModuleDefinition(
        String moduleAlias,
        String name,
        List<EntityDefinition> entities,
        List<EntityRelationDefinition> relations,
        List<EntityReferenceDefinition> references
) {
    public ModuleDefinition(String moduleAlias, String name, List<EntityDefinition> entities) {
        this(moduleAlias, name, entities, List.of(), List.of());
    }

    public ModuleDefinition(String moduleAlias,
                            String name,
                            List<EntityDefinition> entities,
                            List<EntityRelationDefinition> relations) {
        this(moduleAlias, name, entities, relations, List.of());
    }

    public ModuleDefinition {
        entities = entities == null ? List.of() : List.copyOf(entities);
        relations = relations == null ? List.of() : List.copyOf(relations);
        references = references == null ? List.of() : List.copyOf(references);
    }

    public String code() {
        return moduleAlias;
    }
}
