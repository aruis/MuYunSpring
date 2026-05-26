package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.common.model.SortModel;
import net.ximatai.muyun.spring.common.model.TreeModel;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public interface SortAbility<T extends SortModel> extends CrudAbility<T> {
    default String getSortField() {
        return "sortOrder";
    }

    default void reorder(List<String> orderedIds) {
        Set<String> uniqueIds = new LinkedHashSet<>(orderedIds);
        if (uniqueIds.size() != orderedIds.size()) {
            throw new AbilityException("Cannot reorder duplicate records");
        }
        int order = 1;
        for (String id : orderedIds) {
            T entity = select(id);
            if (entity == null) {
                throw new AbilityException("Cannot reorder missing record: " + id);
            }
            entity.setSortOrder(order++);
            update(entity);
        }
    }

    default void moveBefore(String id, String beforeId) {
        moveRelative(id, beforeId, true);
    }

    default void moveAfter(String id, String afterId) {
        moveRelative(id, afterId, false);
    }

    default List<T> sortedList(Criteria criteria) {
        return getDao().query(activeCriteria(criteria), new PageRequest(0, Integer.MAX_VALUE), Sort.asc(getSortField()));
    }

    private void moveRelative(String id, String targetId, boolean before) {
        T moving = select(id);
        T target = select(targetId);
        if (moving == null || target == null) {
            throw new AbilityException("Cannot move missing record");
        }
        Criteria scope = Criteria.of();
        if (moving instanceof TreeModel movingTree && target instanceof TreeModel targetTree) {
            if (!equalsNullable(movingTree.getParentId(), targetTree.getParentId())) {
                throw new AbilityException("Tree sort can only move records within the same parent");
            }
            scope.eq("parentId", movingTree.getParentId());
        }
        List<T> rows = sortedList(scope);
        List<String> ids = new ArrayList<>();
        for (T row : rows) {
            if (!row.getId().equals(id)) {
                ids.add(row.getId());
            }
        }
        int targetIndex = ids.indexOf(targetId);
        if (targetIndex < 0) {
            throw new AbilityException("Cannot move before/after missing target: " + targetId);
        }
        ids.add(before ? targetIndex : targetIndex + 1, id);
        reorder(ids);
    }

    private boolean equalsNullable(Object left, Object right) {
        return left == null ? right == null : left.equals(right);
    }
}
