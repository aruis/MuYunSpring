package net.ximatai.muyun.spring.ability.query;

import java.util.EnumSet;
import java.util.Set;

public record QueryField(String fieldName,
                         QueryValueType valueType,
                         Set<QueryOperator> operators,
                         QueryOperator defaultOperator,
                         boolean sortable,
                         boolean quickSearch) {
    public QueryField {
        if (fieldName == null || fieldName.isBlank()) {
            throw new IllegalArgumentException("query field name must not be blank");
        }
        valueType = valueType == null ? QueryValueType.STRING : valueType;
        operators = operators == null || operators.isEmpty()
                ? EnumSet.of(QueryOperator.EQ)
                : EnumSet.copyOf(operators);
        defaultOperator = defaultOperator == null ? fallbackDefaultOperator(valueType, operators) : defaultOperator;
        if (!operators.contains(defaultOperator)) {
            throw new IllegalArgumentException("default query operator must be allowed: "
                    + fieldName + "." + defaultOperator);
        }
    }

    public static QueryField of(String fieldName, QueryOperator first, QueryOperator... rest) {
        return of(fieldName, QueryValueType.STRING, first, rest);
    }

    public static QueryField of(String fieldName, QueryValueType valueType, QueryOperator first,
                                QueryOperator... rest) {
        EnumSet<QueryOperator> operators = EnumSet.of(first, rest);
        return new QueryField(fieldName, valueType, operators, null, false, false);
    }

    public QueryField withDefaultOperator(QueryOperator operator) {
        return new QueryField(fieldName, valueType, operators, operator, sortable, quickSearch);
    }

    public QueryField withSortable() {
        return new QueryField(fieldName, valueType, operators, defaultOperator, true, quickSearch);
    }

    public QueryField withQuickSearch() {
        return new QueryField(fieldName, valueType, operators, defaultOperator, sortable, true);
    }

    private static QueryOperator fallbackDefaultOperator(QueryValueType valueType, Set<QueryOperator> operators) {
        QueryOperator typeDefault = valueType.defaultOperator();
        return operators.contains(typeDefault) ? typeDefault : operators.iterator().next();
    }
}
