package net.ximatai.muyun.spring.dynamic.metadata;

import net.ximatai.muyun.spring.ability.child.ChildPlan;
import net.ximatai.muyun.spring.ability.reference.ReferenceTargetUnavailablePolicy;

import java.util.List;

public record EntityRelationDefinition(
        String code,
        String parentEntityAlias,
        String childEntityAlias,
        String childForeignKeyField,
        boolean autoPopulate
) {
    public static EntityRelationDefinition child(String code,
                                                 String parentEntityAlias,
                                                 String childEntityAlias,
                                                 String childForeignKeyField) {
        return new EntityRelationDefinition(code, parentEntityAlias, childEntityAlias, childForeignKeyField, false);
    }

    public EntityRelationDefinition withAutoPopulate() {
        return new EntityRelationDefinition(code, parentEntityAlias, childEntityAlias, childForeignKeyField, true);
    }

    public ChildPlan plan(String moduleAlias, List<EntityReferenceDefinition> references) {
        return new ChildPlan(code, parentEntityAlias, childEntityAlias, childForeignKeyField, autoPopulate,
                cascadeOnParentUnavailable(moduleAlias, references));
    }

    public boolean cascadeOnParentUnavailable(String moduleAlias, List<EntityReferenceDefinition> references) {
        return moduleAlias != null && references != null && references.stream().anyMatch(reference -> childEntityAlias.equals(reference.sourceEntityAlias())
                && childForeignKeyField.equals(reference.sourceField())
                && moduleAlias.equals(reference.target().moduleAlias())
                && parentEntityAlias.equals(reference.target().entityAlias())
                && reference.integrity().onTargetUnavailable() == ReferenceTargetUnavailablePolicy.CASCADE_DELETE);
    }
}
