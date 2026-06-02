package net.ximatai.muyun.spring.dynamic.runtime;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.dynamic.metadata.DynamicQueryOperator;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.DynamicFieldValueSupport;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinitionException;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class DynamicQueryCriteriaBuilder {
    private final EntityDefinition entity;
    private final Map<String, FieldDefinition> fields;

    public DynamicQueryCriteriaBuilder(EntityDefinition entity) {
        this.entity = entity;
        this.fields = entity.fields().stream()
                .collect(Collectors.toUnmodifiableMap(FieldDefinition::fieldName, Function.identity()));
    }

    public Criteria build(Collection<DynamicQueryCondition> conditions) {
        Criteria criteria = Criteria.of();
        if (conditions == null || conditions.isEmpty()) {
            return criteria;
        }
        for (DynamicQueryCondition condition : conditions) {
            append(criteria, condition);
        }
        return criteria;
    }

    private void append(Criteria criteria, DynamicQueryCondition condition) {
        FieldDefinition field = field(condition.fieldName());
        if (!field.queryDefinition().queryable()) {
            throw new ModuleDefinitionException("field is not queryable: " + entity.alias() + "." + condition.fieldName());
        }
        DynamicQueryOperator operator = condition.operator() == null
                ? field.queryDefinition().defaultOperator()
                : condition.operator();
        if (!field.queryDefinition().operators().contains(operator)) {
            throw new ModuleDefinitionException("query operator is not allowed: " + condition.fieldName() + "." + operator);
        }
        List<?> values = condition.values();
        switch (operator) {
            case EQ -> criteria.eq(field.fieldName(), singleValue(field, condition, values));
            case LIKE -> criteria.like(field.fieldName(), String.valueOf(singleValue(field, condition, values)));
            case IN -> criteria.in(field.fieldName(), listValues(field, condition, values));
            case BETWEEN -> criteria.between(field.fieldName(), rangeValue(field, condition, values, 0), rangeValue(field, condition, values, 1));
            case GT -> criteria.gt(field.fieldName(), singleValue(field, condition, values));
            case GTE -> criteria.gte(field.fieldName(), singleValue(field, condition, values));
            case LT -> criteria.lt(field.fieldName(), singleValue(field, condition, values));
            case LTE -> criteria.lte(field.fieldName(), singleValue(field, condition, values));
        }
    }

    private FieldDefinition field(String fieldName) {
        FieldDefinition field = fields.get(fieldName);
        if (field == null) {
            throw new ModuleDefinitionException("unknown query field: " + entity.alias() + "." + fieldName);
        }
        return field;
    }

    private Object singleValue(FieldDefinition field, DynamicQueryCondition condition, List<?> values) {
        if (values.size() != 1) {
            throw new ModuleDefinitionException("query operator requires exactly one value: "
                    + condition.fieldName() + "." + condition.operator());
        }
        return normalizedValue(field, condition, values.getFirst());
    }

    private List<?> listValues(FieldDefinition field, DynamicQueryCondition condition, List<?> values) {
        if (values.isEmpty()) {
            throw new ModuleDefinitionException("query operator requires at least one value: "
                    + condition.fieldName() + "." + condition.operator());
        }
        return values.stream()
                .map(value -> normalizedValue(field, condition, value))
                .toList();
    }

    private Object rangeValue(FieldDefinition field, DynamicQueryCondition condition, List<?> values, int index) {
        if (values.size() != 2) {
            throw new ModuleDefinitionException("query operator requires exactly two values: "
                    + condition.fieldName() + "." + condition.operator());
        }
        return normalizedValue(field, condition, values.get(index));
    }

    private Object normalizedValue(FieldDefinition field, DynamicQueryCondition condition, Object value) {
        if (value == null) {
            throw new ModuleDefinitionException("null query value is not supported: "
                    + condition.fieldName() + "." + condition.operator());
        }
        try {
            return DynamicFieldValueSupport.normalize(field.type(), value);
        } catch (RuntimeException e) {
            throw new ModuleDefinitionException("invalid query value type: "
                    + condition.fieldName() + "." + condition.operator(), e);
        }
    }
}
