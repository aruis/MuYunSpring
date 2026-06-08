package net.ximatai.muyun.spring.boot.code;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.boot.web.WebQueryCondition;
import net.ximatai.muyun.spring.boot.web.WebQueryRequest;
import net.ximatai.muyun.spring.boot.web.WebSort;

import java.util.List;
import java.util.Set;

final class CodeWebQuerySupport {
    private CodeWebQuerySupport() {
    }

    static Criteria criteria(WebQueryRequest request, Set<String> allowedFields, String scopeName) {
        Criteria criteria = Criteria.of();
        if (request == null || request.conditions().isEmpty()) {
            return criteria;
        }
        for (WebQueryCondition condition : request.conditions()) {
            append(criteria, condition, allowedFields, scopeName);
        }
        return criteria;
    }

    static Sort[] sorts(WebQueryRequest request, Set<String> allowedFields, Sort... defaults) {
        if (request == null || request.sorts().isEmpty()) {
            return defaults == null ? new Sort[0] : defaults;
        }
        return request.sorts().stream()
                .map(sort -> sort(sort, allowedFields))
                .toArray(Sort[]::new);
    }

    private static void append(Criteria criteria,
                               WebQueryCondition condition,
                               Set<String> allowedFields,
                               String scopeName) {
        String field = requireAllowedField(condition.fieldName(), allowedFields, scopeName);
        String operator = condition.operator() == null || condition.operator().isBlank()
                ? "EQ"
                : condition.operator().trim().toUpperCase();
        List<Object> values = condition.values();
        switch (operator) {
            case "EQ" -> criteria.eq(field, singleValue(condition, values));
            case "LIKE" -> criteria.like(field, String.valueOf(singleValue(condition, values)));
            case "IN" -> criteria.in(field, listValues(condition, values));
            case "GT" -> criteria.gt(field, singleValue(condition, values));
            case "GTE" -> criteria.gte(field, singleValue(condition, values));
            case "LT" -> criteria.lt(field, singleValue(condition, values));
            case "LTE" -> criteria.lte(field, singleValue(condition, values));
            case "BETWEEN" -> criteria.between(field, rangeValue(condition, values, 0), rangeValue(condition, values, 1));
            default -> throw new IllegalArgumentException("query operator is not supported by "
                    + scopeName + ": " + operator);
        }
    }

    private static Sort sort(WebSort sort, Set<String> allowedFields) {
        String field = requireAllowedField(sort.field(), allowedFields, "code web sort");
        return sort.desc() ? Sort.desc(field) : Sort.asc(field);
    }

    private static String requireAllowedField(String field, Set<String> allowedFields, String scopeName) {
        if (field == null || field.isBlank() || !allowedFields.contains(field)) {
            throw new IllegalArgumentException("query field is not supported by " + scopeName + ": " + field);
        }
        return field;
    }

    private static Object singleValue(WebQueryCondition condition, List<Object> values) {
        if (values.size() != 1 || values.getFirst() == null) {
            throw new IllegalArgumentException("query operator requires exactly one non-null value: "
                    + condition.fieldName() + "." + condition.operator());
        }
        return values.getFirst();
    }

    private static List<?> listValues(WebQueryCondition condition, List<Object> values) {
        if (values.isEmpty() || values.stream().anyMatch(value -> value == null)) {
            throw new IllegalArgumentException("query operator requires non-empty non-null values: "
                    + condition.fieldName() + "." + condition.operator());
        }
        return values;
    }

    private static Object rangeValue(WebQueryCondition condition, List<Object> values, int index) {
        if (values.size() != 2 || values.stream().anyMatch(value -> value == null)) {
            throw new IllegalArgumentException("query operator requires exactly two non-null values: "
                    + condition.fieldName() + "." + condition.operator());
        }
        return values.get(index);
    }
}
