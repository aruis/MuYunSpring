package net.ximatai.muyun.spring.boot.web;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.security.FieldOutputContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface ReadOnlyWeb<T extends EntityContract, S extends CrudAbility<T>> extends ScopedWeb<S> {
    default Criteria queryCriteria(WebQueryRequest request) {
        if (request != null && !request.conditions().isEmpty()) {
            throw new IllegalArgumentException("query conditions are not supported by " + webScopeName());
        }
        return Criteria.of();
    }

    default Sort[] querySorts(WebQueryRequest request) {
        if (request != null && !request.sorts().isEmpty()) {
            throw new IllegalArgumentException("query sorts are not supported by " + webScopeName());
        }
        return new Sort[0];
    }

    @PostMapping("/query")
    @ActionEndpoint(PlatformAction.QUERY)
    default WebPageResponse<T> query(@RequestBody(required = false) WebQueryRequest request) {
        return webScope(() -> {
            WebPageRequest page = request == null ? WebPageRequest.DEFAULT : request.pageOrDefault();
            PageResult<T> result = service().pageQuery(
                    queryCriteria(request),
                    PageRequest.of(page.pageNum(), page.pageSize()),
                    querySorts(request));
            return WebPageResponse.from(WebOutputSupport.page(service(), result, FieldOutputContext.LIST));
        });
    }

    @GetMapping("/view/{id}")
    @ActionEndpoint(PlatformAction.VIEW)
    default T view(@PathVariable String id) {
        return webScope(() -> WebOutputSupport.record(
                service(), service().select(id), FieldOutputContext.VIEW));
    }
}
