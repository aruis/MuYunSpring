package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.common.model.capability.TreeCapable;
import net.ximatai.muyun.spring.common.schema.PlatformAbilityFields;
import net.ximatai.muyun.spring.common.schema.StandardEntitySchema;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public interface TreeAbility<T extends TreeCapable> extends SortAbility<T> {
    String ROOT_ID = "root";

    default List<T> children(String parentId) {
        if (parentId == null || parentId.isBlank()) {
            return List.of();
        }
        if (!ROOT_ID.equals(parentId) && selectActiveRaw(parentId) == null) {
            return List.of();
        }
        Criteria criteria = activeCriteria(Criteria.of().eq(PlatformAbilityFields.TREE_PARENT_FIELD, parentId));
        return getDao().query(criteria, new PageRequest(0, Integer.MAX_VALUE), Sort.asc(PlatformAbilityFields.SORT_FIELD));
    }

    /**
     * Resolves children for trees partitioned by a business scope, such as an application,
     * menu scheme, or dictionary category. The scope criteria is intentionally explicit so
     * services keep their business-shaped root methods while sharing tree mechanics.
     */
    default List<T> children(Criteria scopeCriteria, String parentId) {
        if (parentId == null || parentId.isBlank()) {
            return List.of();
        }
        if (!ROOT_ID.equals(parentId) && selectInScope(scopeCriteria, parentId) == null) {
            return List.of();
        }
        Criteria criteria = scopedTreeCriteria(scopeCriteria, parentId);
        return getDao().query(activeCriteria(criteria), new PageRequest(0, Integer.MAX_VALUE), Sort.asc(PlatformAbilityFields.SORT_FIELD));
    }

    /**
     * Builds the common sort/query scope for a tree node inside a business scope.
     */
    default Criteria scopedTreeCriteria(Criteria scopeCriteria, String parentId) {
        Criteria criteria = Criteria.of().eq(PlatformAbilityFields.TREE_PARENT_FIELD, parentId);
        if (scopeCriteria != null && !scopeCriteria.isEmpty()) {
            criteria.andGroup(scopeCriteria.getRoot());
        }
        return criteria;
    }

    /**
     * Use from services that require a business scope to resolve root nodes.
     */
    default void rejectRootChildrenLookup(String scopedLookupName) {
        throw new PlatformException("Use " + scopedLookupName + " to resolve scoped root tree nodes");
    }

    @Override
    default Criteria sortScope(T entity) {
        return Criteria.of().eq(PlatformAbilityFields.TREE_PARENT_FIELD, entity.getParentId());
    }

    @Override
    default void validateSortScope(T left, T right) {
        if (!SortAbility.sameValue(left.getParentId(), right.getParentId())) {
            throw new PlatformException("Tree sort can only move records within the same parent");
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
                throw new PlatformException("Tree cycle detected while resolving ancestors: " + id);
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
            throw new PlatformException("Tree node cannot use itself as parent: " + id);
        }
        if (select(parentId) == null) {
            throw new PlatformException("Tree node cannot use missing parent: " + parentId);
        }
        if (ancestorIds(parentId).contains(id)) {
            throw new PlatformException("Tree node cannot move under its descendant: " + id);
        }
    }

    /**
     * Validates normal tree placement and additionally requires the parent to be
     * visible in the same business scope as the incoming node.
     */
    default void validateTreePlacementInScope(T entity, Criteria scopeCriteria, String message) {
        String parentId = entity.getParentId();
        if (parentId == null || parentId.isBlank() || ROOT_ID.equals(parentId)) {
            return;
        }
        validateTreePlacement(entity);
        if (selectInScope(scopeCriteria, parentId) == null) {
            throw new PlatformException(message);
        }
    }

    private T selectInScope(Criteria scopeCriteria, String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        Criteria criteria = Criteria.of().eq(StandardEntitySchema.ID_FIELD, id);
        if (scopeCriteria != null && !scopeCriteria.isEmpty()) {
            criteria.andGroup(scopeCriteria.getRoot());
        }
        return getDao().query(activeCriteria(criteria), new PageRequest(0, 1))
                .stream()
                .findFirst()
                .orElse(null);
    }

    private void collectDescendantIds(String parentId, List<String> result, Set<String> visited) {
        if (!visited.add(parentId)) {
            throw new PlatformException("Tree cycle detected while resolving descendants: " + parentId);
        }
        for (T child : children(parentId)) {
            if (visited.contains(child.getId())) {
                throw new PlatformException("Tree cycle detected while resolving descendants: " + parentId);
            }
            result.add(child.getId());
            collectDescendantIds(child.getId(), result, visited);
        }
        visited.remove(parentId);
    }
}
