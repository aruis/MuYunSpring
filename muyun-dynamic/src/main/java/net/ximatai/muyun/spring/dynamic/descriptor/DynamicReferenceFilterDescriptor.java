package net.ximatai.muyun.spring.dynamic.descriptor;

import net.ximatai.muyun.spring.dynamic.metadata.DynamicQueryOperator;
import net.ximatai.muyun.spring.dynamic.metadata.EntityReferenceFilterDefinition;

public record DynamicReferenceFilterDescriptor(
        String formField,
        String referenceField,
        DynamicQueryOperator operator
) {
    public static DynamicReferenceFilterDescriptor from(EntityReferenceFilterDefinition filter) {
        return new DynamicReferenceFilterDescriptor(filter.formField(), filter.referenceField(), filter.operator());
    }
}
