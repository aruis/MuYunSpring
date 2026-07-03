package net.ximatai.muyun.spring.boot.web;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.ability.DataScopeAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.ability.form.FormAbility;
import net.ximatai.muyun.spring.ability.form.FormSchema;
import net.ximatai.muyun.spring.ability.query.QueryAbility;
import net.ximatai.muyun.spring.ability.query.QuerySchema;
import net.ximatai.muyun.spring.boot.platform.ModuleUiFormSchemaAdapter;
import net.ximatai.muyun.spring.boot.platform.StaticRecordReadProjectionService;
import net.ximatai.muyun.spring.boot.platform.StaticModuleUiContributor;
import net.ximatai.muyun.spring.boot.web.query.WebQueryRequests;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContext;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContextHolder;
import net.ximatai.muyun.spring.common.platform.ActionExecutionPolicy;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.schema.PlatformAbilityFields;
import net.ximatai.muyun.spring.common.security.FieldOutputContext;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.QueryParam;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

public interface CrudWeb<T extends EntityContract, S extends CrudAbility<T>>
        extends ScopedWeb<S>, RecordLabelWeb<T> {
    default PageResult<T> queryRecords(WebQueryRequest request) {
        WebPageRequest page = request == null ? WebPageRequest.DEFAULT : request.pageOrDefault();
        PageRequest pageRequest = PageRequest.of(page.pageNum(), page.pageSize());
        if (service() instanceof DataScopeAbility<?>) {
            DataScopeAbility<?> dataScopeAbility = DataScopeAbility.cast(service());
            @SuppressWarnings("unchecked")
            PageResult<T> result = (PageResult<T>) dataScopeAbility.pageQueryForAction(
                    PlatformAction.QUERY, queryCriteria(request), pageRequest, querySorts(request));
            return result;
        }
        return service().pageQuery(queryCriteria(request), pageRequest, querySorts(request));
    }

    default StaticRecordReadProjectionService staticRecordReadProjectionService() {
        return null;
    }

    default List<T> queryListRecords(WebQueryRequest request) {
        requireUnpagedQuerySupported(request);
        if (service() instanceof DataScopeAbility<?>) {
            DataScopeAbility<?> dataScopeAbility = DataScopeAbility.cast(service());
            @SuppressWarnings("unchecked")
            List<T> result = (List<T>) dataScopeAbility.listForAction(
                    PlatformAction.QUERY, queryCriteria(request), querySorts(request));
            return result;
        }
        return service().list(queryCriteria(request), querySorts(request));
    }

    default boolean supportsUnpagedQuery() {
        return false;
    }

    default void requireUnpagedQuerySupported(WebQueryRequest request) {
        if (!supportsUnpagedQuery()) {
            throw new IllegalArgumentException("unpaged query is not supported by " + webScopeName());
        }
        if (request != null && request.page() != null) {
            throw new IllegalArgumentException("unpaged query cannot specify page");
        }
        if (request != null && request.navigationSessionEnabled()) {
            throw new IllegalArgumentException("unpaged query navigation is not supported by " + webScopeName());
        }
    }

    default Criteria queryCriteria(WebQueryRequest request) {
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

    default Sort[] querySorts(WebQueryRequest request) {
        if (service() instanceof QueryAbility<?> queryAbility) {
            Sort[] sorts = queryAbility.querySorts(WebQueryRequests.from(request));
            return sorts == null ? new Sort[0] : sorts;
        }
        if (request != null && !request.sorts().isEmpty()) {
            throw new IllegalArgumentException("query sorts are not supported by " + webScopeName());
        }
        if (service() instanceof SortAbility<?>) {
            return new Sort[]{Sort.asc(PlatformAbilityFields.SORT_FIELD)};
        }
        return new Sort[0];
    }

    @GET
    @Path("/query/schema")
    @ActionEndpoint(PlatformAction.QUERY)
    default QuerySchema querySchema(@QueryParam("uiConfigId") String uiConfigId) {
        return webScope(() -> {
            if (service() instanceof QueryAbility<?> queryAbility) {
                return queryAbility.querySchema();
            }
            throw new IllegalArgumentException("query schema is not supported by " + webScopeName());
        });
    }

    @GET
    @Path("/form/schema")
    @ActionEndpoint(PlatformAction.VIEW)
    default FormSchema formSchema(@QueryParam("uiConfigId") String uiConfigId) {
        return webScope(() -> {
            if (this instanceof StaticModuleUiContributor contributor) {
                if (isCurrentModuleUiDefinition(contributor)) {
                    FormSchema schema = ModuleUiFormSchemaAdapter.formSchema(contributor.moduleUiDefinition(),
                            formSchemaModelClass());
                    if (schema != null) {
                        return schema;
                    }
                }
            }
            if (service() instanceof FormAbility<?> formAbility) {
                return formAbility.formSchema();
            }
            throw new IllegalArgumentException("form schema is not supported by " + webScopeName());
        });
    }

    private boolean isCurrentModuleUiDefinition(StaticModuleUiContributor contributor) {
        return contributor.moduleUiDefinition() != null
                && webScopeName().equals(contributor.moduleUiDefinition().moduleAlias());
    }

    private Class<?> formSchemaModelClass() {
        Class<?> modelClass = service().modelClass();
        if (modelClass != null) {
            return modelClass;
        }
        return resolveCrudWebModelClass(getClass());
    }

    private Class<?> resolveCrudWebModelClass(Type type) {
        if (type instanceof ParameterizedType parameterizedType) {
            Type rawType = parameterizedType.getRawType();
            if (rawType instanceof Class<?> rawClass && CrudWeb.class.equals(rawClass)) {
                return classOf(parameterizedType.getActualTypeArguments()[0]);
            }
            if (rawType instanceof Class<?> rawClass) {
                return resolveCrudWebModelClass(rawClass);
            }
        }
        if (type instanceof Class<?> typeClass) {
            for (Type candidate : typeClass.getGenericInterfaces()) {
                Class<?> resolved = resolveCrudWebModelClass(candidate);
                if (!Object.class.equals(resolved)) {
                    return resolved;
                }
            }
            Type superclass = typeClass.getGenericSuperclass();
            if (superclass != null) {
                return resolveCrudWebModelClass(superclass);
            }
        }
        return Object.class;
    }

    private Class<?> classOf(Type type) {
        if (type instanceof Class<?> typeClass) {
            return typeClass;
        }
        if (type instanceof ParameterizedType parameterizedType
                && parameterizedType.getRawType() instanceof Class<?> rawClass) {
            return rawClass;
        }
        return Object.class;
    }

    @POST
    @Path("/query")
    @ActionEndpoint(PlatformAction.QUERY)
    default WebPageResponse<T> query(WebQueryRequest request) {
        return webScope(() -> {
            WebPageResponse<T> response;
            if (request != null && request.unpagedEnabled()) {
                List<T> records = WebOutputSupport.records(service(), queryListRecords(request), FieldOutputContext.LIST);
                response = WebPageResponse.fromList(records);
            } else {
                response = WebPageResponse.from(WebOutputSupport.page(service(), queryRecords(request), FieldOutputContext.LIST));
            }
            return projectStaticDefaultList(response);
        });
    }

    private WebPageResponse<T> projectStaticDefaultList(WebPageResponse<T> response) {
        StaticRecordReadProjectionService projectionService = staticRecordReadProjectionService();
        if (projectionService == null || !(this instanceof StaticModuleUiContributor contributor)) {
            return response;
        }
        return projectionService.projectDefaultList(contributor.moduleUiDefinition().moduleAlias(), response, service());
    }

    @GET
    @Path("/view/{id}")
    @ActionEndpoint(PlatformAction.VIEW)
    default T view(@PathParam("id") String id) {
        return webScope(() -> WebOutputSupport.record(service(),
                selectForAction(PlatformAction.VIEW, id), FieldOutputContext.VIEW));
    }

    @POST
    @Path("/insert")
    @ActionEndpoint(PlatformAction.CREATE)
    default WebRecordResponse<T> insert(T record) {
        return webScope(() -> {
            String id = service().insert(record);
            T saved = WebOutputSupport.record(service(), service().select(id), FieldOutputContext.VIEW);
            return new WebRecordResponse<>(saved, successMessage(saved, "已保存"));
        });
    }

    @POST
    @Path("/update/{id}")
    @ActionEndpoint(PlatformAction.UPDATE)
    default WebRecordResponse<T> update(@PathParam("id") String id, T record) {
        record.setId(id);
        return webScope(() -> {
            requireDataScopeRecord(PlatformAction.UPDATE, id);
            service().update(record);
            T saved = WebOutputSupport.record(service(), selectForAction(PlatformAction.VIEW, id), FieldOutputContext.VIEW);
            return new WebRecordResponse<>(saved, successMessage(saved, "已保存"));
        });
    }

    @POST
    @Path("/delete/{id}")
    @ActionEndpoint(PlatformAction.DELETE)
    default WebCountResponse delete(@PathParam("id") String id) {
        return webScope(() -> {
            requireDataScopeRecord(PlatformAction.DELETE, id);
            T record = service().select(id);
            return new WebCountResponse(service().delete(id), successMessage(record, "已删除"));
        });
    }

    private T selectForAction(PlatformAction action, String id) {
        if (service() instanceof DataScopeAbility<?>) {
            DataScopeAbility<?> dataScopeAbility = DataScopeAbility.cast(service());
            @SuppressWarnings("unchecked")
            T record = (T) dataScopeAbility.selectForAction(action, id);
            return record;
        }
        return service().select(id);
    }

    private void requireDataScopeRecord(PlatformAction action, String id) {
        if (service() instanceof DataScopeAbility<?>) {
            DataScopeAbility<?> dataScopeAbility = DataScopeAbility.cast(service());
            dataScopeAbility.requireRecordScope(actionPolicy(action), java.util.List.of(id));
        }
    }

    private ActionExecutionPolicy actionPolicy(PlatformAction fallback) {
        return ActionExecutionContextHolder.current()
                .filter(context -> context.moduleAlias().equals(webScopeName()))
                .map(ActionExecutionContext::actionPolicy)
                .orElseGet(fallback::executionPolicy);
    }
}
