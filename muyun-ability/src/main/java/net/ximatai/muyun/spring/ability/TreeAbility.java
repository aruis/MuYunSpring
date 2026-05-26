package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.common.model.TreeCapable;
import net.ximatai.muyun.spring.common.schema.PlatformAbilityFields;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public interface TreeAbility<T extends TreeCapable> extends SortAbility<T> {
    String ROOT_ID = "root";

    default List<T> children(String parentId) {
        Criteria criteria = activeCriteria(Criteria.of().eq(PlatformAbilityFields.TREE_PARENT_FIELD, parentId));
        return getDao().query(criteria, new PageRequest(0, Integer.MAX_VALUE), Sort.asc(PlatformAbilityFields.SORT_FIELD));
    }

    @Override
    default Criteria sortScope(T entity) {
        return Criteria.of().eq(PlatformAbilityFields.TREE_PARENT_FIELD, entity.getParentId());
    }

    @Override
    default void validateSortScope(T left, T right) {
        if (!SortAbility.sameValue(left.getParentId(), right.getParentId())) {
            throw new AbilityException("Tree sort can only move records within the same parent");
        }
    }

    default List<String> ancestorIds(String id) {
        T current = select(id);
        if (current == null) {
            return List.of();
        }

        List<String> ancestors = new ArrayList<>();
        Set<String> visited = new LinkedHashSet<>();
        String parentId = current.getParentId();
        while (parentId != null && !parentId.isBlank() && !ROOT_ID.equals(parentId)) {
            if (!visited.add(parentId)) {
                throw new AbilityException("Tree cycle detected while resolving ancestors: " + id);
            }
            T parent = select(parentId);
            if (parent == null) {
                break;
            }
            ancestors.add(0, parent.getId());
            parentId = parent.getParentId();
        }
        return ancestors;
    }

    default List<String> ancestorIdsAndSelf(String id) {
        if (select(id) == null) {
            return List.of();
        }
        List<String> ids = new ArrayList<>(ancestorIds(id));
        ids.add(id);
        return ids;
    }

    default List<String> descendantIds(String id) {
        List<String> result = new ArrayList<>();
        collectDescendantIds(id, result, new LinkedHashSet<>());
        return result;
    }

    default void validateTreePlacement(T entity) {
        String id = entity.getId();
        String parentId = entity.getParentId();
        if (parentId == null || parentId.isBlank() || ROOT_ID.equals(parentId)) {
            return;
        }
        if (parentId.equals(id)) {
            throw new AbilityException("Tree node cannot use itself as parent: " + id);
        }
        if (select(parentId) == null) {
            throw new AbilityException("Tree node cannot use missing parent: " + parentId);
        }
        if (ancestorIds(parentId).contains(id)) {
            throw new AbilityException("Tree node cannot move under its descendant: " + id);
        }
    }

    private void collectDescendantIds(String parentId, List<String> result, Set<String> visited) {
        if (!visited.add(parentId)) {
            throw new AbilityException("Tree cycle detected while resolving descendants: " + parentId);
        }
        for (T child : children(parentId)) {
            if (visited.contains(child.getId())) {
                throw new AbilityException("Tree cycle detected while resolving descendants: " + parentId);
            }
            result.add(child.getId());
            collectDescendantIds(child.getId(), result, visited);
        }
        visited.remove(parentId);
    }
}
