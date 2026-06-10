package net.ximatai.muyun.spring.dynamic.metadata;

public record EntityReferenceFilterDefinition(
        String formField,
        String referenceField,
        DynamicQueryOperator operator
) {
    public EntityReferenceFilterDefinition {
        operator = operator == null ? DynamicQueryOperator.EQ : operator;
    }
}
