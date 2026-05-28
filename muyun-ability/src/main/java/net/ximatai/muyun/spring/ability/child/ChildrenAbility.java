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
        return childRelation(
                (Class<P>) requireModelClass("childRelation(Class, ...)"),
                childAbility,
                setParentId,
                extractChildren,
                populateChildren
        );
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default <C extends EntityContract> ChildRelation<C, P> childRelation(ChildAbility<C> childAbility) {
        StaticChildResolver.ChildRule rule = StaticChildResolver.singleRule(
                requireModelClass("childRelation(Class, ...)")
        );
        validateShortcutChildModel(rule, childAbility);
        return childAbility.toChildRelation(
                rule.plan(),
                rule::setParentId,
                rule::children,
                rule::populate
        );
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    default <C extends EntityContract> ChildRelation<C, P> childRelation(String relationCode,
                                                                         ChildAbility<C> childAbility) {
        StaticChildResolver.ChildRule rule = StaticChildResolver.rule(
                requireModelClass("childRelation(Class, String, ...)"),
                relationCode
        );
        validateShortcutChildModel(rule, childAbility);
        return childAbility.toChildRelation(
                rule.plan(),
                rule::setParentId,
                rule::children,
                rule::populate
        );
    }

    @SuppressWarnings("unchecked")
    default <C extends EntityContract> ChildRelation<C, P> childRelation(String relationCode,
                                                                         ChildAbility<C> childAbility,
                                                                         BiConsumer<C, String> setParentId,
                                                                         Function<P, List<C>> extractChildren,
                                                                         BiConsumer<P, List<C>> populateChildren) {
        return childRelation(
                (Class<P>) requireModelClass("childRelation(Class, String, ...)"),
                relationCode,
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
        StaticChildResolver.ChildRule rule = StaticChildResolver.singleRule(parentModelClass);
        validateChildModel(rule, childAbility);
        return childAbility.toChildRelation(
                rule.plan(),
                setParentId,
                extractChildren,
                populateChildren
        );
    }

    default <C extends EntityContract> ChildRelation<C, P> childRelation(Class<P> parentModelClass,
                                                                         String relationCode,
                                                                         ChildAbility<C> childAbility,
                                                                         BiConsumer<C, String> setParentId,
                                                                         Function<P, List<C>> extractChildren,
                                                                         BiConsumer<P, List<C>> populateChildren) {
        StaticChildResolver.ChildRule rule = StaticChildResolver.rule(parentModelClass, relationCode);
        validateChildModel(rule, childAbility);
        return childAbility.toChildRelation(
                rule.plan(),
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

    private Class<?> requireModelClass(String explicitFallback) {
        Class<?> modelClass = modelClass();
        if (modelClass == null) {
            throw new AbilityException("child relation requires modelClass: "
                    + getModuleAlias()
                    + ", extend AbstractAbilityService or use " + explicitFallback);
        }
        return modelClass;
    }

    private void validateChildModel(StaticChildResolver.ChildRule rule, ChildAbility<?> childAbility) {
        Class<?> actualChildModel = childAbility.modelClass();
        if (actualChildModel == null || rule.childModel().equals(actualChildModel)) {
            return;
        }
        throw new AbilityException("child relation model mismatch: "
                + rule.plan().relationCode()
                + ", expected " + rule.childModel().getName()
                + ", actual " + actualChildModel.getName());
    }

    private void validateShortcutChildModel(StaticChildResolver.ChildRule rule, ChildAbility<?> childAbility) {
        Class<?> actualChildModel = childAbility.modelClass();
        if (rule.childModel().equals(actualChildModel)) {
            return;
        }
        String actual = actualChildModel == null ? "null" : actualChildModel.getName();
        throw new AbilityException("child relation model mismatch: "
                + rule.plan().relationCode()
                + ", expected " + rule.childModel().getName()
                + ", actual " + actual);
    }
}
