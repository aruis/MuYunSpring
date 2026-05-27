package net.ximatai.muyun.spring.module.metadata;

import net.ximatai.muyun.spring.ability.ReferenceTarget;
import net.ximatai.muyun.spring.ability.ReferenceCardinality;
import net.ximatai.muyun.spring.ability.ReferencePlan;
import net.ximatai.muyun.spring.ability.ReferenceProjection;

import java.util.LinkedHashSet;
import java.util.List;

public record EntityReferenceDefinition(
        String sourceEntity,
        String sourceField,
        String targetQualifiedName,
        ReferenceCardinality cardinality,
        boolean autoTitle,
        String titleOutputField,
        List<ReferenceProjection> projections
) {
    public EntityReferenceDefinition(String sourceEntity, String sourceField, String targetQualifiedName) {
        this(sourceEntity, sourceField, targetQualifiedName, ReferenceCardinality.ONE, false, "");
    }

    public EntityReferenceDefinition(String sourceEntity,
                                     String sourceField,
                                     String targetQualifiedName,
                                     ReferenceCardinality cardinality,
                                     boolean autoTitle,
                                     String titleOutputField) {
        this(sourceEntity, sourceField, targetQualifiedName, cardinality, autoTitle, titleOutputField, List.of());
    }

    public EntityReferenceDefinition {
        if (cardinality == null) {
            cardinality = ReferenceCardinality.ONE;
        }
        if (titleOutputField == null) {
            titleOutputField = "";
        }
        projections = projections == null ? List.of() : List.copyOf(projections);
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
        return new ReferencePlan(sourceField, target(), cardinality, autoTitle, titleOutputField, projections);
    }

    public EntityReferenceDefinition many() {
        return new EntityReferenceDefinition(sourceEntity, sourceField, targetQualifiedName,
                ReferenceCardinality.MANY, autoTitle, titleOutputField, projections);
    }

    public EntityReferenceDefinition withAutoTitle(String outputField) {
        return new EntityReferenceDefinition(sourceEntity, sourceField, targetQualifiedName,
                cardinality, true, outputField, projections);
    }

    public EntityReferenceDefinition withProjection(String targetField, String outputField) {
        LinkedHashSet<ReferenceProjection> next = new LinkedHashSet<>(projections);
        next.add(new ReferenceProjection(targetField, outputField));
        return new EntityReferenceDefinition(this.sourceEntity, this.sourceField, targetQualifiedName,
                cardinality, autoTitle, titleOutputField, List.copyOf(next));
    }
}
