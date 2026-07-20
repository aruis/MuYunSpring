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

    public static Builder builder(String moduleAlias, String name) {
        return new Builder(moduleAlias, name);
    }

    public static final class Builder {
        private final String moduleAlias;
        private final String name;
        private List<EntityDefinition> entities = List.of();
        private List<EntityRelationDefinition> relations = List.of();
        private List<EntityReferenceDefinition> references = List.of();
        private List<EntityViewDefinition> views = List.of();
        private List<EntityAssociationViewDefinition> associationViews = List.of();
        private List<EntityActionDefinition> actions = List.of();
        private String mainEntityAlias;

        private Builder(String moduleAlias, String name) {
            this.moduleAlias = moduleAlias;
            this.name = name;
        }

        public Builder entities(List<EntityDefinition> entities) {
            this.entities = entities;
            return this;
        }

        public Builder relations(List<EntityRelationDefinition> relations) {
            this.relations = relations;
            return this;
        }

        public Builder references(List<EntityReferenceDefinition> references) {
            this.references = references;
            return this;
        }

        public Builder views(List<EntityViewDefinition> views) {
            this.views = views;
            return this;
        }

        public Builder associationViews(List<EntityAssociationViewDefinition> associationViews) {
            this.associationViews = associationViews;
            return this;
        }

        public Builder actions(List<EntityActionDefinition> actions) {
            this.actions = actions;
            return this;
        }

        public Builder mainEntityAlias(String mainEntityAlias) {
            this.mainEntityAlias = mainEntityAlias;
            return this;
        }

        public ModuleDefinition build() {
            return new ModuleDefinition(moduleAlias, name, entities, relations, references, views, associationViews,
                    actions, mainEntityAlias);
        }
    }
}
