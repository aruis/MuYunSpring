package net.ximatai.muyun.spring.dynamic.descriptor;

import net.ximatai.muyun.spring.dynamic.metadata.EntityCapability;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityAssociationViewDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityViewDefinition;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public record DynamicEntityDescriptor(
        String entityCode,
        String title,
        Set<String> capabilities,
        List<DynamicFieldDescriptor> fields,
        List<DynamicActionDescriptor> actions,
        List<DynamicViewDescriptor> views,
        List<DynamicAssociationViewDescriptor> associationViews
) {
    public DynamicEntityDescriptor {
        fields = fields == null ? List.of() : List.copyOf(fields);
        actions = actions == null ? List.of() : List.copyOf(actions);
        views = views == null ? List.of() : List.copyOf(views);
        associationViews = associationViews == null ? List.of() : List.copyOf(associationViews);
    }

    public static DynamicEntityDescriptor from(EntityDefinition entity) {
        return from(entity, List.of(), List.of());
    }

    public static DynamicEntityDescriptor from(EntityDefinition entity, List<EntityViewDefinition> views) {
        return from(entity, views, List.of());
    }

    public static DynamicEntityDescriptor from(EntityDefinition entity,
                                               List<EntityViewDefinition> views,
                                               List<EntityActionDefinition> actions) {
        return from(null, entity, views, List.of(), actions);
    }

    public static DynamicEntityDescriptor from(String moduleAlias,
                                               EntityDefinition entity,
                                               List<EntityViewDefinition> views,
                                               List<EntityAssociationViewDefinition> associationViews,
                                               List<EntityActionDefinition> actions) {
        return new DynamicEntityDescriptor(
                entity.code(),
                entity.name(),
                entity.capabilities().stream()
                        .map(EntityCapability::name)
                        .collect(Collectors.toUnmodifiableSet()),
                entity.fields().stream().map(DynamicFieldDescriptor::from).toList(),
                DynamicStandardActions.from(moduleAlias, entity, actions),
                DynamicViewDescriptors.from(entity, views),
                scopedAssociationViews(entity, associationViews)
        );
    }

    private static List<DynamicAssociationViewDescriptor> scopedAssociationViews(EntityDefinition entity,
                                                                                List<EntityAssociationViewDefinition> views) {
        return views == null ? List.of() : views.stream()
                .filter(view -> entity.code().equals(view.sourceEntity()))
                .map(DynamicAssociationViewDescriptor::from)
                .toList();
    }
}
