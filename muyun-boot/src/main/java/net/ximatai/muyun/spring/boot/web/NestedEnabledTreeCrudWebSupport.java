package net.ximatai.muyun.spring.boot.web;

import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.spring.common.model.capability.EnabledCapable;
import net.ximatai.muyun.spring.common.model.capability.TreeCapable;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public abstract class NestedEnabledTreeCrudWebSupport<
        T extends EntityContract & EnabledCapable & TreeCapable,
        S extends CrudAbility<T> & EnableAbility<T> & TreeAbility<T>>
        extends NestedCrudWebSupport<T, S> {
    @PostMapping("/enable/{id}")
    @ActionEndpoint(PlatformAction.ENABLE)
    public WebCountResponse enable(HttpServletRequest servletRequest, @PathVariable String id) {
        return webScope(() -> {
            requireScopedRecord(servletRequest, id);
            return new WebCountResponse(service().enable(id));
        });
    }

    @PostMapping("/disable/{id}")
    @ActionEndpoint(PlatformAction.DISABLE)
    public WebCountResponse disable(HttpServletRequest servletRequest, @PathVariable String id) {
        return webScope(() -> {
            requireScopedRecord(servletRequest, id);
            return new WebCountResponse(service().disable(id));
        });
    }

    @PostMapping("/sort/{id}")
    @ActionEndpoint(PlatformAction.SORT)
    public WebCountResponse sort(HttpServletRequest servletRequest,
                                 @PathVariable String id,
                                 @RequestBody(required = false) TreeSortWebRequest request) {
        return webScope(() -> {
            TreeSortWebRequest normalized = request == null ? new TreeSortWebRequest(null, null, null) : request;
            requireSortInput(normalized);
            requireScopedRecord(servletRequest, id);
            requireScopedNeighbor(servletRequest, normalized.previousId());
            requireScopedNeighbor(servletRequest, normalized.nextId());
            requireScopedParent(servletRequest, normalized.parentId());
            Criteria scopeCriteria = treeScopeCriteria(servletRequest);
            if (scopeCriteria == null || scopeCriteria.isEmpty()) {
                service().moveInTree(id, normalized.previousId(), normalized.nextId(), normalized.parentId());
            } else {
                service().moveInTree(scopeCriteria, id, normalized.previousId(), normalized.nextId(),
                        normalized.parentId());
            }
            return new WebCountResponse(1);
        });
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

    private void requireSortInput(TreeSortWebRequest request) {
        if ((request.previousId() == null || request.previousId().isBlank())
                && (request.nextId() == null || request.nextId().isBlank())
                && (request.parentId() == null || request.parentId().isBlank())) {
            throw new IllegalArgumentException("tree sort requires previousId, nextId, or parentId");
        }
    }
}
