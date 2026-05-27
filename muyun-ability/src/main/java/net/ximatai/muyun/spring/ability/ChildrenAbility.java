package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.common.model.EntityContract;

import java.util.List;

public interface ChildrenAbility<P extends EntityContract> extends CrudAbility<P> {
    default List<ChildRelation<? extends EntityContract, P>> childRelations() {
        return List.of();
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
