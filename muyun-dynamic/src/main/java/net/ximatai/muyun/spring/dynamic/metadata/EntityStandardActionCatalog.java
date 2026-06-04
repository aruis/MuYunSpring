package net.ximatai.muyun.spring.dynamic.metadata;

import java.util.List;

public final class EntityStandardActionCatalog {
    private EntityStandardActionCatalog() {
    }

    public static List<EntityActionDefinition> from(EntityDefinition entity) {
        return PlatformActionResolver.standardActions(entity);
    }

    public static boolean supportsStandardAction(EntityDefinition entity, String actionCode) {
        return from(entity).stream()
                .filter(action -> action.actionCode().equals(actionCode))
                .findFirst()
                .isPresent();
    }

}
