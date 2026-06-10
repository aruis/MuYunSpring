package net.ximatai.muyun.spring.dynamic.metadata;

public record EntityReferenceAffectDefinition(
        String referenceField,
        String targetField
) {
}
