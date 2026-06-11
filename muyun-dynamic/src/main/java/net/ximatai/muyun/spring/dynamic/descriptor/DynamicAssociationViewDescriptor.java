package net.ximatai.muyun.spring.dynamic.descriptor;

import net.ximatai.muyun.spring.dynamic.metadata.AssociationViewDisplayMode;
import net.ximatai.muyun.spring.dynamic.metadata.EntityAssociationViewDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityViewType;

public record DynamicAssociationViewDescriptor(
        String code,
        String sourceEntityAlias,
        String targetModuleAlias,
        String targetEntityAlias,
        AssociationViewDisplayMode displayMode,
        String relationCode,
        String referenceField,
        EntityViewType viewType,
        boolean queryable,
        java.util.List<net.ximatai.muyun.spring.dynamic.metadata.AssociationViewPathStep> path,
        net.ximatai.muyun.spring.dynamic.metadata.AssociationViewRootQueryMapping rootQueryMapping,
        String targetUiConfigId,
        String targetQueryTemplateId
) {
    public DynamicAssociationViewDescriptor {
        path = path == null ? java.util.List.of() : java.util.List.copyOf(path);
    }

    public DynamicAssociationViewDescriptor(String code,
                                            String sourceEntityAlias,
                                            String targetModuleAlias,
                                            String targetEntityAlias,
                                            AssociationViewDisplayMode displayMode,
                                            String relationCode,
                                            String referenceField,
                                            EntityViewType viewType,
                                            boolean queryable) {
        this(code, sourceEntityAlias, targetModuleAlias, targetEntityAlias, displayMode, relationCode, referenceField,
                viewType, queryable, java.util.List.of(), null, null, null);
    }

    public static DynamicAssociationViewDescriptor from(EntityAssociationViewDefinition view) {
        return new DynamicAssociationViewDescriptor(
                view.code(),
                view.sourceEntityAlias(),
                view.targetModuleAlias(),
                view.targetEntityAlias(),
                view.displayMode(),
                view.relationCode(),
                view.referenceField(),
                view.viewType(),
                view.queryable(),
                view.path(),
                view.rootQueryMapping(),
                view.targetUiConfigId(),
                view.targetQueryTemplateId()
        );
    }
}
