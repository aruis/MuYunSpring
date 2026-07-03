package net.ximatai.muyun.spring.boot.web;

import jakarta.ws.rs.core.UriInfo;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.spring.common.model.capability.EnabledCapable;
import net.ximatai.muyun.spring.common.model.capability.TreeCapable;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;

public abstract class NestedEnabledTreeCrudWebSupport<
        T extends EntityContract & EnabledCapable & TreeCapable,
        S extends CrudAbility<T> & EnableAbility<T> & TreeAbility<T>>
        extends NestedCrudWebSupport<T, S> {
    @POST
    @Path("/enable/{id}")
    @ActionEndpoint(PlatformAction.ENABLE)
    public WebCountResponse enable(@Context UriInfo uriInfo, @PathParam("id") String id) {
        return webScope(() -> {
            requireScopedRecord(requestScope(uriInfo), id);
            int count = service().enable(id);
            return new WebCountResponse(count, successMessage(service().select(id), "已启用"));
        });
    }

    @POST
    @Path("/disable/{id}")
    @ActionEndpoint(PlatformAction.DISABLE)
    public WebCountResponse disable(@Context UriInfo uriInfo, @PathParam("id") String id) {
        return webScope(() -> {
            requireScopedRecord(requestScope(uriInfo), id);
            int count = service().disable(id);
            return new WebCountResponse(count, successMessage(service().select(id), "已停用"));
        });
    }

    @POST
    @Path("/sort/{id}")
    @ActionEndpoint(PlatformAction.SORT)
    public WebCountResponse sort(@Context UriInfo uriInfo,
                                 @PathParam("id") String id,
                                 TreeSortWebRequest request) {
        return webScope(() -> {
            WebRequestScope scope = requestScope(uriInfo);
            TreeSortWebRequest normalized = request == null ? new TreeSortWebRequest(null, null, null) : request;
            requireSortInput(normalized);
            requireScopedRecord(scope, id);
            requireScopedNeighbor(scope, normalized.previousId());
            requireScopedNeighbor(scope, normalized.nextId());
            requireScopedParent(scope, normalized.parentId());
            Criteria scopeCriteria = treeScopeCriteria(scope);
            if (scopeCriteria == null || scopeCriteria.isEmpty()) {
                service().moveInTree(id, normalized.previousId(), normalized.nextId(), normalized.parentId());
            } else {
                service().moveInTree(scopeCriteria, id, normalized.previousId(), normalized.nextId(),
                        normalized.parentId());
            }
            return new WebCountResponse(1);
        });
    }

    protected Criteria treeScopeCriteria(WebRequestScope scope) {
        return Criteria.of();
    }

    private void requireScopedNeighbor(WebRequestScope scope, String id) {
        if (id != null && !id.isBlank()) {
            requireScopedRecord(scope, id);
        }
    }

    private void requireScopedParent(WebRequestScope scope, String parentId) {
        if (parentId != null && !parentId.isBlank() && !TreeAbility.ROOT_ID.equals(parentId)) {
            requireScopedRecord(scope, parentId);
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
