package net.ximatai.muyun.spring.ability.query;

import net.ximatai.muyun.database.core.orm.Sort;

import java.util.Collection;
import java.util.Set;

public final class QueryDescriptors {
    private static final Set<String> BOOLEAN_FIELDS = Set.of(
            "enabled",
            "systemManaged",
            "dataScopeEnabled",
            "globalDefault",
            "approvalEnabled",
            "autoTitle",
            "autoPopulate",
            "cascadeDelete",
            "cloneable",
            "unitRequired",
            "moneyCurrencyRequired",
            "visible",
            "readOnly",
            "required",
            "requiredOverride",
            "uniqueField",
            "indexed",
            "sortableField",
            "titleField",
            "actionAuth",
            "dataAuth",
            "allowExternalValue",
            "defaultTemplate",
            "published",
            "defaultSet"
    );
    private static final Set<String> INTEGER_FIELDS = Set.of(
            "sortOrder",
            "versionNo",
            "currentVersionNo",
            "retryCount",
            "decimalScale",
            "defaultLength",
            "defaultPrecision",
            "defaultScale",
            "scale",
            "priority"
    );
    private static final Set<String> LONG_FIELDS = Set.of(
            "currentValue"
    );
    private static final Set<String> DECIMAL_FIELDS = Set.of(
            "rate",
            "factor",
            "factorToBase",
            "offsetToBase"
    );
    private static final Set<String> INSTANT_FIELDS = Set.of(
            "createdAt",
            "updatedAt",
            "deletedAt",
            "publishedAt"
    );
    private static final Set<String> DATE_TIME_FIELDS = Set.of(
            "effectiveFrom",
            "effectiveTo"
    );
    private static final Set<String> DATE_FIELDS = Set.of(
            "effectiveDate"
    );

    private QueryDescriptors() {
    }

    public static QueryDescriptor simple(String scopeName, Collection<String> fields, Sort... defaultSorts) {
        QueryDescriptor.Builder builder = QueryDescriptor.builder(scopeName);
        fields.forEach(field -> builder.field(field(field)));
        if (defaultSorts != null) {
            for (Sort sort : defaultSorts) {
                builder.defaultSort(sort);
            }
        }
        return builder.build();
    }

    public static QueryField field(String fieldName) {
        QueryValueType valueType = valueType(fieldName);
        QueryField field = switch (valueType) {
            case BOOLEAN -> QueryField.of(fieldName, valueType, QueryOperator.EQ);
            case INTEGER, LONG, DECIMAL, DATE, INSTANT, DATETIME -> QueryField.of(fieldName, valueType,
                    QueryOperator.EQ,
                    QueryOperator.IN,
                    QueryOperator.GT,
                    QueryOperator.GTE,
                    QueryOperator.LT,
                    QueryOperator.LTE,
                    QueryOperator.BETWEEN);
            case JSON -> QueryField.of(fieldName, valueType, QueryOperator.EQ);
            case STRING, TEXT -> QueryField.of(fieldName, valueType,
                    QueryOperator.EQ,
                    QueryOperator.LIKE,
                    QueryOperator.IN);
        };
        if (isSortableByConvention(fieldName)) {
            return field.withSortable();
        }
        return field;
    }

    private static QueryValueType valueType(String fieldName) {
        if (BOOLEAN_FIELDS.contains(fieldName)) {
            return QueryValueType.BOOLEAN;
        }
        if (INTEGER_FIELDS.contains(fieldName) || fieldName.endsWith("Count")) {
            return QueryValueType.INTEGER;
        }
        if (LONG_FIELDS.contains(fieldName)) {
            return QueryValueType.LONG;
        }
        if (DECIMAL_FIELDS.contains(fieldName)) {
            return QueryValueType.DECIMAL;
        }
        if (INSTANT_FIELDS.contains(fieldName) || fieldName.endsWith("At")) {
            return QueryValueType.INSTANT;
        }
        if (DATE_TIME_FIELDS.contains(fieldName)) {
            return QueryValueType.DATETIME;
        }
        if (DATE_FIELDS.contains(fieldName)) {
            return QueryValueType.DATE;
        }
        return QueryValueType.STRING;
    }

    private static boolean isSortableByConvention(String fieldName) {
        return "sortOrder".equals(fieldName)
                || "title".equals(fieldName)
                || "code".equals(fieldName)
                || "alias".equals(fieldName)
                || "createdAt".equals(fieldName)
                || "updatedAt".equals(fieldName)
                || "publishedAt".equals(fieldName)
                || "effectiveDate".equals(fieldName)
                || "priority".equals(fieldName)
                || "versionNo".equals(fieldName)
                || "currentVersionNo".equals(fieldName);
    }
}
