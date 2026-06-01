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
            put(actions, action(moduleAlias, entity.code(), standard, null));
        }
        for (EntityActionDefinition configured : configuredActions) {
            if (entity.code().equals(configured.entityCode())) {
                actions.put(configured.actionCode(), action(moduleAlias, entity.code(), configured,
                        actions.get(configured.actionCode())));
            }
        }
        return List.copyOf(actions.values());
    }

    private static DynamicActionDescriptor action(String moduleAlias,
                                                  String entityCode,
                                                  EntityActionDefinition configured,
                                                  DynamicActionDescriptor standard) {
        DynamicActionKind kind = standard == null ? DynamicActionKind.from(configured.kind()) : standard.kind();
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
                configured.authInheritActionAlias(),
                configured.hasAvailabilityCondition(),
                configured.unavailableMessage(),
                configured.executorType(),
                configured.executorKey()
        );
    }

    private static void put(Map<String, DynamicActionDescriptor> actions, DynamicActionDescriptor action) {
        actions.put(action.code(), action);
    }

}
