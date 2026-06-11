package net.ximatai.muyun.spring.dynamic.metadata;

import net.ximatai.muyun.spring.ability.reference.ReferenceCardinality;

public record EntityAssociationViewDefinition(
        String code,
        String sourceEntityAlias,
        String targetModuleAlias,
        String targetEntityAlias,
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
                                                                String sourceEntityAlias,
                                                                String targetModuleAlias,
                                                                String targetEntityAlias,
                                                                String relationCode) {
        return new EntityAssociationViewDefinition(code, sourceEntityAlias, targetModuleAlias, targetEntityAlias,
                AssociationViewDisplayMode.INLINE_LIST, relationCode, null, EntityViewType.LIST, true);
    }

    public static EntityAssociationViewDefinition reference(String code,
                                                            String sourceEntityAlias,
                                                            String targetModuleAlias,
                                                            String targetEntityAlias,
                                                            String referenceField) {
        return reference(code, sourceEntityAlias, targetModuleAlias, targetEntityAlias, referenceField, ReferenceCardinality.ONE);
    }

    public static EntityAssociationViewDefinition reference(String code,
                                                            String sourceEntityAlias,
                                                            String targetModuleAlias,
                                                            String targetEntityAlias,
                                                            String referenceField,
                                                            ReferenceCardinality cardinality) {
        boolean many = cardinality == ReferenceCardinality.MANY;
        return new EntityAssociationViewDefinition(code, sourceEntityAlias, targetModuleAlias, targetEntityAlias,
                many ? AssociationViewDisplayMode.LINKED_LIST : AssociationViewDisplayMode.LINKED_RECORD,
                null,
                referenceField,
                many ? EntityViewType.LIST : EntityViewType.FORM,
                true);
    }
}
