package net.ximatai.muyun.spring.platform.web;

import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.ability.query.QueryAbility;
import net.ximatai.muyun.spring.web.RecordActionWebRequest;
import net.ximatai.muyun.spring.web.RecordWebProjectionPolicy;
import net.ximatai.muyun.spring.web.StandardMutation;
import net.ximatai.muyun.spring.web.StandardMutationKind;
import net.ximatai.muyun.spring.web.StandardMutationResultSupport;
import net.ximatai.muyun.spring.web.SystemScope;
import net.ximatai.muyun.spring.web.WebOutputSupport;
import net.ximatai.muyun.spring.web.WebPageRequest;
import net.ximatai.muyun.spring.web.WebPageResponse;
import net.ximatai.muyun.spring.web.WebQueryRequest;
import net.ximatai.muyun.spring.web.WebSupport;
import net.ximatai.muyun.spring.web.query.WebQueryRequests;
import net.ximatai.muyun.spring.common.model.capability.EnabledCapable;
import net.ximatai.muyun.spring.common.model.capability.SortCapable;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.security.FieldOutputContext;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;
import java.util.Objects;

abstract class ModuleScopedRuleTreeWebSupport<
        T extends EntityContract & EnabledCapable & SortCapable,
        S extends CrudAbility<T> & EnableAbility<T> & SortAbility<T>>
        extends WebSupport<S> implements SystemScope<S>, RecordWebProjectionPolicy {
    private final String scopeField;

    protected ModuleScopedRuleTreeWebSupport(String scopeField) {
        this.scopeField = Objects.requireNonNull(scopeField, "scopeField must not be null");
    }

    @PostMapping("/query")
    @ActionEndpoint(PlatformAction.QUERY)
    public WebPageResponse<T> query(HttpServletRequest servletRequest,
                                    @RequestBody(required = false) WebQueryRequest request) {
        return webScope(() -> {
            Criteria criteria = queryCriteria(request);
            criteria.eq(scopeField, moduleAlias(servletRequest));
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

    @PostMapping("/delete/{id}")
    @ActionEndpoint(PlatformAction.DELETE)
    @StandardMutation(StandardMutationKind.DELETE)
    public int delete(HttpServletRequest servletRequest, @PathVariable String id,
                      @RequestBody RecordActionWebRequest request) {
        return webScope(() -> {
            requireScopedRecord(servletRequest, id);
            return StandardMutationResultSupport.deleted(this, id, () -> service().delete(id, request.version()));
        });
    }

    @Override
    public void requireRecord(HttpServletRequest request, PlatformAction action, String id) {
        requireScopedRecord(request, id);
    }

    protected void requireExistingRuleInScope(HttpServletRequest request, T rule) {
        if (rule == null || !hasText(rule.getId())) {
            return;
        }
        requireScopedRecord(request, rule.getId());
    }

    protected T requireScopedRecord(HttpServletRequest request, String id) {
        T record = service().select(id);
        String moduleAlias = moduleAlias(request);
        if (record == null || !moduleAlias.equals(scopeValue(record))) {
            throw new IllegalArgumentException("rule does not belong to module: " + moduleAlias + "." + id);
        }
        return record;
    }

    protected String moduleAlias(HttpServletRequest request) {
        return PlatformNameRules.requireModuleAlias(pathVariable(request, "moduleAlias"));
    }

    protected abstract String scopeValue(T record);

    private String pathVariable(HttpServletRequest request, String key) {
        Object value = pathVariables(request).get(key);
        return value == null ? null : value.toString();
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> pathVariables(HttpServletRequest request) {
        Object attribute = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (!(attribute instanceof Map<?, ?> variables)) {
            return Map.of();
        }
        return variables.entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        entry -> entry.getKey().toString(),
                        entry -> entry.getValue().toString()
                ));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
