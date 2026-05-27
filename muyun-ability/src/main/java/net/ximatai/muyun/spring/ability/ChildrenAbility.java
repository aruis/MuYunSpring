package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.common.model.EntityContract;

import java.util.List;

public interface ChildrenAbility<P extends EntityContract> extends CrudAbility<P> {
    default List<ChildRelation<? extends EntityContract, P>> childRelations() {
        return List.of();
    }

    @Override
    default void afterPlatformInsert(String id, P parent) {
        for (ChildRelation<? extends EntityContract, P> relation : childRelations()) {
            relation.insertChildren(id, parent);
        }
    }

    @Override
    default void afterPlatformUpdate(P parent, int updated) {
        if (updated <= 0) {
            return;
        }
        for (ChildRelation<? extends EntityContract, P> relation : childRelations()) {
            relation.replaceChildren(parent.getId(), parent);
        }
    }

    @Override
    default void afterPlatformDelete(String id, P parent, int deleted) {
        if (deleted <= 0) {
            return;
        }
        for (ChildRelation<? extends EntityContract, P> relation : childRelations()) {
            if (relation.isAutoDeleteWithParent()) {
                relation.clearChildren(id);
            }
        }
    }

    @Override
    default void afterPlatformSelect(P parent) {
        for (ChildRelation<? extends EntityContract, P> relation : childRelations()) {
            relation.loadChildren(parent);
        }
    }
}
