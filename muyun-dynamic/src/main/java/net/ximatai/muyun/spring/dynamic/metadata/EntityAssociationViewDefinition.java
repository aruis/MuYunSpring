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
        boolean queryable,
        java.util.List<AssociationViewPathStep> path,
        AssociationViewRootQueryMapping rootQueryMapping,
        String targetUiConfigId,
        String targetQueryTemplateId
) {
    public EntityAssociationViewDefinition {
        displayMode = displayMode == null ? AssociationViewDisplayMode.INLINE_LIST : displayMode;
        viewType = viewType == null ? EntityViewType.LIST : viewType;
        path = path == null ? defaultPath(sourceEntityAlias, targetModuleAlias, targetEntityAlias,
                relationCode, referenceField) : java.util.List.copyOf(path);
        targetUiConfigId = normalize(targetUiConfigId);
        targetQueryTemplateId = normalize(targetQueryTemplateId);
    }

    public EntityAssociationViewDefinition(String code,
                                           String sourceEntityAlias,
                                           String targetModuleAlias,
                                           String targetEntityAlias,
                                           AssociationViewDisplayMode displayMode,
                                           String relationCode,
                                           String referenceField,
                                           EntityViewType viewType,
                                           boolean queryable) {
        this(code, sourceEntityAlias, targetModuleAlias, targetEntityAlias, displayMode, relationCode, referenceField,
                viewType, queryable, null, null, null, null);
    }

    public static EntityAssociationViewDefinition childRelation(String code,
                                                                String sourceEntityAlias,
                                                                String targetModuleAlias,
                                                                String targetEntityAlias,
                                                                String relationCode) {
        return new EntityAssociationViewDefinition(code, sourceEntityAlias, targetModuleAlias, targetEntityAlias,
                AssociationViewDisplayMode.INLINE_LIST, relationCode, null, EntityViewType.LIST, true,
                null, null, null, null);
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
                true,
                null,
                null,
                null,
                null);
    }

    private static java.util.List<AssociationViewPathStep> defaultPath(String sourceEntityAlias,
                                                                       String targetModuleAlias,
                                                                       String targetEntityAlias,
                                                                       String relationCode,
                                                                       String referenceField) {
        if (relationCode != null && !relationCode.isBlank()) {
            return java.util.List.of(AssociationViewPathStep.relation(relationCode, sourceEntityAlias,
                    targetModuleAlias, targetEntityAlias));
        }
        if (referenceField != null && !referenceField.isBlank()) {
            return java.util.List.of(AssociationViewPathStep.reference(referenceField, sourceEntityAlias,
                    targetModuleAlias, targetEntityAlias));
        }
        return java.util.List.of();
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
