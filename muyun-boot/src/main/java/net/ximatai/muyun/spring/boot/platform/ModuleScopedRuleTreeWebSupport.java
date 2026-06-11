package net.ximatai.muyun.spring.boot.platform;

import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.boot.web.SortWebRequest;
import net.ximatai.muyun.spring.boot.web.SystemScope;
import net.ximatai.muyun.spring.boot.web.WebCountResponse;
import net.ximatai.muyun.spring.boot.web.WebOutputSupport;
import net.ximatai.muyun.spring.boot.web.WebPageRequest;
import net.ximatai.muyun.spring.boot.web.WebPageResponse;
import net.ximatai.muyun.spring.boot.web.WebQueryRequest;
import net.ximatai.muyun.spring.boot.web.WebSupport;
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
import java.util.Set;

abstract class ModuleScopedRuleTreeWebSupport<
        T extends EntityContract & EnabledCapable & SortCapable,
        S extends CrudAbility<T> & EnableAbility<T> & SortAbility<T>>
        extends WebSupport<S> implements SystemScope<S> {
    private final Set<String> queryFields;
    private final String scopeField;

    protected ModuleScopedRuleTreeWebSupport(Set<String> queryFields, String scopeField) {
        this.queryFields = Set.copyOf(queryFields);
        this.scopeField = Objects.requireNonNull(scopeField, "scopeField must not be null");
    }

    @PostMapping("/query")
    @ActionEndpoint(PlatformAction.QUERY)
    public WebPageResponse<T> query(HttpServletRequest servletRequest,
                                    @RequestBody(required = false) WebQueryRequest request) {
        return webScope(() -> {
            Criteria criteria = PlatformConfigWebQuerySupport.criteria(request, queryFields, webScopeName());
            criteria.eq(scopeField, moduleAlias(servletRequest));
            WebPageRequest page = request == null ? WebPageRequest.DEFAULT : request.pageOrDefault();
            PageResult<T> result = service().pageQuery(
                    criteria,
                    PageRequest.of(page.pageNum(), page.pageSize()),
                    PlatformConfigWebQuerySupport.sorts(request, queryFields, Sort.asc("sortOrder")));
            return WebPageResponse.from(WebOutputSupport.page(service(), result, FieldOutputContext.LIST));
        });
    }

    @PostMapping("/delete/{id}")
    @ActionEndpoint(PlatformAction.DELETE)
    public WebCountResponse delete(HttpServletRequest servletRequest, @PathVariable String id) {
        return webScope(() -> {
            requireScopedRecord(servletRequest, id);
            return new WebCountResponse(service().delete(id));
        });
    }

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
                                 @RequestBody(required = false) SortWebRequest request) {
        return webScope(() -> {
            SortWebRequest normalized = request == null ? new SortWebRequest(null, null) : request;
            requireScopedRecord(servletRequest, id);
            if (hasText(normalized.previousId())) {
                requireScopedRecord(servletRequest, normalized.previousId());
                service().moveAfter(id, normalized.previousId());
                return new WebCountResponse(1);
            }
            if (hasText(normalized.nextId())) {
                requireScopedRecord(servletRequest, normalized.nextId());
                service().moveBefore(id, normalized.nextId());
                return new WebCountResponse(1);
            }
            throw new IllegalArgumentException("rule sort requires previousId or nextId");
        });
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
