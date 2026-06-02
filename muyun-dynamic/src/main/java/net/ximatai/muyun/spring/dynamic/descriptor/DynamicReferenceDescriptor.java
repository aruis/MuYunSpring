package net.ximatai.muyun.spring.dynamic.descriptor;

import net.ximatai.muyun.spring.ability.reference.ReferenceCardinality;
import net.ximatai.muyun.spring.ability.reference.ReferenceTarget;
import net.ximatai.muyun.spring.dynamic.metadata.EntityReferenceDefinition;

import java.util.List;

public record DynamicReferenceDescriptor(
        String sourceEntityAlias,
        String sourceField,
        String targetModuleAlias,
        String targetEntityAlias,
        ReferenceCardinality cardinality,
        boolean autoTitle,
        String titleOutputField,
        List<DynamicReferenceProjectionDescriptor> projections
) {
    public DynamicReferenceDescriptor {
        projections = projections == null ? List.of() : List.copyOf(projections);
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
                reference.projections().stream().map(DynamicReferenceProjectionDescriptor::from).toList()
        );
    }
}
