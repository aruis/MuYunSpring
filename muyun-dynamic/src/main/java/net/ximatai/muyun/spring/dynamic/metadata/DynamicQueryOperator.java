package net.ximatai.muyun.spring.dynamic.metadata;

import java.util.EnumSet;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public enum DynamicQueryOperator {
    EQ,
    LIKE,
    IN,
    BETWEEN,
    GT,
    GTE,
    LT,
    LTE;

    public static Set<DynamicQueryOperator> defaultOperators(FieldType fieldType) {
        return switch (fieldType) {
            case STRING, TEXT -> EnumSet.of(EQ, LIKE, IN);
            case BOOLEAN -> EnumSet.of(EQ);
            case INTEGER, LONG, DECIMAL, TIMESTAMP, ZONED_TIMESTAMP, DATE -> EnumSet.of(EQ, BETWEEN, GT, GTE, LT, LTE, IN);
            case JSON -> EnumSet.of(EQ);
        };
    }

    public static DynamicQueryOperator defaultOperator(FieldType fieldType) {
        return switch (fieldType) {
            case STRING, TEXT -> LIKE;
            default -> EQ;
        };
    }

    public boolean supports(FieldType fieldType) {
        return defaultOperators(fieldType).contains(this);
    }

    public static List<DynamicQueryOperator> ordered(Set<DynamicQueryOperator> operators) {
        if (operators == null || operators.isEmpty()) {
            return List.of();
        }
        return List.of(values()).stream()
                .filter(operators::contains)
                .toList();
    }

    public static String format(Set<DynamicQueryOperator> operators) {
        return String.join(",", ordered(operators).stream().map(DynamicQueryOperator::name).toList());
    }

    public static Set<String> names(Set<DynamicQueryOperator> operators) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        ordered(operators).forEach(operator -> names.add(operator.name()));
        return Collections.unmodifiableSet(names);
    }

    public static Set<DynamicQueryOperator> parseNames(Set<String> operators) {
        if (operators == null || operators.isEmpty()) {
            return Set.of();
        }
        LinkedHashSet<DynamicQueryOperator> parsed = new LinkedHashSet<>();
        for (String value : operators) {
            if (value != null && !value.isBlank()) {
                parsed.add(DynamicQueryOperator.valueOf(value.trim()));
            }
        }
        return Collections.unmodifiableSet(parsed);
    }
}
