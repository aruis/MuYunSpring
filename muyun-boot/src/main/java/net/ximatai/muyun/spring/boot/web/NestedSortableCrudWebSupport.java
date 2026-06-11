package net.ximatai.muyun.spring.boot.web;

import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.common.model.capability.SortCapable;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public abstract class NestedSortableCrudWebSupport<
        T extends EntityContract & SortCapable,
        S extends CrudAbility<T> & SortAbility<T>>
        extends NestedCrudWebSupport<T, S> {
    @PostMapping("/sort/{id}")
    @ActionEndpoint(PlatformAction.SORT)
    public WebCountResponse sort(HttpServletRequest servletRequest,
                                 @PathVariable String id,
                                 @RequestBody(required = false) SortWebRequest request) {
        return webScope(() -> moveWithinScope(servletRequest, id, request, "sort requires previousId or nextId"));
    }

    protected WebCountResponse moveWithinScope(HttpServletRequest servletRequest,
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
