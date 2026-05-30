package net.ximatai.muyun.spring.dynamic.metadata;

import java.util.List;

public record ModuleDefinition(
        String moduleAlias,
        String name,
        List<EntityDefinition> entities,
        List<EntityRelationDefinition> relations,
        List<EntityReferenceDefinition> references,
        List<EntityViewDefinition> views
) {
    public ModuleDefinition(String moduleAlias, String name, List<EntityDefinition> entities) {
        this(moduleAlias, name, entities, List.of(), List.of(), List.of());
    }

    public ModuleDefinition(String moduleAlias,
                            String name,
                            List<EntityDefinition> entities,
                            List<EntityRelationDefinition> relations) {
        this(moduleAlias, name, entities, relations, List.of(), List.of());
    }

    public ModuleDefinition(String moduleAlias,
                            String name,
                            List<EntityDefinition> entities,
                            List<EntityRelationDefinition> relations,
                            List<EntityReferenceDefinition> references) {
        this(moduleAlias, name, entities, relations, references, List.of());
    }

    public ModuleDefinition {
        entities = entities == null ? List.of() : List.copyOf(entities);
        relations = relations == null ? List.of() : List.copyOf(relations);
        references = references == null ? List.of() : List.copyOf(references);
        views = views == null ? List.of() : List.copyOf(views);
    }

    public String code() {
        return moduleAlias;
    }
}
