package net.ximatai.muyun.spring.ability.query;

import java.util.List;

public record QueryCriteria(QueryGroupOperator operator,
                            List<QueryCondition> conditions,
                            List<QueryCriteria> groups) {
    public QueryCriteria {
        operator = operator == null ? QueryGroupOperator.AND : operator;
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
        groups = groups == null ? List.of() : List.copyOf(groups);
    }

    public boolean isEmpty() {
        return conditions.isEmpty() && groups.isEmpty();
    }
}
