package net.ximatai.muyun.spring.boot.web;

import java.util.List;

public record WebQueryCriteria(WebQueryGroupOperator operator,
                               List<WebQueryCondition> conditions,
                               List<WebQueryCriteria> groups) {
    public WebQueryCriteria {
        operator = operator == null ? WebQueryGroupOperator.AND : operator;
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
        groups = groups == null ? List.of() : List.copyOf(groups);
    }

    public boolean isEmpty() {
        return conditions.isEmpty() && groups.isEmpty();
    }
}
