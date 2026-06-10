package net.ximatai.muyun.spring.dynamic.descriptor;

import net.ximatai.muyun.spring.ability.reference.ReferenceCardinality;
import net.ximatai.muyun.spring.ability.reference.ReferenceTarget;
import net.ximatai.muyun.spring.dynamic.metadata.EntityReferenceDefinition;

import java.util.List;
import java.util.Set;

public record DynamicReferenceDescriptor(
        String sourceEntityAlias,
        String sourceField,
        String targetModuleAlias,
        String targetEntityAlias,
        ReferenceCardinality cardinality,
        boolean autoTitle,
        String titleOutputField,
        List<DynamicReferenceProjectionDescriptor> projections,
        String keyField,
        String labelField,
        String generateRuleId,
        String queryTemplateId,
        Set<String> plusFields,
        List<DynamicReferenceFilterDescriptor> filters,
        List<DynamicReferenceAffectDescriptor> affects
) {
    public DynamicReferenceDescriptor(String sourceEntityAlias,
                                      String sourceField,
                                      String targetModuleAlias,
                                      String targetEntityAlias,
                                      ReferenceCardinality cardinality,
                                      boolean autoTitle,
                                      String titleOutputField,
                                      List<DynamicReferenceProjectionDescriptor> projections) {
        this(sourceEntityAlias, sourceField, targetModuleAlias, targetEntityAlias, cardinality, autoTitle,
                titleOutputField, projections, null, null, null, null, Set.of(), List.of(), List.of());
    }

    public DynamicReferenceDescriptor {
        projections = projections == null ? List.of() : List.copyOf(projections);
        plusFields = plusFields == null ? Set.of() : Set.copyOf(plusFields);
        filters = filters == null ? List.of() : List.copyOf(filters);
        affects = affects == null ? List.of() : List.copyOf(affects);
    }

    public static DynamicReferenceDescriptor from(EntityReferenceDefinition reference) {
        ReferenceTarget target = reference.target();
        return new DynamicReferenceDescriptor(
                reference.sourceEntityAlias(),
                reference.sourceField(),
                target.moduleAlias(),
                target.entityAlias(),
                reference.cardinality(),
                reference.autoTitle(),
                reference.titleOutputField(),
                reference.projections().stream().map(DynamicReferenceProjectionDescriptor::from).toList(),
                reference.keyField(),
                reference.labelField(),
                reference.generateRuleId(),
                reference.queryTemplateId(),
                reference.plusFields(),
                reference.filters().stream().map(DynamicReferenceFilterDescriptor::from).toList(),
                reference.affects().stream().map(DynamicReferenceAffectDescriptor::from).toList()
        );
    }
}
