package net.ximatai.muyun.spring.dynamic.descriptor;

import net.ximatai.muyun.spring.dynamic.metadata.EntityReferenceAffectDefinition;

public record DynamicReferenceAffectDescriptor(
        String referenceField,
        String targetField
) {
    public static DynamicReferenceAffectDescriptor from(EntityReferenceAffectDefinition affect) {
        return new DynamicReferenceAffectDescriptor(affect.referenceField(), affect.targetField());
    }
}
