package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.common.model.EntityContract;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public interface ChildAbility<C extends EntityContract> extends CrudAbility<C> {
    default <P extends EntityContract> ChildRelation<C, P> toChildRelation(BiConsumer<C, String> setParentId,
                                                                           String childForeignKeyField,
                                                                           Function<P, List<C>> extractChildren) {
        return new ChildRelation<>(this, setParentId, childForeignKeyField, extractChildren);
    }

    default <P extends EntityContract> ChildRelation<C, P> toChildRelation(ChildPlan plan,
                                                                           BiConsumer<C, String> setParentId,
                                                                           Function<P, List<C>> extractChildren,
                                                                           BiConsumer<P, List<C>> populateChildren) {
        ChildRelation<C, P> relation = toChildRelation(setParentId, plan.childForeignKeyField(), extractChildren);
        if (plan.autoPopulate()) {
            if (populateChildren == null) {
                throw new AbilityException("auto populate child relation requires populateChildren: " + plan.relationCode());
            }
            relation.autoPopulate(populateChildren);
        }
        if (plan.autoDeleteWithParent()) {
            relation.autoDeleteWithParent();
        }
        return relation;
    }

    default C selectIgnoreSoftDeleteIfPossible(String id) {
        if (this instanceof SoftDeleteAbility<?> softDeleteAbility) {
            @SuppressWarnings("unchecked")
            C selected = (C) softDeleteAbility.selectIgnoreSoftDelete(id);
            return selected;
        }
        return select(id);
    }
}
