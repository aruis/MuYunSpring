package net.ximatai.muyun.spring.boot.web;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.ability.DataScopeAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContext;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContextHolder;
import net.ximatai.muyun.spring.common.platform.ActionExecutionPolicy;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.schema.PlatformAbilityFields;
import net.ximatai.muyun.spring.common.security.FieldOutputContext;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

public interface CrudWeb<T extends EntityContract, S extends CrudAbility<T>> extends ScopedWeb<S> {
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

    default Criteria queryCriteria(WebQueryRequest request) {
        if (request != null && !request.conditions().isEmpty()) {
            throw new IllegalArgumentException("query conditions are not supported by " + webScopeName());
        }
        if (request != null && request.criteria() != null && !request.criteria().isEmpty()) {
            throw new IllegalArgumentException("query criteria are not supported by " + webScopeName());
        }
        return Criteria.of();
    }

    default Sort[] querySorts(WebQueryRequest request) {
        if (request != null && !request.sorts().isEmpty()) {
            throw new IllegalArgumentException("query sorts are not supported by " + webScopeName());
        }
        if (service() instanceof SortAbility<?>) {
            return new Sort[]{Sort.asc(PlatformAbilityFields.SORT_FIELD)};
        }
        return new Sort[0];
    }

    @PostMapping("/query")
    @ActionEndpoint(PlatformAction.QUERY)
    default WebPageResponse<T> query(@RequestBody(required = false) WebQueryRequest request) {
        return webScope(() -> WebPageResponse.from(WebOutputSupport.page(service(), queryRecords(request), FieldOutputContext.LIST)));
    }

    @GetMapping("/view/{id}")
    @ActionEndpoint(PlatformAction.VIEW)
    default T view(@PathVariable String id) {
        return webScope(() -> WebOutputSupport.record(service(),
                selectForAction(PlatformAction.VIEW, id), FieldOutputContext.VIEW));
    }

    @PostMapping("/insert")
    @ActionEndpoint(PlatformAction.CREATE)
    @ResponseStatus(HttpStatus.CREATED)
    default T insert(@RequestBody T record) {
        return webScope(() -> {
            String id = service().insert(record);
            return WebOutputSupport.record(service(), service().select(id), FieldOutputContext.VIEW);
        });
    }

    @PostMapping("/update/{id}")
    @ActionEndpoint(PlatformAction.UPDATE)
    default T update(@PathVariable String id, @RequestBody T record) {
        record.setId(id);
        return webScope(() -> {
            requireDataScopeRecord(PlatformAction.UPDATE, id);
            service().update(record);
            return WebOutputSupport.record(service(), selectForAction(PlatformAction.VIEW, id), FieldOutputContext.VIEW);
        });
    }

    @PostMapping("/delete/{id}")
    @ActionEndpoint(PlatformAction.DELETE)
    default WebCountResponse delete(@PathVariable String id) {
        return webScope(() -> {
            requireDataScopeRecord(PlatformAction.DELETE, id);
            return new WebCountResponse(service().delete(id));
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
