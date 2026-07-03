package net.ximatai.muyun.spring.boot.platform;

import jakarta.ws.rs.core.UriInfo;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.ability.query.QueryAbility;
import net.ximatai.muyun.spring.boot.web.SortWebRequest;
import net.ximatai.muyun.spring.boot.web.SystemScope;
import net.ximatai.muyun.spring.boot.web.WebCountResponse;
import net.ximatai.muyun.spring.boot.web.WebOutputSupport;
import net.ximatai.muyun.spring.boot.web.WebPageRequest;
import net.ximatai.muyun.spring.boot.web.WebPageResponse;
import net.ximatai.muyun.spring.boot.web.WebQueryRequest;
import net.ximatai.muyun.spring.boot.web.WebRequestScope;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import net.ximatai.muyun.spring.boot.web.query.WebQueryRequests;
import net.ximatai.muyun.spring.common.model.capability.EnabledCapable;
import net.ximatai.muyun.spring.common.model.capability.SortCapable;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.security.FieldOutputContext;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;

import java.util.Objects;

abstract class ModuleScopedRuleTreeWebSupport<
        T extends EntityContract & EnabledCapable & SortCapable,
        S extends CrudAbility<T> & EnableAbility<T> & SortAbility<T>>
        extends WebSupport<S> implements SystemScope<S> {
    private final String scopeField;

    protected ModuleScopedRuleTreeWebSupport(String scopeField) {
        this.scopeField = Objects.requireNonNull(scopeField, "scopeField must not be null");
    }

    @POST
    @Path("/query")
    @ActionEndpoint(PlatformAction.QUERY)
    public WebPageResponse<T> query(@Context UriInfo uriInfo,
                                    WebQueryRequest request) {
        return webScope(() -> {
            WebRequestScope scope = requestScope(uriInfo);
            Criteria criteria = queryCriteria(request);
            criteria.eq(scopeField, moduleAlias(scope));
            WebPageRequest page = request == null ? WebPageRequest.DEFAULT : request.pageOrDefault();
            PageResult<T> result = service().pageQuery(
                    criteria,
                    PageRequest.of(page.pageNum(), page.pageSize()),
                    querySorts(request));
            return WebPageResponse.from(WebOutputSupport.page(service(), result, FieldOutputContext.LIST));
        });
    }

    protected Criteria queryCriteria(WebQueryRequest request) {
        if (service() instanceof QueryAbility<?> queryAbility) {
            Criteria criteria = queryAbility.queryCriteria(WebQueryRequests.from(request));
            return criteria == null ? Criteria.of() : criteria;
        }
        if (request != null && !request.conditions().isEmpty()) {
            throw new IllegalArgumentException("query conditions are not supported by " + webScopeName());
        }
        if (request != null && request.criteria() != null && !request.criteria().isEmpty()) {
            throw new IllegalArgumentException("query criteria are not supported by " + webScopeName());
        }
        return Criteria.of();
    }

    protected Sort[] querySorts(WebQueryRequest request) {
        if (service() instanceof QueryAbility<?> queryAbility) {
            Sort[] sorts = queryAbility.querySorts(WebQueryRequests.from(request));
            return sorts == null ? new Sort[]{Sort.asc("sortOrder")} : sorts;
        }
        if (request != null && !request.sorts().isEmpty()) {
            throw new IllegalArgumentException("query sorts are not supported by " + webScopeName());
        }
        return new Sort[]{Sort.asc("sortOrder")};
    }

    @POST
    @Path("/delete/{id}")
    @ActionEndpoint(PlatformAction.DELETE)
    public WebCountResponse delete(@Context UriInfo uriInfo, @PathParam("id") String id) {
        return webScope(() -> {
            requireScopedRecord(requestScope(uriInfo), id);
            return new WebCountResponse(service().delete(id));
        });
    }

    @POST
    @Path("/enable/{id}")
    @ActionEndpoint(PlatformAction.ENABLE)
    public WebCountResponse enable(@Context UriInfo uriInfo, @PathParam("id") String id) {
        return webScope(() -> {
            requireScopedRecord(requestScope(uriInfo), id);
            return new WebCountResponse(service().enable(id));
        });
    }

    @POST
    @Path("/disable/{id}")
    @ActionEndpoint(PlatformAction.DISABLE)
    public WebCountResponse disable(@Context UriInfo uriInfo, @PathParam("id") String id) {
        return webScope(() -> {
            requireScopedRecord(requestScope(uriInfo), id);
            return new WebCountResponse(service().disable(id));
        });
    }

    @POST
    @Path("/sort/{id}")
    @ActionEndpoint(PlatformAction.SORT)
    public WebCountResponse sort(@Context UriInfo uriInfo,
                                 @PathParam("id") String id,
                                 SortWebRequest request) {
        return webScope(() -> {
            WebRequestScope scope = requestScope(uriInfo);
            SortWebRequest normalized = request == null ? new SortWebRequest(null, null) : request;
            requireScopedRecord(scope, id);
            if (hasText(normalized.previousId())) {
                requireScopedRecord(scope, normalized.previousId());
                service().moveAfter(id, normalized.previousId());
                return new WebCountResponse(1);
            }
            if (hasText(normalized.nextId())) {
                requireScopedRecord(scope, normalized.nextId());
                service().moveBefore(id, normalized.nextId());
                return new WebCountResponse(1);
            }
            throw new IllegalArgumentException("rule sort requires previousId or nextId");
        });
    }

    protected void requireExistingRuleInScope(WebRequestScope scope, T rule) {
        if (rule == null || !hasText(rule.getId())) {
            return;
        }
        requireScopedRecord(scope, rule.getId());
    }

    protected T requireScopedRecord(WebRequestScope scope, String id) {
        T record = service().select(id);
        String moduleAlias = moduleAlias(scope);
        if (record == null || !moduleAlias.equals(scopeValue(record))) {
            throw new IllegalArgumentException("rule does not belong to module: " + moduleAlias + "." + id);
        }
        return record;
    }

    protected String moduleAlias(WebRequestScope scope) {
        return PlatformNameRules.requireModuleAlias(pathVariable(scope, "moduleAlias"));
    }

    protected abstract String scopeValue(T record);

    protected WebRequestScope requestScope(UriInfo uriInfo) {
        return WebRequestScope.from(uriInfo);
    }

    private String pathVariable(WebRequestScope scope, String key) {
        return scope == null ? null : scope.pathVariable(key);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
