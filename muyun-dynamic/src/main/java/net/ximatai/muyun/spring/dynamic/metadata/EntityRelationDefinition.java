package net.ximatai.muyun.spring.dynamic.metadata;

import net.ximatai.muyun.spring.ability.child.ChildPlan;

public record EntityRelationDefinition(
        String code,
        String parentEntityAlias,
        String childEntityAlias,
        String childForeignKeyField,
        boolean autoPopulate,
        boolean autoDeleteWithParent
) {
    public static EntityRelationDefinition child(String code,
                                                 String parentEntityAlias,
                                                 String childEntityAlias,
                                                 String childForeignKeyField) {
        return new EntityRelationDefinition(code, parentEntityAlias, childEntityAlias, childForeignKeyField, false, false);
    }

    public EntityRelationDefinition withAutoPopulate() {
        return new EntityRelationDefinition(code, parentEntityAlias, childEntityAlias, childForeignKeyField, true, autoDeleteWithParent);
    }

    public EntityRelationDefinition withAutoDeleteWithParent() {
        return new EntityRelationDefinition(code, parentEntityAlias, childEntityAlias, childForeignKeyField, autoPopulate, true);
    }

    public ChildPlan plan() {
        return new ChildPlan(code, parentEntityAlias, childEntityAlias, childForeignKeyField, autoPopulate, autoDeleteWithParent);
    }
}
