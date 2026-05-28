package net.ximatai.muyun.spring.ability.child;

import net.ximatai.muyun.spring.ability.AbilityException;
import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public interface ChildrenAbility<P extends EntityContract> extends CrudAbility<P> {
    default List<ChildRelation<? extends EntityContract, P>> childRelations() {
        return List.of();
    }

    @SuppressWarnings("unchecked")
    default <C extends EntityContract> ChildRelation<C, P> childRelation(ChildAbility<C> childAbility,
                                                                         BiConsumer<C, String> setParentId,
                                                                         Function<P, List<C>> extractChildren,
                                                                         BiConsumer<P, List<C>> populateChildren) {
        Class<?> modelClass = modelClass();
        if (modelClass == null) {
            throw new AbilityException("child relation requires modelClass: "
                    + getModuleAlias()
                    + ", extend AbstractAbilityService or use childRelation(Class, ...)");
        }
        return childRelation(
                (Class<P>) modelClass,
                childAbility,
                setParentId,
                extractChildren,
                populateChildren
        );
    }

    default <C extends EntityContract> ChildRelation<C, P> childRelation(Class<P> parentModelClass,
                                                                         ChildAbility<C> childAbility,
                                                                         BiConsumer<C, String> setParentId,
                                                                         Function<P, List<C>> extractChildren,
                                                                         BiConsumer<P, List<C>> populateChildren) {
        return childAbility.toChildRelation(
                StaticChildResolver.singlePlan(parentModelClass),
                setParentId,
                extractChildren,
                populateChildren
        );
    }

    default void afterChildrenInsert(String id, P parent) {
        for (ChildRelation<? extends EntityContract, P> relation : childRelations()) {
            relation.insertChildren(id, parent);
        }
    }

    default void afterChildrenUpdate(P parent, int updated) {
        if (updated <= 0) {
            return;
        }
        for (ChildRelation<? extends EntityContract, P> relation : childRelations()) {
            relation.replaceChildren(parent.getId(), parent);
        }
    }

    default void afterChildrenDelete(String id, P parent, int deleted) {
        if (deleted <= 0) {
            return;
        }
        for (ChildRelation<? extends EntityContract, P> relation : childRelations()) {
            if (relation.isAutoDeleteWithParent()) {
                relation.clearChildren(id);
            }
        }
    }

    default void afterChildrenSelect(P parent) {
        for (ChildRelation<? extends EntityContract, P> relation : childRelations()) {
            relation.loadChildren(parent);
        }
    }
}
