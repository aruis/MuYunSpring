package net.ximatai.muyun.spring.module.metadata;

import net.ximatai.muyun.spring.ability.ReferenceTarget;
import net.ximatai.muyun.spring.ability.ReferenceCardinality;
import net.ximatai.muyun.spring.ability.ReferencePlan;

public record EntityReferenceDefinition(
        String sourceEntity,
        String sourceField,
        String targetQualifiedName,
        boolean autoTitle,
        String titleOutputField
) {
    public EntityReferenceDefinition(String sourceEntity, String sourceField, String targetQualifiedName) {
        this(sourceEntity, sourceField, targetQualifiedName, false, "");
    }

    public static EntityReferenceDefinition to(String sourceEntity, String sourceField, ReferenceTarget target) {
        return new EntityReferenceDefinition(sourceEntity, sourceField, target.qualifiedName());
    }

    public static EntityReferenceDefinition to(String sourceEntity, String sourceField, String targetQualifiedName) {
        return new EntityReferenceDefinition(sourceEntity, sourceField, targetQualifiedName);
    }

    public ReferenceTarget target() {
        return ReferenceTarget.parse(targetQualifiedName);
    }

    public ReferencePlan plan() {
        return new ReferencePlan(sourceField, target(), ReferenceCardinality.ONE, autoTitle, titleOutputField);
    }

    public EntityReferenceDefinition withAutoTitle(String outputField) {
        return new EntityReferenceDefinition(sourceEntity, sourceField, targetQualifiedName, true, outputField);
    }
}
