package net.ximatai.muyun.spring.boot.web;

import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.spring.common.model.capability.EnabledCapable;
import net.ximatai.muyun.spring.common.model.capability.TreeCapable;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.common.platform.PlatformAction;

import java.util.List;

public abstract class NestedEnabledTreeCrudWebSupport<
        T extends EntityContract & EnabledCapable & TreeCapable,
        S extends CrudAbility<T> & EnableAbility<T> & TreeAbility<T>>
        extends NestedCrudWebSupport<T, S>
        implements RecordWebProjectionPolicy, TreeWebProjectionPolicy<T, S> {

    @Override
    public void requireRecord(HttpServletRequest request, PlatformAction action, String id) {
        requireScopedRecord(request, id);
    }

    @Override
    public T treeSelect(HttpServletRequest request, String id) {
        return requireScopedRecord(request, id);
    }

    @Override
    public List<T> treeChildren(HttpServletRequest request, String parentId) {
        Criteria criteria = treeScopeCriteria(request);
        return criteria == null || criteria.isEmpty()
                ? service().children(parentId)
                : service().children(criteria, parentId);
    }

    @Override
    public void requireTreeSortScope(HttpServletRequest request, String id, TreeSortWebRequest sortRequest) {
        requireScopedRecord(request, id);
        requireScopedNeighbor(request, sortRequest.previousId());
        requireScopedNeighbor(request, sortRequest.nextId());
        requireScopedParent(request, sortRequest.parentId());
    }

    @Override
    public void moveTree(HttpServletRequest request, String id, TreeSortWebRequest sortRequest) {
        Criteria criteria = treeScopeCriteria(request);
        if (criteria == null || criteria.isEmpty()) {
            service().moveInTree(id, sortRequest.previousId(), sortRequest.nextId(), sortRequest.parentId());
        } else {
            service().moveInTree(criteria, id,
                    sortRequest.previousId(), sortRequest.nextId(), sortRequest.parentId());
        }
    }

    protected Criteria treeScopeCriteria(HttpServletRequest request) {
        return Criteria.of();
    }

    private void requireScopedNeighbor(HttpServletRequest request, String id) {
        if (id != null && !id.isBlank()) {
            requireScopedRecord(request, id);
        }
    }

    private void requireScopedParent(HttpServletRequest request, String parentId) {
        if (parentId != null && !parentId.isBlank() && !TreeAbility.ROOT_ID.equals(parentId)) {
            requireScopedRecord(request, parentId);
        }
    }
}
