package net.ximatai.muyun.spring.dynamic.metadata;

import java.util.Set;

public record FieldQueryDefinition(
        boolean queryable,
        DynamicQueryOperator defaultOperator,
        Set<DynamicQueryOperator> operators
) {
    public FieldQueryDefinition {
        operators = operators == null ? Set.of() : Set.copyOf(operators);
        if (!queryable) {
            defaultOperator = null;
            operators = Set.of();
        } else if (defaultOperator == null) {
            throw new IllegalArgumentException("defaultOperator must not be null for queryable field");
        } else if (!operators.contains(defaultOperator)) {
            throw new IllegalArgumentException("operators must contain defaultOperator: " + defaultOperator);
        }
    }

    public static FieldQueryDefinition disabled() {
        return new FieldQueryDefinition(false, null, Set.of());
    }

    public static FieldQueryDefinition enabled(FieldType fieldType) {
        return enabled(DynamicQueryOperator.defaultOperator(fieldType), DynamicQueryOperator.defaultOperators(fieldType));
    }

    public static FieldQueryDefinition enabled(FieldType fieldType,
                                               DynamicQueryOperator defaultOperator,
                                               Set<DynamicQueryOperator> operators) {
        FieldQueryDefinition definition = enabled(defaultOperator, operators);
        for (DynamicQueryOperator operator : definition.operators()) {
            if (!operator.supports(fieldType)) {
                throw new IllegalArgumentException("query operator is not supported by fieldType: " + fieldType + "." + operator);
            }
        }
        return definition;
    }

    public static FieldQueryDefinition enabled(DynamicQueryOperator defaultOperator, Set<DynamicQueryOperator> operators) {
        return new FieldQueryDefinition(true, defaultOperator, operators);
    }
}
