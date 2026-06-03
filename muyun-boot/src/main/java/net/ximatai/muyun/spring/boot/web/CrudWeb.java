package net.ximatai.muyun.spring.boot.web;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.common.schema.PlatformAbilityFields;
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
        return service().pageQuery(queryCriteria(request), pageRequest, querySorts(request));
    }

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
        if (service() instanceof SortAbility<?>) {
            return new Sort[]{Sort.asc(PlatformAbilityFields.SORT_FIELD)};
        }
        return new Sort[0];
    }

    @PostMapping("/query")
    default WebPageResponse<T> query(@RequestBody(required = false) WebQueryRequest request) {
        return webScope(() -> WebPageResponse.from(queryRecords(request)));
    }

    @GetMapping("/view/{id}")
    default T view(@PathVariable String id) {
        return webScope(() -> service().select(id));
    }

    @PostMapping("/insert")
    @ResponseStatus(HttpStatus.CREATED)
    default T insert(@RequestBody T record) {
        return webScope(() -> {
            String id = service().insert(record);
            return service().select(id);
        });
    }

    @PostMapping("/update/{id}")
    default T update(@PathVariable String id, @RequestBody T record) {
        record.setId(id);
        return webScope(() -> {
            service().update(record);
            return service().select(id);
        });
    }

    @PostMapping("/delete/{id}")
    default WebCountResponse delete(@PathVariable String id) {
        return webScope(() -> new WebCountResponse(service().delete(id)));
    }
}
