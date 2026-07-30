package net.ximatai.muyun.spring.dynamic.metadata;

import net.ximatai.muyun.spring.ability.reference.ReferenceTarget;
import net.ximatai.muyun.spring.ability.reference.ReferenceCardinality;
import net.ximatai.muyun.spring.ability.reference.ReferencePlan;
import net.ximatai.muyun.spring.ability.reference.ReferenceProjection;
import net.ximatai.muyun.spring.ability.reference.ReferenceIntegrityPolicy;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public record EntityReferenceDefinition(
        String sourceEntityAlias,
        String sourceField,
        String targetQualifiedName,
        ReferenceCardinality cardinality,
        List<ReferenceProjection> projections,
        String keyField,
        String labelField,
        String generateRuleId,
        String queryTemplateId,
        Set<String> plusFields,
        List<EntityReferenceFilterDefinition> filters,
        List<EntityReferenceAffectDefinition> affects,
        ReferenceIntegrityPolicy integrity
) {
    public EntityReferenceDefinition(String sourceEntityAlias, String sourceField, String targetQualifiedName) {
        this(sourceEntityAlias, sourceField, targetQualifiedName, ReferenceCardinality.ONE, List.of(),
                null, null, null, null, Set.of(), List.of(), List.of(), ReferenceIntegrityPolicy.DEFAULT);
    }

    public EntityReferenceDefinition(String sourceEntityAlias, String sourceField, String targetQualifiedName,
                                     ReferenceCardinality cardinality, List<ReferenceProjection> projections) {
        this(sourceEntityAlias, sourceField, targetQualifiedName, cardinality, projections,
                null, null, null, null, Set.of(), List.of(), List.of(), ReferenceIntegrityPolicy.DEFAULT);
    }


    public EntityReferenceDefinition {
        if (cardinality == null) {
            cardinality = ReferenceCardinality.ONE;
        }
        projections = projections == null ? List.of() : List.copyOf(projections);
        plusFields = plusFields == null ? Set.of() : Set.copyOf(plusFields);
        filters = filters == null ? List.of() : List.copyOf(filters);
        affects = affects == null ? List.of() : List.copyOf(affects);
        integrity = integrity == null ? ReferenceIntegrityPolicy.DEFAULT : integrity;
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
        return new ReferencePlan(sourceField, target(), cardinality, projections, integrity);
    }

    public EntityReferenceDefinition many() {
        return new EntityReferenceDefinition(sourceEntityAlias, sourceField, targetQualifiedName,
                ReferenceCardinality.MANY, projections,
                keyField, labelField, generateRuleId, queryTemplateId, plusFields, filters, affects, integrity);
    }

    public EntityReferenceDefinition withProjection(String targetField, String outputField) {
        LinkedHashSet<ReferenceProjection> next = new LinkedHashSet<>(projections);
        next.add(new ReferenceProjection(targetField, outputField));
        return new EntityReferenceDefinition(this.sourceEntityAlias, this.sourceField, targetQualifiedName,
                cardinality, List.copyOf(next),
                keyField, labelField, generateRuleId, queryTemplateId, plusFields, filters, affects, integrity);
    }


    public EntityReferenceDefinition withRuntimeConfig(String keyField,
                                                       String labelField,
                                                       String generateRuleId,
                                                       String queryTemplateId,
                                                       Set<String> plusFields) {
        return new EntityReferenceDefinition(sourceEntityAlias, sourceField, targetQualifiedName,
                cardinality, projections,
                keyField, labelField, generateRuleId, queryTemplateId, plusFields, filters, affects, integrity);
    }

    public EntityReferenceDefinition withInteractionRules(List<EntityReferenceFilterDefinition> filters,
                                                          List<EntityReferenceAffectDefinition> affects) {
        return new EntityReferenceDefinition(sourceEntityAlias, sourceField, targetQualifiedName,
                cardinality, projections,
                keyField, labelField, generateRuleId, queryTemplateId, plusFields, filters, affects, integrity);
    }

    public EntityReferenceDefinition withIntegrity(ReferenceIntegrityPolicy integrity) {
        return new EntityReferenceDefinition(sourceEntityAlias, sourceField, targetQualifiedName,
                cardinality, projections,
                keyField, labelField, generateRuleId, queryTemplateId, plusFields, filters, affects, integrity);
    }
}
