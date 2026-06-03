package net.ximatai.muyun.spring.boot.dynamic;

import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.boot.web.WebQueryCondition;
import net.ximatai.muyun.spring.boot.web.WebSort;
import net.ximatai.muyun.spring.dynamic.metadata.DynamicQueryOperator;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicQueryCondition;

import java.util.Collection;
import java.util.List;

final class DynamicWebQueryMapper {
    private DynamicWebQueryMapper() {
    }

    static List<DynamicQueryCondition> queryConditions(Collection<DynamicWebQueryCondition> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return List.of();
        }
        return conditions.stream()
                .map(condition -> new DynamicQueryCondition(
                        condition.fieldName(),
                        condition.operator(),
                        condition.values()
                ))
                .toList();
    }

    static List<DynamicQueryCondition> queryConditionsFromWeb(Collection<WebQueryCondition> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return List.of();
        }
        return conditions.stream()
                .map(DynamicWebQueryMapper::queryConditionFromWeb)
                .toList();
    }

    static PageRequest page(DynamicWebPageRequest request) {
        DynamicWebPageRequest normalized = request == null ? DynamicWebPageRequest.DEFAULT : request;
        return PageRequest.of(normalized.pageNum(), normalized.pageSize());
    }

    static Sort[] sorts(List<DynamicWebSort> sorts) {
        if (sorts == null || sorts.isEmpty()) {
            return new Sort[0];
        }
        return sorts.stream()
                .map(sort -> sort.desc() ? Sort.desc(sort.field()) : Sort.asc(sort.field()))
                .toArray(Sort[]::new);
    }

    static Sort[] sortsFromWeb(List<WebSort> sorts) {
        if (sorts == null || sorts.isEmpty()) {
            return new Sort[0];
        }
        return sorts.stream()
                .map(sort -> sort.desc() ? Sort.desc(sort.field()) : Sort.asc(sort.field()))
                .toArray(Sort[]::new);
    }

    private static DynamicQueryCondition queryConditionFromWeb(WebQueryCondition condition) {
        DynamicQueryOperator operator = condition.operator() == null || condition.operator().isBlank()
                ? null
                : DynamicQueryOperator.valueOf(condition.operator());
        return new DynamicQueryCondition(condition.fieldName(), operator, condition.values());
    }
}
