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
import net.ximatai.muyun.spring.common.platform.DataScopeCriteriaResult;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.schema.PlatformAbilityFields;
import net.ximatai.muyun.spring.common.security.FieldOutputContext;
import org.springframework.http.HttpStatus;
import org.springframework.core.ResolvableType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

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

    @GetMapping("/query/schema")
    @ActionEndpoint(PlatformAction.QUERY)
    default QuerySchema querySchema(@RequestParam(required = false) String uiConfigId) {
        return webScope(() -> {
            StaticRecordReadProjectionService projectionService = staticRecordReadProjectionService();
            if (projectionService != null && this instanceof StaticModuleUiContributor contributor
                    && isCurrentModuleUiDefinition(contributor)
                    && projectionService.hasModuleDefinition(contributor.moduleUiDefinition().moduleAlias())) {
                return projectionService.querySchema(contributor.moduleUiDefinition().moduleAlias(), service());
            }
            if (service() instanceof QueryAbility<?> queryAbility) {
                return queryAbility.querySchema();
            }
            throw new IllegalArgumentException("query schema is not supported by " + webScopeName());
        });
    }

    @GetMapping("/form/schema")
    @ActionEndpoint(PlatformAction.VIEW)
    default FormSchema formSchema(@RequestParam(required = false) String uiConfigId) {
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
        return ResolvableType.forClass(CrudWeb.class, getClass()).resolveGeneric(0);
    }

    @PostMapping("/query")
    @ActionEndpoint(PlatformAction.QUERY)
    default WebPageResponse<T> query(@RequestBody(required = false) WebQueryRequest request) {
        return webScope(() -> {
            if (request == null || !request.unpagedEnabled()) {
                java.util.Optional<WebPageResponse<T>> projected = queryStaticProjectedDefaultList(request);
                if (projected.isPresent()) {
                    return projected.get();
                }
            }
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

    @SuppressWarnings("unchecked")
    private java.util.Optional<WebPageResponse<T>> queryStaticProjectedDefaultList(WebQueryRequest request) {
        StaticRecordReadProjectionService projectionService = staticRecordReadProjectionService();
        if (projectionService == null || !(this instanceof StaticModuleUiContributor contributor)) {
            return java.util.Optional.empty();
        }
        String moduleAlias = contributor.moduleUiDefinition().moduleAlias();
        if (!projectionService.supportsDefaultListQuery(moduleAlias, service())) {
            return java.util.Optional.empty();
        }
        WebPageRequest page = request == null ? WebPageRequest.DEFAULT : request.pageOrDefault();
        PageRequest pageRequest = PageRequest.of(page.pageNum(), page.pageSize());
        Criteria criteria = projectionService.queryCriteria(moduleAlias, service(), WebQueryRequests.from(request));
        Sort[] sorts = projectionService.querySorts(moduleAlias, service(), WebQueryRequests.from(request));
        if (service() instanceof DataScopeAbility<?>) {
            DataScopeAbility<T> dataScopeAbility = DataScopeAbility.cast(service());
            DataScopeCriteriaResult scope = dataScopeAbility.readScopeByPolicy(actionPolicy(PlatformAction.QUERY), criteria);
            Criteria activeCriteria = service().activeCriteria(scope.criteria());
            return dataScopeAbility.withDataScopeTenant(scope,
                    () -> projectionService.queryDefaultList(
                            moduleAlias,
                            activeCriteria,
                            pageRequest,
                            service(),
                            sorts
                    ));
        }
        Criteria activeCriteria = service().activeCriteria(criteria);
        return projectionService.queryDefaultList(
                moduleAlias,
                activeCriteria,
                pageRequest,
                service(),
                sorts
        );
    }

    private WebPageResponse<T> projectStaticDefaultList(WebPageResponse<T> response) {
        StaticRecordReadProjectionService projectionService = staticRecordReadProjectionService();
        if (projectionService == null || !(this instanceof StaticModuleUiContributor contributor)) {
            return response;
        }
        return projectionService.projectDefaultList(contributor.moduleUiDefinition().moduleAlias(), response, service());
    }

    @GetMapping("/view/{id}")
    @ActionEndpoint(PlatformAction.VIEW)
    default T view(@PathVariable String id) {
        return webScope(() -> WebOutputSupport.record(service(),
                selectForAction(PlatformAction.VIEW, id), FieldOutputContext.VIEW));
    }

    @PostMapping("/insert")
    @ActionEndpoint(PlatformAction.CREATE)
    @StandardMutation(StandardMutationKind.CREATE)
    @ResponseStatus(HttpStatus.CREATED)
    default WebRecordResponse<T> insert(@RequestBody T record) {
        return MutationTenantScopeExecutor.forCreate(this, record, () -> webScope(() -> {
            String id = service().insert(record);
            T saved = WebOutputSupport.record(service(), service().select(id), FieldOutputContext.VIEW);
            return new WebRecordResponse<>(saved, successMessage(saved, "已保存"));
        }));
    }

    @PostMapping("/update/{id}")
    @ActionEndpoint(PlatformAction.UPDATE)
    @StandardMutation(StandardMutationKind.UPDATE)
    default WebRecordResponse<T> update(@PathVariable String id, @RequestBody T record) {
        record.setId(id);
        return MutationTenantScopeExecutor.forUpdate(this, id, record, () -> webScope(() -> {
            requireDataScopeRecord(PlatformAction.UPDATE, id);
            service().update(record);
            T saved = WebOutputSupport.record(service(), selectForAction(PlatformAction.VIEW, id), FieldOutputContext.VIEW);
            return new WebRecordResponse<>(saved, successMessage(saved, "已保存"));
        }));
    }

    @PostMapping("/delete/{id}")
    @ActionEndpoint(PlatformAction.DELETE)
    @StandardMutation(StandardMutationKind.DELETE)
    default WebCountResponse delete(@PathVariable String id) {
        return MutationTenantScopeExecutor.forExistingRecord(this, id, () -> webScope(() -> {
            requireDataScopeRecord(PlatformAction.DELETE, id);
            T record = service().select(id);
            int count = service().delete(id);
            return new WebCountResponse(count, successMessage(record, "已删除"));
        }));
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
