package net.ximatai.muyun.spring.dynamic.metadata;

import java.util.List;

public record ModuleDefinition(
        String moduleAlias,
        String name,
        List<EntityDefinition> entities,
        List<EntityRelationDefinition> relations,
        List<EntityReferenceDefinition> references,
        List<EntityViewDefinition> views,
        List<EntityAssociationViewDefinition> associationViews,
        List<EntityActionDefinition> actions,
        String mainEntityAlias
) {
    public ModuleDefinition(String moduleAlias, String name, List<EntityDefinition> entities) {
        this(moduleAlias, name, entities, List.of(), List.of(), List.of(), List.of(), List.of(), null);
    }

    public ModuleDefinition(String moduleAlias,
                            String name,
                            List<EntityDefinition> entities,
                            List<EntityRelationDefinition> relations) {
        this(moduleAlias, name, entities, relations, List.of(), List.of(), List.of(), List.of(), null);
    }

    public ModuleDefinition(String moduleAlias,
                            String name,
                            List<EntityDefinition> entities,
                            List<EntityRelationDefinition> relations,
                            List<EntityReferenceDefinition> references) {
        this(moduleAlias, name, entities, relations, references, List.of(), List.of(), List.of(), null);
    }

    public ModuleDefinition(String moduleAlias,
                            String name,
                            List<EntityDefinition> entities,
                            List<EntityRelationDefinition> relations,
                            List<EntityReferenceDefinition> references,
                            List<EntityViewDefinition> views) {
        this(moduleAlias, name, entities, relations, references, views, List.of(), List.of(), null);
    }

    public ModuleDefinition(String moduleAlias,
                            String name,
                            List<EntityDefinition> entities,
                            List<EntityRelationDefinition> relations,
                            List<EntityReferenceDefinition> references,
                            List<EntityViewDefinition> views,
                            List<EntityActionDefinition> actions) {
        this(moduleAlias, name, entities, relations, references, views, List.of(), actions, null);
    }

    public ModuleDefinition(String moduleAlias,
                            String name,
                            List<EntityDefinition> entities,
                            List<EntityRelationDefinition> relations,
                            List<EntityReferenceDefinition> references,
                            List<EntityViewDefinition> views,
                            List<EntityAssociationViewDefinition> associationViews,
                            List<EntityActionDefinition> actions) {
        this(moduleAlias, name, entities, relations, references, views, associationViews, actions, null);
    }

    public ModuleDefinition {
        entities = entities == null ? List.of() : List.copyOf(entities);
        relations = relations == null ? List.of() : List.copyOf(relations);
        references = references == null ? List.of() : List.copyOf(references);
        views = views == null ? List.of() : List.copyOf(views);
        associationViews = associationViews == null ? List.of() : List.copyOf(associationViews);
        actions = actions == null ? List.of() : List.copyOf(actions);
        if (mainEntityAlias == null || mainEntityAlias.isBlank()) {
            mainEntityAlias = entities.isEmpty() ? null : entities.getFirst().alias();
        }
    }

    public String code() {
        return moduleAlias;
    }
}
