package net.ximatai.muyun.spring.dynamic.descriptor;

import net.ximatai.muyun.spring.ability.reference.ReferenceProjection;

public record DynamicReferenceProjectionDescriptor(
        String targetField,
        String outputField
) {
    public static DynamicReferenceProjectionDescriptor from(ReferenceProjection projection) {
        return new DynamicReferenceProjectionDescriptor(projection.targetField(), projection.outputField());
    }
}
