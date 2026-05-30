package net.ximatai.muyun.spring.dynamic.descriptor;

import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinition;

import java.util.List;

public record DynamicModuleDescriptor(
        String moduleAlias,
        String title,
        List<DynamicEntityDescriptor> entities,
        List<DynamicRelationDescriptor> relations,
        List<DynamicReferenceDescriptor> references
) {
    public DynamicModuleDescriptor {
        entities = entities == null ? List.of() : List.copyOf(entities);
        relations = relations == null ? List.of() : List.copyOf(relations);
        references = references == null ? List.of() : List.copyOf(references);
    }

    public static DynamicModuleDescriptor from(ModuleDefinition module) {
        return new DynamicModuleDescriptor(
                module.moduleAlias(),
                module.name(),
                module.entities().stream().map(entity -> DynamicEntityDescriptor.from(entity, module.views())).toList(),
                module.relations().stream().map(DynamicRelationDescriptor::from).toList(),
                module.references().stream().map(DynamicReferenceDescriptor::from).toList()
        );
    }
}
