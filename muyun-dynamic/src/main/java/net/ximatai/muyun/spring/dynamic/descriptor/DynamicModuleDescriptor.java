package net.ximatai.muyun.spring.dynamic.descriptor;

import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;

import java.util.List;

public record DynamicModuleDescriptor(
        String moduleAlias,
        String title,
        List<DynamicActionDescriptor> actions,
        List<DynamicEntityDescriptor> entities,
        List<DynamicRelationDescriptor> relations,
        List<DynamicReferenceDescriptor> references
) {
    public DynamicModuleDescriptor {
        actions = actions == null ? List.of() : List.copyOf(actions);
        entities = entities == null ? List.of() : List.copyOf(entities);
        relations = relations == null ? List.of() : List.copyOf(relations);
        references = references == null ? List.of() : List.copyOf(references);
    }

    public static DynamicModuleDescriptor from(ModuleDefinition module) {
        List<EntityDefinition> moduleEntities = module.entities();
        return new DynamicModuleDescriptor(
                module.moduleAlias(),
                module.name(),
                mainEntityActions(module, moduleEntities),
                moduleEntities.stream()
                        .map(entity -> DynamicEntityDescriptor.from(module.moduleAlias(), entity, module.views(), module.actions()))
                        .toList(),
                module.relations().stream().map(DynamicRelationDescriptor::from).toList(),
                module.references().stream().map(DynamicReferenceDescriptor::from).toList()
        );
    }

    private static List<DynamicActionDescriptor> mainEntityActions(ModuleDefinition module,
                                                                   List<EntityDefinition> entities) {
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }
        return DynamicEntityDescriptor.from(module.moduleAlias(), entities.getFirst(), module.views(), module.actions()).actions();
    }
}
