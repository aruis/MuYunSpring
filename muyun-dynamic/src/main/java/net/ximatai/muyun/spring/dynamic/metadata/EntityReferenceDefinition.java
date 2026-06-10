package net.ximatai.muyun.spring.dynamic.metadata;

import net.ximatai.muyun.spring.ability.reference.ReferenceTarget;
import net.ximatai.muyun.spring.ability.reference.ReferenceCardinality;
import net.ximatai.muyun.spring.ability.reference.ReferencePlan;
import net.ximatai.muyun.spring.ability.reference.ReferenceProjection;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public record EntityReferenceDefinition(
        String sourceEntityAlias,
        String sourceField,
        String targetQualifiedName,
        ReferenceCardinality cardinality,
        boolean autoTitle,
        String titleOutputField,
        List<ReferenceProjection> projections,
        String keyField,
        String labelField,
        String generateRuleId,
        String queryTemplateId,
        Set<String> plusFields,
        List<EntityReferenceFilterDefinition> filters,
        List<EntityReferenceAffectDefinition> affects
) {
    public EntityReferenceDefinition(String sourceEntityAlias, String sourceField, String targetQualifiedName) {
        this(sourceEntityAlias, sourceField, targetQualifiedName, ReferenceCardinality.ONE, false, "");
    }

    public EntityReferenceDefinition(String sourceEntityAlias,
                                     String sourceField,
                                     String targetQualifiedName,
                                     ReferenceCardinality cardinality,
                                     boolean autoTitle,
                                     String titleOutputField) {
        this(sourceEntityAlias, sourceField, targetQualifiedName, cardinality, autoTitle, titleOutputField, List.of());
    }

    public EntityReferenceDefinition(String sourceEntityAlias,
                                     String sourceField,
                                     String targetQualifiedName,
                                     ReferenceCardinality cardinality,
                                     boolean autoTitle,
                                     String titleOutputField,
                                     List<ReferenceProjection> projections) {
        this(sourceEntityAlias, sourceField, targetQualifiedName, cardinality, autoTitle, titleOutputField, projections,
                null, null, null, null, Set.of(), List.of(), List.of());
    }

    public EntityReferenceDefinition {
        if (cardinality == null) {
            cardinality = ReferenceCardinality.ONE;
        }
        if (titleOutputField == null) {
            titleOutputField = "";
        }
        projections = projections == null ? List.of() : List.copyOf(projections);
        plusFields = plusFields == null ? Set.of() : Set.copyOf(plusFields);
        filters = filters == null ? List.of() : List.copyOf(filters);
        affects = affects == null ? List.of() : List.copyOf(affects);
    }

    public static EntityReferenceDefinition to(String sourceEntityAlias, String sourceField, ReferenceTarget target) {
        return new EntityReferenceDefinition(sourceEntityAlias, sourceField, target.qualifiedName());
    }

    public static EntityReferenceDefinition to(String sourceEntityAlias, String sourceField, String targetQualifiedName) {
        return new EntityReferenceDefinition(sourceEntityAlias, sourceField, targetQualifiedName);
    }

    public ReferenceTarget target() {
        return ReferenceTarget.parse(targetQualifiedName);
    }

    public ReferencePlan plan() {
        return new ReferencePlan(sourceField, target(), cardinality, autoTitle, titleOutputField, projections);
    }

    public EntityReferenceDefinition many() {
        return new EntityReferenceDefinition(sourceEntityAlias, sourceField, targetQualifiedName,
                ReferenceCardinality.MANY, autoTitle, titleOutputField, projections,
                keyField, labelField, generateRuleId, queryTemplateId, plusFields, filters, affects);
    }

    public EntityReferenceDefinition withAutoTitle(String outputField) {
        return new EntityReferenceDefinition(sourceEntityAlias, sourceField, targetQualifiedName,
                cardinality, true, outputField, projections,
                keyField, labelField, generateRuleId, queryTemplateId, plusFields, filters, affects);
    }

    public EntityReferenceDefinition withProjection(String targetField, String outputField) {
        LinkedHashSet<ReferenceProjection> next = new LinkedHashSet<>(projections);
        next.add(new ReferenceProjection(targetField, outputField));
        return new EntityReferenceDefinition(this.sourceEntityAlias, this.sourceField, targetQualifiedName,
                cardinality, autoTitle, titleOutputField, List.copyOf(next),
                keyField, labelField, generateRuleId, queryTemplateId, plusFields, filters, affects);
    }

    public EntityReferenceDefinition withRuntimeConfig(String keyField,
                                                       String labelField,
                                                       String generateRuleId,
                                                       String queryTemplateId,
                                                       Set<String> plusFields) {
        return new EntityReferenceDefinition(sourceEntityAlias, sourceField, targetQualifiedName,
                cardinality, autoTitle, titleOutputField, projections,
                keyField, labelField, generateRuleId, queryTemplateId, plusFields, filters, affects);
    }

    public EntityReferenceDefinition withInteractionRules(List<EntityReferenceFilterDefinition> filters,
                                                          List<EntityReferenceAffectDefinition> affects) {
        return new EntityReferenceDefinition(sourceEntityAlias, sourceField, targetQualifiedName,
                cardinality, autoTitle, titleOutputField, projections,
                keyField, labelField, generateRuleId, queryTemplateId, plusFields, filters, affects);
    }
}
