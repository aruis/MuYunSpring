package net.ximatai.muyun.spring.dynamic.descriptor;

import net.ximatai.muyun.spring.dynamic.metadata.EntityActionDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityStandardActionCatalog;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class DynamicStandardActions {
    private DynamicStandardActions() {
    }

    static List<DynamicActionDescriptor> from(String moduleAlias,
                                              EntityDefinition entity,
                                              List<EntityActionDefinition> configuredActions) {
        Map<String, DynamicActionDescriptor> actions = new LinkedHashMap<>();
        for (EntityActionDefinition standard : EntityStandardActionCatalog.from(entity)) {
            put(actions, action(moduleAlias, entity.alias(), standard, null));
        }
        for (EntityActionDefinition configured : configuredActions) {
            if (entity.alias().equals(configured.entityAlias())) {
                actions.put(configured.actionCode(), action(moduleAlias, entity.alias(), configured,
                        actions.get(configured.actionCode())));
            }
        }
        return List.copyOf(actions.values());
    }

    private static DynamicActionDescriptor action(String moduleAlias,
                                                  String entityAlias,
                                                  EntityActionDefinition configured,
                                                  DynamicActionDescriptor standard) {
        DynamicActionKind kind = standard == null ? DynamicActionKind.from(configured.kind()) : standard.kind();
        EntityActionDefinition execution = standard == null ? configured : null;
        return new DynamicActionDescriptor(
                configured.actionCode(),
                kind,
                configured.title(),
                configured.enabled(),
                configured.style(),
                configured.level(),
                configured.category(),
                configured.accessMode(),
                configured.actionAuth(),
                configured.dataAuth(),
                configured.authInheritActionCode(),
                configured.hasAvailabilityCondition(),
                configured.unavailableMessage(),
                execution == null ? standard.executorType() : execution.executorType(),
                execution == null ? standard.executorKey() : execution.executorKey()
        );
    }

    private static void put(Map<String, DynamicActionDescriptor> actions, DynamicActionDescriptor action) {
        actions.put(action.code(), action);
    }

}
