package net.ximatai.muyun.spring.dynamic.metadata;

import net.ximatai.muyun.spring.ability.reference.ReferenceCardinality;

public record EntityAssociationViewDefinition(
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
    public EntityAssociationViewDefinition {
        displayMode = displayMode == null ? AssociationViewDisplayMode.INLINE_LIST : displayMode;
        viewType = viewType == null ? EntityViewType.LIST : viewType;
    }

    public static EntityAssociationViewDefinition childRelation(String code,
                                                                String sourceEntity,
                                                                String targetModuleAlias,
                                                                String targetEntity,
                                                                String relationCode) {
        return new EntityAssociationViewDefinition(code, sourceEntity, targetModuleAlias, targetEntity,
                AssociationViewDisplayMode.INLINE_LIST, relationCode, null, EntityViewType.LIST, true);
    }

    public static EntityAssociationViewDefinition reference(String code,
                                                            String sourceEntity,
                                                            String targetModuleAlias,
                                                            String targetEntity,
                                                            String referenceField) {
        return reference(code, sourceEntity, targetModuleAlias, targetEntity, referenceField, ReferenceCardinality.ONE);
    }

    public static EntityAssociationViewDefinition reference(String code,
                                                            String sourceEntity,
                                                            String targetModuleAlias,
                                                            String targetEntity,
                                                            String referenceField,
                                                            ReferenceCardinality cardinality) {
        boolean many = cardinality == ReferenceCardinality.MANY;
        return new EntityAssociationViewDefinition(code, sourceEntity, targetModuleAlias, targetEntity,
                many ? AssociationViewDisplayMode.LINKED_LIST : AssociationViewDisplayMode.LINKED_RECORD,
                null,
                referenceField,
                many ? EntityViewType.LIST : EntityViewType.FORM,
                many);
    }
}
