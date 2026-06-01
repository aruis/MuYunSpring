package net.ximatai.muyun.spring.dynamic.descriptor;

import net.ximatai.muyun.spring.dynamic.metadata.AssociationViewDisplayMode;
import net.ximatai.muyun.spring.dynamic.metadata.EntityAssociationViewDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityViewType;

public record DynamicAssociationViewDescriptor(
        String code,
        String sourceEntity,
        String targetModuleAlias,
        String targetEntity,
        AssociationViewDisplayMode displayMode,
        String relationCode,
        String referenceField,
        EntityViewType viewType,
        boolean queryable
) {
    public static DynamicAssociationViewDescriptor from(EntityAssociationViewDefinition view) {
        return new DynamicAssociationViewDescriptor(
                view.code(),
                view.sourceEntity(),
                view.targetModuleAlias(),
                view.targetEntity(),
                view.displayMode(),
                view.relationCode(),
                view.referenceField(),
                view.viewType(),
                view.queryable()
        );
    }
}
