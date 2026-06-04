package net.ximatai.muyun.spring.dynamic.descriptor;

import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinitionException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record DynamicModuleDescriptor(
        String moduleAlias,
        String title,
        String mainEntityAlias,
        List<DynamicActionDescriptor> actions,
        List<DynamicEntityDescriptor> entities,
        List<DynamicRelationDescriptor> relations,
        List<DynamicReferenceDescriptor> references,
        List<DynamicAssociationViewDescriptor> associationViews
) {
    public DynamicModuleDescriptor {
        actions = actions == null ? List.of() : List.copyOf(actions);
        entities = entities == null ? List.of() : List.copyOf(entities);
        relations = relations == null ? List.of() : List.copyOf(relations);
        references = references == null ? List.of() : List.copyOf(references);
        associationViews = associationViews == null ? List.of() : List.copyOf(associationViews);
    }

    public static DynamicModuleDescriptor from(ModuleDefinition module) {
        List<EntityDefinition> moduleEntities = module.entities();
        return new DynamicModuleDescriptor(
                module.moduleAlias(),
                module.name(),
                module.mainEntityAlias(),
                moduleActions(module, moduleEntities),
                moduleEntities.stream()
                        .map(entity -> DynamicEntityDescriptor.from(module.moduleAlias(), entity, module.references(),
                                module.views(), module.associationViews(), module.actions()))
                        .toList(),
                module.relations().stream().map(DynamicRelationDescriptor::from).toList(),
                module.references().stream().map(DynamicReferenceDescriptor::from).toList(),
                module.associationViews().stream().map(DynamicAssociationViewDescriptor::from).toList()
        );
    }

    private static List<DynamicActionDescriptor> moduleActions(ModuleDefinition module,
                                                               List<EntityDefinition> entities) {
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }
        Map<String, EntityDefinition> entityByAlias = entities.stream()
                .collect(LinkedHashMap::new, (map, entity) -> map.put(entity.alias(), entity), LinkedHashMap::putAll);
        EntityDefinition mainEntity = requireEntity(module, entityByAlias, module.mainEntityAlias());
        List<DynamicActionDescriptor> actions = new ArrayList<>(DynamicEntityDescriptor.from(module.moduleAlias(),
                mainEntity, module.views(), module.associationViews(), module.actions()).actions());
        Set<String> actionCodes = actions.stream()
                .map(DynamicActionDescriptor::code)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
        for (EntityActionDefinition action : module.actions()) {
            if (action.entityAlias().equals(module.mainEntityAlias())) {
                continue;
            }
            EntityDefinition entity = requireEntity(module, entityByAlias, action.entityAlias());
            DynamicEntityDescriptor entityDescriptor = DynamicEntityDescriptor.from(module.moduleAlias(),
                    entity, module.views(), module.associationViews(), module.actions());
            entityDescriptor.actions().stream()
                    .filter(descriptor -> descriptor.code().equals(action.actionCode()))
                    .findFirst()
                    .ifPresent(descriptor -> {
                        if (!actionCodes.add(descriptor.code())) {
                            throw new ModuleDefinitionException("module action code duplicated: "
                                    + module.moduleAlias() + "." + descriptor.code());
                        }
                        actions.add(descriptor);
                    });
        }
        return List.copyOf(actions);
    }

    private static EntityDefinition requireEntity(ModuleDefinition module,
                                                  Map<String, EntityDefinition> entityByAlias,
                                                  String entityAlias) {
        EntityDefinition entity = entityByAlias.get(entityAlias);
        if (entity == null) {
            throw new ModuleDefinitionException("module entity not found: "
                    + module.moduleAlias() + "." + entityAlias);
        }
        return entity;
    }
}
