package net.ximatai.muyun.spring.dynamic.metadata;

import net.ximatai.muyun.spring.ability.child.ChildPlan;

public record EntityRelationDefinition(
        String code,
        String parentEntity,
        String childEntity,
        String childForeignKeyField,
        boolean autoPopulate,
        boolean autoDeleteWithParent
) {
    public static EntityRelationDefinition child(String code,
                                                 String parentEntity,
                                                 String childEntity,
                                                 String childForeignKeyField) {
        return new EntityRelationDefinition(code, parentEntity, childEntity, childForeignKeyField, false, false);
    }

    public EntityRelationDefinition withAutoPopulate() {
        return new EntityRelationDefinition(code, parentEntity, childEntity, childForeignKeyField, true, autoDeleteWithParent);
    }

    public EntityRelationDefinition withAutoDeleteWithParent() {
        return new EntityRelationDefinition(code, parentEntity, childEntity, childForeignKeyField, autoPopulate, true);
    }

    public ChildPlan plan() {
        return new ChildPlan(code, parentEntity, childEntity, childForeignKeyField, autoPopulate, autoDeleteWithParent);
    }
}
