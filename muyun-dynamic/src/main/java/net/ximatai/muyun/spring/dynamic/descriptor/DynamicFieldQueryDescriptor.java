package net.ximatai.muyun.spring.dynamic.descriptor;

import net.ximatai.muyun.spring.dynamic.metadata.DynamicQueryOperator;
import net.ximatai.muyun.spring.dynamic.metadata.FieldQueryDefinition;

import java.util.List;

public record DynamicFieldQueryDescriptor(
        boolean queryable,
        String defaultOperator,
        List<String> operators
) {
    public static DynamicFieldQueryDescriptor from(FieldQueryDefinition definition) {
        return new DynamicFieldQueryDescriptor(
                definition.queryable(),
                definition.defaultOperator() == null ? null : definition.defaultOperator().name(),
                DynamicQueryOperator.ordered(definition.operators()).stream()
                        .map(DynamicQueryOperator::name)
                        .toList()
        );
    }
}
