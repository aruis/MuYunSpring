package net.ximatai.muyun.spring.boot.web;

import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.common.model.capability.EnabledCapable;
import net.ximatai.muyun.spring.common.model.capability.SortCapable;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;

public abstract class NestedEnabledSortableCrudWebSupport<
        T extends EntityContract & EnabledCapable & SortCapable,
        S extends CrudAbility<T> & EnableAbility<T> & SortAbility<T>>
        extends NestedCrudWebSupport<T, S> {
    @POST
    @Path("/enable/{id}")
    @ActionEndpoint(PlatformAction.ENABLE)
    public WebCountResponse enable(@Context HttpServletRequest servletRequest, @PathParam("id") String id) {
        return webScope(() -> {
            requireScopedRecord(servletRequest, id);
            int count = service().enable(id);
            return new WebCountResponse(count, successMessage(service().select(id), "已启用"));
        });
    }

    @POST
    @Path("/disable/{id}")
    @ActionEndpoint(PlatformAction.DISABLE)
    public WebCountResponse disable(@Context HttpServletRequest servletRequest, @PathParam("id") String id) {
        return webScope(() -> {
            requireScopedRecord(servletRequest, id);
            int count = service().disable(id);
            return new WebCountResponse(count, successMessage(service().select(id), "已停用"));
        });
    }

    @POST
    @Path("/sort/{id}")
    @ActionEndpoint(PlatformAction.SORT)
    public WebCountResponse sort(@Context HttpServletRequest servletRequest,
                                 @PathParam("id") String id,
                                 SortWebRequest request) {
        return webScope(() -> moveWithinScope(servletRequest, id, request, "sort requires previousId or nextId"));
    }

    protected WebCountResponse moveWithinScope(@Context HttpServletRequest servletRequest,
                                               String id,
                                               SortWebRequest request,
                                               String errorMessage) {
        SortWebRequest normalized = request == null ? new SortWebRequest(null, null) : request;
        requireScopedRecord(servletRequest, id);
        if (normalized.previousId() != null && !normalized.previousId().isBlank()) {
            requireScopedRecord(servletRequest, normalized.previousId());
            service().moveAfter(id, normalized.previousId());
            return new WebCountResponse(1);
        }
        if (normalized.nextId() != null && !normalized.nextId().isBlank()) {
            requireScopedRecord(servletRequest, normalized.nextId());
            service().moveBefore(id, normalized.nextId());
            return new WebCountResponse(1);
        }
        throw new IllegalArgumentException(errorMessage);
    }
}
