package net.ximatai.muyun.spring.boot.dynamic;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.boot.web.WebPageRequest;
import net.ximatai.muyun.spring.boot.web.WebQueryCondition;
import net.ximatai.muyun.spring.boot.web.WebQueryCriteria;
import net.ximatai.muyun.spring.boot.web.WebQueryGroupOperator;
import net.ximatai.muyun.spring.boot.web.WebSort;
import net.ximatai.muyun.spring.dynamic.metadata.DynamicQueryOperator;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicQueryCondition;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

final class DynamicWebQueryMapper {
    private DynamicWebQueryMapper() {
    }

    static List<DynamicQueryCondition> queryConditions(Collection<WebQueryCondition> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return List.of();
        }
        return conditions.stream()
                .map(DynamicWebQueryMapper::queryConditionFromWeb)
                .toList();
    }

    static PageRequest page(WebPageRequest request) {
        WebPageRequest normalized = request == null ? WebPageRequest.DEFAULT : request;
        return PageRequest.of(normalized.pageNum(), normalized.pageSize());
    }

    static Sort[] sorts(List<WebSort> sorts) {
        if (sorts == null || sorts.isEmpty()) {
            return new Sort[0];
        }
        return sorts.stream()
                .map(sort -> sort.desc() ? Sort.desc(sort.field()) : Sort.asc(sort.field()))
                .toArray(Sort[]::new);
    }

    static Criteria queryCriteria(WebQueryCriteria criteria,
                                  Function<List<DynamicQueryCondition>, Criteria> conditionCompiler) {
        if (criteria == null || criteria.isEmpty()) {
            return Criteria.of();
        }
        Criteria compiled = Criteria.of();
        for (WebQueryCondition condition : criteria.conditions()) {
            Criteria child = conditionCompiler.apply(queryConditions(List.of(condition)));
            appendGroup(compiled, criteria.operator(), child);
        }
        for (WebQueryCriteria group : criteria.groups()) {
            Criteria child = queryCriteria(group, conditionCompiler);
            appendGroup(compiled, criteria.operator(), child);
        }
        return compiled;
    }

    private static void appendGroup(Criteria target, WebQueryGroupOperator operator, Criteria child) {
        if (child == null || child.isEmpty()) {
            return;
        }
        if (target.isEmpty() || operator != WebQueryGroupOperator.OR) {
            target.andGroup(child.getRoot());
            return;
        }
        target.orGroup(child.getRoot());
    }

    private static DynamicQueryCondition queryConditionFromWeb(WebQueryCondition condition) {
        DynamicQueryOperator operator = condition.operator() == null || condition.operator().isBlank()
                ? null
                : DynamicQueryOperator.valueOf(condition.operator());
        return new DynamicQueryCondition(condition.fieldName(), operator, condition.values());
    }
}
