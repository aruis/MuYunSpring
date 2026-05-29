package net.ximatai.muyun.spring.dynamic.runtime;

import net.ximatai.muyun.spring.dynamic.metadata.DynamicQueryOperator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record DynamicQueryCondition(
        String fieldName,
        DynamicQueryOperator operator,
        List<?> values
) {
    public DynamicQueryCondition {
        values = values == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(values));
    }

    public static DynamicQueryCondition of(String fieldName, Object value) {
        return new DynamicQueryCondition(fieldName, null, List.of(value));
    }

    public static DynamicQueryCondition of(String fieldName, DynamicQueryOperator operator, Object... values) {
        return new DynamicQueryCondition(fieldName, operator, values == null ? List.of() : List.of(values));
    }
}
