package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.common.model.capability.SortCapable;
import net.ximatai.muyun.spring.common.schema.PlatformAbilityFields;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public interface SortAbility<T extends SortCapable> extends CrudAbility<T> {
    default void reorder(List<String> orderedIds) {
        if (orderedIds == null || orderedIds.isEmpty()) {
            throw new PlatformException("Cannot reorder empty records");
        }
        Set<String> uniqueIds = new LinkedHashSet<>(orderedIds);
        if (uniqueIds.size() != orderedIds.size()) {
            throw new PlatformException("Cannot reorder duplicate records");
        }
        T first = select(orderedIds.getFirst());
        if (first == null) {
            throw new PlatformException("Cannot reorder missing record: " + orderedIds.getFirst());
        }
        List<T> entities = new ArrayList<>();
        for (String id : orderedIds) {
            T entity = select(id);
            if (entity == null) {
                throw new PlatformException("Cannot reorder missing record: " + id);
            }
            validateSortScope(first, entity);
            entities.add(entity);
        }
        List<String> scopedIds = sortedList(sortScope(first)).stream()
                .map(SortCapable::getId)
                .toList();
        if (!new LinkedHashSet<>(scopedIds).equals(uniqueIds)) {
            throw new PlatformException("Cannot reorder partial records; orderedIds must cover complete scope");
        }
        int order = 1;
        for (T entity : entities) {
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
        return getDao().query(activeCriteria(criteria), new PageRequest(0, Integer.MAX_VALUE), Sort.asc(PlatformAbilityFields.SORT_FIELD));
    }

    default Criteria sortScope(T entity) {
        return Criteria.of();
    }

    default void validateSortScope(T left, T right) {
    }

    private void moveRelative(String id, String targetId, boolean before) {
        T moving = select(id);
        T target = select(targetId);
        if (moving == null || target == null) {
            throw new PlatformException("Cannot move missing record");
        }
        validateSortScope(moving, target);
        Criteria scope = sortScope(moving);
        List<T> rows = sortedList(scope);
        List<String> ids = new ArrayList<>();
        for (T row : rows) {
            if (!row.getId().equals(id)) {
                ids.add(row.getId());
            }
        }
        int targetIndex = ids.indexOf(targetId);
        if (targetIndex < 0) {
            throw new PlatformException("Cannot move before/after missing target: " + targetId);
        }
        ids.add(before ? targetIndex : targetIndex + 1, id);
        reorder(ids);
    }

    static boolean sameValue(Object left, Object right) {
        return Objects.equals(left, right);
    }
}
