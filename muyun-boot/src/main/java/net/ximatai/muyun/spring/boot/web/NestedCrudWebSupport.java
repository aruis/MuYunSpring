package net.ximatai.muyun.spring.boot.web;

import jakarta.servlet.http.HttpServletRequest;
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
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import java.util.Map;

public abstract class NestedCrudWebSupport<T extends EntityContract, S extends CrudAbility<T>>
        extends WebSupport<S> implements SystemScope<S>, RecordLabelWeb<T> {
    public static final String PATH_VARIABLES_ATTRIBUTE = NestedCrudWebSupport.class.getName() + ".PATH_VARIABLES";

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

    protected abstract void appendScope(Criteria criteria, @Context HttpServletRequest request);

    protected abstract void bindScope(T record, @Context HttpServletRequest request);

    protected abstract boolean inScope(T record, @Context HttpServletRequest request);

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
    public WebPageResponse<T> query(@Context HttpServletRequest servletRequest,
                                    WebQueryRequest request) {
        return webScope(() -> {
            Criteria criteria = queryCriteria(request);
            appendScope(criteria, servletRequest);
            WebPageRequest page = request == null ? WebPageRequest.DEFAULT : request.pageOrDefault();
            PageResult<T> result = service().pageQuery(criteria,
                    PageRequest.of(page.pageNum(), page.pageSize()), querySorts(request));
            return WebPageResponse.from(WebOutputSupport.page(service(), result, FieldOutputContext.LIST));
        });
    }

    @GET
    @Path("/view/{id}")
    @ActionEndpoint(PlatformAction.VIEW)
    public T view(@Context HttpServletRequest servletRequest, @PathParam("id") String id) {
        return webScope(() -> WebOutputSupport.record(service(), requireScopedRecord(servletRequest, id),
                FieldOutputContext.VIEW));
    }

    @POST
    @Path("/insert")
    @ActionEndpoint(PlatformAction.CREATE)
    public WebRecordResponse<T> insert(@Context HttpServletRequest servletRequest, T record) {
        return webScope(() -> {
            bindScope(record, servletRequest);
            String id = service().insert(record);
            T saved = WebOutputSupport.record(service(), service().select(id), FieldOutputContext.VIEW);
            return new WebRecordResponse<>(saved, successMessage(saved, "已保存"));
        });
    }

    @POST
    @Path("/update/{id}")
    @ActionEndpoint(PlatformAction.UPDATE)
    public WebRecordResponse<T> update(@Context HttpServletRequest servletRequest, @PathParam("id") String id,
                                       T record) {
        return webScope(() -> {
            requireScopedRecord(servletRequest, id);
            record.setId(id);
            bindScope(record, servletRequest);
            service().update(record);
            T saved = WebOutputSupport.record(service(), service().select(id), FieldOutputContext.VIEW);
            return new WebRecordResponse<>(saved, successMessage(saved, "已保存"));
        });
    }

    @POST
    @Path("/delete/{id}")
    @ActionEndpoint(PlatformAction.DELETE)
    public WebCountResponse delete(@Context HttpServletRequest servletRequest, @PathParam("id") String id) {
        return webScope(() -> {
            T record = requireScopedRecord(servletRequest, id);
            return new WebCountResponse(service().delete(id), successMessage(record, "已删除"));
        });
    }

    protected T requireScopedRecord(@Context HttpServletRequest request, String id) {
        T record = service().select(id);
        if (record == null || !inScope(record, request)) {
            throw new IllegalArgumentException(scopedRecordNotFoundMessage(request, id));
        }
        return record;
    }

    protected String pathVariable(@Context HttpServletRequest request, String key) {
        Object value = pathVariables(request).get(key);
        return value == null ? null : value.toString();
    }

    protected String scopedRecordNotFoundMessage(@Context HttpServletRequest request, String id) {
        return "nested record does not belong to request scope: " + id;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> pathVariables(@Context HttpServletRequest request) {
        if (request == null) {
            return Map.of();
        }
        Object value = request.getAttribute(PATH_VARIABLES_ATTRIBUTE);
        if (value instanceof Map<?, ?> map) {
            return (Map<String, String>) map;
        }
        return Map.of();
    }
}
