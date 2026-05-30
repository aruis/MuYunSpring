package net.ximatai.muyun.spring.dynamic.descriptor;

import net.ximatai.muyun.spring.dynamic.metadata.EntityCapability;
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
        List<DynamicViewDescriptor> views
) {
    public DynamicEntityDescriptor {
        fields = fields == null ? List.of() : List.copyOf(fields);
        actions = actions == null ? List.of() : List.copyOf(actions);
        views = views == null ? List.of() : List.copyOf(views);
    }

    public static DynamicEntityDescriptor from(EntityDefinition entity) {
        return from(entity, List.of());
    }

    public static DynamicEntityDescriptor from(EntityDefinition entity, List<EntityViewDefinition> views) {
        return new DynamicEntityDescriptor(
                entity.code(),
                entity.name(),
                entity.capabilities().stream()
                        .map(EntityCapability::name)
                        .collect(Collectors.toUnmodifiableSet()),
                entity.fields().stream().map(DynamicFieldDescriptor::from).toList(),
                DynamicStandardActions.from(entity),
                DynamicViewDescriptors.from(entity, views)
        );
    }
}
