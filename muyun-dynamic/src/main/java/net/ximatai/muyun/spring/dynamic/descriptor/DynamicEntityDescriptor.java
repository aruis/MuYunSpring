package net.ximatai.muyun.spring.dynamic.descriptor;

import net.ximatai.muyun.spring.dynamic.metadata.EntityCapability;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public record DynamicEntityDescriptor(
        String entityCode,
        String title,
        Set<String> capabilities,
        List<DynamicFieldDescriptor> fields
) {
    public static DynamicEntityDescriptor from(EntityDefinition entity) {
        return new DynamicEntityDescriptor(
                entity.code(),
                entity.name(),
                entity.capabilities().stream()
                        .map(EntityCapability::name)
                        .collect(Collectors.toUnmodifiableSet()),
                entity.fields().stream().map(DynamicFieldDescriptor::from).toList()
        );
    }
}
