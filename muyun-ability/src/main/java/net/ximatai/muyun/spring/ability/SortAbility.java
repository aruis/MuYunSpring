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
    int SORT_STEP = 100;

    default void prepareSortOrderForInsert(T entity) {
        if (entity.getSortOrder() == null) {
            entity.setSortOrder(nextSortOrder(entity));
        }
    }

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
        int order = SORT_STEP;
        for (T entity : entities) {
            entity.setSortOrder(order);
            order += SORT_STEP;
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

    default Criteria sortScopeByFields(T entity, String... fieldNames) {
        return BusinessScope.criteria(entity, fieldNames);
    }

    default void validateSortScopeByFields(T left, T right, String message, String... fieldNames) {
        BusinessScope.requireSame(left, right, message, fieldNames);
    }

    default boolean moveBetween(T moving, T previous, T next) {
        Integer order = sortOrderBetween(previous, next);
        if (order == null) {
            return false;
        }
        moving.setSortOrder(order);
        update(moving);
        return true;
    }

    private int nextSortOrder(T entity) {
        Criteria scope = sortScope(entity);
        int maxOrder = sortedList(scope).stream()
                .map(SortCapable::getSortOrder)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);
        if (maxOrder > Integer.MAX_VALUE - SORT_STEP) {
            List<String> orderedIds = sortedList(scope).stream()
                    .map(SortCapable::getId)
                    .toList();
            if (!orderedIds.isEmpty()) {
                reorder(orderedIds);
                maxOrder = sortedList(scope).stream()
                        .map(SortCapable::getSortOrder)
                        .filter(Objects::nonNull)
                        .max(Integer::compareTo)
                        .orElse(0);
            }
        }
        if (maxOrder > Integer.MAX_VALUE - SORT_STEP) {
            throw new PlatformException("Cannot allocate sort order; sort scope is too large");
        }
        return maxOrder + SORT_STEP;
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
        T previous = before ? previousRow(ids, targetIndex) : target;
        T next = before ? target : nextRow(ids, targetIndex);
        if (moveBetween(moving, previous, next)) {
            return;
        }
        ids.add(before ? targetIndex : targetIndex + 1, id);
        reorder(ids);
    }

    private T previousRow(List<String> ids, int targetIndex) {
        if (targetIndex <= 0) {
            return null;
        }
        return select(ids.get(targetIndex - 1));
    }

    private T nextRow(List<String> ids, int targetIndex) {
        if (targetIndex >= ids.size() - 1) {
            return null;
        }
        return select(ids.get(targetIndex + 1));
    }

    private Integer sortOrderBetween(T previous, T next) {
        Integer previousOrder = previous == null ? null : previous.getSortOrder();
        Integer nextOrder = next == null ? null : next.getSortOrder();
        if (previousOrder == null && nextOrder == null) {
            return SORT_STEP;
        }
        if (previousOrder == null) {
            return nextOrder > 1 ? nextOrder / 2 : null;
        }
        if (nextOrder == null) {
            if (previousOrder > Integer.MAX_VALUE - SORT_STEP) {
                return null;
            }
            return previousOrder + SORT_STEP;
        }
        int gap = nextOrder - previousOrder;
        return gap > 1 ? previousOrder + gap / 2 : null;
    }

    static boolean sameValue(Object left, Object right) {
        return Objects.equals(left, right);
    }
}
