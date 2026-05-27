package net.ximatai.muyun.spring.module.metadata;

import net.ximatai.muyun.spring.ability.ReferenceTarget;

public record EntityReferenceDefinition(
        String sourceEntity,
        String sourceField,
        String targetQualifiedName
) {
    public static EntityReferenceDefinition to(String sourceEntity, String sourceField, ReferenceTarget target) {
        return new EntityReferenceDefinition(sourceEntity, sourceField, target.qualifiedName());
    }

    public static EntityReferenceDefinition to(String sourceEntity, String sourceField, String targetQualifiedName) {
        return new EntityReferenceDefinition(sourceEntity, sourceField, targetQualifiedName);
    }

    public ReferenceTarget target() {
        return ReferenceTarget.parse(targetQualifiedName);
    }
}
