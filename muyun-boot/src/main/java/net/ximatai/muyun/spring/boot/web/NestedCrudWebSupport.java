package net.ximatai.muyun.spring.boot.web;

import jakarta.ws.rs.core.UriInfo;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.ability.query.QueryAbility;
import net.ximatai.muyun.spring.boot.web.query.WebQueryRequests;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.security.FieldOutputContext;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;

public abstract class NestedCrudWebSupport<T extends EntityContract, S extends CrudAbility<T>>
        extends WebSupport<S> implements SystemScope<S>, RecordLabelWeb<T> {
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

    protected abstract void appendScope(Criteria criteria, WebRequestScope scope);

    protected abstract void bindScope(T record, WebRequestScope scope);

    protected abstract boolean inScope(T record, WebRequestScope scope);

    protected Sort[] querySorts(WebQueryRequest request) {
        if (service() instanceof QueryAbility<?> queryAbility) {
            Sort[] sorts = queryAbility.querySorts(WebQueryRequests.from(request));
            return sorts == null ? new Sort[0] : sorts;
        }
        if (request != null && !request.sorts().isEmpty()) {
            throw new IllegalArgumentException("query sorts are not supported by " + webScopeName());
        }
        return new Sort[0];
    }

    @POST
    @Path("/query")
    @ActionEndpoint(PlatformAction.QUERY)
    public WebPageResponse<T> query(@Context UriInfo uriInfo,
                                    WebQueryRequest request) {
        return webScope(() -> {
            WebRequestScope scope = requestScope(uriInfo);
            Criteria criteria = queryCriteria(request);
            appendScope(criteria, scope);
            WebPageRequest page = request == null ? WebPageRequest.DEFAULT : request.pageOrDefault();
            PageResult<T> result = service().pageQuery(criteria,
                    PageRequest.of(page.pageNum(), page.pageSize()), querySorts(request));
            return WebPageResponse.from(WebOutputSupport.page(service(), result, FieldOutputContext.LIST));
        });
    }

    @GET
    @Path("/view/{id}")
    @ActionEndpoint(PlatformAction.VIEW)
    public T view(@Context UriInfo uriInfo, @PathParam("id") String id) {
        return webScope(() -> WebOutputSupport.record(service(), requireScopedRecord(requestScope(uriInfo), id),
                FieldOutputContext.VIEW));
    }

    @POST
    @Path("/insert")
    @ActionEndpoint(PlatformAction.CREATE)
    public WebRecordResponse<T> insert(@Context UriInfo uriInfo, T record) {
        return webScope(() -> {
            WebRequestScope scope = requestScope(uriInfo);
            bindScope(record, scope);
            String id = service().insert(record);
            T saved = WebOutputSupport.record(service(), service().select(id), FieldOutputContext.VIEW);
            return new WebRecordResponse<>(saved, successMessage(saved, "已保存"));
        });
    }

    @POST
    @Path("/update/{id}")
    @ActionEndpoint(PlatformAction.UPDATE)
    public WebRecordResponse<T> update(@Context UriInfo uriInfo, @PathParam("id") String id,
                                       T record) {
        return webScope(() -> {
            WebRequestScope scope = requestScope(uriInfo);
            requireScopedRecord(scope, id);
            record.setId(id);
            bindScope(record, scope);
            service().update(record);
            T saved = WebOutputSupport.record(service(), service().select(id), FieldOutputContext.VIEW);
            return new WebRecordResponse<>(saved, successMessage(saved, "已保存"));
        });
    }

    @POST
    @Path("/delete/{id}")
    @ActionEndpoint(PlatformAction.DELETE)
    public WebCountResponse delete(@Context UriInfo uriInfo, @PathParam("id") String id) {
        return webScope(() -> {
            T record = requireScopedRecord(requestScope(uriInfo), id);
            return new WebCountResponse(service().delete(id), successMessage(record, "已删除"));
        });
    }

    protected T requireScopedRecord(WebRequestScope scope, String id) {
        T record = service().select(id);
        if (record == null || !inScope(record, scope)) {
            throw new IllegalArgumentException(scopedRecordNotFoundMessage(scope, id));
        }
        return record;
    }

    protected String pathVariable(WebRequestScope scope, String key) {
        return scope == null ? null : scope.pathVariable(key);
    }

    protected String scopedRecordNotFoundMessage(WebRequestScope scope, String id) {
        return "nested record does not belong to request scope: " + id;
    }

    protected WebRequestScope requestScope(UriInfo uriInfo) {
        return WebRequestScope.from(uriInfo);
    }
}
